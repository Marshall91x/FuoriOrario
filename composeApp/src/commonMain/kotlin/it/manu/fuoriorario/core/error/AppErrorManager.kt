package it.manu.fuoriorario.core.error

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.error_permission
import fuoriorario.composeapp.generated.resources.load_failed
import fuoriorario.composeapp.generated.resources.retry
import fuoriorario.composeapp.generated.resources.session_expired
import io.github.jan.supabase.exceptions.RestException
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.session.SessionExpiry
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.isNetworkException
import org.jetbrains.compose.resources.stringResource

/**
 * The first of [handlers] that claims a failure draws it. The order is the contract: session expired, permission
 * denied, network, then [FallbackHandler] (ARCHITECTURE "Gestione errori").
 * A refused session also ends: [sessionExpiry] takes the app back to login, the error screen is what shows meanwhile.
 */
class AppErrorManager(override val handlers: List<ErrorHandler>, private val sessionExpiry: SessionExpiry) :
    ErrorManager {
    override fun handle(error: Exception) = expireIfRefused(super.handle(error))

    override fun handle(error: Exception, retryFunction: () -> Unit) =
        expireIfRefused(super.handle(error, retryFunction))

    private fun expireIfRefused(handler: ErrorHandler) = handler.also {
        if (it is SessionExpiredErrorHandler) sessionExpiry.expire()
    }
}

/** RLS refused the write: shouldn't happen if the UI respects roles (ARCHITECTURE "Gestione errori"). */
class PermissionDeniedException : Exception()

/** The server refused the token: nothing to retry, the session is over and the app is going back to login. */
class SessionExpiredErrorHandler : ErrorHandler {
    override var e: Exception? = null

    override fun canHandleError(error: Exception) = error is RestException && error.statusCode == 401

    @Composable
    override fun ErrorScreenContent() = ErrorContent(stringResource(Res.string.session_expired), onRetry = null)
}

class PermissionDeniedErrorHandler : ErrorHandler {
    override var e: Exception? = null

    override fun canHandleError(error: Exception) = error is PermissionDeniedException

    @Composable
    override fun ErrorScreenContent() = ErrorContent(stringResource(Res.string.error_permission), onRetry = null)
}

/** The request never reached Supabase: the same request may work in a moment. */
class NetworkErrorHandler : ErrorHandlerWithRetry {
    override var e: Exception? = null
    override var retryFunction: (() -> Unit)? = null

    override fun canHandleError(error: Exception) = error.isNetworkException()

    @Composable
    override fun ErrorScreenContent() = ErrorContent(stringResource(Res.string.load_failed), retryFunction)
}

/** Like the old `LoadFailed`: message, and "Riprova" only when there's something to retry. */
@Composable
internal fun ErrorContent(message: String, onRetry: (() -> Unit)?) {
    Panel(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = FuoriOrarioTheme.colors.muted)
        onRetry?.let { GhostButton(stringResource(Res.string.retry), it) }
    }
}
