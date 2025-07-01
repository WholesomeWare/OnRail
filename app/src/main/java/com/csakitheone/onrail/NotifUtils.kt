package com.csakitheone.onrail

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.csakitheone.onrail.data.model.EMMAVehiclePosition

class NotifUtils {
    companion object {

        private val CHANNEL_TRAIN_UPDATES = "train_updates"

        private lateinit var notificationManager: NotificationManager
        private lateinit var shortcutManager: ShortcutManager

        fun init(activity: ComponentActivity, permissionCallback: (Boolean) -> Unit = {}) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {
                    permissionCallback(it)
                }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }

            notificationManager = activity.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        CHANNEL_TRAIN_UPDATES,
                        "Infók követett vonatról",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ),
                )
            )

            shortcutManager = activity.getSystemService(ShortcutManager::class.java)
        }

        fun showBubble(context: Context, train: EMMAVehiclePosition) {
            val target = Intent(context, TrainActivity::class.java)
                .setAction(Intent.ACTION_DEFAULT)
                .putExtra("trainJson", train.toString())
                .putExtra("bubble", true)
            val bubbleIntent =
                PendingIntent.getActivity(context, 0, target, PendingIntent.FLAG_MUTABLE)

            val trainDisplayName = train.trip.tripShortName
            val trainBot = Person.Builder()
                .setName(trainDisplayName)
                .setBot(true)
                .build()

            val shortcut = ShortcutInfo.Builder(context, train.trip.gtfsId)
                .setIntent(target)
                .setLongLived(true)
                .setShortLabel(trainDisplayName)
                .build()

            shortcutManager.removeAllDynamicShortcuts()
            shortcutManager.addDynamicShortcuts(listOf(shortcut))

            val bubbleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Notification.BubbleMetadata.Builder(
                    bubbleIntent,
                    Icon.createWithResource(context, R.drawable.ic_launcher_foreground)
                )
                    .setDesiredHeight(720)
                    .setAutoExpandBubble(true)
                    .build()
            } else {
                Notification.BubbleMetadata.Builder()
                    .setIntent(bubbleIntent)
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
                    .setDesiredHeight(720)
                    .setAutoExpandBubble(true)
                    .build()
            }

            val notification = Notification.Builder(context, CHANNEL_TRAIN_UPDATES)
                .setContentTitle(trainDisplayName)
                .setContentIntent(bubbleIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setBubbleMetadata(bubbleData)
                .setShortcutId(train.trip.gtfsId)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .addPerson(trainBot)
                .setStyle(
                    Notification.MessagingStyle(trainBot)
                )
                .build()

            notificationManager.notify(train.trip.gtfsId.hashCode(), notification)
        }

    }
}