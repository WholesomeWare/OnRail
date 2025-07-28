package com.csakitheone.onrail.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csakitheone.onrail.data.model.EMMAVehiclePosition
import com.csakitheone.onrail.ui.theme.colorDelayDrastic
import com.csakitheone.onrail.ui.theme.colorDelayMajor
import com.csakitheone.onrail.ui.theme.colorDelayMinor
import com.csakitheone.onrail.ui.theme.colorDelayNone
import com.csakitheone.onrail.ui.theme.onColorDelayDrastic
import com.csakitheone.onrail.ui.theme.onColorDelayMajor
import com.csakitheone.onrail.ui.theme.onColorDelayMinor
import com.csakitheone.onrail.ui.theme.onColorDelayNone
import kotlin.math.max

@Composable
fun TrainDelayDistributionIndicator(
    modifier: Modifier = Modifier,
    trains: List<EMMAVehiclePosition>,
) {
    val lineHeight = remember { 8.dp }

    val delayPerfectCount by remember(trains) {
        derivedStateOf { trains.count { it.delayMinutes < 1 } }
    }
    val delayPerfectPercent by remember(delayPerfectCount, trains.size) {
        derivedStateOf { if (trains.isEmpty()) 0f else delayPerfectCount.toFloat() / trains.size }
    }
    val delayNoneCount by remember(trains) {
        derivedStateOf { trains.count { it.delayColor == colorDelayNone } }
    }
    val delayNonePercent by remember(delayNoneCount, trains.size) {
        derivedStateOf { if (trains.isEmpty()) 0f else delayNoneCount.toFloat() / trains.size }
    }
    val delayMinorCount by remember(trains) {
        derivedStateOf { trains.count { it.delayColor == colorDelayMinor } }
    }
    val delayMinorPercent by remember(delayMinorCount, trains.size) {
        derivedStateOf { if (trains.isEmpty()) 0f else delayMinorCount.toFloat() / trains.size }
    }
    val delayMajorCount by remember(trains) {
        derivedStateOf { trains.count { it.delayColor == colorDelayMajor } }
    }
    val delayMajorPercent by remember(delayMajorCount, trains.size) {
        derivedStateOf { if (trains.isEmpty()) 0f else delayMajorCount.toFloat() / trains.size }
    }
    val delayDrasticCount by remember(trains) {
        derivedStateOf { trains.count { it.delayColor == colorDelayDrastic } }
    }
    val delayDrasticPercent by remember(delayDrasticCount, trains.size) {
        derivedStateOf { if (trains.isEmpty()) 0f else delayDrasticCount.toFloat() / trains.size }
    }

    var isInfoTextVisible by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier,
        onClick = { isInfoTextVisible = !isInfoTextVisible },
    ) {
        AnimatedVisibility(isInfoTextVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f + delayNonePercent * .5f),
                    color = colorDelayNone,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = "0-5 p\n${delayNoneCount} - ${(delayNonePercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = onColorDelayNone,
                    )
                }
                Surface(
                    modifier = Modifier.weight(1f + delayMinorPercent * .5f),
                    color = colorDelayMinor,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = "5-15 p\n${delayMinorCount} - ${(delayMinorPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = onColorDelayMinor,
                    )
                }
                Surface(
                    modifier = Modifier.weight(1f + delayMajorPercent * .5f),
                    color = colorDelayMajor,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = "15-60 p\n${delayMajorCount} - ${(delayMajorPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = onColorDelayMajor,
                    )
                }
                Surface(
                    modifier = Modifier.weight(1f + delayDrasticPercent * .5f),
                    color = colorDelayDrastic,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = "1+ óra\n${delayDrasticCount} - ${(delayDrasticPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = onColorDelayDrastic,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .height(lineHeight)
                    .background(colorDelayDrastic)
                    .fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .height(lineHeight)
                    .background(colorDelayMajor)
                    .fillMaxWidth(delayNonePercent + delayMinorPercent + delayMajorPercent)
            )
            Box(
                modifier = Modifier
                    .height(lineHeight)
                    .background(colorDelayMinor)
                    .fillMaxWidth(delayNonePercent + delayMinorPercent)
            )
            Box(
                modifier = Modifier
                    .height(lineHeight)
                    .background(colorDelayNone)
                    .fillMaxWidth(delayNonePercent)
            )
            Box(
                modifier = Modifier
                    .height(lineHeight)
                    .alpha(.1f)
                    .background(Color.Green)
                    .fillMaxWidth(delayPerfectPercent)
            )
        }
    }
}