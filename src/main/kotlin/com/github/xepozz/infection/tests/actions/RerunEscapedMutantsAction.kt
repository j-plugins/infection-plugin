package com.github.xepozz.infection.tests.actions

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.results.MutantRecord
import com.github.xepozz.infection.results.MutantStatus
import com.github.xepozz.infection.results.MutationResultsService
import com.github.xepozz.infection.tests.run.InfectionRunConfiguration
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentContainer
import java.nio.file.Path
import kotlin.io.path.relativeTo

class RerunEscapedMutantsAction(
    componentContainer: ComponentContainer,
    properties: SMTRunnerConsoleProperties?,
) : AbstractRerunFailedTestsAction(componentContainer) {

    init {
        this.init(properties)
        templatePresentation.text = InfectionBundle.message(
            "action.com.github.xepozz.infection.tests.actions.RerunEscapedMutantsAction.text"
        )
        templatePresentation.description = InfectionBundle.message(
            "action.com.github.xepozz.infection.tests.actions.RerunEscapedMutantsAction.description"
        )
    }

    override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile? {
        val profile = this.myConsoleProperties.configuration as? InfectionRunConfiguration ?: return null
        val project = profile.project
        val escaped = MutationResultsService.getInstance(project)
            .allMutants()
            .filter { it.status == MutantStatus.ESCAPED }
        if (escaped.isEmpty()) return null

        val filterArg = "--filter=" + buildFilterPaths(project, escaped).joinToString(",")

        return object : MyRunProfile(profile) {
            override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
                val source = peer as InfectionRunConfiguration
                val clone = source.clone() as InfectionRunConfiguration
                val rs = clone.infectionSettings.runnerSettings
                val current = rs.testRunnerOptions.orEmpty()
                rs.testRunnerOptions = if (current.isBlank()) filterArg else "$current $filterArg"
                return clone.getState(executor, env)
            }
        }
    }

    private fun buildFilterPaths(project: Project, escaped: List<MutantRecord>): List<String> {
        val basePath = project.basePath?.let(Path::of)
        return escaped.asSequence()
            .map { it.filePath }
            .distinct()
            .map { absolute ->
                if (basePath == null) absolute
                else runCatching { Path.of(absolute).relativeTo(basePath).toString() }
                    .getOrDefault(absolute)
            }
            .toList()
    }
}
