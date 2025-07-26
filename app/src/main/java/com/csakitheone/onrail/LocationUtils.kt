package com.csakitheone.onrail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableZooming
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.tan

class LocationUtils {
    companion object {

        var current by mutableStateOf(LatLng.ZERO)
            private set

        private var locationPermissionRequest: ActivityResultLauncher<Array<String>>? = null
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

        fun requestPermissions(context: Context, callback: (Boolean) -> Unit) {
            val isFineLocationGranted = ActivityCompat.checkSelfPermission(
                context.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (isFineLocationGranted) {
                Log.d("LocationUtils", "Location permission already granted")
                callback(true)
                return
            }

            onPermissionGranted = callback

            try {
                locationPermissionRequest?.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } catch (e: Exception) {
                Log.e("LocationUtils", "Error requesting permissions: ${e.message}")
                onPermissionGranted(false)
            }
        }

        @Composable
        fun rememberLocationUpdates(
            enabled: Boolean = true,
        ) {
            val context = LocalContext.current

            DisposableEffect(enabled) {
                if (!enabled) return@DisposableEffect onDispose {}

                val isFineLocationGranted = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!isFineLocationGranted) {
                    return@DisposableEffect onDispose {}
                }

                if (fusedLocationClient == null) {
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                }

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        if (locationResult.lastLocation != null) {
                            current = LatLng(
                                locationResult.lastLocation!!.latitude,
                                locationResult.lastLocation!!.longitude
                            )
                        }
                    }
                }

                fusedLocationClient?.requestLocationUpdates(
                    LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        10_000
                    ).build(),
                    locationCallback,
                    Looper.getMainLooper(),
                )

                onDispose {
                    fusedLocationClient?.removeLocationUpdates(locationCallback)
                }
            }
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
                    val newLatLng = LatLng(location.latitude, location.longitude)
                    if (current != newLatLng) current = newLatLng
                    callback(newLatLng)
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

        private fun getMapTile(
            context: Context,
            row: Int,
            col: Int,
            zoom: Int
        ): InputStream? {
            if (zoom < 0) {
                return null
            }

            val cacheFile = File(context.cacheDir, "tiles/tile_${zoom}_${col}_${row}.png")

            if (cacheFile.exists()) {
                return cacheFile.inputStream()
            }

            if (PowerUtils.isPowerSaveMode) {
                return getMapTile(context, row, col, zoom - 1)
            }

            if (cacheFile.parentFile?.exists() == false) {
                cacheFile.parentFile?.mkdirs()
            }

            return runCatching {
                val inputStream = URL("https://tile.openstreetmap.org/${zoom}/${col}/${row}.png")
                    .openConnection()
                    .apply { setRequestProperty("User-Agent", "OnRailApp/1.0") }
                    .inputStream

                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                cacheFile.inputStream()
            }.getOrNull()
        }

        fun getMapState(context: Context): MapState {
            val level = 18
            val tileSize = 1024
            val dimension = 2.0.pow(log2(tileSize.toDouble()) - 1 + level).toInt()

            return MapState(
                levelCount = level,
                fullWidth = dimension,
                fullHeight = dimension,
                tileSize = tileSize,
            ).apply {
                addLayer({ row, col, zoom ->
                    getMapTile(context, row, col, zoom)
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

    /**
     * Get distance in meters between two locations.
     */
    fun distanceFrom(other: LatLng?): Float {
        val results = floatArrayOf(0f)
        Location.distanceBetween(
            latitude, longitude,
            other?.latitude ?: 0.0, other?.longitude ?: 0.0,
            results
        )
        return results[0]
    }

    override fun toString(): String {
        return "$latitude,$longitude"
    }

    override fun hashCode(): Int {
        return latitude.hashCode() * 31 + longitude.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is LatLng) other.latitude == latitude && other.longitude == longitude
        else false
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