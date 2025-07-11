package com.csakitheone.onrail.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.csakitheone.onrail.data.Auth
import com.csakitheone.onrail.data.model.Message
import com.csakitheone.onrail.data.sources.RTDB

@Composable
fun MessageDisplay(
    modifier: Modifier = Modifier,
    message: Message,
    isMarker: Boolean = false,
    selectable: Boolean = true,
    onClick: (Message) -> Unit = {},
    onRemoveRequest: (Message) -> Unit = {},
) {
    val context = LocalContext.current
    val time = remember {
        DateFormat.format(
            "HH:mm",
            message.timestamp
        )
    }

    var isSelected by remember { mutableStateOf(false) }

    if (isSelected) {
        AlertDialog(
            onDismissRequest = { isSelected = false },
            text = {
                Column {
                    MessageDisplay(
                        modifier = Modifier.fillMaxWidth(),
                        message = message,
                        selectable = false,
                        onClick = { isSelected = false },
                    )

                }
            },
            confirmButton = {
                Row {
                    if (Auth.currentUser != null && message.senderId == Auth.currentUser?.uid) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onRemoveRequest(message)
                                isSelected = false
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                            )
                            Text(
                                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                                text = "Törlés",
                            )
                        }
                    }
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val clipboardManager =
                                context.getSystemService(ClipboardManager::class.java)
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Üzenet másolása",
                                    message.content
                                )
                            )
                            isSelected = false
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                        )
                        Text(
                            modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                            text = "Másolás",
                        )
                    }
                }
            },
        )
    }

    if (isMarker) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FilledIconButton(
                onClick = { isSelected = true },
            ) {
                Icon(
                    imageVector = Message.getImageVector(message),
                    contentDescription = null,
                )
            }
            Badge {
                Text(text = "${time}")
            }
        }
    }
    else {
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
                        onClick = {
                            if (selectable) isSelected = true
                            else onClick(message)
                        },
                        label = { Text(text = message.content) },
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                                imageVector = Message.getImageVector(message),
                                contentDescription = null,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(message.color),
                        ),
                    )
                }

                Message.TYPE_TEXT -> {
                    Card(
                        onClick = {
                            if (selectable) isSelected = true
                            else onClick(message)
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.senderId == Auth.currentUser?.uid) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (message.senderId == Auth.currentUser?.uid) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = message.content,
                        )
                    }
                }
            }

        }
    }
}