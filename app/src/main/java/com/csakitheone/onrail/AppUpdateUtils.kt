package com.csakitheone.onrail

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class AppUpdateUtils {
    companion object {

        var isUpdateAvailable by mutableStateOf(false)
            private set

        private var appUpdateManager: AppUpdateManager? = null
        private var appUpdateInfo: AppUpdateInfo? = null
        private var activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

        fun checkForUpdates(activity: ComponentActivity) {
            appUpdateManager = AppUpdateManagerFactory.create(activity)
            val appUpdateInfoTask = appUpdateManager!!.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { newAppUpdateInfo ->
                Log.d(
                    "AppUpdateUtils",
                    "Update info received: ${newAppUpdateInfo.updateAvailability()} " +
                            "isFlexible: ${newAppUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)} " +
                            "Priority: ${newAppUpdateInfo.updatePriority()}"
                )
                appUpdateInfo = newAppUpdateInfo
                isUpdateAvailable =
                    newAppUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            }

            activityResultLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) {}
        }

        fun startUpdate() {
            if (appUpdateManager == null || appUpdateInfo == null || activityResultLauncher == null) {
                return
            }

            appUpdateManager!!.startUpdateFlowForResult(
                appUpdateInfo!!,
                activityResultLauncher!!,
                AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
            )
        }

    }
}