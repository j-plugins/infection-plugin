package com.github.xepozz.infection.tests

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.util.pathmapper.PhpPathMapper

class InfectionTestLocatorTest : BasePlatformTestCase() {

    private lateinit var locator: InfectionTestLocator

    override fun setUp() {
        super.setUp()
        locator = InfectionTestLocator(PhpPathMapper.create(emptyList()))
    }

    fun testParseMutationPath_simple() {
        val info = InfectionTestLocator.parseMutationPath("/abs/path/source.php::342-380")
        assertNotNull(info)
        assertEquals("/abs/path/source.php", info!!.filePath)
        assertEquals(342, info.startOffset)
        assertEquals(380, info.endOffsetInclusive)
    }

    fun testParseMutationPath_singleCharRange() {
        val info = InfectionTestLocator.parseMutationPath("/abs/path/source.php::10-10")
        assertNotNull(info)
        assertEquals(10, info!!.startOffset)
        assertEquals(10, info.endOffsetInclusive)
    }

    fun testParseMutationPath_pathWithDoubleColonInside() {
        // The last `::` separates path from range, even if the path itself contains `::`.
        val info = InfectionTestLocator.parseMutationPath("/weird::path/source.php::5-9")
        assertNotNull(info)
        assertEquals("/weird::path/source.php", info!!.filePath)
        assertEquals(5, info.startOffset)
        assertEquals(9, info.endOffsetInclusive)
    }

    fun testParseMutationPath_missingDoubleColon() {
        assertNull(InfectionTestLocator.parseMutationPath("/abs/path/source.php:10-20"))
    }

    fun testParseMutationPath_missingDash() {
        assertNull(InfectionTestLocator.parseMutationPath("/abs/path/source.php::1020"))
    }

    fun testParseMutationPath_nonNumericStart() {
        assertNull(InfectionTestLocator.parseMutationPath("/abs/path/source.php::abc-20"))
    }

    fun testParseMutationPath_nonNumericEnd() {
        assertNull(InfectionTestLocator.parseMutationPath("/abs/path/source.php::10-xyz"))
    }

    fun testParseMutationPath_endBeforeStart() {
        assertNull(InfectionTestLocator.parseMutationPath("/abs/path/source.php::20-10"))
    }

    fun testParseMutationPath_negativeStart() {
        assertNull(InfectionTestLocator.parseMutationPath("/abs/path/source.php::-5-10"))
    }

    fun testParseMutationPath_emptyFilePath() {
        assertNull(InfectionTestLocator.parseMutationPath("::1-2"))
    }

    fun testParseMutationPath_trailingDash() {
        assertNull(InfectionTestLocator.parseMutationPath("/abs/path/source.php::10-"))
    }

    fun testGetLocation_unknownProtocol() {
        val locations = locator.getLocation(
            "weird-proto",
            "/whatever",
            project,
            GlobalSearchScope.allScope(project),
        )
        assertTrue(locations.isEmpty())
    }

    fun testGetLocation_infectionProtocol_unresolvableFile() {
        val locations = locator.getLocation(
            InfectionTestLocator.PROTOCOL_INFECTION,
            "/this/file/does/not/exist.php::1-2",
            project,
            GlobalSearchScope.allScope(project),
        )
        assertTrue(locations.isEmpty())
    }

    fun testGetLocation_infectionProtocol_malformedRange() {
        val locations = locator.getLocation(
            InfectionTestLocator.PROTOCOL_INFECTION,
            "/whatever.php::not-a-range",
            project,
            GlobalSearchScope.allScope(project),
        )
        assertTrue(locations.isEmpty())
    }

    fun testGetLocation_fileProtocol_unresolvable() {
        val locations = locator.getLocation(
            InfectionTestLocator.PROTOCOL_FILE,
            "/this/file/does/not/exist.php",
            project,
            GlobalSearchScope.allScope(project),
        )
        assertTrue(locations.isEmpty())
    }
}
