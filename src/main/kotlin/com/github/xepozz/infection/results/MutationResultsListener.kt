package com.github.xepozz.infection.results

import com.intellij.util.messages.Topic

interface MutationResultsListener {
    fun onResultsChanged(stats: MutationStats)

    companion object {
        @JvmField
        val TOPIC: Topic<MutationResultsListener> = Topic.create(
            "Infection mutation results",
            MutationResultsListener::class.java,
        )
    }
}
