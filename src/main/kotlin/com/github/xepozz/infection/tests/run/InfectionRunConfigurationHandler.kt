package com.github.xepozz.infection.tests.run

import com.github.xepozz.infection.config.StaticAnalyzerOptions
import com.github.xepozz.infection.config.TestingFrameworkOptions
import com.github.xepozz.infection.tryRelativeTo
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.php.config.commandLine.PhpCommandSettings
import com.jetbrains.php.testFramework.run.PhpTestRunConfigurationHandler
import java.nio.file.Path
import kotlin.io.path.relativeTo

class InfectionRunConfigurationHandler : PhpTestRunConfigurationHandler {
    companion object {
        @JvmField
        val INSTANCE = InfectionRunConfigurationHandler()
    }

    override fun getConfigFileOption() = "--configuration"

    override fun prepareCommand(
        project: Project,
        commandSettings: PhpCommandSettings,
        executable: String,
        version: String?,
    ) {
        prepareCommand(project, commandSettings, executable, version, "run")
    }

    fun prepareCommand(
        project: Project,
        commandSettings: PhpCommandSettings,
        executable: String,
        version: String?,
        command: String,
    ) {
        commandSettings.apply {
            val executable = executable.tryRelativeTo(commandSettings.workingDirectory)

            setScript(executable, true)
            addArgument(command)
        }
        println("commandSettings: $commandSettings")
    }

    override fun runType(
        project: Project,
        commandSettings: PhpCommandSettings,
        type: String,
        workingDirectory: String
    ) {
        println("runType: $type, $workingDirectory")
    }

    override fun runDirectory(
        project: Project,
        commandSettings: PhpCommandSettings,
        directory: String,
        workingDirectory: String
    ) {
        println("runDirectory: $directory")
        if (directory.isEmpty()) return

        commandSettings.apply {
            addArgument("--filter")
            addPathArgument(directory)
        }
    }

    override fun runFile(
        project: Project,
        commandSettings: PhpCommandSettings,
        file: String,
        workingDirectory: String
    ) {
        println("runFile: $file")
        if (file.isEmpty()) return

        val path = Path.of(file)
        val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(path)
        if (virtualFile == null) {
            println("File not found: $file")
            return
        }

        val workingDirectory = Path.of(workingDirectory)
        val relativePath = virtualFile.toNioPath().relativeTo(workingDirectory)

        val fileIndex = ProjectFileIndex.getInstance(project)

        if (fileIndex.isInTestSourceContent(virtualFile)) {
            commandSettings.apply {
                addArgument("--test-framework-option")
                addPathArgument(relativePath.toString())
            }
            return
        }

        if (fileIndex.isInSourceContent(virtualFile)) {
            commandSettings.apply {
                addArgument("--filter")
                addPathArgument(relativePath.toString())
                addArgument("--map-source-class-to-test")
            }
        }

    }

    override fun runMethod(
        project: Project,
        commandSettings: PhpCommandSettings,
        file: String,
        methodName: String,
        workingDirectory: String
    ) {
        println("runMethod: $file, $methodName")
        if (file.isEmpty()) return

        commandSettings.apply {
            addArgument("--test-framework-options=filter=$methodName")
        }
    }

    fun prepareArguments(arguments: MutableList<String?>, infectionSettings: InfectionRunConfigurationSettings) {
        val runnerSettings = infectionSettings.runnerSettings

        if (runnerSettings.staticAnalyzer != StaticAnalyzerOptions.AUTO) {
            arguments.add("--static-analysis-tool=${runnerSettings.staticAnalyzer.value}")
        }
        if (runnerSettings.staticAnalyzerOptions.isNotEmpty()) {
            arguments.add("--static-analysis-tool-options=${runnerSettings.staticAnalyzerOptions}")
        }
        if (runnerSettings.testingFramework != TestingFrameworkOptions.AUTO) {
            arguments.add("--test-framework=${runnerSettings.testingFramework.value}")
        }
        if (runnerSettings.testingFrameworkOptions.isNotEmpty()) {
            arguments.add("--test-framework-options=${runnerSettings.testingFrameworkOptions}")
        }
    }

    fun prepareEnv(env: MutableMap<String?, String?>, withDebugger: Boolean) {
        if (withDebugger) {
            if (env is HashMap) {
                env["INFECTION_ALLOW_XDEBUG"] = "1"
            }
        }
    }
}