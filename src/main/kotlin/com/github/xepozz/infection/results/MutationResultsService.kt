package com.github.xepozz.infection.results

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
@State(name = "InfectionMutationResults", storages = [Storage("infection-results.xml")])
class MutationResultsService(private val project: Project) : PersistentStateComponent<MutationResultsState> {

    @Volatile
    private var state: MutationResultsState = MutationResultsState()

    private val byFile: MutableMap<String, List<MutantRecord>> = ConcurrentHashMap()
    private val byOffset: MutableMap<String, Map<Int, List<MutantRecord>>> = ConcurrentHashMap()

    @Volatile
    private var statsCache: MutationStats = MutationStats.EMPTY

    @Volatile
    var minMsiThreshold: Double? = null
        private set

    @Volatile
    var minCoveredMsiThreshold: Double? = null
        private set

    override fun getState(): MutationResultsState = state

    override fun loadState(loadedState: MutationResultsState) {
        state = loadedState
        rebuildIndexes()
        recomputeStats()
    }

    override fun noStateLoaded() {
        rebuildIndexes()
        recomputeStats()
    }

    fun getResultsFor(filePath: String): List<MutantRecord> =
        byFile[filePath] ?: emptyList()

    fun getRecordsAtOffset(filePath: String, startOffset: Int): List<MutantRecord> =
        byOffset[filePath]?.get(startOffset) ?: emptyList()

    fun allMutants(): List<MutantRecord> = state.mutants.toList()

    fun getProjectStats(): MutationStats = statsCache

    fun setThresholds(minMsi: Double?, minCoveredMsi: Double?) {
        minMsiThreshold = minMsi
        minCoveredMsiThreshold = minCoveredMsi
        recomputeStats()
        notifyListeners()
    }

    fun replaceResults(records: Collection<MutantRecord>) {
        val timestamp = System.currentTimeMillis()
        val list = records.toMutableList()
        list.forEach {
            if (it.runTimestamp == 0L) it.runTimestamp = timestamp
        }
        state.mutants = list
        state.lastRunTimestamp = timestamp
        rebuildIndexes()
        recomputeStats()
        notifyListeners()
    }

    fun clear() {
        state.mutants = mutableListOf()
        state.lastRunTimestamp = 0L
        rebuildIndexes()
        recomputeStats()
        notifyListeners()
    }

    private fun rebuildIndexes() {
        byFile.clear()
        byOffset.clear()
        state.mutants
            .groupBy { it.filePath }
            .forEach { (path, list) ->
                byFile[path] = list
                byOffset[path] = list.groupBy { it.startOffset }
            }
    }

    private fun recomputeStats() {
        val base = MutationStats.from(state.mutants, state.lastRunTimestamp)
        statsCache = base.copy(
            minMsiThreshold = minMsiThreshold,
            minCoveredMsiThreshold = minCoveredMsiThreshold,
        )
    }

    private fun notifyListeners() {
        if (project.isDisposed) return
        project.messageBus
            .syncPublisher(MutationResultsListener.TOPIC)
            .onResultsChanged(statsCache)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MutationResultsService = project.service()
    }
}
