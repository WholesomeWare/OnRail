package com.csakitheone.onrail.data

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.csakitheone.onrail.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Auth {
    companion object {

        private val auth = Firebase.auth

        var currentUser: FirebaseUser? by mutableStateOf(null)
            private set

        init {
            currentUser = auth.currentUser
        }

        suspend fun signInWithGoogle(
            activity: Activity,
            callback: (user: FirebaseUser?) -> Unit = {}
        ) {
            val credentialManager = CredentialManager.create(activity)

            try {
                val result = credentialManager.getCredential(
                    activity,
                    GetCredentialRequest.Builder()
                        .addCredentialOption(
                            GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(activity.getString(R.string.web_client_id))
                                .build()
                        )
                        .build(),
                )
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)

                auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(
                        googleIdTokenCredential.idToken,
                        null
                    )
                )
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            currentUser = task.result.user
                            callback(task.result.user)
                        } else {
                            currentUser = null
                            callback(null)
                        }
                    }
            } catch (e: NoCredentialException) {
                activity.startActivity(
                    Intent(Settings.ACTION_ADD_ACCOUNT)
                        .putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                )
                currentUser = null
                callback(null)
            } catch (e: Exception) {
                // Handle other exceptions
                currentUser = null
                callback(null)
            }
        }

        fun signOut() {
            currentUser = null
            auth.signOut()
        }

    }
}