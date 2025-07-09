package com.csakitheone.onrail.data.model

import android.os.Parcelable
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.csakitheone.onrail.ui.theme.colorDelayDrastic
import com.csakitheone.onrail.ui.theme.colorDelayMajor
import com.csakitheone.onrail.ui.theme.colorDelayMinor
import com.csakitheone.onrail.ui.theme.colorDelayNone
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

@Parcelize
data class EMMAVehiclePosition(
    val vehicleId: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val speed: Double = 0.0,
    val heading: Double = 0.0,
    val label: String = "",
    val trip: Trip = Trip(),
) : Parcelable {
    @Parcelize
    data class Trip(
        val gtfsId: String = "",
        val tripShortName: String = "",
        val tripHeadsign: String = "",
        val stoptimes: List<Stoptime> = emptyList(),
        val arrivalStoptime: Stoptime = Stoptime(),
    ) : Parcelable

    @Parcelize
    data class Stop(
        val name: String = "",
        val lat: Double = 0.0,
        val lon: Double = 0.0,
    ) : Parcelable

    @Parcelize
    data class Stoptime(
        val stop: Stop = Stop(),
        val arrivalDelay: Int = 0,
    ) : Parcelable

    val delayMinutes: Int
        get() = trip.arrivalStoptime.arrivalDelay / 60

    val delayColor: Color
        get() = when {
            delayMinutes < 5 -> colorDelayNone
            delayMinutes < 15 -> colorDelayMinor
            delayMinutes < 60 -> colorDelayMajor
            else -> colorDelayDrastic
        }

    override fun toString(): String {
        // Convert the VehiclePosition to a JSON string representation
        return JSONObject().apply {
            put("vehicleId", vehicleId)
            put("lat", lat)
            put("lon", lon)
            put("speed", speed)
            put("heading", heading)
            put("label", label)
            put("trip", JSONObject().apply {
                put("gtfsId", trip.gtfsId)
                put("tripShortName", trip.tripShortName)
                put("tripHeadsign", trip.tripHeadsign)
                put("stoptimes", JSONArray().apply {
                    trip.stoptimes.forEach { stoptime ->
                        put(JSONObject().apply {
                            put("stop", JSONObject().apply {
                                put("name", stoptime.stop.name)
                                put("lat", stoptime.stop.lat)
                                put("lon", stoptime.stop.lon)
                            })
                            put("arrivalDelay", stoptime.arrivalDelay)
                        })
                    }
                })
                put("arrivalStoptime", JSONObject().apply {
                    put("stop", JSONObject().apply {
                        put("name", trip.arrivalStoptime.stop.name)
                        put("lat", trip.arrivalStoptime.stop.lat)
                        put("lon", trip.arrivalStoptime.stop.lon)
                    })
                    put("arrivalDelay", trip.arrivalStoptime.arrivalDelay)
                })
            })
        }.toString()
    }

    companion object {
        fun fromJson(json: String?): EMMAVehiclePosition {
            if (json.isNullOrBlank()) return EMMAVehiclePosition()

            Log.d("EMMAVehiclePosition", "Parsing JSON: $json")

            try {
                val jsonObject = JSONObject(json)
                val tripJson = jsonObject.getJSONObject("trip")
                return EMMAVehiclePosition(
                    vehicleId = jsonObject.getString("vehicleId"),
                    lat = jsonObject.getDouble("lat"),
                    lon = jsonObject.getDouble("lon"),
                    speed = jsonObject.getDouble("speed"),
                    heading = jsonObject.optDouble("heading", -1.0),
                    label = jsonObject.getString("label"),
                    trip = Trip(
                        gtfsId = tripJson.getString("gtfsId"),
                        tripShortName = tripJson.getString("tripShortName"),
                        tripHeadsign = tripJson.getString("tripHeadsign"),
                        stoptimes = tripJson.getJSONArray("stoptimes").let { stoptimesArray ->
                            List(stoptimesArray.length()) { index ->
                                val stoptimeJson = stoptimesArray.getJSONObject(index)
                                Stoptime(
                                    stop = Stop(
                                        name = stoptimeJson.getJSONObject("stop").getString("name"),
                                        lat = stoptimeJson.getJSONObject("stop").getDouble("lat"),
                                        lon = stoptimeJson.getJSONObject("stop").getDouble("lon")
                                    ),
                                    arrivalDelay = stoptimeJson.optInt("arrivalDelay", 0)
                                )
                            }
                        },
                        arrivalStoptime = Stoptime(
                            stop = Stop(
                                name = tripJson.getJSONObject("arrivalStoptime").getJSONObject("stop")
                                    .getString("name"),
                                lat = tripJson.getJSONObject("arrivalStoptime").getJSONObject("stop")
                                    .getDouble("lat"),
                                lon = tripJson.getJSONObject("arrivalStoptime").getJSONObject("stop")
                                    .getDouble("lon"),
                            ),
                            arrivalDelay = tripJson.getJSONObject("arrivalStoptime")
                                .optInt("arrivalDelay", 0),
                        ),
                    ),
                )
            } catch (_: Exception) {
                return EMMAVehiclePosition() // Return a default instance if parsing fails
            }
        }
    }
}