package com.csakitheone.onrail

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
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
import com.csakitheone.onrail.ui.components.MessageDisplay
import com.csakitheone.onrail.ui.components.ProfileIcon
import com.csakitheone.onrail.ui.fadingEdge
import com.csakitheone.onrail.ui.theme.OnRailTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.removeAllMarkers
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

        var isLoading by remember { mutableStateOf(false) }
        var selectedTab by rememberSaveable { mutableIntStateOf(TAB_MAP) }
        var initialTrain by rememberSaveable { mutableStateOf(EMMAVehiclePosition()) }
        var train by rememberSaveable { mutableStateOf(EMMAVehiclePosition()) }
        var trainsLastUpdated by rememberSaveable { mutableLongStateOf(0L) }
        val trainsLastUpdatedText by remember(trainsLastUpdated, isLoading) {
            derivedStateOf {
                if (trainsLastUpdated == 0L) {
                    "Nincs adat"
                } else {
                    "Utoljára frissítve: ${DateFormat.format("HH:mm", trainsLastUpdated)}"
                }
            }
        }
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
        var messageText by rememberSaveable { mutableStateOf("") }
        var isSendingMessage by remember { mutableStateOf(false) }
        var isLocationSendingDialogOpen by rememberSaveable { mutableStateOf(false) }
        var isAddReportMenuOpen by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(train, messages, selectedTab, LocationUtils.current) {
            val latestMessage = messages
                .filter { it.location.isNotBlank() }
                .maxByOrNull { it.timestamp }
            val trainLatLng = LatLng(train.lat, train.lon)
            val latestLatLng = if ((latestMessage?.timestamp ?: 0L) > trainsLastUpdated) {
                LatLng.fromString(latestMessage?.location)
            } else if (trainLatLng != LatLng.ZERO) {
                trainLatLng
            } else if (LocationUtils.current != LatLng.ZERO) {
                LocationUtils.current
            } else {
                // Default to Budapest
                LatLng(47.4979, 19.0402)
            }

            mapState.removeAllMarkers()

            train.trip.stoptimes.forEach { stoptime ->
                val latLng = LatLng(stoptime.stop.lat, stoptime.stop.lon)
                mapState.addMarker(
                    id = "stop-${stoptime.stop.name}",
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
                    id = train.trip.gtfsId,
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
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Train,
                                    contentDescription = "Train position",
                                    tint = Color.Black.copy(alpha = .6f),
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
                            )
                        }
                        Badge {
                            Text(text = "MÁV szerinti pozíció")
                        }
                    }
                }
            }

            messages
                .filter { it.location.isNotBlank() }
                .sortedByDescending { it.timestamp }
                .take(30)
                .forEachIndexed { index, msg ->
                    val latLng = LatLng.fromString(msg.location)
                    val time = DateFormat.format("HH:mm", msg.timestamp)
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

            if (LocationUtils.current == LatLng.ZERO) {
                LocationUtils.getLastKnownLocation(this@TrainActivity) {}
            } else {
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

            mapState.scrollTo(
                x = latestLatLng.normalized.longitude,
                y = latestLatLng.normalized.latitude,
                destScale = .02,
            )
        }

        LaunchedEffect(selectedTab, readableMessages) {
            if (readableMessages.isNotEmpty() && chatListState.layoutInfo.totalItemsCount > 0) {
                chatListState.scrollToItem(chatListState.layoutInfo.totalItemsCount - 1)
            }
        }

        DisposableEffect(Unit) {
            initialTrain = EMMAVehiclePosition.fromJson(intent.getStringExtra("trainJson"))
            train = initialTrain.copy()

            RTDB.listenForMessages(
                chatRoomType = RTDB.ChatRoomType.TRAIN,
                chatRoomId = train.trip.tripShortName,
                onMessageAdded = {
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
                    isLoading = true
                    TrainsProvider.getTrains(this@TrainActivity) { newTrains, lastUpdated ->
                        train = newTrains.firstOrNull { it.trip.gtfsId == train.trip.gtfsId }
                            ?: initialTrain.copy(lat = 0.0, lon = 0.0)

                        trainsLastUpdated = lastUpdated
                        isLoading = false
                    }
                }, 0L, TrainsProvider.SERVER_UPDATE_INTERVAL)
            }

            onDispose {
                RTDB.stopListeningForMessages()
                trainTimer.cancel()
            }
        }

        OnRailTheme {
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

            if (isAddReportMenuOpen) {
                ModalBottomSheet(
                    onDismissRequest = { isAddReportMenuOpen = false },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (isSendingLocationEnabled) Icons.Default.GpsFixed
                            else Icons.Default.GpsOff,
                            contentDescription = null,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Helyadatok küldése")
                            AnimatedVisibility(isSendingLocationEnabled) {
                                Text(
                                    text = "Köszönjük, hogy segítesz <3",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
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
                            checked = isSendingLocationEnabled,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled) {
                                    LocationUtils.requestPermissions(this@TrainActivity) { isGranted ->
                                        isSendingLocationEnabled = isGranted
                                    }
                                } else {
                                    isSendingLocationEnabled = false
                                }
                            },
                            thumbContent = {
                                AnimatedVisibility(isSendingLocationEnabled) {
                                    Icon(
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                    )
                                }
                            },
                        )
                    }

                    HorizontalDivider()

                    FlowRow(
                        modifier = Modifier
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Message.reportOptions.forEach { reportOption ->
                            NavigationBarItem(
                                enabled = isSendingLocationEnabled,
                                selected = false,
                                onClick = {
                                    isSendingMessage = true
                                    isAddReportMenuOpen = false

                                    val message = reportOption.copy(
                                        timestamp = System.currentTimeMillis(),
                                        senderId = Auth.currentUser!!.uid,
                                        senderName = Auth.currentUser!!.displayName
                                            ?: "Ismeretlen",
                                    )

                                    if (isSendingLocationEnabled) {
                                        LocationUtils.getCurrentLocation(
                                            this@TrainActivity
                                        ) { latLng ->
                                            RTDB.sendMessage(
                                                chatRoomType = RTDB.ChatRoomType.TRAIN,
                                                chatRoomId = train.trip.tripShortName,
                                                message = message.copy(
                                                    location = latLng.toString(),
                                                ),
                                            ) {
                                                if (!it) {
                                                    Toast.makeText(
                                                        this@TrainActivity,
                                                        "Hiba történt az üzenet küldésekor!",
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                                isSendingMessage = false
                                            }
                                        }
                                        return@NavigationBarItem
                                    } else {
                                        Toast.makeText(
                                            this@TrainActivity,
                                            "Engedélyezd a helyadatok küldését ehhez!",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        isSendingMessage = false
                                        return@NavigationBarItem
                                    }
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
                                        text = trainsLastUpdatedText,
                                        style = MaterialTheme.typography.labelMedium,
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

                                DropdownMenuItem(
                                    onClick = {
                                        if (LocalSettings.savedTrainTripNames
                                                .contains(train.trip.tripShortName)
                                        ) {
                                            LocalSettings.savedTrainTripNames -= train.trip.tripShortName
                                        } else {
                                            LocalSettings.savedTrainTripNames += train.trip.tripShortName
                                        }
                                        LocalSettings.save(this@TrainActivity)
                                        dismiss()
                                    },
                                    text = {
                                        Text(
                                            text = if (LocalSettings.savedTrainTripNames
                                                    .contains(train.trip.tripShortName)
                                            ) {
                                                "Eltávolítás a mentettek közül"
                                            } else {
                                                "Hozzáadás a mentettekhez"
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null
                                        )
                                    },
                                )
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
                    } else {
                        ToggleButton(
                            checked = isSendingLocationEnabled,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled) {
                                    LocationUtils.requestPermissions(this@TrainActivity) { isGranted ->
                                        isSendingLocationEnabled = isGranted
                                    }
                                } else {
                                    isSendingLocationEnabled = false
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (isSendingLocationEnabled) Icons.Default.GpsFixed
                                else Icons.Default.GpsOff,
                                contentDescription = null,
                            )
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
                                    AnimatedContent(train) {
                                        Text(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            text = "Végállomás: ${it.trip.tripHeadsign}\n" +
                                                    "Késés: ${it.delayMinutes} perc",
                                        )
                                    }
                                } else {
                                    TextField(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(MaterialTheme.shapes.extraLarge),
                                        value = messageText,
                                        onValueChange = { messageText = it.take(500) },
                                        leadingIcon = {
                                            IconButton(
                                                enabled = !isSendingMessage,
                                                onClick = { isAddReportMenuOpen = true },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddCircle,
                                                    contentDescription = null
                                                )
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

                                            if (isSendingLocationEnabled) {
                                                LocationUtils.getCurrentLocation(this@TrainActivity) { latLng ->
                                                    RTDB.sendMessage(
                                                        chatRoomType = RTDB.ChatRoomType.TRAIN,
                                                        chatRoomId = train.trip.tripShortName,
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
                                                chatRoomType = RTDB.ChatRoomType.TRAIN,
                                                chatRoomId = train.trip.tripShortName,
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
                                            AnimatedVisibility(isSendingLocationEnabled) {
                                                Icon(
                                                    modifier = Modifier.size(12.dp),
                                                    imageVector = Icons.Default.GpsFixed,
                                                    contentDescription = "Send message with location",
                                                )
                                            }
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
                                        coroutineScope.launch {
                                            Auth.signInWithGoogle(this@TrainActivity)
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
