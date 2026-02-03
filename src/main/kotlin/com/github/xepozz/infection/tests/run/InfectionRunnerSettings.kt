package com.github.xepozz.infection.tests.run

import com.github.xepozz.infection.config.StaticAnalyzerOptions
import com.github.xepozz.infection.config.TestingFrameworkOptions
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.jetbrains.php.phpunit.coverage.PhpUnitCoverageEngine.CoverageEngine
import com.jetbrains.php.testFramework.run.PhpTestRunnerSettings

@Tag("InfectionRunnerSettings")
class InfectionRunnerSettings : PhpTestRunnerSettings() {
    @Attribute("static_analyzer")
    var staticAnalyzer: StaticAnalyzerOptions = StaticAnalyzerOptions.AUTO
    @Attribute("static_analyzer_options")
    var staticAnalyzerOptions: String = ""
    @Attribute("testing_framework")
    var testingFramework: TestingFrameworkOptions = TestingFrameworkOptions.AUTO
    @Attribute("testing_framework_options")
    var testingFrameworkOptions: String = ""
    var coverageEngine: CoverageEngine = CoverageEngine.XDEBUG
    var parallelTestingEnabled: Boolean = false

    companion object {
        @JvmStatic
        fun fromPhpTestRunnerSettings(settings: PhpTestRunnerSettings): InfectionRunnerSettings {
            val infectionSettings = InfectionRunnerSettings()

            infectionSettings.scope = settings.scope
            infectionSettings.selectedType = settings.selectedType
            infectionSettings.directoryPath = settings.directoryPath
            infectionSettings.filePath = settings.filePath
            infectionSettings.methodName = settings.methodName
            infectionSettings.isUseAlternativeConfigurationFile = settings.isUseAlternativeConfigurationFile
            infectionSettings.configurationFilePath = settings.configurationFilePath
            infectionSettings.testRunnerOptions = settings.testRunnerOptions
            if (settings is InfectionRunnerSettings) {
                infectionSettings.staticAnalyzer = settings.staticAnalyzer
                infectionSettings.staticAnalyzerOptions = settings.staticAnalyzerOptions
                infectionSettings.testingFramework = settings.testingFramework
                infectionSettings.testingFrameworkOptions = settings.testingFrameworkOptions
            }

            return infectionSettings
        }
    }
}