package it.manu.fuoriorario.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.in_progress
import fuoriorario.composeapp.generated.resources.load_failed
import fuoriorario.composeapp.generated.resources.loading
import fuoriorario.composeapp.generated.resources.months_short
import fuoriorario.composeapp.generated.resources.retry
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** Scrolling page: 16dp gutter outside a 560dp column, like the prototype's body padding + `.wrap`. */
@Composable
fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .padding(16.dp, 18.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
    }
}

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

/** Prototype button variants drawn by [GhostButton]. */
enum class GhostStyle {
    /** `.btn.ghost.small` */
    PLAIN,

    /** `.role-toggle` */
    PILL,

    /** `.btn.danger.small`: the second tap of a removal. */
    DANGER
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GhostStyle = GhostStyle.PLAIN,
    enabled: Boolean = true
) {
    val c = FuoriOrarioTheme.colors
    val pill = style == GhostStyle.PILL
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = if (pill) RoundedCornerShape(50) else RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (style == GhostStyle.DANGER) c.accent else c.line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = when (style) {
                GhostStyle.PLAIN -> c.ink
                GhostStyle.PILL -> c.muted
                GhostStyle.DANGER -> c.accent
            },
            disabledContentColor = c.muted.copy(alpha = 0.4f)
        ),
        contentPadding = if (pill) PaddingValues(10.dp, 6.dp) else PaddingValues(11.dp, 7.dp)
    ) { Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
}

/** Prototype `.role-toggle` with `aria-pressed`: accent while [on]. */
@Composable
fun TogglePill(text: String, on: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = FuoriOrarioTheme.colors
    Text(
        text,
        modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, if (on) c.accent else c.line, RoundedCornerShape(50))
            .toggleable(on, role = Role.Switch, onValueChange = onToggle)
            .padding(10.dp, 6.dp),
        color = if (on) c.accent else c.muted,
        style = MaterialTheme.typography.bodyMedium,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold
    )
}

/** Prototype `.seg`: one pill per option, the selected one inked. [tag] gives each option's test tag. */
@Composable
fun <T> SegmentedControl(
    options: Map<T, StringResource>,
    selected: T,
    onSelect: (T) -> Unit,
    tag: (T) -> String,
    modifier: Modifier = Modifier
) {
    val c = FuoriOrarioTheme.colors
    Row(
        modifier
            .background(c.surface2, RoundedCornerShape(50))
            .border(1.dp, c.line, RoundedCornerShape(50))
            .padding(3.dp)
            .selectableGroup()
    ) {
        options.forEach { (option, label) ->
            val isSelected = option == selected
            Text(
                stringResource(label),
                Modifier
                    .testTag(tag(option))
                    .background(if (isSelected) c.ink else Color.Transparent, RoundedCornerShape(50))
                    .selectable(isSelected, role = Role.RadioButton) { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                color = if (isSelected) c.bg else c.muted,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Prototype `.field`: uppercase label over a filled input; [singleLine] false for a `textarea`. [tag] is the test tag. */
@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onDone: () -> Unit = {},
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    singleLine: Boolean = true
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
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onDone = { onDone() })
        )
    }
}

/** Prototype `.field select`: uppercase label over the current option, opening a menu of [options]. [tag] is the test tag. */
@Composable
fun <T> SelectField(
    label: String,
    value: T,
    options: List<T>,
    text: (T) -> String,
    tag: String,
    modifier: Modifier = Modifier,
    onChange: (T) -> Unit
) {
    val c = FuoriOrarioTheme.colors
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.muted)
        Box {
            Text(
                text(value),
                Modifier
                    .fillMaxWidth()
                    .testTag(tag)
                    .background(c.surface2, RoundedCornerShape(9.dp))
                    .border(1.dp, c.line, RoundedCornerShape(9.dp))
                    .clickable(role = Role.DropdownList) { open = true }
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = c.ink
            )
            DropdownMenu(open, { open = false }, containerColor = c.surface) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text(option), color = c.ink) },
                        onClick = {
                            onChange(option)
                            open = false
                        }
                    )
                }
            }
        }
    }
}

/** Prototype `fmtShort`: "6 ott". On web the months load asynchronously: empty until then. */
@Composable
fun LocalDate.short() = "$day ${stringArrayResource(Res.array.months_short).getOrElse(month.ordinal) { "" }}"

/** Where screens send toasts: `LocalToast.current.show("Sessione salvata")`. Provided by the home. */
val LocalToast = staticCompositionLocalOf<SnackbarHostState> { error("No ToastHost in composition") }

/** Like the prototype's `toast()`: replaces the message on screen instead of queueing behind it. */
suspend fun SnackbarHostState.show(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}

/** A load went wrong: the message and "Riprova". */
@Composable
fun LoadFailed(onRetry: () -> Unit) {
    Panel(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.load_failed), color = FuoriOrarioTheme.colors.muted)
        GhostButton(stringResource(Res.string.retry), onRetry)
    }
}

/**
 * A load in flight: in place of what hasn't arrived yet, or over what is being loaded again (Piano's week).
 * Every load shows this one.
 */
@Composable
fun Loader(modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.loading)
    Box(modifier.fillMaxWidth().heightIn(min = 96.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            Modifier.testTag("loader").semantics { contentDescription = label },
            color = FuoriOrarioTheme.colors.accent
        )
    }
}

/** A write in flight: a spinner over the whole screen, which takes every tap and the back until it ends. */
@Composable
fun LoadingOverlay() {
    Dialog({}, DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        val label = stringResource(Res.string.in_progress)
        CircularProgressIndicator(
            Modifier.testTag("loading_overlay").semantics { contentDescription = label },
            color = FuoriOrarioTheme.colors.accent
        )
    }
}

/** Prototype `.toast`: an ink pill showing [state]'s current message. */
@Composable
fun ToastHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val c = FuoriOrarioTheme.colors
    SnackbarHost(state, modifier.padding(horizontal = 16.dp)) { data ->
        Text(
            data.visuals.message,
            Modifier
                .background(c.ink, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 9.dp),
            color = c.bg,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Prototype `.legend`: a swatch per colour. */
@Composable
fun Legend(vararg items: Pair<Color, StringResource>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
                Text(stringResource(label), fontSize = 12.5.sp, color = FuoriOrarioTheme.colors.muted)
            }
        }
    }
}
