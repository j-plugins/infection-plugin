package com.github.xepozz.infection.tests

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

        fun parse(url: String): InfectionLocationHint? {
            val sep = url.indexOf(SCHEME_SEPARATOR)
            if (sep <= 0) return null
            val protocol = url.substring(0, sep)
            val path = url.substring(sep + SCHEME_SEPARATOR.length)
            return when (protocol) {
                PROTOCOL_FILE -> path.takeIf { it.isNotEmpty() && !it.contains("::") }?.let(::File)
                PROTOCOL_INFECTION -> parseInfectionPath(path)
                else -> null
            }
        }

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
