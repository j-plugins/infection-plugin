package com.github.xepozz.infection.tests.run

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.tests.InfectionConsoleProperties
import com.github.xepozz.infection.tests.InfectionFrameworkType
import com.github.xepozz.infection.tests.actions.RerunEscapedMutantsAction
import com.github.xepozz.infection.tryRelativeTo
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
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
import com.jetbrains.php.testFramework.run.PhpTestRunConfigurationHandler
import com.jetbrains.php.testFramework.run.PhpTestRunnerConfigurationEditor
import com.jetbrains.php.testFramework.run.PhpTestRunnerSettings

class InfectionRunConfiguration(project: Project, factory: ConfigurationFactory) : PhpTestRunConfiguration(
    project,
    factory,
    InfectionBundle.message("infection.local.run.display.name"),
    InfectionFrameworkType.INSTANCE,
    InfectionTestRunnerSettingsValidator,
    InfectionRunConfigurationHandler.INSTANCE,
), PhpAsyncRunConfiguration {

    val infectionSettings: InfectionRunConfigurationSettings
        get() = settings as InfectionRunConfigurationSettings

    override fun createMethodFieldCompletionProvider(
        editor: PhpTestRunnerConfigurationEditor,
    ): TextFieldCompletionProvider = EmptyMethodCompletionProvider

    override fun createCommand(
        interpreter: PhpInterpreter,
        env: MutableMap<String?, String?>,
        arguments: MutableList<String?>,
        frameworkConfig: PhpTestFrameworkConfiguration?,
        withDebugger: Boolean,
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

        logger.debug { "envs: ${env.entries} withDebugger: $withDebugger" }
        val handler = InfectionRunConfigurationHandler.INSTANCE
        handler.prepareArguments(arguments, infectionSettings)
        handler.prepareEnv(env, withDebugger)
        handler.prepareCommand(project, command, executablePath, null, infectionSettings.runnerSettings.command)

        command.importCommandLineSettings(settings.commandLineSettings, workingDirectory)
        command.addEnvs(env)

        fillTestRunnerArguments(
            project,
            workingDirectory,
            settings.runnerSettings,
            arguments,
            command,
            frameworkConfig,
            handler
        )
        return command
    }

    override fun createSettings() = InfectionRunConfigurationSettings()

    override fun createRerunAction(
        consoleView: ConsoleView,
        properties: SMTRunnerConsoleProperties,
    ): AbstractRerunFailedTestsAction = RerunEscapedMutantsAction(consoleView, properties)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        val names = enumMapOf<PhpTestRunnerSettings.Scope, String>()
        names[PhpTestRunnerSettings.Scope.ConfigurationFile] = "Configuration File"

        val editor = getConfigurationEditor(names)
        editor.setRunnerOptionsDocumentation("https://infection.github.io/guide/command-line-options.html")

        return InfectionTestRunConfigurationEditor(editor, this)
    }

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
        )
    }

    private object EmptyMethodCompletionProvider : TextFieldCompletionProvider() {
        override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) = Unit
    }

    companion object {
        const val ID = "InfectionConsoleCommandRunConfiguration"

        private val logger = Logger.getInstance(InfectionRunConfiguration::class.java)

        private fun fillTestRunnerArguments(
            project: Project,
            workingDirectory: String,
            testRunnerSettings: PhpTestRunnerSettings,
            arguments: MutableList<String?>,
            command: PhpCommandSettings,
            configuration: PhpTestFrameworkConfiguration?,
            handler: PhpTestRunConfigurationHandler,
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

            when (testRunnerSettings.scope) {
                PhpTestRunnerSettings.Scope.Type -> handler.runType(
                    project,
                    command,
                    StringUtil.notNullize(testRunnerSettings.selectedType),
                    workingDirectory,
                )

                PhpTestRunnerSettings.Scope.Directory -> handler.runDirectory(
                    project,
                    command,
                    StringUtil.notNullize(testRunnerSettings.directoryPath),
                    workingDirectory,
                )

                PhpTestRunnerSettings.Scope.File -> handler.runFile(
                    project,
                    command,
                    StringUtil.notNullize(testRunnerSettings.filePath),
                    workingDirectory,
                )

                PhpTestRunnerSettings.Scope.Method -> handler.runMethod(
                    project,
                    command,
                    StringUtil.notNullize(testRunnerSettings.filePath),
                    testRunnerSettings.methodName,
                    workingDirectory,
                )

                PhpTestRunnerSettings.Scope.ConfigurationFile -> Unit
            }
        }
    }
}
