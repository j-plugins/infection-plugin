package com.github.xepozz.infection.tests

import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor

/**
 * Rewrites Infection's `testFailed` service messages **before** they reach the standard
 * proxy state machine, so we get exactly one [com.intellij.execution.testframework.stacktrace.DiffHyperlink]
 * per failure (not a [com.intellij.execution.testframework.sm.runner.states.CompoundTestFailedState]
 * with stacked hyperlinks, which is what re-calling `setTestComparisonFailed` from a listener produces).
 *
 * Wire we get from Infection (`MutationAnalysis\TeamCity\TeamCity::testFinished`):
 *  - `type=comparisonFailure`
 *  - `expectedFile=<absolute path to source>`
 *  - `actual=<5-line context window of original code>`
 *  - `expected=<5-line context window of mutated code>`
 *
 * Wire we hand to the standard parser:
 *  - `expected=<full original file content>` (matches what the IDE loads from `expectedFile`)
 *  - `actual=<full mutated file content>` (original spliced with the mutation)
 */
class InfectionTestEventsConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties,
) : OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

    private val logger = Logger.getInstance(InfectionTestEventsConverter::class.java)

    /** path → (modificationStamp, fullContent), valid for one test run. */
    private val fileCache = mutableMapOf<String, Pair<Long, String>>()

    override fun processServiceMessage(message: ServiceMessage, visitor: ServiceMessageVisitor) {
        try {
            enrichTestFailed(message)
        } catch (e: Throwable) {
            logger.warn("[infection-converter] enrichment failed for ${message.messageName}", e)
        }
        super.processServiceMessage(message, visitor)
    }

    override fun finishTesting() {
        fileCache.clear()
        super.finishTesting()
    }

    private fun enrichTestFailed(message: ServiceMessage) {
        if (message.messageName != "testFailed") return
        val attrs = message.attributes
        if (attrs["type"] != "comparisonFailure") return

        val expectedFile = attrs["expectedFile"]?.takeIf { it.isNotEmpty() } ?: return
        // Infection's inversion: wire `actual` carries original snippet, wire `expected` carries mutated snippet.
        val originalSnippet = attrs["actual"]?.takeIf { it.isNotEmpty() } ?: return
        val mutatedSnippet = attrs["expected"]?.takeIf { it.isNotEmpty() } ?: return

        val originalFull = readFileContent(expectedFile) ?: run {
            logger.warn("[infection-converter] cannot read $expectedFile")
            return
        }
        val snippetStart = originalFull.indexOf(originalSnippet)
        if (snippetStart < 0) {
            logger.warn("[infection-converter] snippet not found in $expectedFile")
            return
        }
        val mutatedFull = buildString {
            append(originalFull, 0, snippetStart)
            append(mutatedSnippet)
            append(originalFull, snippetStart + originalSnippet.length, originalFull.length)
        }

        // Swap the inversion: now expected=original (matches expectedFile content),
        // actual=mutated (synthetic). This is the IDE-natural semantic.
        mutateAttributes(
            message,
            mapOf(
                "expected" to originalFull,
                "actual" to mutatedFull,
            ),
        )
    }

    /**
     * `ServiceMessage.getAttributes()` returns an unmodifiable wrapper, but the underlying
     * `myAttributes` field is a plain mutable map. We need to mutate it in place because the
     * `ServiceMessage` constructors are `protected` and there's no public copy-with-replaced-attrs.
     */
    private fun mutateAttributes(message: ServiceMessage, replacements: Map<String, String>) {
        val field = ServiceMessage::class.java.getDeclaredField("myAttributes")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val attrs = field.get(message) as MutableMap<String, String>
        replacements.forEach { (k, v) -> attrs[k] = v }
    }

    private fun readFileContent(path: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        val cached = fileCache[path]
        if (cached != null && cached.first == virtualFile.modificationStamp) return cached.second
        val content = ApplicationManager.getApplication().runReadAction(Computable {
            FileDocumentManager.getInstance().getDocument(virtualFile)?.text
        }) ?: return null
        fileCache[path] = virtualFile.modificationStamp to content
        return content
    }
}
