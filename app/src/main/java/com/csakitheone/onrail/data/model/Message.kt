package com.csakitheone.onrail.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt

data class Message(
    val timestamp: Long = 0,
    val senderId: String = "",
    val senderName: String = "",
    val messageType: String = TYPE_TEXT,
    val content: String = "",
    val location: String = "",
    val color: Int = COLOR_DEFAULT,
) {
    companion object {
        val COLOR_DEFAULT = Color.Transparent.toArgb()
        val COLOR_WARNING = 0xFFFFA500.toInt()
        val COLOR_ERROR = Color.Red.toArgb()
        const val TYPE_TEXT = "text"
        const val TYPE_REPORT = "report"
        const val TYPE_LOCATION_PING = "location_ping"

        fun report(
            content: String,
            color: Int = COLOR_ERROR,
        ) = Message(
            messageType = TYPE_REPORT,
            content = content,
            color = color,
        )

        val LOCATION_PING = Message(
            messageType = TYPE_LOCATION_PING,
            content = "Hely küldése üzenet nélkül",
        )
        val REPORT_OPTION_DELAY_MINOR = report(
            content = "Kis késés (5-15 perc)",
            color = COLOR_WARNING,
        )
        val REPORT_OPTION_DELAY_MODERATE = report(
            content = "Közepes késés (15-60 perc)",
            color = COLOR_WARNING,
        )
        val REPORT_OPTION_DELAY_MAJOR = report(
            content = "Nagy késés (1 óra+)",
        )
        val REPORT_TRAIN_STOPPED = report(
            content = "Vonat megállt",
        )
        val REPORT_TRACK_BLOCKED = report(
            content = "Pálya elzárva",
        )
        val REPORT_TECHNICAL_ISSUE = report(
            content = "Műszaki hiba",
        )
        val REPORT_EMERGENCY_ACCIDENT = report(
            content = "Vészhelyzet / baleset",
        )
        val REPORT_CROWDING = report(
            content = "Tömeg / zsúfoltság",
            color = COLOR_WARNING,
        )
        val REPORT_TEMPERATURE_HIGH = report(
            content = "Magas hőmérséklet",
            color = COLOR_WARNING,
        )
        val REPORT_TEMPERATURE_LOW = report(
            content = "Alacsony hőmérséklet",
            color = COLOR_WARNING,
        )
        val REPORT_POLICE_ACTIVITY = report(
            content = "Rendőrségi intézkedés",
            color = Color.Blue.toArgb(),
        )

        val reportOptions = listOf(
            LOCATION_PING,
            REPORT_OPTION_DELAY_MINOR,
            REPORT_OPTION_DELAY_MODERATE,
            REPORT_OPTION_DELAY_MAJOR,
            REPORT_TRAIN_STOPPED,
            REPORT_TRACK_BLOCKED,
            REPORT_TECHNICAL_ISSUE,
            REPORT_EMERGENCY_ACCIDENT,
            REPORT_CROWDING,
            REPORT_TEMPERATURE_HIGH,
            REPORT_TEMPERATURE_LOW,
            REPORT_POLICE_ACTIVITY,
        )
    }
}