package com.github.xepozz.infection.results

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MutantAnchorTest {

    @Test
    fun `returns saved offsets when the file content at them still matches the fingerprint`() {
        val text = "abcXY_target_ZWend"
        val record = record(start = 5, end = 12, range = "_target_")
        val anchor = MutantAnchor.reanchor(record, text)
        assertNotNull(anchor)
        assertEquals(5, anchor!!.startOffset)
        assertEquals(13, anchor.endOffsetExclusive)
    }

    @Test
    fun `re-anchors to the matching range when the file shifted right`() {
        val text = "// new comment line\nabcXY_target_ZWend"
        val record = record(start = 5, end = 12, range = "_target_")
        val anchor = MutantAnchor.reanchor(record, text)
        assertNotNull(anchor)
        val expected = text.indexOf("_target_")
        assertEquals(expected, anchor!!.startOffset)
        assertEquals(expected + "_target_".length, anchor.endOffsetExclusive)
    }

    @Test
    fun `picks the occurrence closest to the saved offset when the range repeats`() {
        val text = "foo bar foo bar foo bar"
        val savedStart = text.indexOf("foo", 10)
        val record = record(start = savedStart, end = savedStart + 2, range = "foo")
        val anchor = MutantAnchor.reanchor(record, text)
        assertNotNull(anchor)
        assertEquals(savedStart, anchor!!.startOffset)
    }

    @Test
    fun `returns null when the range vanished from the file`() {
        val text = "completely different content"
        val record = record(start = 0, end = 4, range = "vanished")
        assertNull(MutantAnchor.reanchor(record, text))
    }

    @Test
    fun `trusts saved offsets when no fingerprint was captured and offsets are in-bounds`() {
        val text = "0123456789"
        val record = record(start = 2, end = 4, range = null)
        val anchor = MutantAnchor.reanchor(record, text)
        assertNotNull(anchor)
        assertEquals(2, anchor!!.startOffset)
        assertEquals(5, anchor.endOffsetExclusive)
    }

    @Test
    fun `returns null when no fingerprint and offsets fell off the end`() {
        val text = "short"
        val record = record(start = 10, end = 12, range = null)
        assertNull(MutantAnchor.reanchor(record, text))
    }

    @Test
    fun `does not anchor to the surrounding diff context block`() {
        // Regression: Infection's `comparisonFailure.actual` carries the whole method body, which
        // starts several lines above the real mutation. The anchor must rely on the exact range
        // fingerprint and ignore that wider snippet entirely.
        val text = """
            class Foo {
                public function describe(): string
                {
                    // header

                    return 'X' . 'Y';
                }
            }
        """.trimIndent()
        val mutationStart = text.indexOf("'X'")
        val mutationEnd = mutationStart + "'X'".length - 1
        val record = MutantRecord(
            mutationId = "id",
            mutatorName = "ConcatOperandRemoval",
            filePath = "/Foo.php",
            startOffset = mutationStart,
            endOffsetInclusive = mutationEnd,
            status = MutantStatus.ESCAPED,
            originalSnippet = "    {\n        // header\n\n        return 'X' . 'Y';\n",
            mutatedSnippet = "    {\n        // header\n\n        return 'X';\n",
            originalRange = "'X'",
        )
        val anchor = MutantAnchor.reanchor(record, text)
        assertNotNull(anchor)
        assertEquals(mutationStart, anchor!!.startOffset)
        assertEquals(mutationEnd + 1, anchor.endOffsetExclusive)
    }

    private fun record(start: Int, end: Int, range: String?) = MutantRecord(
        mutationId = "id",
        mutatorName = "TrueValue",
        filePath = "/x.php",
        startOffset = start,
        endOffsetInclusive = end,
        status = MutantStatus.ESCAPED,
        originalRange = range,
    )
}
