package com.csakitheone.onrail.data

import android.content.Context
import com.csakitheone.onrail.data.model.EMMAVehiclePosition
import com.csakitheone.onrail.data.sources.EMMA
import com.csakitheone.onrail.data.sources.RTDB
import org.json.JSONArray
import java.io.File

class TrainsProvider {
    companion object {

        val SERVER_UPDATE_INTERVAL = 1000L * 30 // 30 seconds

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

        fun getTrains(context: Context, callback: (List<EMMAVehiclePosition>, Long) -> Unit) {

            // 1. If relevance is too old, fetch from remote source
            RTDB.getVehiclePositionsRelevance { lastUpdated ->
                RTDB.getConfigLong(
                    RTDB.CONFIG_KEY_EMMA_API_CALL_COOLDOWN,
                    1000L * 60 * 10, // Default to 10 minutes
                ) { apiCallCooldown ->
                    if (lastUpdated < System.currentTimeMillis() - apiCallCooldown) {
                        EMMA.fetchTrains { trains ->
                            callback(trains, System.currentTimeMillis())
                            RTDB.updateVehicleData(trains)
                            updateLocalCache(context, trains, System.currentTimeMillis())
                        }
                        return@getConfigLong
                    }

                    // 2. If relevance is recent, try to get trains from own database
                    RTDB.getVehiclePositions { trains ->
                        if (trains.isNotEmpty()) {
                            RTDB.getVehiclePositionsRelevance {
                                callback(trains, it)
                                updateLocalCache(context, trains, it)
                            }
                            return@getVehiclePositions
                        }

                        // 3. If database can't provide data, fallback to local cache
                        val localCacheFile = File(context.cacheDir, "trains_cache.json")
                        if (localCacheFile.exists()) {
                            try {
                                val cachedData = JSONArray(localCacheFile.readText())
                                val cachedTrains = (0 until cachedData.length()).map { index ->
                                    val vehicleJson = cachedData.getJSONObject(index)
                                    EMMAVehiclePosition.fromJson(vehicleJson.toString())
                                }.sortedBy { it.trip.tripShortName }
                                val lastUpdatedTimestamp =
                                    File(context.cacheDir, "trains_cache_metadata.json")
                                        .takeIf { it.exists() }
                                        ?.let { metadataFile ->
                                            val metadata = metadataFile.readText()
                                            metadata.substringAfter("\"lastUpdated\": ")
                                                .substringBefore("}").toLongOrNull()
                                                ?: 0L
                                        } ?: 0L
                                callback(cachedTrains, lastUpdatedTimestamp)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(emptyList(), 0)
                            }
                        } else {
                            callback(emptyList(), 0)
                        }
                    }
                }
            }
        }

    }
}