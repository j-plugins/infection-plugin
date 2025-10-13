package com.github.xepozz.infection.tests.run

import com.github.xepozz.infection.staticAnalyzer.StaticAnalyzerOptions
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.php.testFramework.run.PhpTestRunConfigurationEditor

class InfectionTestRunConfigurationEditor(
    private val parentEditor: PhpTestRunConfigurationEditor,
    val configuration: InfectionRunConfiguration
) : SettingsEditor<InfectionRunConfiguration>() {
    private val myMainPanel = panel {
        val runnerSettings = configuration.infectionSettings.runnerSettings

        row {
            cell(parentEditor.component)
        }
        group("Static Analyzer") {
            row {
                comboBox(StaticAnalyzerOptions.entries.toList())
                    .label("Tool:")
                    .bindItem({ runnerSettings.staticAnalyzer }, { runnerSettings.staticAnalyzer = it!! })
            }
                .layout(RowLayout.PARENT_GRID)
                .rowComment($$"--static-analysis-tool=$value")

            row {
                textField()
                    .label("Options:")
                    .bindText(runnerSettings::staticAnalyzerOptions)
            }
                .layout(RowLayout.PARENT_GRID)
                .rowComment($$"--static-analysis-tool-options=$value")
        }
    }

    override fun createEditor() = myMainPanel

    override fun isSpecificallyModified() = myMainPanel.isModified() || parentEditor.isSpecificallyModified

    override fun resetEditorFrom(infectionRunConfiguration: InfectionRunConfiguration) {
        myMainPanel.reset()
        parentEditor.resetFrom(infectionRunConfiguration)
    }

    override fun applyEditorTo(infectionRunConfiguration: InfectionRunConfiguration) {
        parentEditor.applyTo(infectionRunConfiguration)
        myMainPanel.apply()
    }
}