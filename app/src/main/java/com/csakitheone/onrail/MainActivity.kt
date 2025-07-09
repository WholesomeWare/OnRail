package com.csakitheone.onrail

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import com.csakitheone.onrail.data.TrainsProvider
import com.csakitheone.onrail.data.model.EMMAVehiclePosition
import com.csakitheone.onrail.data.sources.LocalSettings
import com.csakitheone.onrail.data.sources.MAVINFORM
import com.csakitheone.onrail.data.sources.RTDB
import com.csakitheone.onrail.ui.components.MIArticleDisplay
import com.csakitheone.onrail.ui.components.ProfileIcon
import com.csakitheone.onrail.ui.theme.OnRailTheme
import com.csakitheone.onrail.ui.theme.colorDelayDrastic
import com.csakitheone.onrail.ui.theme.colorDelayMajor
import com.csakitheone.onrail.ui.theme.colorDelayMinor
import com.csakitheone.onrail.ui.theme.colorDelayNone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addClusterer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.removeClusterer
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy
import java.util.Timer
import kotlin.concurrent.timerTask

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            MainScreen()
        }

        AppUpdateUtils.checkForUpdates(this)

        LocationUtils.register(this)
        NotifUtils.init(this)
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Preview
    @Composable
    fun MainScreen() {
        OnRailTheme {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val colorScheme = MaterialTheme.colorScheme
            val mapState = remember { LocationUtils.getMapState(context) }

            val TAB_MAP = 0
            val TAB_MAVINFORM = 1

            var motdText by remember { mutableStateOf("") }
            var isMotdCollapsed by rememberSaveable { mutableStateOf(false) }
            var isLoading by remember { mutableStateOf(true) }
            var trains by remember { mutableStateOf(emptyList<EMMAVehiclePosition>()) }
            var trainsLastUpdated by remember { mutableLongStateOf(0L) }

            var isUpdateInfoDialogOpen by remember { mutableStateOf(false) }
            var selectedTab by rememberSaveable { mutableIntStateOf(TAB_MAP) }

            var mapFilterTrains by remember { mutableStateOf(true) }
            var mapFilterMavinform by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }
            var isLoadingLocation by remember { mutableStateOf(false) }

            val trainsLastUpdatedText by remember(isLoading, trainsLastUpdated) {
                derivedStateOf {
                    if (isLoading) {
                        "Frissítés..."
                    } else if (trainsLastUpdated == 0L) {
                        "Nincs adat"
                    } else {
                        "Utoljára frissítve: ${DateFormat.format("HH:mm", trainsLastUpdated)}"
                    }
                }
            }
            val visibleTrains by remember(trains, searchQuery) {
                derivedStateOf {
                    trains.filter { train ->
                        train.trip.tripShortName.contains(searchQuery, ignoreCase = true) ||
                                train.trip.tripHeadsign.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            DisposableEffect(Unit) {
                RTDB.getConfigString(RTDB.CONFIG_KEY_MOTD) { motdText = it }

                MAVINFORM.fetchArticles()

                val latLngHungary = LatLng(47.1625, 19.5033)
                coroutineScope.launch {
                    mapState.scrollTo(
                        x = latLngHungary.normalized.longitude,
                        y = latLngHungary.normalized.latitude,
                        destScale = .0005,
                    )
                }

                val trainTimer = Timer("trainTimer").apply {
                    schedule(timerTask {
                        isLoading = true
                        TrainsProvider.getTrains(this@MainActivity) { newTrains, lastUpdated ->
                            trains = newTrains
                            trainsLastUpdated = lastUpdated
                            isLoading = false
                        }
                    }, 0L, TrainsProvider.SERVER_UPDATE_INTERVAL)
                }

                onDispose {
                    trainTimer.cancel()
                }
            }

            LaunchedEffect(
                LocationUtils.current,
                visibleTrains,
                MAVINFORM.articles,
                mapFilterTrains,
                mapFilterMavinform,
            ) {
                mapState.removeAllMarkers()
                mapState.removeClusterer("trains")

                mapState.addClusterer("trains") { ids ->
                    val worstDelay = ids.mapNotNull { id ->
                        visibleTrains.firstOrNull { it.trip.gtfsId == id }?.delayMinutes
                    }.max()
                    val worstDelayColor = EMMAVehiclePosition.getDelayColor(worstDelay)

                    return@addClusterer {
                        Surface(
                            shape = CircleShape,
                            color = worstDelayColor,
                        ) {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = ids.size.toString(),
                                color = Color.Black,
                            )
                        }
                    }
                }

                if (mapFilterTrains) {
                    visibleTrains.forEach { train ->
                        val latLng = LatLng(train.lat, train.lon)
                        mapState.addMarker(
                            id = train.trip.gtfsId,
                            x = latLng.normalized.longitude,
                            y = latLng.normalized.latitude,
                            relativeOffset = Offset(-.5f, -.5f),
                            renderingStrategy = RenderingStrategy.Clustering("trains"),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val interactionSource = remember { MutableInteractionSource() }
                                val viewConfiguration = LocalViewConfiguration.current
                                LaunchedEffect(interactionSource) {
                                    var isLongPress = false
                                    interactionSource.interactions.collectLatest {
                                        when (it) {
                                            is PressInteraction.Press -> {
                                                isLongPress = false
                                                delay(viewConfiguration.longPressTimeoutMillis)
                                                isLongPress = true
                                                NotifUtils.showBubbleForTrain(
                                                    this@MainActivity,
                                                    train
                                                )
                                            }

                                            is PressInteraction.Release -> {
                                                if (!isLongPress) {
                                                    startActivity(
                                                        Intent(
                                                            this@MainActivity,
                                                            TrainActivity::class.java
                                                        ).putExtra("trainJson", train.toString())
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                FilledIconButton(
                                    onClick = {},
                                    interactionSource = interactionSource,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = train.delayColor,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Train,
                                        contentDescription = null,
                                        tint = Color.Black.copy(alpha = .6f),
                                    )
                                }
                                Badge {
                                    Text(text = train.trip.tripShortName)
                                }
                            }
                        }
                    }
                }

                if (mapFilterMavinform) {
                    MAVINFORM.Territory.entries.forEach { territory ->
                        val newsCount = MAVINFORM.articles
                            .count { it.scopes.contains(territory.displayName) }

                        mapState.addMarker(
                            id = "territory-${territory.id}",
                            x = territory.latLng.normalized.longitude,
                            y = territory.latLng.normalized.latitude,
                            relativeOffset = Offset(-.5f, -.5f),
                        ) {
                            Column(
                                modifier = Modifier.alpha(.5f + newsCount * .2f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val interactionSource = remember { MutableInteractionSource() }
                                val viewConfiguration = LocalViewConfiguration.current
                                LaunchedEffect(interactionSource) {
                                    var isLongPress = false
                                    interactionSource.interactions.collectLatest {
                                        when (it) {
                                            is PressInteraction.Press -> {
                                                isLongPress = false
                                                delay(viewConfiguration.longPressTimeoutMillis)
                                                isLongPress = true
                                                NotifUtils.showBubbleForTerritory(
                                                    this@MainActivity,
                                                    territory
                                                )
                                            }

                                            is PressInteraction.Release -> {
                                                if (!isLongPress) {
                                                    startActivity(
                                                        Intent(
                                                            context,
                                                            TerritoryActivity::class.java
                                                        ).putExtra(
                                                            "territoryName",
                                                            territory.displayName
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                FilledIconButton(
                                    modifier = Modifier.size((24 + newsCount * 3).dp),
                                    onClick = {},
                                    interactionSource = interactionSource,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                    )
                                }
                                Badge {
                                    Text(
                                        text = territory.displayName + if (newsCount > 0) " ($newsCount)" else "",
                                    )
                                }
                            }
                        }
                    }
                }

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
                                .background(Color.Blue),
                        )
                    }
                }
            }

            if (isUpdateInfoDialogOpen) {
                AlertDialog(
                    onDismissRequest = { isUpdateInfoDialogOpen = false },
                    title = { Text(text = trainsLastUpdatedText) },
                    text = {
                        Column {
                            Text(
                                text = "Az alkalmazás egy saját szerveren keresztül kéri le az " +
                                        "adatokat, hogy a MÁV szerverei ne terhelődjenek. " +
                                        "Így a MÁV szerverek ugyanannyi kérést kapnak 1, 100 vagy " +
                                        "10000 felhasználónál is.\n" +
                                        "Az utolsó frissítés ideje ezeknek a kéréseknek az idejét " +
                                        "mutatja.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = "Utas telója",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = "Firebase SDK",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_database_24px),
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = "Saját szerver",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = "EMMA API",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_host_24px),
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = "MÁV szerver",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { isUpdateInfoDialogOpen = false }) {
                            Text(text = "OK")
                        }
                    }
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                AnimatedVisibility(
                    visible = selectedTab == TAB_MAP,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    MapUI(
                        modifier = Modifier.fillMaxSize(),
                        state = mapState,
                    )
                }

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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(stringResource(R.string.app_name))
                                Text(
                                    modifier = Modifier.clickable {
                                        isUpdateInfoDialogOpen = true
                                    },
                                    text = trainsLastUpdatedText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        ProfileIcon(
                            showGreeting = true,
                            extraDropdownMenuItems = { dismiss ->
                                var isSavedTrainsMenuOpen by remember { mutableStateOf(false) }

                                DropdownMenuItem(
                                    onClick = {
                                        isSavedTrainsMenuOpen = true
                                    },
                                    text = {
                                        Text(text = "Mentett vonatok")
                                        DropdownMenu(
                                            expanded = isSavedTrainsMenuOpen,
                                            onDismissRequest = { isSavedTrainsMenuOpen = false },
                                            modifier = Modifier.widthIn(max = 300.dp),
                                        ) {
                                            if (LocalSettings.savedTrainTripNames.isEmpty()) {
                                                DropdownMenuItem(
                                                    enabled = false,
                                                    onClick = { isSavedTrainsMenuOpen = false },
                                                    text = { Text(text = "Nincsenek mentett vonatok") },
                                                )
                                            } else {
                                                LocalSettings.savedTrainTripNames.forEach { trainTripName ->
                                                    DropdownMenuItem(
                                                        enabled = trains.any { it.trip.tripShortName == trainTripName },
                                                        onClick = {
                                                            val train =
                                                                trains.firstOrNull { it.trip.tripShortName == trainTripName }

                                                            if (train == null) {
                                                                Toast.makeText(
                                                                    this@MainActivity,
                                                                    "A vonat jelenleg nem elérhető",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            } else {
                                                                startActivity(
                                                                    Intent(
                                                                        this@MainActivity,
                                                                        TrainActivity::class.java
                                                                    ).putExtra(
                                                                        "trainJson",
                                                                        train.toString()
                                                                    )
                                                                )
                                                            }

                                                            isSavedTrainsMenuOpen = false
                                                            dismiss()
                                                        },
                                                        text = { Text(text = trainTripName) },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Default.Train,
                                                                contentDescription = null
                                                            )
                                                        },
                                                    )
                                                }
                                                HorizontalDivider()
                                                DropdownMenuItem(
                                                    onClick = {
                                                        LocalSettings.savedTrainTripNames =
                                                            emptySet()
                                                        LocalSettings.save(this@MainActivity)

                                                        isSavedTrainsMenuOpen = false
                                                        dismiss()
                                                    },
                                                    text = { Text(text = "Összes törlése") },
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.ClearAll,
                                                            contentDescription = null
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Bookmarks,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Default.ArrowRight,
                                            contentDescription = null
                                        )
                                    },
                                )
                            },
                        )
                    }

                    if (AppUpdateUtils.isUpdateAvailable) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = "Új verzió elérhető!",
                                )
                                Button(
                                    onClick = {
                                        AppUpdateUtils.startUpdate()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Update,
                                        contentDescription = null,
                                    )
                                    Text(
                                        modifier = Modifier
                                            .padding(start = ButtonDefaults.IconSpacing),
                                        text = "Frissítés",
                                    )
                                }
                            }
                        }
                    }

                    if (motdText.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .animateContentSize(),
                            onClick = { isMotdCollapsed = !isMotdCollapsed },
                        ) {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = motdText,
                                style = MaterialTheme.typography.bodySmall,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = if (isMotdCollapsed) 1 else Int.MAX_VALUE,
                            )
                        }
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
                            checked = selectedTab == TAB_MAVINFORM,
                            onCheckedChange = { selectedTab = TAB_MAVINFORM },
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Reports",
                            )
                            AnimatedVisibility(selectedTab == TAB_MAVINFORM) {
                                Text(
                                    modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing),
                                    text = "MÁVINFORM"
                                )
                            }
                            AnimatedVisibility(selectedTab != TAB_MAVINFORM && MAVINFORM.articles.isNotEmpty()) {
                                Text(
                                    modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing),
                                    text = MAVINFORM.articles.first().dateLastUpdated.substringAfter(" ")
                                )
                            }
                        }
                    }

                    when (selectedTab) {
                        TAB_MAVINFORM -> LazyColumn(
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(8.dp),
                        ) {
                            items(items = MAVINFORM.articles) { article ->
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
                                                context,
                                                MAVINFORM.mavinformTrainsUrl.toUri()
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

                        else -> {
                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Szűrők",
                                )
                                ElevatedFilterChip(
                                    selected = mapFilterTrains,
                                    onClick = { mapFilterTrains = !mapFilterTrains },
                                    label = { Text(text = "Vonatok") },
                                )
                                ElevatedFilterChip(
                                    selected = mapFilterMavinform,
                                    onClick = { mapFilterMavinform = !mapFilterMavinform },
                                    label = { Text(text = "MÁVINFORM") },
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HorizontalFloatingToolbar(
                                    modifier = Modifier.weight(1f),
                                    expanded = false,
                                ) {
                                    TextField(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.extraLarge),
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it.take(100) },
                                        placeholder = {
                                            Text(
                                                text = "Keresés járatszám vagy végállomás alapján",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { searchQuery = "" },
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Törlés",
                                                    )
                                                }
                                            }
                                        },
                                        maxLines = 1,
                                    )
                                }
                                FloatingActionButton(
                                    onClick = {
                                        if (isLoadingLocation) return@FloatingActionButton

                                        isLoadingLocation = true
                                        LocationUtils.requestPermissions { granted ->
                                            if (!granted) {
                                                isLoadingLocation = false
                                                return@requestPermissions
                                            }

                                            LocationUtils.getCurrentLocation(this@MainActivity) {
                                                coroutineScope.launch {
                                                    mapState.scrollTo(
                                                        x = it.normalized.longitude,
                                                        y = it.normalized.latitude,
                                                        destScale = .02,
                                                    )
                                                    isLoadingLocation = false
                                                }
                                            }
                                        }
                                    },
                                ) {
                                    if (isLoadingLocation) {
                                        LoadingIndicator(
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (LocationUtils.current != LatLng.ZERO) Icons.Default.GpsFixed
                                            else Icons.Default.GpsNotFixed,
                                            contentDescription = "Get current location",
                                        )
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
