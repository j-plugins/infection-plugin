package com.github.xepozz.infection.config

import com.github.xepozz.infection.InfectionBundle

enum class TestingFrameworkOptions(private val titleKey: String?, private val rawTitle: String?, val value: String) {
    AUTO("option.auto", null, ""),
    PHPUNIT(null, "PHPUnit", "phpunit"),
    CODECEPTION(null, "Codeception", "codeception"),
    PHPSPEC(null, "PHPSpec", "phpspec");

    val title: String
        get() = titleKey?.let(InfectionBundle::message) ?: rawTitle.orEmpty()
}