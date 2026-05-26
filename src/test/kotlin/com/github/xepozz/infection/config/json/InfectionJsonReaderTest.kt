package com.github.xepozz.infection.config.json

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InfectionJsonReaderTest {

    @Test
    fun `parses plain JSON thresholds`() {
        val result = InfectionJsonReader.parse("""{"minMsi": 80.5, "minCoveredMsi": 90}""")
        assertEquals(80.5, result.minMsi!!, 0.0001)
        assertEquals(90.0, result.minCoveredMsi!!, 0.0001)
    }

    @Test
    fun `tolerates line comments`() {
        val result = InfectionJsonReader.parse(
            """
            {
                // top-level comment
                "minMsi": 70, // trailing
                "source": { "directories": ["src"] }
            }
            """.trimIndent()
        )
        assertEquals(70.0, result.minMsi!!, 0.0001)
        assertNull(result.minCoveredMsi)
    }

    @Test
    fun `tolerates block comments`() {
        val result = InfectionJsonReader.parse(
            """
            {
                /* block
                   spanning */
                "minMsi": 55,
                "minCoveredMsi": 75
            }
            """.trimIndent()
        )
        assertEquals(55.0, result.minMsi!!, 0.0001)
        assertEquals(75.0, result.minCoveredMsi!!, 0.0001)
    }

    @Test
    fun `tolerates trailing commas`() {
        val result = InfectionJsonReader.parse(
            """
            {
                "minMsi": 42,
                "minCoveredMsi": 55,
            }
            """.trimIndent()
        )
        assertEquals(42.0, result.minMsi!!, 0.0001)
        assertEquals(55.0, result.minCoveredMsi!!, 0.0001)
    }

    @Test
    fun `no thresholds at all returns nulls`() {
        val result = InfectionJsonReader.parse("""{"source": {"directories": ["src"]}}""")
        assertNull(result.minMsi)
        assertNull(result.minCoveredMsi)
    }
}
