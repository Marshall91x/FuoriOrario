package it.manu.fuoriorario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.brand_first
import fuoriorario.composeapp.generated.resources.brand_second
import fuoriorario.composeapp.generated.resources.sign_out
import fuoriorario.composeapp.generated.resources.subtitle_player
import fuoriorario.composeapp.generated.resources.subtitle_staff
import it.manu.fuoriorario.data.AuthRepository
import it.manu.fuoriorario.data.Session
import it.manu.fuoriorario.data.SupabaseAuthRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.ui.auth.LoginScreen
import it.manu.fuoriorario.ui.auth.PrivacyScreen
import it.manu.fuoriorario.ui.components.GhostButton
import it.manu.fuoriorario.ui.home.Home
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(auth: AuthRepository = remember { SupabaseAuthRepository() }) {
    FuoriOrarioTheme {
        val session by auth.session.collectAsState(Session.Loading)
        val scope = rememberCoroutineScope()
        val signOut: () -> Unit = { scope.launch { auth.signOut() } }
        // Bottom inset is left to each screen: the home's tab bar runs to the bottom edge.
        Box(
            Modifier
                .fillMaxSize()
                .background(FuoriOrarioTheme.colors.bg)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
        ) {
            val bottom = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            when (val s = session) {
                Session.Loading -> Page(bottom) { Header() }
                Session.SignedOut -> Page(bottom) {
                    Header()
                    LoginScreen(auth)
                }
                is Session.SignedIn ->
                    if (s.member.privacyAckAt == null) {
                        Page(bottom) {
                            Header(s.member, signOut)
                            PrivacyScreen(auth)
                        }
                    } else {
                        Home(s.member, signOut)
                    }
            }
        }
    }
}

/** Scrolling page: 16dp gutter outside a 560dp column, like the prototype's body padding + `.wrap`. */
@Composable
internal fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
    }
}

/** Prototype `.top`: brand + subtitle; when signed in, the `.who` row with name and logout. */
@Composable
internal fun Header(member: Member? = null, onSignOut: () -> Unit = {}) {
    val c = FuoriOrarioTheme.colors
    Column(Modifier.padding(top = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val first = stringResource(Res.string.brand_first).uppercase()
            val second = stringResource(Res.string.brand_second).uppercase()
            Text(
                buildAnnotatedString {
                    append("$first ")
                    withStyle(SpanStyle(color = c.accent)) { append(second) }
                },
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.headlineMedium,
                color = c.ink
            )
            val subtitle = if (member?.role == Role.STAFF) Res.string.subtitle_staff else Res.string.subtitle_player
            Text(
                stringResource(subtitle).uppercase(),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
        }
        if (member != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(member.displayName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                GhostButton(stringResource(Res.string.sign_out), onSignOut, pill = true)
            }
        }
        HorizontalDivider(Modifier.padding(top = 10.dp), color = c.line)
    }
}
