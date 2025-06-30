package com.csakitheone.onrail

import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.csakitheone.onrail.data.Auth
import com.csakitheone.onrail.data.model.EMMAVehiclePosition
import com.csakitheone.onrail.data.model.Message
import com.csakitheone.onrail.data.sources.LocalSettings
import com.csakitheone.onrail.data.sources.RTDB
import com.csakitheone.onrail.ui.components.MessageDisplay
import com.csakitheone.onrail.ui.components.ProfileIcon
import com.csakitheone.onrail.ui.fadingEdge
import com.csakitheone.onrail.ui.theme.OnRailTheme
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableZooming
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import java.net.URL
import kotlin.math.pow

class TrainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrainScreen()
        }

        LocationUtils.register(this)
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Preview
    @Composable
    fun TrainScreen() {
        val activity = LocalActivity.current

        val TAB_MAP = 0
        val TAB_CHAT = 1
        val TAB_REPORTS_ONLY = 2

        val chatListState = rememberLazyListState()
        val mapState = remember { LocationUtils.getMapState() }

        var selectedTab by remember { mutableIntStateOf(TAB_MAP) }
        var train by remember { mutableStateOf(EMMAVehiclePosition()) }
        var trainsLastUpdated by remember { mutableLongStateOf(0L) }
        val trainsLastUpdatedText by remember(trainsLastUpdated) {
            derivedStateOf {
                if (trainsLastUpdated == 0L) {
                    "Nincs adat"
                } else {
                    "Utoljára frissítve: ${DateFormat.format("HH:mm", trainsLastUpdated)}"
                }
            }
        }
        var messages by remember { mutableStateOf(listOf<Message>()) }
        val visibleMessages by remember(messages, selectedTab) {
            derivedStateOf {
                if (selectedTab == TAB_REPORTS_ONLY) {
                    messages.filter { it.messageType == Message.TYPE_REPORT }
                } else {
                    messages.filter {
                        listOf(
                            Message.TYPE_TEXT,
                            Message.TYPE_REPORT,
                        ).contains(it.messageType)
                    }
                }
            }
        }
        var isLocationSendingDialogOpen by remember { mutableStateOf(false) }
        var isAddReportMenuOpen by remember { mutableStateOf(false) }
        var messageText by remember { mutableStateOf("") }
        var isSendingMessage by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            train = EMMAVehiclePosition.fromJson(intent.getStringExtra("trainJson"))
            RTDB.getVehiclePositionsRelevance { trainsLastUpdated = it }
        }

        LaunchedEffect(train, messages, selectedTab, LocationUtils.current) {
            val latestMessageLatLng = LatLng.fromString(
                messages
                    .filter { it.location.isNotBlank() }
                    .maxByOrNull { it.timestamp }
                    ?.location
            )
            val trainLatLng = LatLng(train.lat, train.lon)
            val latestLatLng = if (latestMessageLatLng == LatLng.ZERO) {
                trainLatLng
            } else {
                latestMessageLatLng
            }

            mapState.removeAllMarkers()

            mapState.addMarker(
                id = "user",
                x = LocationUtils.current.normalized.longitude,
                y = LocationUtils.current.normalized.latitude,
                relativeOffset = Offset(-.5f, -.5f),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Blue)
                        .alpha(.2f),
                )
            }

            mapState.addMarker(
                id = train.trip.gtfsId,
                x = trainLatLng.normalized.longitude,
                y = trainLatLng.normalized.latitude,
                relativeOffset = Offset(-.5f, -.5f),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Train,
                        contentDescription = "Train position",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Badge {
                        Text(text = "MÁV szerinti pozíció")
                    }
                }
            }

            messages
                .filter { it.location.isNotBlank() }
                .sortedByDescending { it.timestamp }
                .take(20)
                .forEachIndexed { index, msg ->
                    val latLng = LatLng.fromString(msg.location)
                    val time = DateFormat.format("HH:mm", msg.timestamp)
                    val alpha = 1f / (index + 1)
                    mapState.addMarker(
                        id = "${msg.senderId}-${msg.timestamp}",
                        x = latLng.normalized.longitude,
                        y = latLng.normalized.latitude,
                        relativeOffset = Offset(-.5f, -.5f),
                    ) {
                        Column(
                            modifier = Modifier.alpha(alpha),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = when (msg.messageType) {
                                    Message.TYPE_REPORT -> Icons.Default.Report
                                    Message.TYPE_TEXT -> Icons.Default.ChatBubble
                                    else -> Icons.Default.GpsFixed
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Badge {
                                Text(text = "${time}")
                            }
                        }
                    }
                }

            mapState.scrollTo(
                x = latestLatLng.normalized.longitude,
                y = latestLatLng.normalized.latitude,
                destScale = .1,
            )
        }

        LaunchedEffect(visibleMessages) {
            if (visibleMessages.isNotEmpty()) {
                chatListState.animateScrollToItem(visibleMessages.lastIndex)
            }
        }

        DisposableEffect(Unit) {
            RTDB.getMessages(
                trainId = train.trip.tripShortName,
                callback = { newMessages ->
                    messages = newMessages.sortedBy { it.timestamp }
                    RTDB.listenForMessages(
                        trainId = train.trip.tripShortName,
                        onMessageAdded = {
                            messages = (messages + it).sortedBy { msg -> msg.timestamp }
                        },
                        onMessageRemoved = {
                            messages =
                                messages.filter { msg -> msg.timestamp != it.timestamp && msg.senderId != it.senderId }
                        },
                    )
                }
            )

            onDispose {
                RTDB.stopListeningForMessages()
            }
        }

        OnRailTheme {
            if (isLocationSendingDialogOpen) {
                AlertDialog(
                    onDismissRequest = { isLocationSendingDialogOpen = false },
                    title = { Text(text = "Helyadatok küldése") },
                    text = {
                        Text(
                            text = "A helyadatok küldése segít az utastársaknak pontos információkat kapni a vonat helyzetéről. " +
                                    "Ha engedélyezed, az app időnként elküldi a pozíciódat minden utasnak vagy vonatra várónak.",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { isLocationSendingDialogOpen = false },
                        ) {
                            Text(text = "Rendben")
                        }
                    },
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                MapUI(
                    modifier = Modifier.fillMaxSize(),
                    state = mapState,
                )
                AnimatedVisibility(
                    visible = selectedTab != TAB_MAP,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(.8f),
                        color = MaterialTheme.colorScheme.background,
                    ) {}
                }
                Column(
                    modifier = Modifier.systemBarsPadding(),
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
                                    onClick = { finish() },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                        contentDescription = null
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = train.trip.tripShortName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = trainsLastUpdatedText,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                        ProfileIcon()
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            ButtonGroupDefaults.ConnectedSpaceBetween,
                            Alignment.CenterHorizontally,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToggleButton(
                            checked = selectedTab == TAB_MAP,
                            onCheckedChange = { selectedTab = TAB_MAP },
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                            )
                            AnimatedVisibility(selectedTab == TAB_MAP) {
                                Text(
                                    modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing),
                                    text = "Térkép"
                                )
                            }
                        }
                        ToggleButton(
                            checked = selectedTab == TAB_CHAT,
                            onCheckedChange = { selectedTab = TAB_CHAT },
                            shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
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
                        ToggleButton(
                            checked = selectedTab == TAB_REPORTS_ONLY,
                            onCheckedChange = { selectedTab = TAB_REPORTS_ONLY },
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = "Reports",
                            )
                            AnimatedVisibility(selectedTab == TAB_REPORTS_ONLY) {
                                Text(
                                    modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing),
                                    text = "Jelentések"
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        AnimatedVisibility(
                            visible = selectedTab != TAB_MAP
                        ) {
                            LazyColumn(
                                modifier = Modifier
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

                                items(visibleMessages, { it.senderId + it.timestamp }) { message ->
                                    MessageDisplay(
                                        modifier = Modifier.fillMaxWidth(),
                                        message = message,
                                    )
                                }
                            }
                        }
                    }

                    if (Auth.currentUser != null) {
                        HorizontalFloatingToolbar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(8.dp),
                            expanded = true,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = if (LocalSettings.isSendingLocationEnabled) Icons.Default.GpsFixed
                                    else Icons.Default.GpsOff,
                                    contentDescription = null,
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = "Helyadatok küldése")
                                    Text(
                                        text = if (LocalSettings.isSendingLocationEnabled) "Köszönjük, hogy segítesz <3"
                                        else "Segítsd utastársaid pontos infókkal!",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                IconButton(
                                    onClick = { isLocationSendingDialogOpen = true },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.Help,
                                        contentDescription = null,
                                    )
                                }
                                Switch(
                                    checked = LocalSettings.isSendingLocationEnabled,
                                    onCheckedChange = { isEnabled ->
                                        if (isEnabled) {
                                            LocationUtils.requestPermissions { isGranted ->
                                                LocalSettings.isSendingLocationEnabled = isGranted
                                                LocalSettings.save(this@TrainActivity)
                                            }
                                        } else {
                                            LocalSettings.isSendingLocationEnabled = false
                                            LocalSettings.save(this@TrainActivity)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    HorizontalFloatingToolbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(8.dp)
                            .imePadding(),
                        expanded = true,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (Auth.currentUser != null) {
                                if (selectedTab == TAB_MAP) {
                                    Text(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        text = "Végállomás: ${train.trip.tripHeadsign}",
                                    )
                                } else {
                                    TextField(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(MaterialTheme.shapes.extraLarge),
                                        value = messageText,
                                        onValueChange = { messageText = it.take(500) },
                                        leadingIcon = {
                                            IconButton(
                                                onClick = { isAddReportMenuOpen = true },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddCircle,
                                                    contentDescription = null
                                                )
                                                DropdownMenu(
                                                    expanded = isAddReportMenuOpen,
                                                    onDismissRequest = {
                                                        isAddReportMenuOpen = false
                                                    },
                                                ) {
                                                    Message.reportOptions.forEach { reportOption ->
                                                        DropdownMenuItem(
                                                            text = { Text(reportOption.content) },
                                                            onClick = {
                                                                isSendingMessage = true
                                                                isAddReportMenuOpen = false

                                                                val message = reportOption.copy(
                                                                    timestamp = System.currentTimeMillis(),
                                                                    senderId = Auth.currentUser!!.uid,
                                                                    senderName = Auth.currentUser!!.displayName
                                                                        ?: "Ismeretlen",
                                                                )

                                                                if (LocalSettings.isSendingLocationEnabled) {
                                                                    LocationUtils.getCurrentLocation(
                                                                        this@TrainActivity
                                                                    ) { latLng ->
                                                                        RTDB.sendMessage(
                                                                            trainId = train.trip.tripShortName,
                                                                            message = message.copy(
                                                                                location = latLng.toString(),
                                                                            ),
                                                                        ) {
                                                                            isSendingMessage = false
                                                                        }
                                                                    }
                                                                    return@DropdownMenuItem
                                                                } else if (message.messageType == Message.TYPE_LOCATION_PING) {
                                                                    Toast.makeText(
                                                                        this@TrainActivity,
                                                                        "Engedélyezd a helyadatok küldését ehhez!",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                    isSendingMessage = false
                                                                    return@DropdownMenuItem
                                                                }

                                                                RTDB.sendMessage(
                                                                    trainId = train.trip.tripShortName,
                                                                    message = message,
                                                                ) {
                                                                    isSendingMessage = false
                                                                }
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                    )
                                    Button(
                                        enabled = !isSendingMessage,
                                        onClick = {
                                            isSendingMessage = true
                                            selectedTab = TAB_CHAT

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

                                            if (LocalSettings.isSendingLocationEnabled) {
                                                LocationUtils.getLastKnownLocation(this@TrainActivity) { latLng ->
                                                    RTDB.sendMessage(
                                                        trainId = train.trip.tripShortName,
                                                        message = message.copy(
                                                            location = latLng.toString(),
                                                        ),
                                                    ) {
                                                        if (!it) {
                                                            messageText = message.content
                                                        }
                                                        isSendingMessage = false
                                                    }
                                                }
                                                return@Button
                                            }

                                            RTDB.sendMessage(
                                                trainId = train.trip.tripShortName,
                                                message = message,
                                            ) {
                                                if (!it) {
                                                    messageText = message.content
                                                }
                                                isSendingMessage = false
                                            }
                                        }
                                    ) {
                                        if (isSendingMessage) {
                                            LoadingIndicator(
                                                modifier = Modifier.size(24.dp),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Default.Send,
                                                contentDescription = "Send message",
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = "Chat használatához jelentkezz be!",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Button(
                                    onClick = {
                                        Auth.signInWithGoogle(this@TrainActivity)
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
