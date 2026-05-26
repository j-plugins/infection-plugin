package com.github.xepozz.infection.tests

import com.github.xepozz.infection.results.MutantStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InfectionTestMessageParserTest {

    @Test
    fun `parses canonical name with FQCN and hash`() {
        val parsed = InfectionTestMessageParser.parseTestName(
            "Infection\\Mutator\\Boolean\\TrueValue (abc123def456)"
        )
        assertEquals("TrueValue", parsed.mutatorName)
        assertEquals("abc123def456", parsed.mutationHash)
    }

    @Test
    fun `parses name without namespace`() {
        val parsed = InfectionTestMessageParser.parseTestName("TrueValue (xyz)")
        assertEquals("TrueValue", parsed.mutatorName)
        assertEquals("xyz", parsed.mutationHash)
    }

    @Test
    fun `falls back when parentheses missing`() {
        val parsed = InfectionTestMessageParser.parseTestName("SomeStrangeName")
        assertEquals("SomeStrangeName", parsed.mutatorName)
        assertNull(parsed.mutationHash)
    }

    @Test
    fun `parses Infection message attribute fully`() {
        val msg = """
            Mutator: Infection\Mutator\Number\IncrementInteger
            Mutation ID: deadbeef
            Mutation result: escaped
        """.trimIndent()
        val parsed = InfectionTestMessageParser.parseMessageAttribute(msg)
        assertEquals("IncrementInteger", parsed.mutator)
        assertEquals("deadbeef", parsed.mutationId)
        assertEquals(MutantStatus.ESCAPED, parsed.status)
    }

    @Test
    fun `maps detection statuses`() {
        assertEquals(MutantStatus.KILLED, InfectionTestMessageParser.mapDetectionStatus("killed by tests"))
        assertEquals(MutantStatus.KILLED, InfectionTestMessageParser.mapDetectionStatus("killed by SA"))
        assertEquals(MutantStatus.ESCAPED, InfectionTestMessageParser.mapDetectionStatus("escaped"))
        assertEquals(MutantStatus.TIMED_OUT, InfectionTestMessageParser.mapDetectionStatus("timed out"))
        assertEquals(MutantStatus.NOT_COVERED, InfectionTestMessageParser.mapDetectionStatus("not covered"))
        assertEquals(MutantStatus.NOT_COVERED, InfectionTestMessageParser.mapDetectionStatus("skipped"))
        assertEquals(MutantStatus.NOT_COVERED, InfectionTestMessageParser.mapDetectionStatus("ignored"))
        assertEquals(MutantStatus.ERROR, InfectionTestMessageParser.mapDetectionStatus("error"))
        assertEquals(MutantStatus.ERROR, InfectionTestMessageParser.mapDetectionStatus("syntax error"))
        assertNull(InfectionTestMessageParser.mapDetectionStatus("bogus"))
    }

    @Test
    fun `empty message returns nulls`() {
        val parsed = InfectionTestMessageParser.parseMessageAttribute(null)
        assertNull(parsed.mutator)
        assertNull(parsed.mutationId)
        assertNull(parsed.status)
    }
}
