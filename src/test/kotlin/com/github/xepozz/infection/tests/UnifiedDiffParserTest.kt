package com.github.xepozz.infection.tests

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedDiffParserTest {

    @Test
    fun `parses infection-style diff with no line numbers`() {
        val diff = """
            --- Original
            +++ New
            @@ @@
            -    public function __construct(${'$'}species, ${'$'}sex, ${'$'}size, ${'$'}age = 0)
            +    public function __construct(${'$'}species, ${'$'}sex, ${'$'}size, ${'$'}age)
                 {
        """.trimIndent()
        val parsed = UnifiedDiffParser.parse(diff)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.originalLines.size)
        assertEquals(1, parsed.mutatedLines.size)
        assertTrue(parsed.originalLines.single().endsWith("age = 0)"))
        assertTrue(parsed.mutatedLines.single().endsWith("age)"))
        assertEquals(1, parsed.contextLines.size)
        assertNull(parsed.hunkStartLine)
    }

    @Test
    fun `parses hunk start line when present`() {
        val diff = """
            --- Original
            +++ New
            @@ -42,3 +42,3 @@
            -before
            +after
        """.trimIndent()
        val parsed = UnifiedDiffParser.parse(diff)
        assertNotNull(parsed)
        assertEquals(42, parsed!!.hunkStartLine)
    }

    @Test
    fun `applyToFile replaces the original lines in the file`() {
        val file = """
            line1
            line2
            line3
            line4
        """.trimIndent()
        val diff = """
            --- Original
            +++ New
            @@ @@
            -line2
            -line3
            +newline
        """.trimIndent()
        val parsed = UnifiedDiffParser.parse(diff)!!
        val applied = UnifiedDiffParser.applyToFile(file, parsed)
        assertNotNull(applied)
        assertEquals("line1\nnewline\nline4", applied!!.mutatedText)
        assertEquals(2, applied.originalLineNumber)
        assertEquals(1, applied.mutatedLineCount)
        assertEquals(2, applied.replacedLineCount)
    }

    @Test
    fun `applyToFile returns null when original lines absent`() {
        val file = "completely\nunrelated\ntext"
        val diff = """
            --- Original
            +++ New
            @@ @@
            -gone
            +here
        """.trimIndent()
        val parsed = UnifiedDiffParser.parse(diff)!!
        assertNull(UnifiedDiffParser.applyToFile(file, parsed))
    }

    @Test
    fun `null or blank input returns null`() {
        assertNull(UnifiedDiffParser.parse(null))
        assertNull(UnifiedDiffParser.parse(""))
        assertNull(UnifiedDiffParser.parse("   "))
    }

    @Test
    fun `applyToFile handles pure insertion via context`() {
        val file = "alpha\nbeta\ngamma"
        val diff = """
            --- Original
            +++ New
            @@ @@
             beta
            +inserted
        """.trimIndent()
        val parsed = UnifiedDiffParser.parse(diff)!!
        val applied = UnifiedDiffParser.applyToFile(file, parsed)
        assertNotNull(applied)
        assertEquals("alpha\ninserted\nbeta\ngamma", applied!!.mutatedText)
    }
}
