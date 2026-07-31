package com.noom.interview.fullstack.sleep.web

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepAverages
import com.noom.interview.fullstack.sleep.domain.SleepLog
import java.time.Instant
import java.time.LocalTime

/**
 * Only the interval and the feeling are sent. The date of the sleep is not a
 * separate field: it is carried by the interval, so the two can never disagree.
 */
data class CreateSleepLogRequest(
    val bedStart: Instant,
    val bedEnd: Instant,
    val morningFeeling: MorningFeeling
)

/**
 * Times go out as ISO-8601 instants, never as formatted calendar dates:
 * resolving a date needs the caller's timezone, which this API deliberately
 * does not take. Clients render local values.
 */
data class SleepLogResponse(
    val id: Long,
    val bedStart: Instant,
    val bedEnd: Instant,
    val totalMinutesInBed: Long,
    val morningFeeling: MorningFeeling
) {
    companion object {
        fun from(sleepLog: SleepLog) = SleepLogResponse(
            id = sleepLog.id,
            bedStart = sleepLog.bedStart,
            bedEnd = sleepLog.bedEnd,
            totalMinutesInBed = sleepLog.timeInBed.toMinutes(),
            morningFeeling = sleepLog.morningFeeling
        )
    }
}

/**
 * Average bedtime and wake time are times of day, not instants - they have no
 * date. They are reported in UTC; a client shifts them to its own offset, which
 * is valid because a circular mean is rotation-equivariant.
 */
data class SleepAveragesResponse(
    val from: Instant,
    val to: Instant,
    val sleepCount: Int,
    val averageMinutesInBed: Long?,
    val averageBedStartUtc: LocalTime?,
    val averageBedEndUtc: LocalTime?,
    val feelingFrequencies: Map<MorningFeeling, Int>
) {
    companion object {
        fun from(averages: SleepAverages) = SleepAveragesResponse(
            from = averages.from,
            to = averages.to,
            sleepCount = averages.sleepCount,
            averageMinutesInBed = averages.averageTimeInBed?.toMinutes(),
            averageBedStartUtc = averages.averageBedStart,
            averageBedEndUtc = averages.averageBedEnd,
            feelingFrequencies = averages.feelingFrequencies
        )
    }
}

data class ApiError(val message: String)
