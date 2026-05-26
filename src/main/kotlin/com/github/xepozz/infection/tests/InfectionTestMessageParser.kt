package com.github.xepozz.infection.tests

import com.github.xepozz.infection.results.MutantStatus

/**
 * Infection's TeamCity logger produces test names like:
 *   `Infection\Mutator\Boolean\TrueValue (abc123def...)`
 * and `testFinished` / `testFailed` messages where the `message` attribute carries
 *
 *   Mutator: <FQCN>
 *   Mutation ID: <hash>
 *   Mutation result: <one of DetectionStatus enum values>
 *
 * See: playground-infection/src/Logger/MutationAnalysis/TeamCity/Test.php
 */
internal object InfectionTestMessageParser {

    data class ParsedName(val mutatorName: String, val mutationHash: String?)

    fun parseTestName(rawName: String): ParsedName {
        val openParen = rawName.lastIndexOf(" (")
        val closeParen = rawName.lastIndexOf(')')
        if (openParen < 0 || closeParen < openParen + 2) {
            return ParsedName(simplifyMutatorFqcn(rawName.trim()), null)
        }
        val fqcn = rawName.substring(0, openParen).trim()
        val hash = rawName.substring(openParen + 2, closeParen).trim()
        return ParsedName(simplifyMutatorFqcn(fqcn), hash.takeIf { it.isNotEmpty() })
    }

    fun simplifyMutatorFqcn(fqcn: String): String {
        val tail = fqcn.substringAfterLast('\\')
        return tail.ifEmpty { fqcn }
    }

    data class ParsedMessage(
        val mutator: String?,
        val mutationId: String?,
        val status: MutantStatus?,
    )

    fun parseMessageAttribute(message: String?): ParsedMessage {
        if (message.isNullOrBlank()) return ParsedMessage(null, null, null)
        var mutator: String? = null
        var mutationId: String? = null
        var status: MutantStatus? = null
        for (rawLine in message.lineSequence()) {
            val line = rawLine.trim()
            val colon = line.indexOf(':')
            if (colon < 0) continue
            val key = line.substring(0, colon).trim()
            val value = line.substring(colon + 1).trim()
            when (key) {
                "Mutator" -> if (value.isNotEmpty()) mutator = simplifyMutatorFqcn(value)
                "Mutation ID" -> if (value.isNotEmpty()) mutationId = value
                "Mutation result" -> status = mapDetectionStatus(value)
            }
        }
        return ParsedMessage(mutator, mutationId, status)
    }

    fun mapDetectionStatus(raw: String): MutantStatus? = when (raw.trim().lowercase()) {
        "killed by tests", "killed by sa", "killed" -> MutantStatus.KILLED
        "escaped" -> MutantStatus.ESCAPED
        "timed out" -> MutantStatus.TIMED_OUT
        "not covered" -> MutantStatus.NOT_COVERED
        "error", "syntax error" -> MutantStatus.ERROR
        "skipped", "ignored" -> MutantStatus.NOT_COVERED
        else -> null
    }
}
