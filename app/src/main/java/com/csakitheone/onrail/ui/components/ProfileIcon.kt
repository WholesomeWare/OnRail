package com.csakitheone.onrail.ui.components

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.csakitheone.onrail.LocationUtils
import com.csakitheone.onrail.data.Auth
import com.csakitheone.onrail.data.sources.LocalSettings
import androidx.core.net.toUri
import com.csakitheone.onrail.R
import com.csakitheone.onrail.TrainActivity
import com.csakitheone.onrail.data.TrainsProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timerTask

@Composable
fun ProfileIcon(
    modifier: Modifier = Modifier,
    showGreeting: Boolean = false,
    extraDropdownMenuItems: @Composable (dismiss: () -> Unit) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalActivity.current

    var isGreetingEnabled by remember { mutableStateOf(false) }
    var greetingText by remember { mutableStateOf("") }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isAboutDialogOpen by remember { mutableStateOf(false) }
    var appVersionInfo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val pInfo = activity?.packageManager?.getPackageInfo(activity.packageName, 0)
        appVersionInfo = "${pInfo?.versionName} (${pInfo?.longVersionCode})"
    }

    LaunchedEffect(Auth.currentUser) {
        if (Auth.currentUser == null) return@LaunchedEffect

        greetingText = "Helló,\n${Auth.currentUser?.displayName ?: "ismeretlen felhasználó"}!"
        isGreetingEnabled = showGreeting

        Timer().schedule(timerTask {
            isGreetingEnabled = false
        }, 5000L)
    }

    if (isAboutDialogOpen) {
        AlertDialog(
            onDismissRequest = { isAboutDialogOpen = false },
            title = { Text(text = "${stringResource(R.string.app_name)} $appVersionInfo") },
            text = {
                Text(
                    text = "A Sínen Vagyunk közösségi vasút információs alkalmazás nem hivatalos " +
                            "és nem áll kapcsolatban a MÁV Csoporttal. Az alkalmazásban megjelenő " +
                            "információk a közösség által megosztott adatokon alapulnak, " +
                            "és nem garantált a pontosságuk."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activity?.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/WholesomeWare/OnRail".toUri()
                            )
                        )
                    },
                ) {
                    Text(text = "GitHub")
                }
                TextButton(
                    onClick = {
                        activity?.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://play.google.com/store/apps/details?id=com.csakitheone.onrail".toUri()
                            )
                        )
                    },
                ) {
                    Text(text = "Play Áruház")
                }
                TextButton(onClick = { isAboutDialogOpen = false }) {
                    Text(text = "Ok")
                }
            },
        )
    }

    ExtendedFloatingActionButton(
        modifier = modifier.widthIn(max = 180.dp),
        onClick = { isMenuOpen = true },
        expanded = isGreetingEnabled,
        text = {
            Text(
                text = greetingText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null
            )
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                if (Auth.currentUser != null) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "Bejelentkezve mint:\n${Auth.currentUser?.displayName ?: "ismeretlen felhasználó"}",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    extraDropdownMenuItems { isMenuOpen = false }
                }
                DropdownMenuItem(
                    onClick = {
                        if (Auth.currentUser == null) {
                            coroutineScope.launch {
                                if (activity != null) Auth.signInWithGoogle(activity)
                            }
                        } else {
                            Auth.signOut()
                        }
                        isMenuOpen = false
                    },
                    text = {
                        Text(
                            text = if (Auth.currentUser == null) "Bejelentkezés Google fiókkal"
                            else "Kijelentkezés"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (Auth.currentUser == null) Icons.AutoMirrored.Default.Login
                            else Icons.AutoMirrored.Default.Logout,
                            contentDescription = null
                        )
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        isAboutDialogOpen = true
                        isMenuOpen = false
                    },
                    text = { Text(text = "Névjegy") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null
                        )
                    },
                )
            }
        },
    )
}