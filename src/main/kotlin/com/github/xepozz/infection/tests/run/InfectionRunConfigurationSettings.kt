package com.github.xepozz.infection.tests.run

import com.jetbrains.php.testFramework.run.PhpTestRunConfigurationSettings
import com.jetbrains.php.testFramework.run.PhpTestRunnerSettings

class InfectionRunConfigurationSettings : PhpTestRunConfigurationSettings() {
    override fun createDefault() = InfectionRunnerSettings().apply {
        testRunnerOptions = "-n --no-progress --teamcity"
    }

    override fun getRunnerSettings() = getInfectionRunnerSettings()

    override fun setRunnerSettings(runnerSettings: PhpTestRunnerSettings) {
        super.setRunnerSettings(InfectionRunnerSettings.fromPhpTestRunnerSettings(runnerSettings))
    }

    fun getInfectionRunnerSettings(): InfectionRunnerSettings {
        val settings = super.getRunnerSettings()
        if (settings is InfectionRunnerSettings) {
            return settings
        }

        val copy = InfectionRunnerSettings.fromPhpTestRunnerSettings(settings)
        setInfectionRunnerSettings(copy)
        return copy
    }

    fun setInfectionRunnerSettings(runnerSettings: InfectionRunnerSettings) {
        setRunnerSettings(runnerSettings)
    }
}