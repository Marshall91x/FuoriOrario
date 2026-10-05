package it.manu.fuoriorario.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme

/** Prototype `.panel`. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = FuoriOrarioTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(14.dp))
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

/** Prototype `.btn`. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = FuoriOrarioTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.accent,
            contentColor = c.accentInk,
            disabledContainerColor = c.accent.copy(alpha = 0.45f),
            disabledContentColor = c.accentInk.copy(alpha = 0.45f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
    ) { Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) }
}

/** Prototype `.btn.ghost.small`, `.role-toggle` when [pill]. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, pill: Boolean = false) {
    val c = FuoriOrarioTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = if (pill) RoundedCornerShape(50) else RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, c.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (pill) c.muted else c.ink),
        contentPadding = if (pill) PaddingValues(10.dp, 6.dp) else PaddingValues(11.dp, 7.dp)
    ) { Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
}

/** Prototype `.field`: uppercase label over a filled input. [tag] is the test tag. */
@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onDone: () -> Unit = {},
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val c = FuoriOrarioTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.muted)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
                .background(c.surface2, RoundedCornerShape(9.dp))
                .border(1.dp, c.line, RoundedCornerShape(9.dp))
                .padding(horizontal = 11.dp, vertical = 9.dp),
            textStyle = textStyle.copy(color = c.ink),
            cursorBrush = SolidColor(c.accent),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onDone = { onDone() })
        )
    }
}
