package com.github.xepozz.infection.tests.metainfo

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges the SMTRunner output converter (which has rich Infection data: mutator name, original
 * and mutated code) and the test events listener (which has direct SMTestProxy references).
 *
 * The converter stashes attribute maps keyed by the SM test name; the listener applies them once
 * the proxy reaches a terminal state and removes the entry. Names produced by Infection's reporter
 * include the mutation hash, so collisions inside one run are not realistic.
 */
@Service(Service.Level.PROJECT)
class InfectionMutationMetainfoStore {

    private val byTestName = ConcurrentHashMap<String, Map<String, String>>()

    fun put(testName: String, attributes: Map<String, String>) {
        if (attributes.isEmpty()) byTestName.remove(testName) else byTestName[testName] = attributes
    }

    fun consume(testName: String): Map<String, String>? = byTestName.remove(testName)

    fun clear() {
        byTestName.clear()
    }

    companion object {
        fun getInstance(project: Project): InfectionMutationMetainfoStore =
            project.getService(InfectionMutationMetainfoStore::class.java)
    }
}
