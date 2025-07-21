package com.csakitheone.onrail.ui.components

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csakitheone.onrail.LocationUtils
import com.csakitheone.onrail.data.Auth
import com.csakitheone.onrail.data.model.Message
import com.csakitheone.onrail.data.sources.RTDB
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatField(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSend: (Message) -> Unit,
    allowAttachments: Boolean = true,
    allowTrainReports: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalActivity.current

    val ATT_TAB_REPORTS = "reports"
    val ATT_TAB_WISECRACKS = "wisecracks"

    var isAttachmentsSheetOpen by remember { mutableStateOf(false) }
    var wisecracks by remember { mutableStateOf(emptyList<String>()) }
    var messageText by remember { mutableStateOf("") }
    val enabledTabs by remember(allowTrainReports, wisecracks) {
        derivedStateOf {
            val tabs = mutableListOf<String>()
            if (allowTrainReports) {
                tabs += ATT_TAB_REPORTS
            }
            if (wisecracks.isNotEmpty()) {
                tabs += ATT_TAB_WISECRACKS
            }
            tabs
        }
    }
    var selectedAttachmentsTab by remember(enabledTabs) {
        mutableStateOf(enabledTabs.firstOrNull())
    }

    fun prepareSendTextMessage() {
        if (messageText.isBlank()) {
            return
        }

        val message = Message(
            timestamp = System.currentTimeMillis(),
            senderId = Auth.currentUser!!.uid,
            senderName = Auth.currentUser!!.displayName
                ?: "Ismeretlen",
            messageType = Message.TYPE_TEXT,
            content = messageText
                .take(RTDB.MESSAGE_CONTENT_LENGTH_LIMIT)
                .trim(),
        )
        messageText = ""

        onSend(message)
    }

    LaunchedEffect(Unit) {
        RTDB.getWisecracks { wisecracks = it }
    }

    if (enabled && isAttachmentsSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isAttachmentsSheetOpen = false },
        ) {
            Column(
                modifier = Modifier.navigationBarsPadding(),
            ) {
                when (selectedAttachmentsTab) {
                    ATT_TAB_REPORTS -> {
                        FlowRow(
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Message.reportOptions.forEach { reportOption ->
                                NavigationBarItem(
                                    selected = false,
                                    onClick = {
                                        val message = reportOption.copy(
                                            timestamp = System.currentTimeMillis(),
                                            senderId = Auth.currentUser!!.uid,
                                            senderName = Auth.currentUser!!.displayName
                                                ?: "Ismeretlen",
                                        )

                                        onSend(message)
                                        isAttachmentsSheetOpen = false
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Message.getImageVector(reportOption),
                                            contentDescription = null,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = reportOption.content,
                                            textAlign = TextAlign.Center,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    ATT_TAB_WISECRACKS -> {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            wisecracks.forEachIndexed { index, wisecrack ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    onClick = {
                                        val message = Message(
                                            timestamp = System.currentTimeMillis(),
                                            senderId = Auth.currentUser!!.uid,
                                            senderName = Auth.currentUser!!.displayName
                                                ?: "Ismeretlen",
                                            messageType = Message.TYPE_TEXT,
                                            content = wisecrack
                                                .take(RTDB.MESSAGE_CONTENT_LENGTH_LIMIT),
                                        )
                                        onSend(message)
                                        isAttachmentsSheetOpen = false
                                    },
                                    shape = when (index) {
                                        0 -> RoundedCornerShape(
                                            topStart = MaterialTheme.shapes.large.topStart,
                                            topEnd = MaterialTheme.shapes.large.topEnd,
                                            bottomStart = CornerSize(0f),
                                            bottomEnd = CornerSize(0f),
                                        )

                                        wisecracks.lastIndex -> RoundedCornerShape(
                                            topStart = CornerSize(0f),
                                            topEnd = CornerSize(0f),
                                            bottomStart = MaterialTheme.shapes.large.bottomStart,
                                            bottomEnd = MaterialTheme.shapes.large.bottomEnd,
                                        )

                                        else -> RectangleShape
                                    }
                                ) {
                                    Text(modifier = Modifier.padding(16.dp), text = wisecrack)
                                }
                            }
                        }
                    }

                    else -> {
                        Text(text = "Jelenleg nem tudsz csatolni semmit.")
                    }
                }
                PrimaryTabRow(
                    selectedTabIndex = enabledTabs.indexOf(selectedAttachmentsTab),
                ) {
                    if (ATT_TAB_REPORTS in enabledTabs) {
                        Tab(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Report,
                                    contentDescription = "Jelentés hozzáadása",
                                )
                            },
                            text = { Text("Jelentések") },
                            selected = selectedAttachmentsTab == ATT_TAB_REPORTS,
                            onClick = {
                                selectedAttachmentsTab = ATT_TAB_REPORTS
                            },
                        )
                    }
                    if (ATT_TAB_WISECRACKS in enabledTabs) {
                        Tab(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.EmojiEmotions,
                                    contentDescription = "Szállóige hozzáadása",
                                )
                            },
                            text = { Text("Szállóigék") },
                            selected = selectedAttachmentsTab == ATT_TAB_WISECRACKS,
                            onClick = {
                                selectedAttachmentsTab = ATT_TAB_WISECRACKS
                            },
                        )
                    }
                }
            }
        }
    }

    HorizontalFloatingToolbar(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(8.dp)
            .navigationBarsPadding()
            .imePadding(),
        expanded = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (Auth.currentUser != null) {
                TextField(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.extraLarge),
                    enabled = enabled,
                    value = messageText,
                    onValueChange = { messageText = it.take(500) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions {
                        prepareSendTextMessage()
                    },
                    leadingIcon = {
                        if (allowAttachments) {
                            IconButton(
                                enabled = enabled,
                                onClick = { isAttachmentsSheetOpen = true },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                )
                Button(
                    enabled = enabled,
                    onClick = {
                        prepareSendTextMessage()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Send,
                        contentDescription = "Send message",
                    )
                }
            } else {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Chat használatához jelentkezz be!",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = {
                        if (activity == null) {
                            return@Button
                        }

                        coroutineScope.launch {
                            Auth.signInWithGoogle(activity)
                        }
                    },
                ) {
                    Text(text = "Bejelentkezés")
                }
            }
        }
    }
}