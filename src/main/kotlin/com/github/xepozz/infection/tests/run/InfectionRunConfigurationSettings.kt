package com.github.xepozz.infection.tests.run

import com.jetbrains.php.testFramework.run.PhpTestRunConfigurationSettings
import com.jetbrains.php.testFramework.run.PhpTestRunnerSettings

class InfectionRunConfigurationSettings : PhpTestRunConfigurationSettings() {
    override fun createDefault() = InfectionRunnerSettings().apply {
        testRunnerOptions = "-q -n --no-progress --logger-gitlab=php://stdout"
    }

    override fun getRunnerSettings() = getInfectionRunnerSettings()

    override fun setRunnerSettings(runnerSettings: PhpTestRunnerSettings) {
        super.setRunnerSettings(InfectionRunnerSettings.fromPhpTestRunnerSettings(runnerSettings))
    }

    fun getInfectionRunnerSettings(): InfectionRunnerSettings {
        val settings = super.getRunnerSettings()
        if (settings is InfectionRunnerSettings) {
            return settings
        } else {
            val copy = InfectionRunnerSettings.fromPhpTestRunnerSettings(settings)
            this.setInfectionRunnerSettings(copy)
            return copy
        }
    }

    fun setInfectionRunnerSettings(runnerSettings: InfectionRunnerSettings) {
        this.setRunnerSettings(runnerSettings)
    }
}