package it.manu.fuoriorario.core

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import it.manu.fuoriorario.BuildKonfig

/** The one client: Auth persists the session on device (SharedPreferences / NSUserDefaults / localStorage). */
val supabase by lazy {
    createSupabaseClient(BuildKonfig.SUPABASE_URL, BuildKonfig.SUPABASE_ANON_KEY) {
        install(Auth)
        install(Postgrest)
    }
}
