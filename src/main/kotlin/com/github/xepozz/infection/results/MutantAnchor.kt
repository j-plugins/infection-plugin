package com.github.xepozz.infection.results

import kotlin.math.abs

/**
 * Resolves a [MutantRecord] saved with offsets from an earlier run against the file's current text.
 *
 * Infection reports byte-accurate `startFilePos`/`endFilePos` for every mutation, so the saved
 * offsets are authoritative as long as the file hasn't changed at that location. We use
 * [MutantRecord.originalRange] — a snapshot of the exact bytes at those offsets at run time —
 * as a freshness fingerprint:
 *
 *   1. If the saved offsets are in-bounds and the file still has the same bytes there, use them.
 *   2. Otherwise, look up the saved range globally and pick the occurrence closest to the saved
 *      offset (handles the case where lines were inserted/removed earlier in the file).
 *   3. If the range can't be found at all, the marker is stale and we return null.
 *
 * Note: [MutantRecord.originalSnippet] is intentionally NOT used here. It carries the full diff
 * context block Infection emits in `comparisonFailure` test events, which is far larger than the
 * mutation range and would anchor markers wherever that block happens to start in the file (the
 * old behavior produced gutter icons several lines above the actual mutation).
 */
object MutantAnchor {

    data class Anchor(val startOffset: Int, val endOffsetExclusive: Int)

    fun reanchor(record: MutantRecord, fileText: CharSequence): Anchor? {
        val savedEnd = record.endOffsetInclusive + 1
        val savedStart = record.startOffset
        val inBounds = savedStart >= 0 && savedEnd <= fileText.length && savedEnd > savedStart

        val range = record.originalRange
        if (range.isNullOrEmpty()) {
            // Legacy records (or mutants for which we never captured the range) — trust the
            // saved offsets when they're in-bounds. This is correct as long as the file hasn't
            // been edited since the run.
            return if (inBounds) Anchor(savedStart, savedEnd) else null
        }

        if (inBounds && fileText.subSequence(savedStart, savedEnd).toString() == range) {
            return Anchor(savedStart, savedEnd)
        }

        // File drifted — try to relocate the exact byte range. Pick the occurrence closest to
        // the saved offset to disambiguate when the range repeats (e.g. a literal `2`).
        val text = fileText.toString()
        val closest = occurrencesOf(text, range).minByOrNull { abs(it - savedStart) } ?: return null
        return Anchor(closest, closest + range.length)
    }

    private fun occurrencesOf(haystack: String, needle: String): Sequence<Int> = sequence {
        var from = 0
        while (true) {
            val idx = haystack.indexOf(needle, from)
            if (idx < 0) return@sequence
            yield(idx)
            from = idx + 1
        }
    }
}
