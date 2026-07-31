package com.noom.interview.fullstack.sleep.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Averages times of day.
 *
 * A clock time wraps at midnight, so the arithmetic mean is wrong: 23:50 and
 * 00:10 average to 12:00 instead of 00:00. Each time is therefore treated as an
 * angle on a 24-hour circle, the unit vectors are averaged, and the resulting
 * direction is converted back to a time.
 *
 * Times are read in UTC. The result can be shifted to any fixed offset because
 * a circular mean is rotation-equivariant - mean(t + c) == mean(t) + c - so a
 * client converts the answer instead of the API having to know its timezone.
 */
object TimeOfDayAverage {

    private const val MINUTES_PER_DAY = 24 * 60
    private const val RADIANS_PER_MINUTE = 2 * Math.PI / MINUTES_PER_DAY

    /**
     * Null for an empty input, and also when the times are spread so evenly
     * around the clock that they cancel out and no direction is meaningful.
     */
    fun of(instants: Collection<Instant>): LocalTime? {
        if (instants.isEmpty()) return null

        var x = 0.0
        var y = 0.0
        instants.forEach { instant ->
            val angle = instant.minuteOfDayUtc() * RADIANS_PER_MINUTE
            x += cos(angle)
            y += sin(angle)
        }

        if (x.isNegligible() && y.isNegligible()) return null

        val meanAngle = atan2(y / instants.size, x / instants.size)
        val meanMinute = Math.round(meanAngle / RADIANS_PER_MINUTE)
        return LocalTime.MIDNIGHT.plusMinutes(Math.floorMod(meanMinute, MINUTES_PER_DAY.toLong()))
    }

    private fun Instant.minuteOfDayUtc(): Int =
        atZone(ZoneOffset.UTC).toLocalTime().toSecondOfDay() / 60

    private fun Double.isNegligible() = kotlin.math.abs(this) < 1e-9
}
