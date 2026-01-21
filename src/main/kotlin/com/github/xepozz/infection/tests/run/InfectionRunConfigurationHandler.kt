package com.github.xepozz.infection.tests.run

import com.intellij.openapi.project.Project
import com.jetbrains.php.config.commandLine.PhpCommandSettings
import com.jetbrains.php.testFramework.run.PhpTestRunConfigurationHandler

class InfectionRunConfigurationHandler : PhpTestRunConfigurationHandler {
    companion object {
        @JvmField
        val INSTANCE = InfectionRunConfigurationHandler()
    }

    override fun getConfigFileOption() = "--configuration"

    override fun prepareCommand(project: Project, commandSettings: PhpCommandSettings, exe: String, version: String?) {
        commandSettings.apply {
            setScript(exe, false)
            addArgument("run")
//            addArgument("--no-progress")
//            addArgument("-n")
//            addArgument("-q")
//            addArgument("--logger-gitlab=php://stdout")
        }
        println("commandSettings: $commandSettings")
    }

    override fun runType(
        project: Project,
        phpCommandSettings: PhpCommandSettings,
        type: String,
        workingDirectory: String
    ) {
        println("runType: $type, $workingDirectory")
    }

    override fun runDirectory(
        project: Project,
        phpCommandSettings: PhpCommandSettings,
        directory: String,
        workingDirectory: String
    ) {
        println("runDirectory: $directory")
        if (!directory.isEmpty()) {
//            phpCommandSettings.addArgument("--filter=$directory")
        }
    }

    override fun runFile(
        project: Project,
        phpCommandSettings: PhpCommandSettings,
        file: String,
        workingDirectory: String
    ) {
        println("runFile: $file")
        if (!file.isEmpty()) {
            phpCommandSettings.addArgument("--filter=$file")
        }
    }

    override fun runMethod(
        project: Project,
        phpCommandSettings: PhpCommandSettings,
        file: String,
        methodName: String,
        workingDirectory: String
    ) {
        println("runMethod: $file, $methodName")
        if (file.isEmpty()) return

        phpCommandSettings.addArgument("--test-framework-options=filter=$methodName")
    }
}