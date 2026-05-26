package com.github.xepozz.infection.config

import com.github.xepozz.infection.InfectionBundle

enum class StaticAnalyzerOptions(private val titleKey: String?, private val rawTitle: String?, val value: String) {
    AUTO("option.auto", null, ""),
    PSALM(null, "Psalm", "psalm"),
    PHPSTAN(null, "PHP Stan", "phpstan"),
    MAGO(null, "Mago", "mago");

    val title: String
        get() = titleKey?.let(InfectionBundle::message) ?: rawTitle.orEmpty()
}