package com.github.xepozz.infection.results

import com.intellij.util.xmlb.annotations.XCollection

class MutationResultsState {
    @XCollection(propertyElementName = "mutants", elementName = "mutant")
    var mutants: MutableList<MutantRecord> = mutableListOf()

    var lastRunTimestamp: Long = 0L
}
