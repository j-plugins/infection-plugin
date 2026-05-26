package com.github.xepozz.infection.tests

import com.github.xepozz.infection.results.MutantRecord
import com.github.xepozz.infection.results.MutantStatus
import com.github.xepozz.infection.results.MutationResultsService
import com.github.xepozz.infection.results.SnippetSplicer
import com.github.xepozz.infection.tests.metainfo.InfectionMutationMetainfoStore
import com.github.xepozz.infection.tests.metainfo.TestProxyMetainfo
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.events.TestFailedEvent
import com.intellij.execution.testframework.sm.runner.events.TestOutputEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor
import jetbrains.buildServer.messages.serviceMessages.TestFailed
import java.util.concurrent.ConcurrentHashMap

class InfectionTestEventsConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties,
) : OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

    private val logger = Logger.getInstance(InfectionTestEventsConverter::class.java)

    private val project: Project = consoleProperties.project

    private val nodeFiles = ConcurrentHashMap<String, NodeLocation>()
    private val testNodeIds = ConcurrentHashMap<String, String>()

    private val accumulated = ConcurrentHashMap<String, MutantAccumulator>()

    private data class NodeLocation(val filePath: String, val mutationRange: IntRange?)

    private class MutantAccumulator(
        val nodeId: String,
        val filePath: String,
        val startOffset: Int,
        val endOffsetInclusive: Int,
    ) {
        @Volatile var mutatorName: String = ""
        @Volatile var mutationHash: String? = null
        @Volatile var status: MutantStatus = MutantStatus.UNKNOWN
        @Volatile var originalSnippet: String? = null
        @Volatile var mutatedSnippet: String? = null
        @Volatile var originalRange: String? = null
        @Volatile var lineNumber: Int? = null
        @Volatile var diff: String? = null
    }

    override fun processServiceMessage(message: ServiceMessage, visitor: ServiceMessageVisitor) {
        var handled = false
        try {
            captureNodeIds(message)
            updateStatus(message)
            handled = dispatchComparisonFailure(message) || dispatchOrphanOutput(message)
            releaseNodeIds(message)
        } catch (e: Throwable) {
            logger.warn("[infection-converter] enrichment failed for ${message.messageName}", e)
        }
        if (!handled) super.processServiceMessage(message, visitor)
    }

    override fun finishTesting() {
        try {
            publishAccumulated()
        } catch (e: Throwable) {
            logger.warn("[infection-converter] failed to publish mutation results", e)
        } finally {
            nodeFiles.clear()
            testNodeIds.clear()
            accumulated.clear()
            if (!project.isDisposed) InfectionMutationMetainfoStore.getInstance(project).clear()
        }
        super.finishTesting()
    }

    companion object {
        private val TERMINAL_MESSAGES = setOf("testFinished", "testFailed", "testIgnored")
    }

    private fun publishAccumulated() {
        if (accumulated.isEmpty() || project.isDisposed) return
        val records = accumulated.values.map { acc ->
            MutantRecord(
                mutationId = acc.mutationHash ?: acc.nodeId,
                mutatorName = acc.mutatorName.ifEmpty { "Unknown" },
                filePath = acc.filePath,
                startOffset = acc.startOffset,
                endOffsetInclusive = acc.endOffsetInclusive,
                status = if (acc.status == MutantStatus.UNKNOWN) MutantStatus.KILLED else acc.status,
                originalSnippet = acc.originalSnippet,
                mutatedSnippet = acc.mutatedSnippet,
                originalRange = acc.originalRange,
                lineNumber = acc.lineNumber,
                diff = acc.diff,
            )
        }
        MutationResultsService.getInstance(project).replaceResults(records)
    }

    private fun captureNodeIds(message: ServiceMessage) {
        if (message.messageName != "testStarted" && message.messageName != "testSuiteStarted") return
        val attrs = message.attributes
        val nodeId = attrs["nodeId"]?.takeIf { it.isNotEmpty() } ?: return
        val name = attrs["name"]?.takeIf { it.isNotEmpty() }
        if (name != null) testNodeIds[name] = nodeId

        val locationHint = attrs["locationHint"]?.takeIf { it.isNotEmpty() } ?: return
        val parsed = InfectionLocationHint.parse(locationHint) ?: return
        val mutation = parsed as? InfectionLocationHint.Mutation
        val range = mutation?.let { it.startOffset..it.endOffsetInclusive }
        nodeFiles[nodeId] = NodeLocation(parsed.filePath, range)

        if (mutation != null && message.messageName == "testStarted") {
            val acc = MutantAccumulator(
                nodeId = nodeId,
                filePath = mutation.filePath,
                startOffset = mutation.startOffset,
                endOffsetInclusive = mutation.endOffsetInclusive,
            )
            // Capture the exact bytes at the mutation range so we have a precise freshness
            // fingerprint for re-anchoring. Done here (not in dispatchComparisonFailure) so it
            // works for every mutant — killed ones don't emit `comparisonFailure` events.
            readFileContent(mutation.filePath)?.let { fileText ->
                val end = (mutation.endOffsetInclusive + 1).coerceAtMost(fileText.length)
                if (mutation.startOffset in 0 until end) {
                    acc.originalRange = fileText.substring(mutation.startOffset, end)
                }
            }
            // Note: don't trust the mutator name parsed from `name` here — Infection wraps every
            // mutator in `IgnoreMutator`, so testStarted's name attribute is always
            // `Infection\Mutator\IgnoreMutator (hash)`. The real mutator name arrives later in the
            // `message: Mutator: ...` field of testFinished/testFailed; updateStatus picks it up.
            if (name != null) {
                val parsedName = InfectionTestMessageParser.parseTestName(name)
                acc.mutationHash = parsedName.mutationHash
                if (parsedName.mutatorName != "IgnoreMutator") {
                    acc.mutatorName = parsedName.mutatorName
                }
            }
            accumulated[nodeId] = acc
        }
    }

    private fun updateStatus(message: ServiceMessage) {
        val attrs = message.attributes
        val nodeId = attrs["nodeId"]?.takeIf { it.isNotEmpty() } ?: return
        val acc = accumulated[nodeId] ?: return

        val parsedMessage = InfectionTestMessageParser.parseMessageAttribute(attrs["message"])
        // Always prefer the mutator name from the message payload — Infection's `name` attribute
        // exposes the IgnoreMutator wrapper, not the real mutator.
        if (parsedMessage.mutator != null) acc.mutatorName = parsedMessage.mutator
        if (parsedMessage.mutationId != null && acc.mutationHash.isNullOrEmpty()) acc.mutationHash = parsedMessage.mutationId

        val details = attrs["details"]
        if (!details.isNullOrBlank() && acc.diff == null) {
            acc.diff = details
            UnifiedDiffParser.parse(details)?.let { parsed ->
                if (acc.originalSnippet.isNullOrEmpty() && parsed.originalLines.isNotEmpty()) {
                    acc.originalSnippet = parsed.originalLines.joinToString("\n")
                }
                if (acc.mutatedSnippet.isNullOrEmpty() && parsed.mutatedLines.isNotEmpty()) {
                    acc.mutatedSnippet = parsed.mutatedLines.joinToString("\n")
                }
                if (acc.lineNumber == null) acc.lineNumber = parsed.hunkStartLine
            }
        }

        when (message.messageName) {
            "testIgnored" -> acc.status = MutantStatus.NOT_COVERED
            "testFinished" -> acc.status = parsedMessage.status
                ?: if (acc.status == MutantStatus.UNKNOWN) MutantStatus.KILLED else acc.status
            "testFailed" -> acc.status = parsedMessage.status
                ?: deriveFailureStatus(attrs["message"].orEmpty(), attrs["details"].orEmpty())
        }

        if (message.messageName in TERMINAL_MESSAGES) {
            stashMetainfoFor(nodeId, acc)
        }
    }

    private fun stashMetainfoFor(nodeId: String, acc: MutantAccumulator) {
        if (project.isDisposed) return
        val testName = testNodeIds.entries.firstOrNull { it.value == nodeId }?.key ?: return
        val attrs = buildMap {
            val mutationId = acc.mutationHash ?: acc.nodeId
            if (mutationId.isNotEmpty()) put(TestProxyMetainfo.KEY_MUTATION_ID, mutationId)
            if (acc.mutatorName.isNotEmpty()) put(TestProxyMetainfo.KEY_MUTATOR_NAME, acc.mutatorName)
            put(TestProxyMetainfo.KEY_STATUS, acc.status.name)
            acc.originalSnippet?.takeIf { it.isNotEmpty() }
                ?.let { put(TestProxyMetainfo.KEY_ORIGINAL_CODE, it) }
            acc.mutatedSnippet?.takeIf { it.isNotEmpty() }
                ?.let { put(TestProxyMetainfo.KEY_MUTATED_CODE, it) }
        }
        InfectionMutationMetainfoStore.getInstance(project).put(testName, attrs)
    }

    /** Last-resort status inference when Infection's `message` payload didn't carry an explicit
     *  `Mutation result:` line — guess from free-text in `message`/`details`. */
    private fun deriveFailureStatus(message: String, details: String): MutantStatus = when {
        message.contains("timeout", ignoreCase = true) ||
            details.contains("timeout", ignoreCase = true) -> MutantStatus.TIMED_OUT
        message.contains("error", ignoreCase = true) &&
            !message.contains("escaped", ignoreCase = true) -> MutantStatus.ERROR
        else -> MutantStatus.ESCAPED
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

        accumulated[nodeId]?.let {
            it.originalSnippet = originalSnippet
            it.mutatedSnippet = mutatedSnippet
        }

        val originalFull = readFileContent(location.filePath) ?: run {
            logger.warn("[infection-converter] cannot read ${location.filePath}")
            return false
        }
        val mutatedFull = SnippetSplicer.splice(
            fileText = originalFull,
            original = originalSnippet,
            mutated = mutatedSnippet,
            containing = location.mutationRange,
        ) ?: run {
            logger.warn("[infection-converter] snippet not found in ${location.filePath}")
            return false
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

    private fun readFileContent(path: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        // VFS already caches `contentsToByteArray()` per modificationStamp inside PersistentFSImpl —
        // no plugin-side cache needed on top of that.
        return runCatching { VfsUtilCore.loadText(virtualFile) }
            .onFailure { logger.warn("[infection-converter] failed to read $path", it) }
            .getOrNull()
    }
}
