package com.github.xepozz.infection.config

enum class TestingFrameworkOptions(val title: String, val value: String) {
    AUTO("Auto", ""),
    PHPUNIT("PHPUnit", "PHPUNIT"),
    CODECEPTION("Codeception", "codeception"),
    PHPSPEC("PHPSpec", "phpspec"),
}