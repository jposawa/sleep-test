package com.noom.interview.fullstack.sleep.web

import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class UnidentifiedCallerException(message: String) : RuntimeException(message)

/** Turns the caller's header into the [CurrentUser] argument a controller declares. */
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUser::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        modelAndViewContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Long {
        val header = webRequest.getHeader(USER_ID_HEADER)
            ?: throw UnidentifiedCallerException("Missing required header: $USER_ID_HEADER")

        return header.toLongOrNull()
            ?: throw UnidentifiedCallerException("$USER_ID_HEADER must be a numeric user id")
    }

    companion object {
        const val USER_ID_HEADER = "X-User-Id"
    }
}
