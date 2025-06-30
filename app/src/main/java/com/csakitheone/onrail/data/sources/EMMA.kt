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


class EMMA {
    companion object {
        private const val BASE_URL =
            "https://emma.mav.hu/otp2-backend/otp/routers/default/index/graphql"

        @OptIn(DelicateCoroutinesApi::class)
        fun fetchTrains(callback: (List<EMMAVehiclePosition>) -> Unit = {}) {
            val client = OkHttpClient()

            val request = Request.Builder()
                .url(BASE_URL)
                .post(
                    "{\"query\":\"{ vehiclePositions(swLat: 45.5, swLon: 16.1, neLat: 48.7, neLon: 22.8, modes: [RAIL, RAIL_REPLACEMENT_BUS]) { trip { gtfsId tripShortName tripHeadsign } vehicleId lat lon label speed heading } }\",\"variables\":{}}".toRequestBody(
                        "application/json".toMediaType()
                    )
                )
                .addHeader("Content-Type", "application/json")
                .build()

            GlobalScope.launch(Dispatchers.IO) {
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    callback(emptyList())
                    return@launch
                }

                val responseBody = response.body?.string() ?: return@launch
                val jsonResponse = JSONObject(responseBody)
                val vehiclesArray = jsonResponse.getJSONObject("data").getJSONArray("vehiclePositions")
                val vehicles = (0 until vehiclesArray.length()).map { index ->
                    val vehicleJson = vehiclesArray.getJSONObject(index)
                    EMMAVehiclePosition.fromJson(vehicleJson.toString())
                }.sortedBy { it.trip.tripShortName }

                callback(vehicles)
            }
        }

    }
}