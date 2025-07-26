package com.csakitheone.onrail

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.csakitheone.onrail.data.Auth
import com.csakitheone.onrail.data.TrainsProvider
import com.csakitheone.onrail.data.model.EMMAVehiclePosition
import com.csakitheone.onrail.data.model.Message
import com.csakitheone.onrail.data.sources.LocalSettings
import com.csakitheone.onrail.data.sources.RTDB
import com.csakitheone.onrail.ui.components.ChatField
import com.csakitheone.onrail.ui.components.MessageDisplay
import com.csakitheone.onrail.ui.components.ProfileIcon
import com.csakitheone.onrail.ui.components.ServerInfoDialog
import com.csakitheone.onrail.ui.fadingEdge
import com.csakitheone.onrail.ui.theme.OnRailTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.scroll
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.MapUI
import java.util.Timer
import kotlin.concurrent.timerTask
import kotlin.math.cos
import kotlin.math.sin

class TrainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrainScreen()
        }

        LocationUtils.register(this)
        NotifUtils.init(this)
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Preview
    @Composable
    fun TrainScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val TAB_MAP = 0
        val TAB_CHAT = 1
        val TAB_REPORTS_ONLY = 2

        val chatListState = rememberLazyListState()
        val mapState = remember { LocationUtils.getMapState(context) }

        var isLoadingTrain by remember { mutableStateOf(false) }
        var isLoadingChat by remember { mutableStateOf(false) }
        var selectedTab by rememberSaveable { mutableIntStateOf(TAB_MAP) }
        var initialTrain by rememberSaveable { mutableStateOf(EMMAVehiclePosition()) }
        var train by rememberSaveable { mutableStateOf(EMMAVehiclePosition()) }
        var trainsLastUpdated by rememberSaveable { mutableLongStateOf(0L) }
        val trainsLastUpdatedText by remember(trainsLastUpdated, isLoadingTrain) {
            derivedStateOf {
                if (trainsLastUpdated == 0L) {
                    "Nincs adat"
                } else {
                    "Utoljára frissítve: ${DateFormat.format("HH:mm", trainsLastUpdated)}"
                }
            }
        }
        var isServerInfoDialogOpen by rememberSaveable { mutableStateOf(false) }
        var isTrainInfoDialogOpen by rememberSaveable { mutableStateOf(false) }
        var isSendingLocationEnabled by rememberSaveable { mutableStateOf(false) }
        var isSendingLocationHintVisible by rememberSaveable { mutableStateOf(true) }
        var messages by remember { mutableStateOf(listOf<Message>()) }
        val readableMessages by remember(messages, selectedTab) {
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
        var isSendingMessage by remember { mutableStateOf(false) }
        var isLocationSendingDialogOpen by rememberSaveable { mutableStateOf(false) }

        LocationUtils.rememberLocationUpdates(enabled = isSendingLocationEnabled)

        DisposableEffect(isSendingLocationEnabled, LocationUtils.current) {
            if (LocationUtils.current != LatLng.ZERO) {
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
            }

            onDispose {
                mapState.removeMarker("user")
            }
        }

        DisposableEffect(train, selectedTab) {
            val trainLatLng = LatLng(train.lat, train.lon)
            val stops = train.trip.stoptimes.map { it.stop }

            stops.forEach { stop ->
                val latLng = LatLng(stop.lat, stop.lon)
                mapState.addMarker(
                    id = "stop-${stop.name}",
                    x = latLng.normalized.longitude,
                    y = latLng.normalized.latitude,
                    relativeOffset = Offset(-.5f, -.5f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_octagon),
                            contentDescription = null,
                            tint = train.delayColor,
                        )
                    }
                }
            }

            if (trainLatLng != LatLng.ZERO) {
                mapState.addMarker(
                    id = "train",
                    x = trainLatLng.normalized.longitude,
                    y = trainLatLng.normalized.latitude,
                    relativeOffset = Offset(-.5f, -.5f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            FilledIconButton(
                                onClick = {
                                    isTrainInfoDialogOpen = true
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = train.delayColor,
                                    contentColor = train.onDelayColor,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Train,
                                    contentDescription = "Train position",
                                    tint = train.onDelayColor,
                                )
                            }
                            Icon(
                                modifier = Modifier
                                    .offset(
                                        x = (sin(Math.PI * train.heading / 180) * 22).dp,
                                        y = (-cos(Math.PI * train.heading / 180) * 22).dp,
                                    )
                                    .clip(CircleShape)
                                    .background(train.delayColor)
                                    .rotate(train.heading.toFloat() - 90f),
                                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = train.onDelayColor,
                            )
                        }
                        Badge {
                            Text(text = "MÁV szerinti pozíció")
                        }
                    }
                }
            }

            coroutineScope.launch {
                mapState.scrollTo(
                    x = trainLatLng.normalized.longitude,
                    y = trainLatLng.normalized.latitude,
                    destScale = .02,
                )
            }

            onDispose {
                stops.forEach { stop ->
                    mapState.removeMarker("stop-${stop.name}")
                }
                mapState.removeMarker("train")
            }
        }

        DisposableEffect(messages) {
            val messageIds = messages.map { "${it.senderId}-${it.timestamp}" }

            messages
                .filter { it.location.isNotBlank() }
                .sortedByDescending { it.timestamp }
                .take(30)
                .forEachIndexed { index, msg ->
                    val latLng = LatLng.fromString(msg.location)
                    val alpha = 1f / (index + 1) * 3f
                    coroutineScope.launch {
                        delay(index * 100L)
                        mapState.addMarker(
                            id = "${msg.senderId}-${msg.timestamp}",
                            x = latLng.normalized.longitude,
                            y = latLng.normalized.latitude,
                            relativeOffset = Offset(-.5f, -.5f),
                        ) {
                            MessageDisplay(
                                modifier = Modifier.alpha(alpha),
                                message = msg,
                                isMarker = true,
                                onRemoveRequest = {
                                    if (Auth.currentUser?.uid == msg.senderId) {
                                        RTDB.removeMessage(
                                            chatRoomType = RTDB.ChatRoomType.TRAIN,
                                            chatRoomId = train.trip.tripShortName,
                                            message = it,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

            val latestMessageLatLng = LatLng.fromString(
                messages
                    .filter { it.location.isNotBlank() }.maxByOrNull { it.timestamp }
                    ?.location
            )
            if (latestMessageLatLng != LatLng.ZERO) {
                coroutineScope.launch {
                    mapState.scrollTo(
                        x = latestMessageLatLng.normalized.longitude,
                        y = latestMessageLatLng.normalized.latitude,
                        destScale = .02,
                    )
                }
            }

            onDispose {
                messageIds.forEach { mapState.removeMarker(it) }
            }
        }

        LaunchedEffect(selectedTab, readableMessages) {
            if (readableMessages.isNotEmpty() && chatListState.layoutInfo.totalItemsCount > 0) {
                chatListState.scrollToItem(chatListState.layoutInfo.totalItemsCount - 1)
            }
        }

        DisposableEffect(Unit) {
            initialTrain = EMMAVehiclePosition.fromJson(intent.getStringExtra("trainJson"))
            train = initialTrain.copy()

            isLoadingChat = true
            RTDB.getChatRelevances {
                isLoadingChat = false
            }

            RTDB.listenForMessages(
                chatRoomType = RTDB.ChatRoomType.TRAIN,
                chatRoomId = train.trip.tripShortName,
                onMessageAdded = {
                    isLoadingChat = false

                    messages = (messages + it).sortedBy { msg -> msg.timestamp }

                    if (intent.getBooleanExtra(
                            "bubble",
                            false
                        ) && it.senderId != Auth.currentUser?.uid
                    ) {
                        when (it.messageType) {
                            Message.TYPE_TEXT -> {
                                NotifUtils.showBubbleForTrain(
                                    this@TrainActivity,
                                    train,
                                    chatMessageSenderName = it.senderName,
                                    chatMessage = it.content
                                )
                            }

                            Message.TYPE_REPORT -> {
                                NotifUtils.showBubbleForTrain(
                                    this@TrainActivity,
                                    train,
                                    chatMessage = "Új jelentés: ${it.content}"
                                )
                            }
                        }
                    }
                },
                onMessageRemoved = {
                    messages =
                        messages.filter { msg -> msg.key != it.key }
                },
            )

            val trainTimer = Timer("trainTimer").apply {
                schedule(timerTask {
                    isLoadingTrain = true
                    TrainsProvider.getTrains(this@TrainActivity) { newTrains, lastUpdated ->
                        train = newTrains.firstOrNull { it.trip.gtfsId == train.trip.gtfsId }
                            ?: initialTrain.copy(lat = 0.0, lon = 0.0)

                        trainsLastUpdated = lastUpdated
                        isLoadingTrain = false
                    }
                }, 0L, TrainsProvider.SERVER_UPDATE_INTERVAL)
            }

            onDispose {
                RTDB.stopListeningForMessages()
                trainTimer.cancel()
            }
        }

        OnRailTheme {
            if (isServerInfoDialogOpen) {
                ServerInfoDialog(
                    title = trainsLastUpdatedText,
                    onDismissRequest = { isServerInfoDialogOpen = false },
                )
            }

            if (isTrainInfoDialogOpen) {
                AlertDialog(
                    onDismissRequest = { isTrainInfoDialogOpen = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = "Train info",
                        )
                    },
                    title = { Text(text = train.trip.tripShortName) },
                    text = {
                        Text(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            text = "Végállomás: ${train.trip.tripHeadsign}\n\n" +
                                    "Jelenlegi pozíció:\n${train.lat}, ${train.lon}\n\n" +
                                    "Sebesség: ${train.speed}\n\n" +
                                    "Megállók (késéssel):\n" +
                                    train.trip.stoptimes.joinToString("\n") { stoptime ->
                                        "${stoptime.stop.name} (${stoptime.arrivalDelay / 60} perc)"
                                    }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { isTrainInfoDialogOpen = false }) {
                            Text("Bezárás")
                        }
                    },
                )
            }

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
                                    onClick = {
                                        startActivity(
                                            Intent(context, MainActivity::class.java)
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
                                        modifier = Modifier.clickable {
                                            isServerInfoDialogOpen = true
                                        },
                                        text = trainsLastUpdatedText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (LocalSettings.savedTrainTripNames
                                                .contains(train.trip.tripShortName)
                                        ) {
                                            LocalSettings.savedTrainTripNames -= train.trip.tripShortName
                                        } else {
                                            LocalSettings.savedTrainTripNames += train.trip.tripShortName
                                        }
                                        LocalSettings.save(this@TrainActivity)
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (LocalSettings.savedTrainTripNames
                                                .contains(train.trip.tripShortName)
                                        ) Icons.Default.Bookmark
                                        else Icons.Default.BookmarkBorder,
                                        contentDescription = "Save train",
                                    )
                                }
                            }
                        }
                        ProfileIcon(
                            extraDropdownMenuItems = { dismiss ->
                                if (!intent.getBooleanExtra("bubble", false)) {
                                    DropdownMenuItem(
                                        onClick = {
                                            NotifUtils.showBubbleForTrain(
                                                this@TrainActivity,
                                                train
                                            )
                                            dismiss()
                                        },
                                        text = {
                                            Column {
                                                Text(text = "Vonat buborékba helyezése")
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
                            enabled = !isLoadingChat,
                            checked = selectedTab != TAB_MAP,
                            onCheckedChange = { selectedTab = TAB_CHAT },
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        ) {
                            if (isLoadingChat) {
                                LoadingIndicator(modifier = Modifier.size(IconButtonDefaults.smallIconSize))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Chat",
                                )
                            }
                            AnimatedVisibility(selectedTab == TAB_MAP && messages.isNotEmpty()) {
                                Text(
                                    modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing),
                                    text = DateFormat.format("HH:mm", messages.last().timestamp)
                                        .toString(),
                                )
                            }
                            AnimatedVisibility(selectedTab != TAB_MAP) {
                                Text(
                                    modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing),
                                    text = "Chat"
                                )
                            }
                        }
                        AnimatedVisibility(
                            modifier = Modifier.padding(start = 8.dp),
                            visible = selectedTab != TAB_MAP,
                        ) {
                            ElevatedFilterChip(
                                selected = selectedTab == TAB_REPORTS_ONLY,
                                onClick = {
                                    selectedTab = if (selectedTab == TAB_REPORTS_ONLY) TAB_CHAT
                                    else TAB_REPORTS_ONLY
                                },
                                label = {
                                    Text(text = "Csak jelentések")
                                },
                                leadingIcon = {
                                    Icon(
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        imageVector = Icons.Default.Report,
                                        contentDescription = "Reports",
                                    )
                                },
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        if (selectedTab != TAB_MAP) {
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

                                items(readableMessages, { it.senderId + it.timestamp }) { message ->
                                    MessageDisplay(
                                        modifier = Modifier.fillMaxWidth(),
                                        message = message,
                                        selfColor = train.delayColor,
                                        onSelfColor = train.onDelayColor,
                                        onRemoveRequest = {
                                            if (Auth.currentUser?.uid == message.senderId) {
                                                RTDB.removeMessage(
                                                    chatRoomType = RTDB.ChatRoomType.TRAIN,
                                                    chatRoomId = train.trip.tripShortName,
                                                    message = it,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (isSendingLocationHintVisible) {
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(),
                            backgroundContent = {},
                            onDismiss = { isSendingLocationHintVisible = false }
                        ) {
                            Card(
                                modifier = Modifier.padding(8.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = "Ezen a vonaton ülsz épp?",
                                        )
                                        TextButton(
                                            onClick = { isSendingLocationHintVisible = false },
                                        ) {
                                            Text(text = "Nem")
                                        }
                                        TextButton(
                                            onClick = {
                                                isSendingLocationHintVisible = false
                                                LocationUtils.requestPermissions(this@TrainActivity) { isGranted ->
                                                    isSendingLocationEnabled = isGranted
                                                }
                                            },
                                        ) {
                                            Text(text = "Igen")
                                        }
                                    }
                                    Text(
                                        text = "Segítsd utastársaid helyadatok küldésével, hogy mindenki " +
                                                "pontos információkat kapjon a vonat helyzetéről.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ElevatedFilterChip(
                            selected = isSendingLocationEnabled,
                            onClick = {
                                if (!isSendingLocationEnabled) {
                                    LocationUtils.requestPermissions(this@TrainActivity) { isGranted ->
                                        isSendingLocationEnabled = isGranted
                                    }
                                } else {
                                    isSendingLocationEnabled = false
                                }
                            },
                            label = { Text(text = "Helyadatok küldése") },
                            leadingIcon = {
                                Icon(
                                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    imageVector = if (isSendingLocationEnabled) Icons.Default.GpsFixed
                                    else Icons.Default.GpsOff,
                                    contentDescription = null,
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    onClick = { isLocationSendingDialogOpen = true },
                                ) {
                                    Icon(
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                        imageVector = Icons.AutoMirrored.Default.Help,
                                        contentDescription = null,
                                    )
                                }
                            },
                        )
                        ElevatedAssistChip(
                            onClick = {},
                            label = { Text(text = "Késés: ${train.delayMinutes} perc") },
                            colors = AssistChipDefaults.elevatedAssistChipColors(
                                containerColor = train.delayColor,
                                labelColor = train.onDelayColor,
                            ),
                        )
                        ElevatedAssistChip(
                            onClick = {},
                            label = { Text(text = "Végállomás: ${train.trip.tripHeadsign}") },
                        )
                    }

                    ChatField(
                        enabled = !isSendingMessage,
                        onSend = { message ->
                            isSendingMessage = true

                            if (
                                (message.messageType == Message.TYPE_LOCATION_PING ||
                                        message.messageType == Message.TYPE_REPORT) &&
                                !isSendingLocationEnabled
                            ) {
                                Toast.makeText(
                                    this@TrainActivity,
                                    "Ehhez engedélyezned kell a helyadatok küldését.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isSendingMessage = false
                                return@ChatField
                            }

                            selectedTab = TAB_CHAT

                            if (isSendingLocationEnabled) {
                                LocationUtils.getCurrentLocation(this@TrainActivity) { latLng ->
                                    RTDB.sendMessage(
                                        chatRoomType = RTDB.ChatRoomType.TRAIN,
                                        chatRoomId = train.trip.tripShortName,
                                        message = message.copy(location = latLng.toString()),
                                    ) {
                                        isSendingMessage = false
                                    }
                                }
                                return@ChatField
                            }

                            RTDB.sendMessage(
                                chatRoomType = RTDB.ChatRoomType.TRAIN,
                                chatRoomId = train.trip.tripShortName,
                                message = message,
                            ) {
                                isSendingMessage = false
                            }
                        },
                        allowTrainReports = true,
                    )
                }
            }
        }
    }
}
