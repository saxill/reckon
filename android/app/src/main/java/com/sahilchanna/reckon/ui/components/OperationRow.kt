package com.sahilchanna.reckon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sahilchanna.reckon.model.OpStats
import com.sahilchanna.reckon.model.Operation
import com.sahilchanna.reckon.ui.theme.Bold
import com.sahilchanna.reckon.ui.theme.Fmt
import com.sahilchanna.reckon.ui.theme.SemiBold
import com.sahilchanna.reckon.ui.theme.display
import com.sahilchanna.reckon.ui.theme.palette
import com.sahilchanna.reckon.ui.theme.ui

/**
 * One row of "where your time goes": symbol, average solve, accuracy, and a bar
 * scaled against the slowest operation, so the worst drill reads as a full bar.
 */
@Composable
fun OperationRow(
    operation: Operation,
    stats: OpStats?,
    worstAverage: Double?,
    isWorst: Boolean,
    modifier: Modifier = Modifier,
) {
    val average = stats?.average
    val fraction = if (average != null && worstAverage != null && worstAverage > 0) {
        (average / worstAverage).coerceIn(0.08, 1.0)
    } else {
        0.0
    }
    val tint = if (isWorst) palette.bad else palette.mute

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${
                    operation.title
                }: ${average?.let { Fmt.seconds(it) } ?: "no data yet"}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = operation.symbol,
            style = display(17f, Bold),
            color = if (stats == null) palette.mute.copy(alpha = 0.6f) else palette.chalk,
            modifier = Modifier.width(18.dp),
        )

        Text(
            text = average?.let { Fmt.seconds(it) } ?: "—",
            style = display(14f, SemiBold),
            color = if (stats == null) palette.mute else tint,
            modifier = Modifier.width(52.dp),
        )

        Text(
            text = stats?.accuracy?.let { Fmt.percent(it) } ?: "",
            style = ui(13f),
            color = palette.mute,
            modifier = Modifier.width(42.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(palette.surface2),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.toFloat())
                    .clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = if (isWorst) 0.9f else 0.55f)),
            )
        }
    }
}
