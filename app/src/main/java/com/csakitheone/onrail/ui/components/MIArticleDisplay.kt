package com.csakitheone.onrail.ui.components

import android.webkit.WebView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.csakitheone.onrail.NetworkUtils
import com.csakitheone.onrail.data.Auth
import com.csakitheone.onrail.data.Gemini
import com.csakitheone.onrail.data.model.MIArticle
import com.csakitheone.onrail.data.sources.MAVINFORM
import com.csakitheone.onrail.data.sources.MAVINFORM.Companion.isCached
import com.csakitheone.onrail.data.sources.RTDB
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MIArticleDisplay(
    modifier: Modifier = Modifier,
    article: MIArticle,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    var isDialogOpen by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    var aiSummary by remember { mutableStateOf(article.aiSummary) }
    var isLoadingAiSummary by remember { mutableStateOf(false) }

    LaunchedEffect(isDialogOpen) {
        if (!isDialogOpen || Auth.currentUser == null || aiSummary.isNotBlank()) return@LaunchedEffect

        isLoadingAiSummary = true
        RTDB.getArticleSummary(article.link) { summary ->
            if (summary.isNotBlank()) {
                aiSummary = summary
                isLoadingAiSummary = false
            } else {
                coroutineScope.launch {
                    if (article.content.isBlank()) {
                        MAVINFORM.fetchArticleContent(context, article) { content ->
                            coroutineScope.launch {
                                // Wait a bit for everything to settle
                                kotlinx.coroutines.delay(500)
                                val generatedSummary = Gemini.summarizeArticle(article.title, content)
                                if (!generatedSummary.startsWith("Hiba:")) {
                                    RTDB.saveArticleSummary(article.link, generatedSummary)
                                }
                                aiSummary = generatedSummary
                                isLoadingAiSummary = false
                            }
                        }
                    } else {
                        val generatedSummary = Gemini.summarizeArticle(article.title, article.content)
                        if (!generatedSummary.startsWith("Hiba:")) {
                            RTDB.saveArticleSummary(article.link, generatedSummary)
                        }
                        aiSummary = generatedSummary
                        isLoadingAiSummary = false
                    }
                }
            }
        }
    }

    if (isDialogOpen) {
        AlertDialog(
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
            ),
            onDismissRequest = { isDialogOpen = false },
            title = { Text(text = article.title) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (article.isDrastic) {
                        AssistChip(
                            modifier = Modifier.align(Alignment.Start),
                            onClick = {},
                            label = { Text("Rendkívüli változás") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Report,
                                    contentDescription = null,
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                trailingIconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = article.dateValidFrom,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = null,
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = article.dateLastUpdated,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (Auth.currentUser == null) {
                        Text(
                            text = "AI összefoglalók használatához bejelentkezés szükséges.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                modifier = Modifier.align(Alignment.Start),
                                text = "AI összefoglaló",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (isLoadingAiSummary) {
                                LoadingIndicator()
                            } else {
                                Text(
                                    text = aiSummary.ifBlank { "Nincs összefoglaló." },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    if (article.content.isNotBlank()) {
                        AndroidView(
                            factory = {
                                fun Color.toCssColor() =
                                    "rgba(${red * 255}, ${green * 255}, ${blue * 255}, ${alpha})"

                                val styles = """
                                    <style>
                                    body {
                                        background: ${colorScheme.surfaceContainerHigh.toCssColor()};
                                        color: ${colorScheme.onSurface.toCssColor()};
                                    }
                                    
                                    .bluebox {
                                        background: ${colorScheme.primaryContainer.toCssColor()};
                                        color: ${colorScheme.onPrimaryContainer.toCssColor()};
                                        padding: 8px;
                                        border-radius: 4px;
                                    }
                                    
                                    .yellowbox {
                                        background: ${colorScheme.tertiaryContainer.toCssColor()};
                                        color: ${colorScheme.onTertiaryContainer.toCssColor()};
                                        padding: 8px;
                                        border-radius: 4px;
                                    }
                                    </style>
                                """.trimIndent()

                                WebView(it).apply {
                                    loadData(
                                        styles + article.content,
                                        "text/html; charset=utf-8",
                                        "UTF-8"
                                    )
                                }
                            },
                        )
                    } else {
                        LoadingIndicator()
                    }
                }
            },
            confirmButton = {
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
                                "${MAVINFORM.baseUrl}${article.link}".toUri()
                            )
                        isDialogOpen = false
                    }
                ) {
                    Text(text = "Megnyitás böngészőben")
                }
                TextButton(
                    onClick = { isDialogOpen = false }
                ) {
                    Text(text = "Bezárás")
                }
            },
        )
    }

    Card(
        modifier = modifier,
        onClick = {
            isDialogOpen = true
            if (article.content.isBlank()) {
                MAVINFORM.fetchArticleContent(context, article)
            }
        },
        colors = if (article.isDrastic) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        else CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = article.scopes.joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = article.readableDateValidFrom,
                    style = MaterialTheme.typography.bodySmall,
                )
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = article.readableDateLastUpdated,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (isDownloading) {
                    LoadingIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .clickable {
                                if (article.isCached(context)) {
                                    Toast.makeText(
                                        context,
                                        "Ez a cikk már le van töltve.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@clickable
                                }

                                if (!NetworkUtils.hasInternet(context)) {
                                    Toast.makeText(
                                        context,
                                        "Nincs internetkapcsolat.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@clickable
                                }

                                isDownloading = true
                                MAVINFORM.fetchArticleContent(context, article) { _ ->
                                    isDownloading = false
                                }
                            },
                        imageVector = if (article.isCached(context)) Icons.Default.OfflinePin
                        else Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}