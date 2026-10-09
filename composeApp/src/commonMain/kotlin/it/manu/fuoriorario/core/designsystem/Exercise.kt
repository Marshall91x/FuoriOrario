package it.manu.fuoriorario.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.category_athletics
import fuoriorario.composeapp.generated.resources.category_ball_handling
import fuoriorario.composeapp.generated.resources.category_defense
import fuoriorario.composeapp.generated.resources.category_footwork
import fuoriorario.composeapp.generated.resources.category_recovery
import fuoriorario.composeapp.generated.resources.category_shooting
import fuoriorario.composeapp.generated.resources.close
import fuoriorario.composeapp.generated.resources.exercise_category
import fuoriorario.composeapp.generated.resources.exercise_description
import fuoriorario.composeapp.generated.resources.exercise_error_days
import fuoriorario.composeapp.generated.resources.exercise_error_title
import fuoriorario.composeapp.generated.resources.exercise_error_video
import fuoriorario.composeapp.generated.resources.exercise_title
import fuoriorario.composeapp.generated.resources.exercise_video
import fuoriorario.composeapp.generated.resources.exercise_volume
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.domain.Category
import it.manu.fuoriorario.domain.DESCRIPTION_MAX
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.PlanItemError
import it.manu.fuoriorario.domain.TITLE_MAX
import it.manu.fuoriorario.domain.VOLUME_MAX
import org.jetbrains.compose.resources.stringResource

/** Italian name of each area. On web the names load asynchronously: empty until then. */
val categoryNames
    @Composable get() = mapOf(
        Category.BALL_HANDLING to stringResource(Res.string.category_ball_handling),
        Category.SHOOTING to stringResource(Res.string.category_shooting),
        Category.FOOTWORK to stringResource(Res.string.category_footwork),
        Category.ATHLETICS to stringResource(Res.string.category_athletics),
        Category.DEFENSE to stringResource(Res.string.category_defense),
        Category.RECOVERY to stringResource(Res.string.category_recovery)
    )

/** Prototype `.chip`: the area in a pill. */
@Composable
fun CategoryChip(category: Category) = Chip(categoryNames.getValue(category))

/** Prototype `.chip`: [text] in a pill. */
@Composable
fun Chip(text: String) {
    val c = FuoriOrarioTheme.colors
    Text(
        text.uppercase(),
        Modifier
            .background(c.surface2, RoundedCornerShape(50))
            .border(1.dp, c.line, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        color = c.muted
    )
}

/** Prototype bottom sheet: [label] and [name] over [content], with "Chiudi". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(label: String, name: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = FuoriOrarioTheme.colors
    ModalBottomSheet(
        onDismiss,
        // Fully open: half way the save button can sit below the screen.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = null
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp, 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.muted)
                    Text(name.uppercase(), style = MaterialTheme.typography.titleLarge)
                }
                GhostButton(stringResource(Res.string.close), onDismiss)
            }
            content()
        }
    }
}

/** Title, area, volume, description and video of [draft] as typed, shared by the plan's and the library's sheets. */
@Composable
fun ExerciseFields(draft: LibraryExercise, onChange: (LibraryExercise) -> Unit) {
    Field(
        stringResource(Res.string.exercise_title),
        draft.title,
        { onChange(draft.copy(title = it.take(TITLE_MAX))) },
        "exercise_title"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val names = categoryNames
        SelectField(
            stringResource(Res.string.exercise_category),
            draft.category,
            Category.entries,
            { names.getValue(it) },
            "exercise_category",
            Modifier.weight(1f)
        ) { onChange(draft.copy(category = it)) }
        Field(
            stringResource(Res.string.exercise_volume),
            draft.volume.orEmpty(),
            { onChange(draft.copy(volume = it.take(VOLUME_MAX))) },
            "exercise_volume",
            Modifier.weight(1f)
        )
    }
    Field(
        stringResource(Res.string.exercise_description),
        draft.description.orEmpty(),
        { onChange(draft.copy(description = it.take(DESCRIPTION_MAX))) },
        "exercise_description",
        singleLine = false
    )
    Field(
        stringResource(Res.string.exercise_video),
        draft.videoUrl.orEmpty(),
        { onChange(draft.copy(videoUrl = it)) },
        "exercise_video",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
}

/** The form's first problem, in accent. */
@Composable
fun ExerciseErrorText(error: PlanItemError?) {
    error?.let {
        val text = when (it) {
            PlanItemError.TITLE -> Res.string.exercise_error_title
            PlanItemError.NO_DAYS -> Res.string.exercise_error_days
            PlanItemError.VIDEO -> Res.string.exercise_error_video
        }
        Text(stringResource(text), color = FuoriOrarioTheme.colors.accent, style = MaterialTheme.typography.bodyMedium)
    }
}
