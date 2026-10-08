package it.manu.fuoriorario.core.viewmodel

import kotlinx.io.IOException

// From sinetwork (utils/UtilsFunctions.kt): only what ComposeViewModel uses.

/** No network: supabase-kt wraps Ktor's failures in `HttpRequestException`, an [IOException]. */
fun Throwable.isNetworkException(): Boolean = this is IOException

class CustomException(val code: Int, val userMessage: String?, val systemMessage: String?) : Exception()

data class ErrorException(val code: Int, val userMessage: String? = null, val systemMessage: String? = null)
