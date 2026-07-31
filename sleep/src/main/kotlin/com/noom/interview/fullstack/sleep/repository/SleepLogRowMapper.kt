package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepLog
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Maps a sleep_log row onto the domain.
 *
 * The driver exchanges TIMESTAMPTZ as OffsetDateTime, so time is converted here,
 * at the database boundary, and the domain only ever deals in absolute instants.
 */
internal object SleepLogRowMapper : RowMapper<SleepLog> {

    override fun mapRow(rs: ResultSet, rowNum: Int) = SleepLog(
        id = rs.getLong("id"),
        userId = rs.getLong("user_id"),
        bedStart = rs.instantAt("bed_start"),
        bedEnd = rs.instantAt("bed_end"),
        morningFeeling = MorningFeeling.valueOf(rs.getString("morning_feeling"))
    )

    private fun ResultSet.instantAt(column: String): Instant =
        getObject(column, OffsetDateTime::class.java).toInstant()
}
