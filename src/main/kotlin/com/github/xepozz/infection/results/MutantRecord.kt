package com.github.xepozz.infection.results

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag

@Tag("mutant")
class MutantRecord {
    @Attribute("mutationId")
    var mutationId: String = ""

    @Attribute("mutator")
    var mutatorName: String = ""

    @Attribute("filePath")
    var filePath: String = ""

    @Attribute("start")
    var startOffset: Int = 0

    @Attribute("end")
    var endOffsetInclusive: Int = 0

    @Attribute("status")
    var status: MutantStatus = MutantStatus.UNKNOWN

    @Attribute("original")
    var originalSnippet: String? = null

    @Attribute("mutated")
    var mutatedSnippet: String? = null

    /**
     * Exact bytes occupying `[startOffset..endOffsetInclusive+1]` at run time. Used as a
     * freshness fingerprint when re-anchoring against the current file — unlike
     * [originalSnippet], which carries the full diff context block and isn't reliable for
     * position lookup.
     */
    @Attribute("originalRange")
    var originalRange: String? = null

    /** 1-based line number in the file where the change begins, when known. */
    @Attribute("line")
    var lineNumber: Int? = null

    /** Raw unified diff as reported by Infection (`MutantExecutionResult::getMutantDiff()`). */
    @Attribute("diff")
    var diff: String? = null

    @Attribute("runTimestamp")
    var runTimestamp: Long = 0L

    constructor()

    constructor(
        mutationId: String,
        mutatorName: String,
        filePath: String,
        startOffset: Int,
        endOffsetInclusive: Int,
        status: MutantStatus,
        originalSnippet: String? = null,
        mutatedSnippet: String? = null,
        originalRange: String? = null,
        lineNumber: Int? = null,
        diff: String? = null,
        runTimestamp: Long = System.currentTimeMillis(),
    ) {
        this.mutationId = mutationId
        this.mutatorName = mutatorName
        this.filePath = filePath
        this.startOffset = startOffset
        this.endOffsetInclusive = endOffsetInclusive
        this.status = status
        this.originalSnippet = originalSnippet
        this.mutatedSnippet = mutatedSnippet
        this.originalRange = originalRange
        this.lineNumber = lineNumber
        this.diff = diff
        this.runTimestamp = runTimestamp
    }
}
