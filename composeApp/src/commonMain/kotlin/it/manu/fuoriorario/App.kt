package it.manu.fuoriorario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.brand_first
import fuoriorario.composeapp.generated.resources.brand_second
import fuoriorario.composeapp.generated.resources.placeholder_body
import fuoriorario.composeapp.generated.resources.placeholder_title
import fuoriorario.composeapp.generated.resources.subtitle_player
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun App() {
    FuoriOrarioTheme {
        val c = FuoriOrarioTheme.colors
        Box(
            Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter
        ) {
            // Single column, max 560dp like the prototype's `.wrap`.
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Header()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(c.surface, RoundedCornerShape(14.dp))
                        .border(1.dp, c.line, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(Res.string.placeholder_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.placeholder_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.muted
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    val c = FuoriOrarioTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val first = stringResource(Res.string.brand_first).uppercase()
            val second = stringResource(Res.string.brand_second).uppercase()
            Text(
                buildAnnotatedString {
                    append("$first ")
                    withStyle(SpanStyle(color = c.accent)) { append(second) }
                },
                style = MaterialTheme.typography.headlineMedium,
                color = c.ink
            )
            Text(
                stringResource(Res.string.subtitle_player).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
        }
        HorizontalDivider(color = c.line)
    }
}
