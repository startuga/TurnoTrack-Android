package com.startuga.turnotrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** "< Outubro 2026 >" — tocar no título volta ao mês atual. */
@Composable
fun MonthSwitcher(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier,
    previousLabel: String = "Mês anterior",
    nextLabel: String = "Próximo mês",
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = previousLabel)
            }
            TextButton(onClick = onTitleClick, modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = nextLabel)
            }
        }
    }
}

/** Pequeno cartão colorido com um valor (ex.: "Horas Trab. 152h 30m"). */
@Composable
fun SummaryTile(
    label: String,
    value: String,
    colors: Pair<Color, Color>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = colors.first,
        contentColor = colors.second,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}
