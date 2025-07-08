package com.csakitheone.onrail

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Report
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
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.csakitheone.onrail.data.model.MIArticle
import com.csakitheone.onrail.data.sources.MAVINFORM
import com.csakitheone.onrail.ui.components.MIArticleDisplay
import com.csakitheone.onrail.ui.components.ProfileIcon
import com.csakitheone.onrail.ui.theme.OnRailTheme

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

            val TAB_ARTICLES = 0
            val TAB_CHAT = 1

            var territory by remember { mutableStateOf(MAVINFORM.Territory.BUDAPEST) }
            val mavinformArticles by remember {
                derivedStateOf {
                    MAVINFORM.articles.filter {
                        it.territoryScopes.contains(territory)
                    }
                }
            }
            var selectedTab by remember { mutableIntStateOf(TAB_ARTICLES) }

            LaunchedEffect(Unit) {
                territory = MAVINFORM.Territory.fromName(
                    intent.getStringExtra("territoryName")
                ) ?: MAVINFORM.Territory.BUDAPEST
            }

            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .systemBarsPadding(),
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
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
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
                                    .weight(1f)
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                }
                            }
                        }

                        TAB_CHAT -> {
                            //TODO: Implement chat functionality
                            Text(text = "A területi chat hamarosan elérhető lesz.")
                        }
                    }
                }
            }
        }
    }
}
