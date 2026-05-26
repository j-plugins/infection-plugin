package com.github.xepozz.infection.tests.metainfo

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

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
