package com.github.xepozz.infection

import com.intellij.openapi.project.Project
import com.jetbrains.php.config.interpreters.PhpInterpretersStateListener

class InfectionInterpreterStateListener : PhpInterpretersStateListener {
    override fun onInterpretersUpdate(project: Project) {
    }
}
