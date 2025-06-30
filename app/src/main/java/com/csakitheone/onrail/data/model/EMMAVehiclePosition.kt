package com.csakitheone.onrail.data.model

import org.json.JSONObject

data class EMMAVehiclePosition(
    val vehicleId: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val speed: Double = 0.0,
    val heading: Double = 0.0,
    val label: String = "",
    val trip: Trip = Trip(),
) {
    data class Trip(
        val gtfsId: String = "",
        val tripShortName: String = "",
        val tripHeadsign: String = "",
    )

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
                    )
                )
            }
            catch (_: Exception) {
                return EMMAVehiclePosition() // Return a default instance if parsing fails
            }
        }
    }
}