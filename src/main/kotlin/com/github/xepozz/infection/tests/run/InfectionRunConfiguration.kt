package com.github.xepozz.infection.tests.run

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.tests.InfectionConsoleProperties
import com.github.xepozz.infection.tests.InfectionFrameworkType
import com.github.xepozz.infection.tryRelativeTo
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.containers.enumMapOf
import com.jetbrains.php.PhpBundle
import com.jetbrains.php.config.commandLine.PhpCommandLinePathProcessor
import com.jetbrains.php.config.commandLine.PhpCommandSettings
import com.jetbrains.php.config.commandLine.PhpCommandSettingsBuilder
import com.jetbrains.php.config.interpreters.PhpInterpreter
import com.jetbrains.php.run.PhpAsyncRunConfiguration
import com.jetbrains.php.run.remote.PhpRemoteInterpreterManager
import com.jetbrains.php.testFramework.PhpTestFrameworkConfiguration
import com.jetbrains.php.testFramework.run.PhpTestRunConfiguration
import com.jetbrains.php.testFramework.run.PhpTestRunConfigurationEditor
import com.jetbrains.php.testFramework.run.PhpTestRunConfigurationHandler
import com.jetbrains.php.testFramework.run.PhpTestRunnerConfigurationEditor
import com.jetbrains.php.testFramework.run.PhpTestRunnerSettings
import org.jetbrains.annotations.Nls
import java.io.File
import java.util.*

class InfectionRunConfiguration(project: Project, factory: ConfigurationFactory) : PhpTestRunConfiguration(
    project,
    factory,
    InfectionBundle.message("infection.local.run.display.name"),
    InfectionFrameworkType.INSTANCE,
    InfectionTestRunnerSettingsValidator,
    InfectionRunConfigurationHandler.INSTANCE,
), PhpAsyncRunConfiguration {
    val myHandler = InfectionRunConfigurationHandler.INSTANCE
    val infectionSettings
        get() = settings as InfectionRunConfigurationSettings

    override fun createMethodFieldCompletionProvider(editor: PhpTestRunnerConfigurationEditor): TextFieldCompletionProvider {
        println("createMethodFieldCompletionProvider $editor")
        return object : TextFieldCompletionProvider() {
            override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
                println("addCompletionVariants: $text, $offset, $prefix")
            }
        }
    }

    override fun createCommand(
        interpreter: PhpInterpreter,
        env: Map<String?, String?>,
        arguments: List<String?>,
        withDebugger: Boolean
    ): PhpCommandSettings {


        println("arguments: $arguments")
        return super.createCommand(interpreter, env, arguments, withDebugger)
    }

    override fun createCommand(
        interpreter: PhpInterpreter,
        env: MutableMap<String?, String?>,
        arguments: MutableList<String?>,
        frameworkConfig: PhpTestFrameworkConfiguration?,
        withDebugger: Boolean
    ): PhpCommandSettings {
        val command = PhpCommandSettingsBuilder(project, interpreter)
            .loadAndStartDebug(withDebugger)
            .build()
        val executablePath = frameworkConfig?.executablePath
        if (frameworkConfig == null || executablePath.isNullOrEmpty()) {
            throw ExecutionException(
                PhpBundle.message(
                    "php.interpreter.base.configuration.is.not.provided.or.empty",
                    frameworkName,
                    if (command.isRemote) "'${interpreter.name}' interpreter" else "local machine",
                )
            )
        }

        val workingDirectory = getWorkingDirectory(project, settings, frameworkConfig)
        if (workingDirectory.isNullOrEmpty()) {
            throw ExecutionException(PhpBundle.message("php.interpreter.base.configuration.working.directory"))
        }
        command.setWorkingDir(workingDirectory)
        infectionSettings.workingDirectory = workingDirectory

//        println("envs: ${env.entries} widthDebugger: $withDebugger")
        myHandler.prepareArguments(arguments, infectionSettings)
        myHandler.prepareEnv(env, withDebugger)
        myHandler.prepareCommand(project, command, executablePath, null)

        command.importCommandLineSettings(settings.commandLineSettings, workingDirectory)
        command.addEnvs(env)

        fillTestRunnerArguments(
            project,
            workingDirectory,
            settings.runnerSettings,
            arguments,
            command,
            frameworkConfig,
            myHandler
        )
        return command
    }

    @Throws(ExecutionException::class)

    override fun createSettings() = InfectionRunConfigurationSettings()

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        val names = enumMapOf<PhpTestRunnerSettings.Scope, String>()
        names[PhpTestRunnerSettings.Scope.ConfigurationFile] = "Configuration File"

        val editor = getConfigurationEditor(names)
        editor.setSupportedTypes(mutableListOf("All", "acceptance", "functional", "unit"))
        editor.setRunnerOptionsDocumentation("https://infection.github.io/guide/command-line-options.html")

