package com.github.xepozz.infection.results

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MutationStatsTest {

    @Test
    fun `empty input returns EMPTY`() {
        val stats = MutationStats.from(emptyList(), runTimestamp = 123L)
        assertFalse(stats.hasData)
        assertEquals(0, stats.totalMutants)
        assertEquals(0.0, stats.msi, 0.0001)
        assertEquals(0.0, stats.coveredMsi, 0.0001)
    }

    @Test
    fun `MSI counts killed plus timed-out plus errors as detected`() {
        val records = listOf(
            record(MutantStatus.KILLED),
            record(MutantStatus.KILLED),
            record(MutantStatus.ESCAPED),
            record(MutantStatus.TIMED_OUT),
            record(MutantStatus.ERROR),
            record(MutantStatus.NOT_COVERED),
        )
        val stats = MutationStats.from(records, runTimestamp = 0L)
        assertTrue(stats.hasData)
        assertEquals(6, stats.totalMutants)
        assertEquals(2, stats.killed)
        assertEquals(1, stats.escaped)
        assertEquals(1, stats.timedOut)
        assertEquals(1, stats.errors)
        assertEquals(1, stats.notCovered)
        // detected = 2 killed + 1 timeout + 1 error = 4, of 6 total → 66.66%
        assertEquals(66.666, stats.msi, 0.01)
        // covered = 6 - 1 notCovered = 5, detected 4 of 5 → 80%
        assertEquals(80.0, stats.coveredMsi, 0.0001)
    }

    @Test
    fun `all escaped gives MSI 0`() {
        val records = List(3) { record(MutantStatus.ESCAPED) }
        val stats = MutationStats.from(records, runTimestamp = 0L)
        assertEquals(0.0, stats.msi, 0.0001)
        assertEquals(0.0, stats.coveredMsi, 0.0001)
    }

    @Test
    fun `all killed gives MSI 100`() {
        val records = List(5) { record(MutantStatus.KILLED) }
        val stats = MutationStats.from(records, runTimestamp = 0L)
        assertEquals(100.0, stats.msi, 0.0001)
        assertEquals(100.0, stats.coveredMsi, 0.0001)
    }

    @Test
    fun `coveredMsi is 0 when nothing is covered`() {
        val records = List(3) { record(MutantStatus.NOT_COVERED) }
        val stats = MutationStats.from(records, runTimestamp = 0L)
        assertEquals(0.0, stats.msi, 0.0001)
        assertEquals(0.0, stats.coveredMsi, 0.0001)
    }

    @Test
    fun `isInteresting hides killed and unknown but keeps signals`() {
        assertTrue(MutantStatus.ESCAPED.isInteresting)
        assertTrue(MutantStatus.TIMED_OUT.isInteresting)
        assertTrue(MutantStatus.NOT_COVERED.isInteresting)
        assertTrue(MutantStatus.ERROR.isInteresting)
        assertFalse(MutantStatus.KILLED.isInteresting)
        assertFalse(MutantStatus.UNKNOWN.isInteresting)
    }

    private fun record(status: MutantStatus) = MutantRecord(
        mutationId = "id-$status",
        mutatorName = "M",
        filePath = "/x.php",
        startOffset = 0,
        endOffsetInclusive = 1,
        status = status,
    )
}
