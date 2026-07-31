package com.noom.interview.fullstack.sleep.web

import com.noom.interview.fullstack.sleep.service.SleepLogService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

/**
 * Authentication is out of scope, so the caller states who it is. How that is
 * read off the request is the resolver's business - see [CurrentUser].
 */
@RestController
@RequestMapping("/api/v1/sleep-logs")
class SleepLogController(private val service: SleepLogService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @CurrentUser userId: Long,
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
    fun lastNight(@CurrentUser userId: Long): ResponseEntity<SleepLogResponse> =
        service.findLastNight(userId)
            ?.let { ResponseEntity.ok(SleepLogResponse.from(it)) }
            ?: ResponseEntity.notFound().build()

    /** An empty window is a valid answer, so this always returns 200. */
    @GetMapping("/averages")
    fun averages(@CurrentUser userId: Long): SleepAveragesResponse =
        SleepAveragesResponse.from(service.findLast30DayAverages(userId))
}
