package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepLog
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class SleepLogRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun create(
        userId: Long,
        bedStart: Instant,
        bedEnd: Instant,
        morningFeeling: MorningFeeling
    ): SleepLog {
        val parameters = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("bedStart", bedStart.toOffsetDateTime())
            .addValue("bedEnd", bedEnd.toOffsetDateTime())
            .addValue("morningFeeling", morningFeeling.name)

        // RETURNING gives back the generated row in the same round trip, so the
        // caller never has to re-read what was just written. The feeling is cast
        // explicitly because the driver sends it as text, and the column is an
        // enum type.
        return jdbcTemplate.queryForObject(
            """
            INSERT INTO sleep_log (user_id, bed_start, bed_end, morning_feeling)
            VALUES (:userId, :bedStart, :bedEnd, CAST(:morningFeeling AS morning_feeling))
            RETURNING $COLUMNS
            """.trimIndent(),
            parameters,
            SleepLogRowMapper
        ) ?: error("INSERT ... RETURNING produced no row")
    }

    /**
     * The user's most recent sleep, or null when they have never logged one -
     * which is the empty state the UI renders.
     */
    fun findLatestByUserId(userId: Long): SleepLog? =
        jdbcTemplate.query(
            """
            SELECT $COLUMNS
            FROM sleep_log
            WHERE user_id = :userId
            ORDER BY bed_start DESC
            LIMIT 1
            """.trimIndent(),
            MapSqlParameterSource("userId", userId),
            SleepLogRowMapper
        ).firstOrNull()

    /**
     * Sleeps a user started within [from] (inclusive) and [to] (exclusive).
     *
     * The interval is half-open so that adjacent windows neither overlap nor
     * leave a gap. Anchored on bed_start: a night belongs to the evening it
     * begins on.
     */
    fun findByUserIdAndBedStartBetween(userId: Long, from: Instant, to: Instant): List<SleepLog> =
        jdbcTemplate.query(
            """
            SELECT $COLUMNS
            FROM sleep_log
            WHERE user_id = :userId
              AND bed_start >= :from
              AND bed_start < :to
            ORDER BY bed_start DESC
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("from", from.toOffsetDateTime())
                .addValue("to", to.toOffsetDateTime()),
            SleepLogRowMapper
        )

    private companion object {
        const val COLUMNS = "id, user_id, bed_start, bed_end, morning_feeling"

        fun Instant.toOffsetDateTime(): OffsetDateTime = atOffset(ZoneOffset.UTC)
    }
}
