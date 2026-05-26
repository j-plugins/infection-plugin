package com.github.xepozz.infection.results

data class MutationStats(
    val totalMutants: Int,
    val killed: Int,
    val escaped: Int,
    val timedOut: Int,
    val notCovered: Int,
    val errors: Int,
    val msi: Double,
    val coveredMsi: Double,
    val runTimestamp: Long,
    val minMsiThreshold: Double? = null,
    val minCoveredMsiThreshold: Double? = null,
) {
    val hasData: Boolean get() = totalMutants > 0

    companion object {
        val EMPTY = MutationStats(
            totalMutants = 0,
            killed = 0, escaped = 0, timedOut = 0, notCovered = 0, errors = 0,
            msi = 0.0, coveredMsi = 0.0,
            runTimestamp = 0L,
        )

        fun from(records: Collection<MutantRecord>, runTimestamp: Long): MutationStats {
            if (records.isEmpty()) return EMPTY
            val total = records.size
            val killed = records.count { it.status == MutantStatus.KILLED }
            val escaped = records.count { it.status == MutantStatus.ESCAPED }
            val timedOut = records.count { it.status == MutantStatus.TIMED_OUT }
            val notCovered = records.count { it.status == MutantStatus.NOT_COVERED }
            val errors = records.count { it.status == MutantStatus.ERROR }

            val detectedTotal = killed + timedOut + errors
            val msi = if (total > 0) detectedTotal.toDouble() * 100.0 / total.toDouble() else 0.0
            val covered = total - notCovered
            val coveredMsi = if (covered > 0) detectedTotal.toDouble() * 100.0 / covered.toDouble() else 0.0

            return MutationStats(
                totalMutants = total,
                killed = killed, escaped = escaped,
                timedOut = timedOut, notCovered = notCovered, errors = errors,
                msi = msi, coveredMsi = coveredMsi,
                runTimestamp = runTimestamp,
            )
        }
    }
}
