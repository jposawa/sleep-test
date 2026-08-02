package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepAverages
import com.noom.interview.fullstack.sleep.domain.SleepLog
import com.noom.interview.fullstack.sleep.repository.SleepLogRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant

class InvalidSleepIntervalException(message: String) : RuntimeException(message) {
    companion object {
        /**
         * Stated once, so that the controller and the service cannot end up
         * rejecting the same request with different words.
         */
        const val INTERVAL_ORDERED_MESSAGE = "bedEnd must be after bedStart"

        const val NOT_IN_FUTURE_MESSAGE = "bedEnd must not be in the future"
    }
}

@Service
class SleepLogService(
    private val repository: SleepLogRepository,
    /** Injected so the 30-day window can be pinned - see SleepLogServiceTest. */
    private val clock: Clock = Clock.systemUTC()
) {

    /**
     * The interval is validated here rather than left to the database: the
     * column is guarded by a CHECK constraint, but a constraint violation
     * surfaces as a driver error that says nothing useful to an API client.
     */
    fun create(
        userId: Long,
        bedStart: Instant,
        bedEnd: Instant,
        morningFeeling: MorningFeeling
    ): SleepLog {
        if (!bedEnd.isAfter(bedStart)) {
            throw InvalidSleepIntervalException(
                InvalidSleepIntervalException.INTERVAL_ORDERED_MESSAGE
            )
        }
        // A sleep being logged is a sleep that ended, so the later end is the
        // one to check: with the ordering rule above, bedStart then lies in the
        // past as well, and a separate check on it could never fire.
        //
        // This rule sits here and not on the request DTO because it needs a
        // clock. Bean validation would have to read the system clock, leaving
        // two sources of "now" for a single rule; the service already has one
        // injected, which is also what makes the rule testable.
        if (bedEnd.isAfter(clock.instant())) {
            throw InvalidSleepIntervalException(
                InvalidSleepIntervalException.NOT_IN_FUTURE_MESSAGE
            )
        }
        return repository.create(userId, bedStart, bedEnd, morningFeeling)
    }

    /** Null when the user has never logged a sleep - the UI's empty state. */
    fun findLastNight(userId: Long): SleepLog? = repository.findLatestByUserId(userId)

    /**
     * Averages over the sleeps started in the last 30 days.
     *
     * An empty window is a valid answer rather than an error: the range is
     * still meaningful, the averages are simply absent.
     */
    fun findLast30DayAverages(userId: Long): SleepAverages {
        val to = clock.instant()
        val from = to.minus(AVERAGE_WINDOW)
        return SleepAverages.of(from, to, repository.findByUserIdAndBedStartBetween(userId, from, to))
    }

    private companion object {
        val AVERAGE_WINDOW: Duration = Duration.ofDays(30)
    }
}
