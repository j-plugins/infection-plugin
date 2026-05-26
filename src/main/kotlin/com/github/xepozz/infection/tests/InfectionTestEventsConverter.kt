package com.github.xepozz.infection.tests

import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.events.TestFailedEvent
import com.intellij.execution.testframework.sm.runner.events.TestOutputEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor
import jetbrains.buildServer.messages.serviceMessages.TestFailed
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class InfectionTestEventsConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties,
) : OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

    private val logger = Logger.getInstance(InfectionTestEventsConverter::class.java)

    private val fileCache = ConcurrentHashMap<String, Pair<Long, String>>()

    private val nodeFiles = ConcurrentHashMap<String, NodeLocation>()

    private val testNodeIds = ConcurrentHashMap<String, String>()

    private data class NodeLocation(val filePath: String, val mutationRange: IntRange?)

    override fun processServiceMessage(message: ServiceMessage, visitor: ServiceMessageVisitor) {
        var handled = false
        try {
            captureNodeIds(message)
            releaseNodeIds(message)
            handled = dispatchComparisonFailure(message) || dispatchOrphanOutput(message)
        } catch (e: Throwable) {
            logger.warn("[infection-converter] enrichment failed for ${message.messageName}", e)
        }
        if (!handled) super.processServiceMessage(message, visitor)
    }

    override fun finishTesting() {
        fileCache.clear()
        nodeFiles.clear()
        testNodeIds.clear()
        super.finishTesting()
    }

    private fun captureNodeIds(message: ServiceMessage) {
        if (message.messageName != "testStarted" && message.messageName != "testSuiteStarted") return
        val attrs = message.attributes
        val nodeId = attrs["nodeId"]?.takeIf { it.isNotEmpty() } ?: return
        attrs["name"]?.takeIf { it.isNotEmpty() }?.let { testNodeIds[it] = nodeId }

        val locationHint = attrs["locationHint"]?.takeIf { it.isNotEmpty() } ?: return
        val parsed = InfectionLocationHint.parse(locationHint) ?: return
        val range = (parsed as? InfectionLocationHint.Mutation)
            ?.let { it.startOffset..it.endOffsetInclusive }
        nodeFiles[nodeId] = NodeLocation(parsed.filePath, range)
    }

    private fun releaseNodeIds(message: ServiceMessage) {
        if (message.messageName != "testFinished" && message.messageName != "testSuiteFinished") return
        val attrs = message.attributes
        attrs["nodeId"]?.takeIf { it.isNotEmpty() }?.let { nodeFiles.remove(it) }
        attrs["name"]?.takeIf { it.isNotEmpty() }?.let { testNodeIds.remove(it) }
    }

    private fun dispatchComparisonFailure(message: ServiceMessage): Boolean {
        if (message !is TestFailed) return false
        val attrs = message.attributes
        if (attrs["type"] != "comparisonFailure") return false

        val name = attrs["name"]?.takeIf { it.isNotEmpty() } ?: return false
        val nodeId = attrs["nodeId"]?.takeIf { it.isNotEmpty() } ?: return false
        val failureMessage = attrs["message"]?.takeIf { it.isNotEmpty() } ?: run {
            logger.warn("[infection-converter] testFailed missing 'message' attribute for nodeId=$nodeId")
            return false
        }
        val location = nodeFiles[nodeId] ?: run {
            logger.warn("[infection-converter] no locationHint captured for nodeId=$nodeId")
            return false
        }
        val processor = processor ?: return false

        val originalSnippet = attrs["actual"]?.takeIf { it.isNotEmpty() } ?: return false
        val mutatedSnippet = attrs["expected"]?.takeIf { it.isNotEmpty() } ?: return false

        val originalFull = readFileContent(location.filePath) ?: run {
            logger.warn("[infection-converter] cannot read ${location.filePath}")
            return false
        }
        val snippetStart = findAnchoredSnippet(originalFull, originalSnippet, location.mutationRange)
        if (snippetStart < 0) {
            logger.warn("[infection-converter] snippet not found in ${location.filePath}")
            return false
        }
        val mutatedFull = buildString {
            append(originalFull, 0, snippetStart)
            append(mutatedSnippet)
            append(originalFull, snippetStart + originalSnippet.length, originalFull.length)
        }

        processor.onTestFailure(
            TestFailedEvent(
                name,
                nodeId,
                failureMessage,
                attrs["details"],
                attrs["error"] != null,
                mutatedFull,
                originalFull,
                location.filePath,
                null,
                false,
                false,
                attrs["duration"]?.toLongOrNull() ?: -1L,
            )
        )
        return true
    }

    private fun dispatchOrphanOutput(message: ServiceMessage): Boolean {
        val stdOut = when (message.messageName) {
            "testStdOut" -> true
            "testStdErr" -> false
            else -> return false
        }
        val attrs = message.attributes
        if (!attrs["nodeId"].isNullOrEmpty()) return false
        val name = attrs["name"]?.takeIf { it.isNotEmpty() } ?: return false
        val nodeId = testNodeIds[name] ?: return false
        val processor = processor ?: return false
        val text = attrs[if (stdOut) "out" else "err"].orEmpty()
        processor.onTestOutput(TestOutputEvent(name, nodeId, text, stdOut))
        return true
    }

    private fun findAnchoredSnippet(haystack: String, needle: String, range: IntRange?): Int {
        if (range == null) return haystack.indexOf(needle)
        var from = 0
        var first = -1
        while (true) {
            val idx = haystack.indexOf(needle, from)
            if (idx < 0) return first
            if (first < 0) first = idx
            val end = idx + needle.length
            if (idx <= range.first && range.last < end) return idx
            from = idx + 1
        }
    }

    private fun readFileContent(path: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        val cached = fileCache[path]
        if (cached != null && cached.first == virtualFile.modificationStamp) return cached.second
        val content = try {
            VfsUtilCore.loadText(virtualFile)
        } catch (e: IOException) {
            logger.warn("[infection-converter] failed to read $path", e)
            return null
        }
        fileCache[path] = virtualFile.modificationStamp to content
        return content
    }
}
