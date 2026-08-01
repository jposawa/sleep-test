package com.noom.interview.fullstack.sleep.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/**
 * Summary of the sleeps a user logged inside a window.
 *
 * Averages are null when the window holds no sleeps: there is nothing to
 * average, and reporting zero would claim the user slept no time at all.
 */
data class SleepAverages(
    val from: Instant,
    val to: Instant,
    val sleepCount: Int,
    val averageTimeInBed: Duration?,
    val averageBedStart: LocalTime?,
    val averageBedEnd: LocalTime?,
    val feelingFrequencies: Map<MorningFeeling, Int>
) {
    companion object {
        fun of(from: Instant, to: Instant, sleepLogs: List<SleepLog>) = SleepAverages(
            from = from,
            to = to,
            sleepCount = sleepLogs.size,
            averageTimeInBed = sleepLogs.averageTimeInBed(),
            averageBedStart = TimeOfDayAverage.of(sleepLogs.map { it.bedStart }),
            averageBedEnd = TimeOfDayAverage.of(sleepLogs.map { it.bedEnd }),
            feelingFrequencies = sleepLogs.feelingFrequencies()
        )

        /** Truncated to the second, which the API reports in whole minutes anyway. */
        private fun List<SleepLog>.averageTimeInBed(): Duration? =
            if (isEmpty()) null else Duration.ofSeconds(sumOf { it.timeInBed.seconds } / size)

        /** Every feeling is reported, including the ones that never occurred. */
        private fun List<SleepLog>.feelingFrequencies(): Map<MorningFeeling, Int> =
            MorningFeeling.values().associateWith { feeling ->
                count { it.morningFeeling == feeling }
            }
    }
}
