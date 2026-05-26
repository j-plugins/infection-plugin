package com.github.xepozz.infection

import com.intellij.openapi.util.IconLoader

// https://intellij-icons.jetbrains.design
// https://plugins.jetbrains.com/docs/intellij/icons.html#new-ui-tool-window-icons
// https://plugins.jetbrains.com/docs/intellij/icons-style.html
object InfectionIcons {
    @JvmField
    val INFECTION = IconLoader.getIcon("/icons/infection/icon.svg", this::class.java)

    @JvmField
    val MUTANT_ESCAPED = IconLoader.getIcon("/icons/infection/gutter/escaped.svg", this::class.java)

    @JvmField
    val MUTANT_KILLED = IconLoader.getIcon("/icons/infection/gutter/killed.svg", this::class.java)

    @JvmField
    val MUTANT_TIMEOUT = IconLoader.getIcon("/icons/infection/gutter/timeout.svg", this::class.java)

    @JvmField
    val MUTANT_NOT_COVERED = IconLoader.getIcon("/icons/infection/gutter/notCovered.svg", this::class.java)

    @JvmField
    val MUTANT_ERROR = IconLoader.getIcon("/icons/infection/gutter/error.svg", this::class.java)
}