package com.csakitheone.onrail.data.sources

import com.csakitheone.onrail.data.model.EMMAVehiclePosition
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EMMA {
    companion object {
        private const val BASE_URL =
            "https://emma.mav.hu/otp2-backend/otp/routers/default/index/graphql"
        private val client = OkHttpClient()

        @OptIn(DelicateCoroutinesApi::class)
        fun fetchTrains(callback: (List<EMMAVehiclePosition>) -> Unit = {}) {
            val query = """
                {
                    vehiclePositions(
                        swLat: 45.5,
                        swLon: 16.1,
                        neLat: 48.7,
                        neLon: 22.8,
                        modes: [RAIL, RAIL_REPLACEMENT_BUS]
                    ) {
                        trip {
                            gtfsId
                            tripShortName
                            tripHeadsign
                            stoptimes {
                                stop { name lat lon }
                                arrivalDelay
                            }
                            arrivalStoptime {
                                stop { name lat lon }
                                arrivalDelay
                            }
                        }
                        vehicleId
                        lat lon
                        label speed heading
                        stopRelationship { status }
                        prevOrCurrentStop { stop { name lat lon } }
                        nextStop { stop { name lat lon } }
                    }
                }
            """.replace("\n", " ")

            val request = Request.Builder()
                .url(BASE_URL)
                .post("{ \"query\": \"${query}\", \"variables\": {} }".toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "OnRail/1.0")
                .addHeader("Content-Type", "application/json")
                .build()

            GlobalScope.launch(Dispatchers.IO) {
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    callback(emptyList())
                    return@launch
                }

                try {
                    val responseBody = response.body?.string() ?: return@launch
                    val jsonResponse = JSONObject(responseBody)
                    val vehiclesArray =
                        jsonResponse.getJSONObject("data").getJSONArray("vehiclePositions")
                    val vehicles = (0 until vehiclesArray.length()).map { index ->
                        val vehicleJson = vehiclesArray.getJSONObject(index)
                        EMMAVehiclePosition.fromJson(vehicleJson.toString())
                    }.sortedBy { it.trip.tripShortName }

                    callback(vehicles)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    callback(emptyList())
                    return@launch
                }
            }
        }

        private fun fetchDelay(gtfsId: String) {
            val date: String? = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val query = """
                {
                    trip(
                        id: \"" + $gtfsId + "\", serviceDay: \"" + $date + "\"
                    ) {
                        stoptimes { arrivalDelay }
                    }
                }
            """.trimIndent()
        }

    }
}