package com.github.xepozz.infection.statusbar

import com.github.xepozz.infection.results.MutationResultsService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import java.io.File

class InfectionMsiWidgetFactory : StatusBarWidgetFactory {

    private val logger = Logger.getInstance(InfectionMsiWidgetFactory::class.java)

    override fun getId(): String = InfectionMsiWidget.ID

    override fun getDisplayName(): String = "Infection MSI"

    override fun isAvailable(project: Project): Boolean {
        if (project.isDisposed) return false
        // Widget is useful once mutation data has been collected, even if the project drops the dep later.
        if (MutationResultsService.getInstance(project).getProjectStats().hasData) return true
        val base = project.basePath ?: return false
        val composer = File(base, "composer.json")
        if (!composer.isFile) return false
        return composerRequiresInfection(composer)
    }

    private fun composerRequiresInfection(file: File): Boolean = try {
        if (file.length() > MAX_COMPOSER_BYTES) false
        else file.readText().contains(INFECTION_PACKAGE)
    } catch (e: Exception) {
        logger.warn("[infection-msi-widget] failed to inspect ${file.path}", e)
        false
    }

    override fun createWidget(project: Project): StatusBarWidget = InfectionMsiWidget(project)

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    companion object {
        private const val INFECTION_PACKAGE = "infection/infection"
        private const val MAX_COMPOSER_BYTES = 1L * 1024 * 1024
    }
}
