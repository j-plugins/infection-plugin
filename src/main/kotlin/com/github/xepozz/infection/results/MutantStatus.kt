package com.github.xepozz.infection.results

enum class MutantStatus(val displayName: String) {
    KILLED("Killed"),
    ESCAPED("Escaped"),
    TIMED_OUT("Timed out"),
    NOT_COVERED("Not covered"),
    ERROR("Error"),
    UNKNOWN("Unknown");

    /**
     * Whether this status is worth surfacing in the editor gutter.
     *
     * Killed mutants confirm tests do their job — flagging every one would turn the gutter into
     * a wall of green icons on well-tested files. Coverage tooling already shows that signal.
     * Only statuses that hint at a problem (or absence of coverage) get a marker.
     */
    val isInteresting: Boolean
        get() = when (this) {
            KILLED, UNKNOWN -> false
            ESCAPED, TIMED_OUT, NOT_COVERED, ERROR -> true
        }

    companion object {
        fun fromTeamCity(messageName: String?, hasFailure: Boolean): MutantStatus = when {
            messageName == "testIgnored" -> NOT_COVERED
            hasFailure -> ESCAPED
            messageName == "testFinished" -> KILLED
            else -> UNKNOWN
        }
    }
}
