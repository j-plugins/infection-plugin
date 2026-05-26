package com.github.xepozz.infection.config

import com.github.xepozz.infection.InfectionBundle

enum class TestingFrameworkOptions(val value: String) {
    AUTO(""),
    PHPUNIT("phpunit"),
    CODECEPTION("codeception"),
    PHPSPEC("phpspec");

    val title: String
        get() = when (this) {
            AUTO -> InfectionBundle.message("option.auto")
            PHPUNIT -> "PHPUnit"
            CODECEPTION -> "Codeception"
            PHPSPEC -> "PHPSpec"
        }
}
