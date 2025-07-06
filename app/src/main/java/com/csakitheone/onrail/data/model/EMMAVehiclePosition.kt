package com.csakitheone.onrail.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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
    val prevOrCurrentStop: Stoptime = Stoptime(),
    val nextStop: Stoptime = Stoptime(),
) : Parcelable {
    @Parcelize
    data class Trip(
        val gtfsId: String = "",
        val tripShortName: String = "",
        val tripHeadsign: String = "",
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
    ) : Parcelable

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
            })
            put("prevOrCurrentStop", JSONObject().apply {
                put("stop", JSONObject().apply {
                    put("name", prevOrCurrentStop.stop.name)
                    put("lat", prevOrCurrentStop.stop.lat)
                    put("lon", prevOrCurrentStop.stop.lon)
                })
            })
            put("nextStop", JSONObject().apply {
                put("stop", JSONObject().apply {
                    put("name", nextStop.stop.name)
                    put("lat", nextStop.stop.lat)
                    put("lon", nextStop.stop.lon)
                })
            })
        }.toString()
    }

    companion object {
        fun fromJson(json: String?): EMMAVehiclePosition {
            if (json.isNullOrBlank()) return EMMAVehiclePosition()

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
                        tripHeadsign = tripJson.getString("tripHeadsign")
                    ),
                    prevOrCurrentStop = Stoptime(
                        stop = Stop(
                            name = jsonObject.getJSONObject("prevOrCurrentStop").getJSONObject("stop")
                                .getString("name"),
                            lat = jsonObject.getJSONObject("prevOrCurrentStop").getJSONObject("stop")
                                .getDouble("lat"),
                            lon = jsonObject.getJSONObject("prevOrCurrentStop").getJSONObject("stop")
                                .getDouble("lon")
                        )
                    ),
                    nextStop = Stoptime(
                        stop = Stop(
                            name = jsonObject.getJSONObject("nextStop").getJSONObject("stop")
                                .getString("name"),
                            lat = jsonObject.getJSONObject("nextStop").getJSONObject("stop")
                                .getDouble("lat"),
                            lon = jsonObject.getJSONObject("nextStop").getJSONObject("stop")
                                .getDouble("lon")
                        )
                    )
                )
            } catch (_: Exception) {
                return EMMAVehiclePosition() // Return a default instance if parsing fails
            }
        }
    }
}