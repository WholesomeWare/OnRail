package com.csakitheone.onrail.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.csakitheone.onrail.data.Auth
import com.csakitheone.onrail.data.model.Message

@Composable
fun MessageDisplay(
    modifier: Modifier = Modifier,
    message: Message,
) {
    val time = remember {
        DateFormat.format(
            "HH:mm",
            message.timestamp
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = if (message.senderId == Auth.currentUser?.uid) Alignment.End
        else Alignment.Start,
    ) {
        Text(
            text = when (message.messageType) {
                Message.TYPE_TEXT -> "${message.senderName} - $time"
                else -> "$time"
            },
            style = MaterialTheme.typography.labelSmall,
        )


        when (message.messageType) {
            Message.TYPE_REPORT -> {
                AssistChip(
                    onClick = {},
                    label = { Text(text = message.content) },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                            imageVector = Icons.Default.Report,
                            contentDescription = null,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(message.color),
                    ),
                )
            }

            Message.TYPE_TEXT -> {
                Card(shape = MaterialTheme.shapes.extraLarge) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = message.content,
                    )
                }
            }
        }

    }
}