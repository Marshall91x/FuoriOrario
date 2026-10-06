package it.manu.fuoriorario.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.last_staff
import fuoriorario.composeapp.generated.resources.load_failed
import fuoriorario.composeapp.generated.resources.retry
import fuoriorario.composeapp.generated.resources.save_denied
import fuoriorario.composeapp.generated.resources.save_failed
import it.manu.fuoriorario.data.LastStaffException
import it.manu.fuoriorario.data.PermissionDeniedException
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
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
    style: GhostStyle = GhostStyle.PLAIN
) {
    val c = FuoriOrarioTheme.colors
    val pill = style == GhostStyle.PILL
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = if (pill) RoundedCornerShape(50) else RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (style == GhostStyle.DANGER) c.accent else c.line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = when (style) {
                GhostStyle.PLAIN -> c.ink
                GhostStyle.PILL -> c.muted
                GhostStyle.DANGER -> c.accent
            }
        ),
        contentPadding = if (pill) PaddingValues(10.dp, 6.dp) else PaddingValues(11.dp, 7.dp)
    ) { Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
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

/** Launches a write; a failure toasts its cause and leaves the form as typed, for a retry. */
fun CoroutineScope.launchWrite(
    toast: SnackbarHostState,
    onDone: () -> Unit = {},
    action: suspend CoroutineScope.() -> Unit
) = launch {
    try {
        action()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val message = when (e) {
            is LastStaffException -> Res.string.last_staff
            is PermissionDeniedException -> Res.string.save_denied
            else -> Res.string.save_failed
        }
        launch { toast.show(getString(message)) }
    } finally {
        onDone()
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
