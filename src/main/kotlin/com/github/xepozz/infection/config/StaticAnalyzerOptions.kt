package com.github.xepozz.infection.config

import com.github.xepozz.infection.InfectionBundle

enum class StaticAnalyzerOptions(val value: String) {
    AUTO(""),
    PSALM("psalm"),
    PHPSTAN("phpstan"),
    MAGO("mago");

    val title: String
        get() = when (this) {
            AUTO -> InfectionBundle.message("option.auto")
            PSALM -> "Psalm"
            PHPSTAN -> "PHP Stan"
            MAGO -> "Mago"
        }
}
