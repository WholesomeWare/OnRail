package com.csakitheone.onrail.ui.components

import android.webkit.WebView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.csakitheone.onrail.data.model.MIArticle
import com.csakitheone.onrail.data.sources.MAVINFORM

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MIArticleDisplay(
    modifier: Modifier = Modifier,
    article: MIArticle,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var isDialogOpen by remember { mutableStateOf(false) }
    var htmlContent by remember { mutableStateOf("") }

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
                    if (htmlContent.isNotBlank()) {
                        AndroidView(
                            factory = {
                                fun Color.toCssColor() = "rgba(${red * 255}, ${green * 255}, ${blue * 255}, ${alpha})"

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
                                        styles + htmlContent,
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
            if (htmlContent.isBlank()) {
                MAVINFORM.fetchArticleContent(article) { htmlContent = it }
            }
        },
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
        }
    }
}