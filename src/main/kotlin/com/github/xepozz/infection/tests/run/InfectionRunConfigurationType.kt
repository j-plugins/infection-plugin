package com.github.xepozz.infection.tests.run

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.InfectionIcons
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class InfectionRunConfigurationType :
    SimpleConfigurationType(
        ID,
        InfectionBundle.message("infection.local.run.display.name"),
        null,
        NotNullLazyValue.createValue { InfectionIcons.INFECTION },
    ) {

    override fun createTemplateConfiguration(project: Project) =
        InfectionRunConfiguration(project, this)

    companion object {
        const val ID = "InfectionRunConfiguration"

        val INSTANCE
            get() = findConfigurationType(InfectionRunConfigurationType::class.java)
    }
}