package com.github.xepozz.infection.tests.run

import com.github.xepozz.infection.config.StaticAnalyzerOptions
import com.github.xepozz.infection.config.TestingFrameworkOptions
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.php.testFramework.run.PhpTestRunConfigurationEditor
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class InfectionTestRunConfigurationEditor(
    private val parentEditor: PhpTestRunConfigurationEditor,
    val configuration: InfectionRunConfiguration
) : SettingsEditor<InfectionRunConfiguration>() {
    private val testingFrameworkField = ComboBox(TestingFrameworkOptions.entries.toTypedArray())
    private val testingFrameworkOptionsField = JBTextField()
    private val staticAnalyzerField = ComboBox(StaticAnalyzerOptions.entries.toTypedArray())
    private val staticAnalyzerOptionsField = JBTextField()

    private val myMainPanel = panel {
        row {
            cell(parentEditor.component)
                .align(AlignX.FILL)
        }
        group("Testing Framework") {
            row {
                label("Framework")
                    .gap(RightGap.COLUMNS)
                cell(testingFrameworkField)
                    .align(AlignX.FILL)
            }
                .layout(RowLayout.PARENT_GRID)
                .rowComment($$"--test-framework=$value")

            row {
                label("Options")
                    .gap(RightGap.COLUMNS)
                cell(testingFrameworkOptionsField)
                    .align(AlignX.FILL)
            }
                .layout(RowLayout.PARENT_GRID)
                .rowComment($$"--test-framework-options=$value")
        }.layout(RowLayout.PARENT_GRID)
        group("Static Analyzer") {
            row {
                label("Analyzer")
                    .gap(RightGap.COLUMNS)
                cell(staticAnalyzerField)
                    .align(AlignX.FILL)
            }
                .layout(RowLayout.PARENT_GRID)
                .rowComment($$"--static-analysis-tool=$value")

            row {
                label("Options")
                    .gap(RightGap.COLUMNS)
                cell(staticAnalyzerOptionsField)
                    .align(AlignX.FILL)
            }
                .layout(RowLayout.PARENT_GRID)
                .rowComment($$"--static-analysis-tool-options=$value")
        }
    }

    init {
        val listener = { fireEditorStateChanged() }
        val documentAdapter = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = listener()
        }

        testingFrameworkField.addActionListener { listener() }
        testingFrameworkOptionsField.document.addDocumentListener(documentAdapter)
        staticAnalyzerField.addActionListener { listener() }
        staticAnalyzerOptionsField.document.addDocumentListener(documentAdapter)
    }

    override fun createEditor(): JComponent = myMainPanel

    override fun isSpecificallyModified(): Boolean =
        testingFrameworkField.selectedItem != configuration.infectionSettings.runnerSettings.testingFramework
                || testingFrameworkOptionsField.text != configuration.infectionSettings.runnerSettings.testingFrameworkOptions
                || staticAnalyzerField.selectedItem != configuration.infectionSettings.runnerSettings.staticAnalyzer
                || staticAnalyzerOptionsField.text != configuration.infectionSettings.runnerSettings.staticAnalyzerOptions
                || parentEditor.isSpecificallyModified

    override fun resetEditorFrom(infectionRunConfiguration: InfectionRunConfiguration) {
        val runnerSettings = infectionRunConfiguration.infectionSettings.runnerSettings
        testingFrameworkField.selectedItem = runnerSettings.testingFramework
        testingFrameworkOptionsField.text = runnerSettings.testingFrameworkOptions ?: ""
        staticAnalyzerField.selectedItem = runnerSettings.staticAnalyzer
        staticAnalyzerOptionsField.text = runnerSettings.staticAnalyzerOptions ?: ""

        parentEditor.resetFrom(infectionRunConfiguration)
    }

    override fun applyEditorTo(infectionRunConfiguration: InfectionRunConfiguration) {
        parentEditor.applyTo(infectionRunConfiguration)

        val runnerSettings = infectionRunConfiguration.infectionSettings.runnerSettings
        runnerSettings.testingFramework = testingFrameworkField.selectedItem as? TestingFrameworkOptions ?: TestingFrameworkOptions.AUTO
        runnerSettings.testingFrameworkOptions = testingFrameworkOptionsField.text
        runnerSettings.staticAnalyzer = staticAnalyzerField.selectedItem as? StaticAnalyzerOptions ?: StaticAnalyzerOptions.AUTO
        runnerSettings.staticAnalyzerOptions = staticAnalyzerOptionsField.text
    }
}