package it.manu.fuoriorario.ui.auth

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import it.manu.fuoriorario.App
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LoginTest {
    @Test
    fun otpLogin() = runComposeUiTest {
        setContent { App(FakeAuthRepository()) }

        awaitText("Invia codice")
        onNodeWithTag("email").performTextInput("sconosciuto@example.com")
        onNodeWithText("Invia codice").performClick()
        awaitText("Non sei in nessuna squadra, contatta lo staff.")

        onNodeWithTag("email").performTextReplacement(" Giocatore@Example.com ")
        onNodeWithText("Invia codice").performClick()
        awaitText("Accedi")

        onNodeWithTag("code").performTextInput("000000")
        onNodeWithText("Accedi").performClick()
        awaitText("Codice non valido o scaduto.")

        onNodeWithTag("code").performTextReplacement("123456")
        onNodeWithText("Accedi").performClick()
        awaitText("Luca B.")
        awaitText("Esci")

        onNodeWithText("Esci").performClick()
        awaitText("Invia codice")
    }
}
