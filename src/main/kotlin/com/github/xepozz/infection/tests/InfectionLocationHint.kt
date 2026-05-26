package com.github.xepozz.infection.tests

/**
 * Parsed shape of a TeamCity `locationHint` URL emitted by Infection.
 *
 * Two URL shapes are supported:
 *  - `file:///abs/path/source.php` — references a whole source file (suite-level).
 *  - `infection:///abs/path/source.php::startFilePos-endFilePos` — references an exact character
 *    range inside a source file (an individual mutation). Both offsets are inclusive char offsets
 *    in the file, as reported by Infection's `Mutation::getAttributes()`.
 */
sealed interface InfectionLocationHint {
    val filePath: String

    data class File(override val filePath: String) : InfectionLocationHint

    data class Mutation(
        override val filePath: String,
        val startOffset: Int,
        val endOffsetInclusive: Int,
    ) : InfectionLocationHint

    companion object {
        const val PROTOCOL_FILE = "file"
        const val PROTOCOL_INFECTION = "infection"

        private const val SCHEME_SEPARATOR = "://"

        /**
         * Parses a full TeamCity `locationHint` URL into [InfectionLocationHint].
         * Returns `null` for unsupported protocols, a missing `://`, or a malformed infection range.
         */
        fun parse(url: String): InfectionLocationHint? {
            val sep = url.indexOf(SCHEME_SEPARATOR)
            if (sep <= 0) return null
            val protocol = url.substring(0, sep)
            val path = url.substring(sep + SCHEME_SEPARATOR.length)
            return when (protocol) {
                PROTOCOL_FILE -> path.takeIf { it.isNotEmpty() }?.let(::File)
                PROTOCOL_INFECTION -> parseInfectionPath(path)
                else -> null
            }
        }

        /**
         * Parses the path portion of an `infection://…` URL — `/abs/path/source.php::startPos-endPos`.
         * Returns `null` for any malformed input (missing `::`, missing `-`, non-numeric range,
         * negative start, or `end < start`).
         */
        fun parseInfectionPath(path: String): Mutation? {
            val sep = path.lastIndexOf("::")
            if (sep <= 0) return null
            val filePath = path.substring(0, sep)
            val range = path.substring(sep + 2)
            val dash = range.indexOf('-')
            if (dash <= 0 || dash == range.length - 1) return null

            val start = range.substring(0, dash).toIntOrNull() ?: return null
            val end = range.substring(dash + 1).toIntOrNull() ?: return null
            if (start < 0 || end < start) return null

            return Mutation(filePath, start, end)
        }
    }
}
