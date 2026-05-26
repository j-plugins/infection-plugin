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
            InfectionLocationHint.PROTOCOL_INFECTION,
            "/this/file/does/not/exist.php::1-2",
            project,
            GlobalSearchScope.allScope(project),
        )
        assertTrue(locations.isEmpty())
    }

    fun testGetLocation_infectionProtocol_malformedRange() {
        val locations = locator.getLocation(
            InfectionLocationHint.PROTOCOL_INFECTION,
            "/whatever.php::not-a-range",
            project,
            GlobalSearchScope.allScope(project),
        )
        assertTrue(locations.isEmpty())
    }

    fun testGetLocation_fileProtocol_unresolvable() {
        val locations = locator.getLocation(
            InfectionLocationHint.PROTOCOL_FILE,
            "/this/file/does/not/exist.php",
            project,
            GlobalSearchScope.allScope(project),
        )
        assertTrue(locations.isEmpty())
    }
}
