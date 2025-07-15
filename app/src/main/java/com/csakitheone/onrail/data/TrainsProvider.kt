package com.csakitheone.onrail.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.csakitheone.onrail.NetworkUtils
import com.csakitheone.onrail.data.model.EMMAVehiclePosition
import com.csakitheone.onrail.data.sources.EMMA
import com.csakitheone.onrail.data.sources.RTDB
import org.json.JSONArray
import java.io.File

class TrainsProvider {
    companion object {

        val SERVER_UPDATE_INTERVAL = 1000L * 60 // 60 seconds

        private fun updateLocalCache(
            context: Context,
            trains: List<EMMAVehiclePosition>,
            lastUpdatedTimestamp: Long,
        ) {
            val localCacheFile = File(context.cacheDir, "trains_cache.json")
            val cacheMetadataFile = File(context.cacheDir, "trains_cache_metadata.json")
            localCacheFile.writeText(
                JSONArray(trains.map { it.toString() }).toString()
            )
            cacheMetadataFile.writeText(
                "{\"lastUpdated\": $lastUpdatedTimestamp}"
            )
        }

        private fun getTrainsFromLocalCache(context: Context): List<EMMAVehiclePosition> {
            val localCacheFile = File(context.cacheDir, "trains_cache.json")
            return if (localCacheFile.exists()) {
                try {
                    val cachedData = JSONArray(localCacheFile.readText())
                    (0 until cachedData.length()).map { index ->
                        val vehicleJson = cachedData.get(index)
                        EMMAVehiclePosition.fromJson(vehicleJson.toString())
                    }.sortedBy { it.trip.tripShortName }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

        private fun getLocalCacheLastUpdated(context: Context): Long {
            val cacheMetadataFile = File(context.cacheDir, "trains_cache_metadata.json")
            return if (cacheMetadataFile.exists()) {
                try {
                    val metadata = cacheMetadataFile.readText()
                    metadata.substringAfter("\"lastUpdated\": ")
                        .substringBefore("}").toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    e.printStackTrace()
                    0L
                }
            } else {
                0L
            }
        }

        private fun getTrainsFromInternet(
            context: Context,
            callback: (List<EMMAVehiclePosition>, Long) -> Unit
        ) {
            RTDB.getVehiclePositionsRelevance { lastUpdated ->
                RTDB.getConfigLong(
                    RTDB.CONFIG_KEY_EMMA_API_CALL_COOLDOWN,
                    1000L * 60 * 10, // Default to 10 minutes
                ) { apiCallCooldown ->
                    val isMyDataOutdated =
                        lastUpdated < System.currentTimeMillis() - SERVER_UPDATE_INTERVAL

                    if (NetworkUtils.hasInternet(context) && isMyDataOutdated) {
                        EMMA.fetchTrains { trains ->
                            callback(trains, System.currentTimeMillis())
                            RTDB.updateVehicleData(trains)
                        }
                        return@getConfigLong
                    }

                    RTDB.getVehiclePositions { trains ->
                        if (trains.isNotEmpty()) {
                            RTDB.getVehiclePositionsRelevance {
                                callback(trains, it)
                            }
                            return@getVehiclePositions
                        }

                        callback(emptyList(), lastUpdated)
                    }
                }
            }
        }

        fun getTrains(context: Context, callback: (List<EMMAVehiclePosition>, Long) -> Unit) {
            val cachedTrains = getTrainsFromLocalCache(context)
            val lastUpdatedTimestamp = getLocalCacheLastUpdated(context)

            // 0. If there is no internet connection, use local cache
            if (!NetworkUtils.hasInternet(context)) {
                callback(cachedTrains, lastUpdatedTimestamp)
            }

            // 1. Get trains from internet if available
            // 1.1. Check if RTDB is outdated
            // 1.2. If outdated, fetch from EMMA
            // 1.3. If not outdated, use RTDB data
            getTrainsFromInternet(context) { trains, lastUpdated ->
                if (trains.isNotEmpty()) {
                    updateLocalCache(context, trains, lastUpdated)
                    callback(trains, lastUpdated)
                } else {
                    // If no trains found, return cached data
                    callback(cachedTrains, lastUpdatedTimestamp)
                }
            }
        }
    }
}