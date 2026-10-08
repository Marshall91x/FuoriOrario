package it.manu.fuoriorario.core.viewmodel

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.save_denied
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.core.error.PermissionDeniedException
import kotlinx.io.IOException
import org.jetbrains.compose.resources.getString

// From sinetwork (utils/UtilsFunctions.kt): only what ComposeViewModel uses.

/** No network: supabase-kt wraps Ktor's failures in `HttpRequestException`, an [IOException]. */
fun Throwable.isNetworkException(): Boolean = this is IOException

/**
 * The toast text for a failed action. Unlike sinetwork it never shows `e.message`: Supabase's are technical
 * and in English, and the app has always said "Salvataggio non riuscito" instead.
 */
suspend fun checkError(e: Exception): String = when (e) {
    is PermissionDeniedException -> getString(Res.string.save_denied)
    else -> getString(Res.string.save_failed)
}

class CustomException(val code: Int, val userMessage: String?, val systemMessage: String?) : Exception()

data class ErrorException(val code: Int, val userMessage: String? = null, val systemMessage: String? = null)
