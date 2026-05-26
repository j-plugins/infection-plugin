package com.github.xepozz.infection.tests

/**
 * Tiny parser for the unified diff Infection emits in the `details` attribute of `testFinished` /
 * `testFailed` messages (the output of `MutantExecutionResult::getMutantDiff()`).
 *
 * Infection's diff looks like:
 *
 * ```
 * --- Original
 * +++ New
 * @@ @@
 *      *     protected $size;
 *
 *      *     protected $age;
 *
 * -    public function __construct($species, $sex, $size, $age = 0)
 * +    public function __construct($species, $sex, $size, $age)
 *      {
 * ```
 *
 * Infection doesn't put real line numbers in the hunk header (`@@ @@` rather than `@@ -10,3 +10,3 @@`),
 * so we don't try to recover them from the header. We return original/mutated **line groups** as the
 * caller can match them against the file text instead.
 */
object UnifiedDiffParser {

    data class ParsedDiff(
        val originalLines: List<String>,
        val mutatedLines: List<String>,
        /** Lines surrounding the change, used to disambiguate when the snippet occurs multiple times. */
        val contextLines: List<String>,
        /** Hunk header start line (1-based) if present, otherwise null. */
        val hunkStartLine: Int?,
    )

    private val hunkHeader = Regex("""^@@ (?:-(\d+)(?:,\d+)? \+\d+(?:,\d+)? )?@@.*$""")

    fun parse(diff: String?): ParsedDiff? {
        if (diff.isNullOrBlank()) return null

        val originals = mutableListOf<String>()
        val mutated = mutableListOf<String>()
        val context = mutableListOf<String>()
        var hunkStartLine: Int? = null
        var sawHunk = false

        for (raw in diff.lineSequence()) {
            val line = raw
            when {
                line.startsWith("---") || line.startsWith("+++") -> Unit
                line.startsWith("@@") -> {
                    sawHunk = true
                    hunkHeader.matchEntire(line)?.let { m ->
                        m.groupValues.getOrNull(1)?.toIntOrNull()?.let { hunkStartLine = it }
                    }
                }
                !sawHunk -> Unit
                line.startsWith("-") && !line.startsWith("---") -> originals.add(line.substring(1))
                line.startsWith("+") && !line.startsWith("+++") -> mutated.add(line.substring(1))
                line.startsWith(" ") -> context.add(line.substring(1))
                else -> Unit
            }
        }

        if (originals.isEmpty() && mutated.isEmpty() && context.isEmpty()) return null
        return ParsedDiff(originals, mutated, context, hunkStartLine)
    }

    /**
     * Apply the parsed diff to [originalFileText] to produce the mutated version of the whole file.
     * Returns null when the original lines can't be located uniquely (file changed since the run).
     */
    fun applyToFile(originalFileText: String, parsed: ParsedDiff): MutatedFile? {
        if (parsed.originalLines.isEmpty() && parsed.mutatedLines.isEmpty()) return null
        val lineSeparator = detectLineSeparator(originalFileText)
        val fileLines = originalFileText.split('\n').let {
            // Preserve trailing empty line so reconstruction is byte-faithful when the file ends with \n.
            it
        }.toMutableList()
        // Strip CR if originals were split by '\n' but file uses CRLF.
        val normalized = fileLines.map { if (it.endsWith("\r")) it.dropLast(1) else it }

        // Find the original block in the file. Prefer the occurrence whose surrounding context matches.
        val originalBlock = parsed.originalLines
        val matchStart = if (originalBlock.isEmpty()) {
            // Pure insertion — anchor on context if available.
            if (parsed.contextLines.isNotEmpty()) findBlock(normalized, parsed.contextLines) else -1
        } else {
            findBlock(normalized, originalBlock)
        }
        if (matchStart < 0) return null

        val newLines = normalized.toMutableList()
        if (originalBlock.isNotEmpty()) {
            for (i in originalBlock.indices) newLines.removeAt(matchStart)
        }
        newLines.addAll(matchStart, parsed.mutatedLines)

        val mutatedText = newLines.joinToString(lineSeparator)
        return MutatedFile(
            mutatedText = mutatedText,
            originalLineNumber = matchStart + 1,
            mutatedLineCount = parsed.mutatedLines.size,
            replacedLineCount = originalBlock.size,
        )
    }

    data class MutatedFile(
        val mutatedText: String,
        /** 1-based line number in the original file where the change begins. */
        val originalLineNumber: Int,
        val mutatedLineCount: Int,
        val replacedLineCount: Int,
    )

    private fun findBlock(haystack: List<String>, needle: List<String>): Int {
        if (needle.isEmpty()) return -1
        if (needle.size > haystack.size) return -1
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun detectLineSeparator(text: String): String {
        val crlf = text.indexOf("\r\n")
        if (crlf >= 0) return "\r\n"
        return "\n"
    }
}