//        return this.addExtensionEditor(editor)!!
        return InfectionTestRunConfigurationEditor(editor, this)
    }

    override fun getConfigurationEditor(scopes: Map<PhpTestRunnerSettings.Scope, @Nls String>): PhpTestRunConfigurationEditor {
        val configurationEditor = super.getConfigurationEditor(scopes)
        return configurationEditor
    }

    override fun getRunnerSettingsEditor(runner: ProgramRunner<*>?): SettingsEditor<ConfigurationPerRunnerSettings?>? {
        val runnerSettingsEditor = super.getRunnerSettingsEditor(runner)
        return runnerSettingsEditor
    }

    override fun createProcessHandler(
        project: Project,
        command: PhpCommandSettings,
        withPty: Boolean,
        commandLine: GeneralCommandLine?
    ): ProcessHandler? {
        val createProcessHandler = super.createProcessHandler(project, command, withPty, commandLine)
        createProcessHandler.addProcessListener(object : ProcessListener {
            override fun startNotified(event: ProcessEvent) {
                commandLine
                println("startNotified $event")
            }
        })
        return createProcessHandler
    }

    override fun addExtensionEditor(testEditor: PhpTestRunConfigurationEditor?): SettingsEditor<out RunConfiguration?>? {
        return super.addExtensionEditor(testEditor)
    }
//
//    override fun getWorkingDirectory(
//        project: Project,
//        settings: PhpTestRunConfigurationSettings,
//        config: PhpTestFrameworkConfiguration?
//    ) = project.basePath + "/abc"


    override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties {
        val manager = PhpRemoteInterpreterManager.getInstance()

        val pathProcessor = when (interpreter?.isRemote) {
            true -> manager?.createPathMapper(project, interpreter!!.phpSdkAdditionalData)
            else -> null
        } ?: PhpCommandLinePathProcessor.LOCAL

        val pathMapper = pathProcessor.createPathMapper(this.project)

        return InfectionConsoleProperties(
            this,
            executor,
            pathMapper,
//            InfectionLocationProvider(pathMapper, this.project, this.getConfigurationFileRootPath())
        )
    }

    companion object {
        const val ID = "InfectionConsoleCommandRunConfiguration"

        private fun fillTestRunnerArguments(
            project: Project,
            workingDirectory: String,
            testRunnerSettings: PhpTestRunnerSettings,
            arguments: MutableList<String?>,
            command: PhpCommandSettings,
            configuration: PhpTestFrameworkConfiguration?,
            handler: PhpTestRunConfigurationHandler
        ) {
            val testRunnerOptions = testRunnerSettings.testRunnerOptions
            if (StringUtil.isNotEmpty(testRunnerOptions)) {
                command.addArguments(ParametersList.parse(testRunnerOptions!!).toList())
            }

            command.addArguments(arguments)
            val configurationFilePath = getConfigurationFile(testRunnerSettings, configuration)
            if (!configurationFilePath.isNullOrEmpty()) {
                command.addArgument(handler.configFileOption)
                val relativeConfigPath = configurationFilePath.tryRelativeTo(workingDirectory)
                command.addPathArgument(relativeConfigPath)
            }
//        TODO: we don't use scopes?
//        val scope = testRunnerSettings.scope
//        when (scope) {
//            PhpTestRunnerSettings.Scope.Type -> handler.runType(
//                project,
//                command,
//                StringUtil.notNullize(testRunnerSettings.selectedType),
//                workingDirectory
//            )
//
//            PhpTestRunnerSettings.Scope.Directory -> handler.runDirectory(
//                project,
//                command,
//                StringUtil.notNullize(testRunnerSettings.directoryPath),
//                workingDirectory
//            )
//
//            PhpTestRunnerSettings.Scope.File -> handler.runFile(
//                project,
//                command,
//                StringUtil.notNullize(testRunnerSettings.filePath),
//                workingDirectory
//            )
//
//            PhpTestRunnerSettings.Scope.Method -> {
//                val filePath = StringUtil.notNullize(testRunnerSettings.filePath)
//                handler.runMethod(project, command, filePath, testRunnerSettings.methodName, workingDirectory)
//            }
//
//            PhpTestRunnerSettings.Scope.ConfigurationFile -> {}
//        }
        }
    }
}
