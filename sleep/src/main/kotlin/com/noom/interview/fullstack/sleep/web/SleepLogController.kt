package com.noom.interview.fullstack.sleep.web

import com.noom.interview.fullstack.sleep.service.SleepLogService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

/**
 * Authentication is out of scope, so the caller states who it is through
 * X-User-Id. The header is required: defaulting it would hide the fact that
 * every sleep log belongs to a user.
 */
@RestController
@RequestMapping("/api/v1/sleep-logs")
class SleepLogController(private val service: SleepLogService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestHeader(USER_ID_HEADER) userId: Long,
        @Valid @RequestBody request: CreateSleepLogRequest
    ): SleepLogResponse =
        SleepLogResponse.from(
            service.create(
                userId = userId,
                bedStart = request.bedStart,
                bedEnd = request.bedEnd,
                morningFeeling = request.morningFeeling
            )
        )

    /** 404 when the user has never logged a sleep, which the UI renders as its empty state. */
    @GetMapping("/last-night")
    fun lastNight(@RequestHeader(USER_ID_HEADER) userId: Long): ResponseEntity<SleepLogResponse> =
        service.findLastNight(userId)
            ?.let { ResponseEntity.ok(SleepLogResponse.from(it)) }
            ?: ResponseEntity.notFound().build()

    /** An empty window is a valid answer, so this always returns 200. */
    @GetMapping("/averages")
    fun averages(@RequestHeader(USER_ID_HEADER) userId: Long): SleepAveragesResponse =
        SleepAveragesResponse.from(service.findLast30DayAverages(userId))

    private companion object {
        const val USER_ID_HEADER = "X-User-Id"
    }
}
