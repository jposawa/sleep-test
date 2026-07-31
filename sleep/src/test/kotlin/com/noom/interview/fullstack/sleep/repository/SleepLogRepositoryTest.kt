package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Runs against the PostgreSQL instance from docker-compose, with the schema
 * created by the Flyway migrations. Each test runs in a transaction that is
 * rolled back afterwards, so tests neither see nor leave behind each other's
 * rows.
 */
@SpringBootTest
@Transactional
@Tag("integration")
class SleepLogRepositoryTest {

    @Autowired
    private lateinit var repository: SleepLogRepository

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun `creates a sleep log and returns it with a generated id`() {
        val userId = createUser()

        val created = repository.create(
            userId = userId,
            bedStart = NIGHT_START,
            bedEnd = NIGHT_END,
            morningFeeling = MorningFeeling.GOOD
        )

        assertThat(created.id).isPositive
        assertThat(created.userId).isEqualTo(userId)
        assertThat(created.bedStart).isEqualTo(NIGHT_START)
        assertThat(created.bedEnd).isEqualTo(NIGHT_END)
        assertThat(created.morningFeeling).isEqualTo(MorningFeeling.GOOD)
        assertThat(created.timeInBed).isEqualTo(Duration.ofHours(8).plusMinutes(12))
    }

    @Test
    fun `preserves the absolute instant regardless of the offset it was written with`() {
        val userId = createUser()
        // 10:53 pm in UTC-3 is 01:53 the next day in UTC.
        val bedStart = OffsetDateTime.parse("2026-11-13T22:53:00-03:00").toInstant()
        val bedEnd = OffsetDateTime.parse("2026-11-14T07:05:00-03:00").toInstant()

        repository.create(userId, bedStart, bedEnd, MorningFeeling.GOOD)

        val stored = repository.findLatestByUserId(userId)!!
        assertThat(stored.bedStart).isEqualTo(Instant.parse("2026-11-14T01:53:00Z"))
        assertThat(stored.bedEnd).isEqualTo(Instant.parse("2026-11-14T10:05:00Z"))
    }

    @Test
    fun `returns null as the latest sleep when the user has never logged one`() {
        val userId = createUser()

        assertThat(repository.findLatestByUserId(userId)).isNull()
    }

    @Test
    fun `returns the most recently started sleep as the latest`() {
        val userId = createUser()
        repository.create(userId, NIGHT_START, NIGHT_END, MorningFeeling.BAD)
        val newer = repository.create(
            userId = userId,
            bedStart = NIGHT_START.plus(Duration.ofDays(1)),
            bedEnd = NIGHT_END.plus(Duration.ofDays(1)),
            morningFeeling = MorningFeeling.GOOD
        )

        assertThat(repository.findLatestByUserId(userId)).isEqualTo(newer)
    }

    @Test
    fun `accepts more than one sleep on the same day`() {
        val userId = createUser()
        repository.create(userId, NIGHT_START, NIGHT_END, MorningFeeling.OK)
        // A nap a few hours after getting up: nothing in the requirements limits
        // a user to a single log per day.
        val nap = repository.create(
            userId = userId,
            bedStart = NIGHT_END.plus(Duration.ofHours(7)),
            bedEnd = NIGHT_END.plus(Duration.ofHours(8)),
            morningFeeling = MorningFeeling.GOOD
        )

        assertThat(repository.findByUserIdAndBedStartBetween(userId, NIGHT_START, FAR_FUTURE))
            .hasSize(2)
        assertThat(repository.findLatestByUserId(userId)).isEqualTo(nap)
    }

    @Test
    fun `includes the lower bound and excludes the upper bound of the window`() {
        val userId = createUser()
        val onLowerBound = repository.create(userId, NIGHT_START, NIGHT_END, MorningFeeling.OK)
        val onUpperBound = NIGHT_START.plus(Duration.ofDays(2))
        repository.create(userId, onUpperBound, onUpperBound.plus(Duration.ofHours(8)), MorningFeeling.BAD)

        val found = repository.findByUserIdAndBedStartBetween(userId, NIGHT_START, onUpperBound)

        assertThat(found).containsExactly(onLowerBound)
    }

    @Test
    fun `returns sleeps newest first`() {
        val userId = createUser()
        val older = repository.create(userId, NIGHT_START, NIGHT_END, MorningFeeling.BAD)
        val newer = repository.create(
            userId = userId,
            bedStart = NIGHT_START.plus(Duration.ofDays(1)),
            bedEnd = NIGHT_END.plus(Duration.ofDays(1)),
            morningFeeling = MorningFeeling.GOOD
        )

        assertThat(repository.findByUserIdAndBedStartBetween(userId, NIGHT_START, FAR_FUTURE))
            .containsExactly(newer, older)
    }

    @Test
    fun `never returns another user's sleeps`() {
        val userId = createUser()
        val otherUserId = createUser()
        repository.create(otherUserId, NIGHT_START, NIGHT_END, MorningFeeling.BAD)

        assertThat(repository.findLatestByUserId(userId)).isNull()
        assertThat(repository.findByUserIdAndBedStartBetween(userId, NIGHT_START, FAR_FUTURE)).isEmpty()
    }

    @Test
    fun `rejects a sleep that ends before it starts`() {
        val userId = createUser()

        assertThatThrownBy {
            repository.create(userId, NIGHT_END, NIGHT_START, MorningFeeling.OK)
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun createUser(): Long =
        jdbcTemplate.queryForObject(
            "INSERT INTO users DEFAULT VALUES RETURNING id",
            MapSqlParameterSource(),
            Long::class.java
        )!!

    private companion object {
        val NIGHT_START: Instant = Instant.parse("2026-11-14T01:53:00Z")
        val NIGHT_END: Instant = Instant.parse("2026-11-14T10:05:00Z")
        val FAR_FUTURE: Instant = Instant.parse("2027-01-01T00:00:00Z")
    }
}
