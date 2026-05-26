package com.github.xepozz.infection.gutter

import com.github.xepozz.infection.results.MutationResultsListener
import com.github.xepozz.infection.results.MutationStats
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

@Service(Service.Level.PROJECT)
class MutationResultsDaemonRefresher(private val project: Project) {
    fun subscribe() {
        project.messageBus.connect().subscribe(
            MutationResultsListener.TOPIC,
            object : MutationResultsListener {
                override fun onResultsChanged(stats: MutationStats) {
                    ApplicationManager.getApplication().invokeLater {
                        if (!project.isDisposed) {
                            DaemonCodeAnalyzer.getInstance(project).restart()
                        }
                    }
                }
            }
        )
    }
}

class MutationResultsRefresherActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.getService(MutationResultsDaemonRefresher::class.java).subscribe()
    }
}
