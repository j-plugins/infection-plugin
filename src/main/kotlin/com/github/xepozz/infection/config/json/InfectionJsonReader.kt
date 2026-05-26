package com.github.xepozz.infection.config.json

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

data class InfectionThresholds(
    val minMsi: Double?,
    val minCoveredMsi: Double?,
)

object InfectionJsonReader {
    private val logger = Logger.getInstance(InfectionJsonReader::class.java)

    private val CANDIDATES = listOf(
        "infection.json",
        "infection.json.dist",
        "infection.json5",
        "infection.json5.dist",
    )

    // kotlinx.serialization's Json supports the JSON5 features Infection's config uses out of the
    // box — no hand-rolled stripping needed:
    //  • `allowComments` — `//` and `/* */`
    //  • `allowTrailingComma` — `…, }` and `…, ]`
    //  • `isLenient` — single-quoted strings, unquoted keys, NaN/Infinity numbers
    private val json5 = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowComments = true
        allowTrailingComma = true
    }

    fun readThresholds(project: Project): InfectionThresholds {
        val basePath = project.basePath ?: return InfectionThresholds(null, null)
        val file = CANDIDATES.asSequence()
            .map { File(basePath, it) }
            .firstOrNull { it.isFile }
            ?: return InfectionThresholds(null, null)

        return try {
            val text = LocalFileSystem.getInstance().findFileByPath(file.path)
                ?.let { String(it.contentsToByteArray()) }
                ?: file.readText()
            parse(text)
        } catch (e: Exception) {
            logger.warn("[infection-json] failed to read ${file.path}", e)
            InfectionThresholds(null, null)
        }
    }

    internal fun parse(text: String): InfectionThresholds {
        val root = json5.parseToJsonElement(text) as? JsonObject
            ?: return InfectionThresholds(null, null)
        return InfectionThresholds(
            minMsi = root["minMsi"]?.jsonPrimitive?.doubleOrNull,
            minCoveredMsi = root["minCoveredMsi"]?.jsonPrimitive?.doubleOrNull,
        )
    }
}
