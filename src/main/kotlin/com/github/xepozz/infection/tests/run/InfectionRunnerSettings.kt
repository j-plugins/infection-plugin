package com.github.xepozz.infection.tests.run

import com.github.xepozz.infection.staticAnalyzer.StaticAnalyzerOptions
import com.jetbrains.php.phpunit.coverage.PhpUnitCoverageEngine.CoverageEngine
import com.jetbrains.php.testFramework.run.PhpTestRunnerSettings

class InfectionRunnerSettings(
    var staticAnalyzer: StaticAnalyzerOptions = StaticAnalyzerOptions.NONE,
    var staticAnalyzerOptions: String = "",
    var coverageEngine: CoverageEngine = CoverageEngine.XDEBUG,
    var parallelTestingEnabled: Boolean = false,
) : PhpTestRunnerSettings() {
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

            return infectionSettings
        }
    }
}