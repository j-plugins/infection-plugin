package com.github.xepozz.infection.results

/**
 * Replaces an `original` snippet with a `mutated` one inside a larger text. Used both for building
 * the "mutated full file" we hand to IDE diff dialogs and for the gutter "Show Mutant Diff" view.
 *
 * When the snippet occurs multiple times, callers can pass a `containing` byte range as a tie-
 * breaker — the splicer picks the occurrence whose extent encloses that range. This matters in
 * theory (the file might have identical concat chains in two places); in practice Infection's
 * `comparisonFailure.actual` is a multi-line block that's almost always unique.
 */
internal object SnippetSplicer {

    fun splice(
        fileText: String,
        original: String,
        mutated: String,
        containing: IntRange? = null,
    ): String? {
        val idx = locate(fileText, original, containing)
        if (idx < 0) return null
        return buildString(fileText.length - original.length + mutated.length) {
            append(fileText, 0, idx)
            append(mutated)
            append(fileText, idx + original.length, fileText.length)
        }
    }

    private fun locate(haystack: String, needle: String, containing: IntRange?): Int {
        val occurrences = generateSequence(haystack.indexOf(needle)) { prev ->
            if (prev < 0) null
            else haystack.indexOf(needle, prev + 1).takeIf { it >= 0 }
        }
        if (containing == null) return occurrences.firstOrNull() ?: -1

        var fallback = -1
        for (idx in occurrences) {
            if (fallback < 0) fallback = idx
            if (idx <= containing.first && containing.last < idx + needle.length) return idx
        }
        return fallback
    }
}
