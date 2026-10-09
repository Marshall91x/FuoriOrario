package it.manu.fuoriorario.core.error

import io.github.jan.supabase.exceptions.RestException

/** RLS refused the write: shouldn't happen if the UI respects roles (ARCHITECTURE "Gestione errori"). */
class PermissionDeniedException : Exception()

/** The server no longer accepts the token: revoked, or the account is gone. */
fun Throwable.isSessionRefused() = this is RestException && statusCode == 401
