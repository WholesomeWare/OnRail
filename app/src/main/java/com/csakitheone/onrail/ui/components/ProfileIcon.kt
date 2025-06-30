package com.csakitheone.onrail.ui.components

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.csakitheone.onrail.LocationUtils
import com.csakitheone.onrail.data.Auth
import com.csakitheone.onrail.data.sources.LocalSettings
import androidx.core.net.toUri

@Composable
fun ProfileIcon(
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current

    var isMenuOpen by remember { mutableStateOf(false) }
    var appVersionInfo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val pInfo = activity?.packageManager?.getPackageInfo(activity.packageName, 0)
        appVersionInfo = "${pInfo?.versionName} (${pInfo?.longVersionCode})"
    }

    FloatingActionButton(
        modifier = modifier,
        onClick = { isMenuOpen = true },
    ) {
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
            }
            DropdownMenuItem(
                onClick = {
                    if (Auth.currentUser == null) {
                        if (activity != null) Auth.signInWithGoogle(activity)
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
            HorizontalDivider()
            Text(
                modifier = Modifier.padding(16.dp),
                text = "App verzió\n$appVersionInfo\n\nUID (Csákin kívül ne oszd meg senkivel!)\n${Auth.currentUser?.uid ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
            )
            DropdownMenuItem(
                onClick = {
                    isMenuOpen = false
                    activity?.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/details?id=com.csakitheone.onrail".toUri()
                        )
                    )
                },
                text = { Text(text = "Play Áruház megnyitása") },
            )
        }
    }
}