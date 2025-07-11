package com.csakitheone.onrail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.csakitheone.onrail.data.Auth
import com.csakitheone.onrail.data.model.Message
import com.csakitheone.onrail.data.sources.MAVINFORM
import com.csakitheone.onrail.data.sources.RTDB
import com.csakitheone.onrail.ui.components.MIArticleDisplay
import com.csakitheone.onrail.ui.components.MessageDisplay
import com.csakitheone.onrail.ui.components.ProfileIcon
import com.csakitheone.onrail.ui.fadingEdge
import com.csakitheone.onrail.ui.theme.OnRailTheme
import kotlinx.coroutines.launch

class TerritoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TerritoryScreen()
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun TerritoryScreen() {
        OnRailTheme {
            val colorScheme = MaterialTheme.colorScheme
            val coroutineScope = rememberCoroutineScope()
            val chatListState = rememberLazyListState()

            val TAB_ARTICLES = 0
            val TAB_CHAT = 1

            var territory by remember { mutableStateOf(MAVINFORM.Territory.BUDAPEST) }
            var selectedTab by remember { mutableIntStateOf(TAB_ARTICLES) }
            val mavinformArticles by remember {
                derivedStateOf {
                    MAVINFORM.articles.filter {
                        it.territoryScopes.contains(territory)
                    }
                }
            }
            var messages by remember { mutableStateOf(listOf<Message>()) }
            var selectedMessage by remember { mutableStateOf<Message?>(null) }
            var messageText by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                territory = MAVINFORM.Territory.fromName(
                    intent.getStringExtra("territoryName")
                ) ?: MAVINFORM.Territory.BUDAPEST
            }

            LaunchedEffect(selectedTab, messages) {
                if (messages.isNotEmpty() && chatListState.layoutInfo.totalItemsCount > 0) {
                    chatListState.scrollToItem(chatListState.layoutInfo.totalItemsCount - 1)
                }
            }

            DisposableEffect(territory) {
                RTDB.listenForMessages(
                    chatRoomType = RTDB.ChatRoomType.TERRITORY,
                    chatRoomId = territory.displayName,
                    onMessageAdded = {
                        messages = (messages + it).sortedBy { msg -> msg.timestamp }

                        if (intent.getBooleanExtra(
                                "bubble",
                                false
                            ) && it.senderId != Auth.currentUser?.uid
                        ) {
                            when (it.messageType) {
                                Message.TYPE_TEXT -> {
                                    NotifUtils.showBubbleForTerritory(
                                        context = this@TerritoryActivity,
                                        territory = territory,
                                        chatMessageSenderName = it.senderName,
                                        chatMessage = it.content
                                    )
                                }
                            }
                        }
                    },
                    onMessageRemoved = {
                        messages =
                            messages.filter { msg -> msg.timestamp != it.timestamp }
                    },
                )

                onDispose {
                    RTDB.stopListeningForMessages()
                }
            }

            if (selectedMessage != null) {
                AlertDialog(
                    onDismissRequest = { selectedMessage = null },
                    title = { Text(text = "Üzenet részletei") },
                    text = {
                        MessageDisplay(
                            modifier = Modifier.fillMaxWidth(),
                            message = selectedMessage!!,
                        )
                    },
                    confirmButton = {
                        if (Auth.currentUser != null && selectedMessage?.senderId == Auth.currentUser?.uid) {
                            TextButton(
                                onClick = {
                                    RTDB.removeMessage(
                                        chatRoomType = RTDB.ChatRoomType.TERRITORY,
                                        chatRoomId = territory.displayName,
                                        message = selectedMessage!!
                                    )
                                    selectedMessage = null
                                }
                            ) {
                                Text("Törlés")
                            }
                        }
                        TextButton(
                            onClick = {
                                val clipboardManager =
                                    getSystemService(ClipboardManager::class.java)
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "Üzenet másolása",
                                        selectedMessage!!.content
                                    )
                                )
                                selectedMessage = null
                            }
                        ) {
                            Text("Másolás")
                        }
                        TextButton(onClick = { selectedMessage = null }) {
                            Text("Bezárás")
                        }
                    },
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HorizontalFloatingToolbar(
                            modifier = Modifier.weight(1f),
                            expanded = true,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconButton(
                                    onClick = {
                                        startActivity(
                                            Intent(this@TerritoryActivity, MainActivity::class.java)
                                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        )
                                        finish()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                        contentDescription = null
                                    )
                                }
                                Text(
                                    text = territory.displayName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        ProfileIcon(
                            extraDropdownMenuItems = { dismiss ->
                                if (!intent.getBooleanExtra("bubble", false)) {
                                    DropdownMenuItem(
                                        onClick = {
                                            NotifUtils.showBubbleForTerritory(
                                                this@TerritoryActivity,
                                                territory
                                            )
                                            dismiss()
                                        },
                                        text = {
                                            Column {
                                                Text(text = "Buborékba helyezés")
                                                Text(
                                                    text = "Csak kompatibilis eszközökön és megfelelő beállításokkal",
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.BubbleChart,
                                                contentDescription = null
                                            )
                                        },
                                    )
                                }
                            },
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            ButtonGroupDefaults.ConnectedSpaceBetween,
                            Alignment.CenterHorizontally,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToggleButton(
                            checked = selectedTab == TAB_ARTICLES,
                            onCheckedChange = { selectedTab = TAB_ARTICLES },
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Newspaper,
                                contentDescription = null,
                            )
                            AnimatedVisibility(selectedTab == TAB_ARTICLES) {
                                Text(
                                    modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing),
                                    text = "Hírek"
                                )
                            }
                        }
                        ToggleButton(
                            checked = selectedTab == TAB_CHAT,
                            onCheckedChange = { selectedTab = TAB_CHAT },
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Chat",
                            )
                            AnimatedVisibility(selectedTab == TAB_CHAT) {
                                Text(
                                    modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing),
                                    text = "Chat"
                                )
                            }
                        }
                    }

                    when (selectedTab) {
                        TAB_ARTICLES -> {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                items(items = mavinformArticles) { article ->
                                    MIArticleDisplay(article = article)
                                }
                                item {
                                    TextButton(
                                        onClick = {
                                            CustomTabsIntent.Builder()
                                                .setDefaultColorSchemeParams(
                                                    CustomTabColorSchemeParams.Builder()
                                                        .setToolbarColor(colorScheme.primary.toArgb())
                                                        .setSecondaryToolbarColor(colorScheme.secondary.toArgb())
                                                        .build()
                                                )
                                                .build()
                                                .launchUrl(
                                                    this@TerritoryActivity,
                                                    territory.getUrl().toUri()
                                                )
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Default.OpenInNew,
                                            contentDescription = null,
                                        )
                                        Text(
                                            modifier = Modifier
                                                .padding(start = ButtonDefaults.IconSpacing),
                                            text = "További hírek a weboldalon",
                                        )
                                    }

                                    Spacer(modifier = Modifier.navigationBarsPadding())
                                }
                            }
                        }

                        TAB_CHAT -> {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fadingEdge(
                                        Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            .1f to Color.Red,
                                            .98f to Color.Red,
                                            1f to Color.Transparent
                                        )
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                state = chatListState,
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 32.dp,
                                    bottom = 16.dp,
                                ),
                            ) {
                                item {
                                    Text(
                                        text = "A régi üzenetek automatikusan törlődnek.",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }

                                items(messages, { it.senderId + it.timestamp }) { message ->
                                    MessageDisplay(
                                        modifier = Modifier.fillMaxWidth(),
                                        message = message,
                                        onClick = { selectedMessage = it },
                                    )
                                }
                            }
                            HorizontalFloatingToolbar(
                                modifier = Modifier
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
                                            value = messageText,
                                            onValueChange = { messageText = it.take(500) },
                                        )
                                        Button(
                                            onClick = {
                                                if (messageText.isBlank()) {
                                                    return@Button
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

                                                RTDB.sendMessage(
                                                    chatRoomType = RTDB.ChatRoomType.TERRITORY,
                                                    chatRoomId = territory.displayName,
                                                    message = message,
                                                ) {
                                                    if (!it) {
                                                        messageText = message.content
                                                    }
                                                }
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
                                                coroutineScope.launch {
                                                    Auth.signInWithGoogle(this@TerritoryActivity)
                                                }
                                            },
                                        ) {
                                            Text(text = "Bejelentkezés")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
