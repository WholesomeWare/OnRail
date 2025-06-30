package com.csakitheone.onrail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableZooming
import ovh.plrapps.mapcompose.ui.state.MapState
import java.net.URL
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

class LocationUtils {
    companion object {

        var current by mutableStateOf(LatLng.ZERO)
            private set

        private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
        private var onPermissionGranted: ((Boolean) -> Unit) = {
            Log.e("LocationUtils", "Permission request callback not set")
        }
        private var fusedLocationClient: FusedLocationProviderClient? = null

        fun register(activity: ComponentActivity) {
            locationPermissionRequest = activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                onPermissionGranted(permissions.values.all { it })
            }
        }

        fun requestPermissions(callback: (Boolean) -> Unit) {
            onPermissionGranted = callback

            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        fun getLastKnownLocation(context: Context, callback: (LatLng) -> Unit) {
            val isFineLocationGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!isFineLocationGranted) {
                return
            }

            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }

            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                if (location != null) {
                    callback(LatLng(location.latitude, location.longitude))
                } else {
                    callback(LatLng.ZERO)
                }
            }
        }

        fun getCurrentLocation(context: Context, callback: (LatLng) -> Unit) {
            val isFineLocationGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!isFineLocationGranted) {
                return
            }

            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }

            fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                ?.addOnSuccessListener { location ->
                    val newLatLng = LatLng(location.latitude, location.longitude)
                    if (current != newLatLng) current = newLatLng
                    callback(newLatLng)
                }
        }

        fun getMapState(): MapState {
            val level = 18
            return MapState(
                levelCount = level,
                fullWidth = 2.0.pow(7 + level).toInt(),
                fullHeight = 2.0.pow(7 + level).toInt(),
            ).apply {
                addLayer({ row, col, zoom ->
                    runCatching {
                        URL("https://tile.openstreetmap.org/${zoom}/${col}/${row}.png")
                            .openConnection()
                            .apply { setRequestProperty("User-Agent", "OnRailApp/1.0") }
                            .inputStream
                    }.getOrNull()
                })

                enableZooming()
            }
        }

    }
}

class LatLng(
    val latitude: Double,
    val longitude: Double,
) {
    val normalized: LatLng
        get() = LatLng(
            run {
                val latRad = latitude * PI / 180.0 // Convert latitude to radians
                val mercatorY =
                    ln(tan(PI / 4 + latRad / 2)) // Mercator Y coordinate, ranging from -PI to PI
                // Normalize this Mercator Y from [-PI, PI] to [0, 1] for a top-down map
                // This is equivalent to mapping [-20037508.34, 20037508.34] to [0, 1]
                (1.0 - (mercatorY / PI + 1.0) / 2.0)
            },
            (longitude + 180) / 360.0
        )

    override fun toString(): String {
        return "$latitude,$longitude"
    }

    companion object {
        val ZERO = LatLng(0.0, 0.0)

        fun fromString(latLng: String?): LatLng {
            if (latLng.isNullOrBlank()) {
                return ZERO
            }

            val parts = latLng.split(",")
            return if (parts.size == 2) {
                LatLng(
                    latitude = parts[0].toDoubleOrNull() ?: 0.0,
                    longitude = parts[1].toDoubleOrNull() ?: 0.0
                )
            } else {
                ZERO
            }
        }
    }
}