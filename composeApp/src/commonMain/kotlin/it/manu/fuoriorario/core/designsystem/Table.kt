package it.manu.fuoriorario.core.designsystem

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme

/** A column of [Table]: [key] is the test tag suffix of its cells. */
class TableColumn(val key: String, val label: String, val width: Dp = 44.dp)

/**
 * A row of [Table]: [values] in the order of the fixed columns then the scrolling ones. [highlighted] stands out,
 * [onClick] null: not tappable. Its cells are tagged "[tag]_key".
 */
class TableRow(
    val tag: String,
    val values: List<String>,
    val highlighted: Boolean = false,
    val bold: Boolean = false,
    val onClick: (() -> Unit)? = null
)

/** On a narrow screen the [columns] scroll sideways together while the [fixed] ones (names) stay put. */
@Composable
fun Table(fixed: List<TableColumn>, columns: List<TableColumn>, rows: List<TableRow>) {
    val c = FuoriOrarioTheme.colors
    val scroll = rememberScrollState()
    Column {
        TableLine(fixed, columns, fixed.map { it.label } + columns.map { it.label }, c.muted, scroll)
        rows.forEach { row ->
            HorizontalDivider(Modifier.width((fixed + columns).fold(0.dp) { w, col -> w + col.width }), color = c.line)
            TableLine(
                fixed,
                columns,
                row.values,
                c.ink,
                scroll,
                Modifier
                    .background(if (row.highlighted) c.surface2 else Color.Transparent, RoundedCornerShape(6.dp))
                    .then(row.onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
                row.tag,
                row.bold
            )
        }
    }
}

@Composable
private fun TableLine(
    fixed: List<TableColumn>,
    columns: List<TableColumn>,
    values: List<String>,
    color: Color,
    scroll: ScrollState,
    modifier: Modifier = Modifier,
    tag: String? = null,
    bold: Boolean = false
) {
    val cellTag = { col: TableColumn -> tag?.let { Modifier.testTag("${it}_${col.key}") } ?: Modifier }
    Row(
        modifier.then(tag?.let { Modifier.testTag(it) } ?: Modifier).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        fixed.zip(values).forEach { (col, value) ->
            Text(
                value,
                Modifier.width(col.width).then(cellTag(col)),
                color = color,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Every row shares [scroll], so the header and the rows stay aligned; a per-row overscroll would stretch one alone.
        Row(Modifier.horizontalScroll(scroll, overscrollEffect = null)) {
            columns.zip(values.drop(fixed.size)).forEach { (col, value) ->
                Text(
                    value,
                    Modifier.width(col.width).then(cellTag(col)),
                    color = color,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
