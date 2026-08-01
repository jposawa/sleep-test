package com.noom.interview.fullstack.sleep.web

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepAverages
import com.noom.interview.fullstack.sleep.domain.SleepLog
import com.noom.interview.fullstack.sleep.service.InvalidSleepIntervalException
import java.time.Instant
import java.time.LocalTime
import javax.validation.constraints.AssertTrue

/**
 * Only the interval and the feeling are sent. The date of the sleep is not a
 * separate field: it is carried by the interval, so the two can never disagree.
 *
 * Absent or unparseable fields are already rejected by deserialisation, since
 * none of them is nullable; what needs declaring is the rule that spans two
 * fields.
 */
data class CreateSleepLogRequest(
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val bedStart: Instant,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val bedEnd: Instant,

    val morningFeeling: MorningFeeling
) {
    @JsonIgnore
    @AssertTrue(message = InvalidSleepIntervalException.INTERVAL_ORDERED_MESSAGE)
    fun isIntervalOrdered(): Boolean = bedEnd.isAfter(bedStart)
}

/**
 * Times go out as ISO-8601 instants, never as formatted calendar dates:
 * resolving a date needs the caller's timezone, which this API deliberately
 * does not take. Clients render local values.
 */
data class SleepLogResponse(
    val id: Long,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val bedStart: Instant,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
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
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val from: Instant,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val to: Instant,

    val sleepCount: Int,
    val averageMinutesInBed: Long?,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TIME_OF_DAY_FORMAT)
    val averageBedStartUtc: LocalTime?,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TIME_OF_DAY_FORMAT)
    val averageBedEndUtc: LocalTime?,

    val feelingFrequencies: Map<MorningFeeling, Int>
) {
    companion object {
        /** Times of day carry no date, so they are rendered as a bare clock time. */
        const val TIME_OF_DAY_FORMAT = "HH:mm:ss"

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
