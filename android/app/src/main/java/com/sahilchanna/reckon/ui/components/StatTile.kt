package com.sahilchanna.reckon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sahilchanna.reckon.ui.theme.Bold
import com.sahilchanna.reckon.ui.theme.card
import com.sahilchanna.reckon.ui.theme.display
import com.sahilchanna.reckon.ui.theme.palette
import com.sahilchanna.reckon.ui.theme.ui

/** One figure on the result screen: icon, value, label. */
@Composable
fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .card(padding = 14.dp, radius = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.amber,
            modifier = Modifier.size(12.dp),
        )
        Text(text = value, style = display(22f, Bold), color = palette.chalk)
        Text(text = label, style = ui(11.5f), color = palette.mute)
    }
}
