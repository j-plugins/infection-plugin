package com.github.xepozz.infection.config

enum class StaticAnalyzerOptions(val title: String, val value: String) {
    AUTO("Auto", ""),
    PSALM("Psalm", "psalm"),
    PHPSTAN("PHP Stan", "phpstan"),
    MAGO("Mago", "mago"),
}