package com.github.xepozz.infection.tests

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InfectionLocationHintTest {

    @Test
    fun `parse file protocol`() {
        assertEquals(
            InfectionLocationHint.File("/abs/path/source.php"),
            InfectionLocationHint.parse("file:///abs/path/source.php"),
        )
    }

    @Test
    fun `parse infection protocol`() {
        assertEquals(
            InfectionLocationHint.Mutation("/abs/path/source.php", 342, 380),
            InfectionLocationHint.parse("infection:///abs/path/source.php::342-380"),
        )
    }

    @Test
    fun `parse infection protocol with single-char range`() {
        assertEquals(
            InfectionLocationHint.Mutation("/abs/path/source.php", 10, 10),
            InfectionLocationHint.parse("infection:///abs/path/source.php::10-10"),
        )
    }

    @Test
    fun `parse infection protocol where file path itself contains double colon`() {
        assertEquals(
            InfectionLocationHint.Mutation("/weird::path/source.php", 1, 2),
            InfectionLocationHint.parse("infection:///weird::path/source.php::1-2"),
        )
    }

    @Test
    fun `parse infection protocol with malformed range`() {
        assertNull(InfectionLocationHint.parse("infection:///abs/path/source.php::not-a-range"))
    }

    @Test
    fun `parse rejects unknown protocol`() {
        assertNull(InfectionLocationHint.parse("weird:///abs/path/source.php"))
    }

    @Test
    fun `parse rejects URL without scheme separator`() {
        assertNull(InfectionLocationHint.parse("/abs/path/source.php"))
    }

    @Test
    fun `parse rejects empty file URL`() {
        assertNull(InfectionLocationHint.parse("file://"))
    }

    @Test
    fun `parse rejects empty string`() {
        assertNull(InfectionLocationHint.parse(""))
    }

    @Test
    fun `parse rejects file protocol with infection-style range suffix`() {
        assertNull(InfectionLocationHint.parse("file:///abs/path/source.php::1-2"))
    }

    @Test
    fun `parse rejects file protocol with double-colon anywhere in path`() {
        assertNull(InfectionLocationHint.parse("file:///weird::path/source.php"))
    }

    @Test
    fun `parseInfectionPath simple`() {
        assertEquals(
            InfectionLocationHint.Mutation("/abs/path/source.php", 342, 380),
            InfectionLocationHint.parseInfectionPath("/abs/path/source.php::342-380"),
        )
    }

    @Test
    fun `parseInfectionPath single-char range`() {
        assertEquals(
            InfectionLocationHint.Mutation("/abs/path/source.php", 10, 10),
            InfectionLocationHint.parseInfectionPath("/abs/path/source.php::10-10"),
        )
    }

    @Test
    fun `parseInfectionPath splits on the last double colon`() {
        assertEquals(
            InfectionLocationHint.Mutation("/weird::path/source.php", 5, 9),
            InfectionLocationHint.parseInfectionPath("/weird::path/source.php::5-9"),
        )
    }

    @Test
    fun `parseInfectionPath rejects missing double colon`() {
        assertNull(InfectionLocationHint.parseInfectionPath("/abs/path/source.php:10-20"))
    }

    @Test
    fun `parseInfectionPath rejects missing dash`() {
        assertNull(InfectionLocationHint.parseInfectionPath("/abs/path/source.php::1020"))
    }

    @Test
    fun `parseInfectionPath rejects non-numeric start`() {
        assertNull(InfectionLocationHint.parseInfectionPath("/abs/path/source.php::abc-20"))
    }

    @Test
    fun `parseInfectionPath rejects non-numeric end`() {
        assertNull(InfectionLocationHint.parseInfectionPath("/abs/path/source.php::10-xyz"))
    }

    @Test
    fun `parseInfectionPath rejects end before start`() {
        assertNull(InfectionLocationHint.parseInfectionPath("/abs/path/source.php::20-10"))
    }

    @Test
    fun `parseInfectionPath rejects negative start`() {
        assertNull(InfectionLocationHint.parseInfectionPath("/abs/path/source.php::-5-10"))
    }

    @Test
    fun `parseInfectionPath rejects empty file path`() {
        assertNull(InfectionLocationHint.parseInfectionPath("::1-2"))
    }

    @Test
    fun `parseInfectionPath rejects trailing dash`() {
        assertNull(InfectionLocationHint.parseInfectionPath("/abs/path/source.php::10-"))
    }
}
