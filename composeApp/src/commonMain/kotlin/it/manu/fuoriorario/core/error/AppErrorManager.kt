package it.manu.fuoriorario.core.error

import it.manu.fuoriorario.core.session.SessionExpiry

/**
 * The first of [handlers] that claims a failed load draws it. The order is the contract: session expired, permission
 * denied, network, then [FallbackHandler] (ARCHITECTURE "Gestione errori").
 * A refused session, in a load or in an action, also ends: [sessionExpiry] takes the app back to login.
 */
class AppErrorManager(override val handlers: List<ErrorHandler>, private val sessionExpiry: SessionExpiry) :
    ErrorManager {
    override fun handle(error: Exception) = super.handle(error).also { expireIfRefused(error) }

    override fun handle(error: Exception, retryFunction: () -> Unit) =
        super.handle(error, retryFunction).also { expireIfRefused(error) }

    override suspend fun checkError(error: Exception) = super.checkError(error).also { expireIfRefused(error) }

    private fun expireIfRefused(error: Exception) {
        if (error.isSessionRefused()) sessionExpiry.expire()
    }
}
