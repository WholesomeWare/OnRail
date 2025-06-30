package com.csakitheone.onrail.data.sources

import com.csakitheone.onrail.data.model.EMMAVehiclePosition
import com.csakitheone.onrail.data.model.Message
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RTDB {
    companion object {

        val CONFIG_KEY_MOTD = "MOTD"
        val CONFIG_KEY_EMMA_API_CALL_COOLDOWN = "EMMA_API_CALL_COOLDOWN"
        val OLD_MESSAGE_CUTOFF = 1000L * 60 * 60 * 24 * 7 // 7 days
        val MESSAGE_CONTENT_LENGTH_LIMIT = 500
        val MESSAGE_SENDING_COOLDOWN = 1000L * 5

        private val ref = Firebase.database.reference
        private var messageListener: ChildEventListener? = null
        private var lastMessageSentTimestamp: Long = 0

        fun getConfigString(
            key: String,
            defaultValue: String = "",
            callback: (String) -> Unit,
        ) {
            ref.child("config/$key").get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        callback(defaultValue)
                        return@addOnSuccessListener
                    }
                    val value = snapshot.getValue(String::class.java) ?: defaultValue
                    callback(value)
                }.addOnFailureListener {
                    callback(defaultValue)
                }
        }

        fun getConfigLong(
            key: String,
            defaultValue: Long = 0L,
            callback: (Long) -> Unit,
        ) {
            ref.child("config/$key").get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        callback(defaultValue)
                        return@addOnSuccessListener
                    }
                    val value = snapshot.getValue(Long::class.java) ?: defaultValue
                    callback(value)
                }.addOnFailureListener {
                    callback(defaultValue)
                }
        }

        fun updateVehicleData(
            vehicleData: List<EMMAVehiclePosition>
        ) {
            ref.child("vehiclePositions").setValue(vehicleData)
            ref.child("stats/relevance/vehiclePositions").setValue(System.currentTimeMillis())
        }

        fun getVehiclePositions(
            callback: (List<EMMAVehiclePosition>) -> Unit
        ) {
            ref.child("vehiclePositions").get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        callback(emptyList())
                        return@addOnSuccessListener
                    }
                    val vehiclePositions =
                        snapshot.children.mapNotNull { it.getValue(EMMAVehiclePosition::class.java) }
                    callback(vehiclePositions)
                }.addOnFailureListener {
                    callback(emptyList())
                }
        }

        fun getVehiclePositionsRelevance(
            callback: (Long) -> Unit
        ) {
            ref.child("stats/relevance/vehiclePositions").get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        callback(0L)
                        return@addOnSuccessListener
                    }
                    val relevance = snapshot.getValue(Long::class.java) ?: 0L
                    callback(relevance)
                }.addOnFailureListener {
                    callback(0L)
                }
        }

        fun getMessages(
            trainId: String,
            callback: (List<Message>) -> Unit
        ) {
            ref.child("trains/$trainId/messages")
                .orderByKey()
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        callback(emptyList())
                        return@addOnSuccessListener
                    }
                    val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                    val oldMessageCutoff = System.currentTimeMillis() - OLD_MESSAGE_CUTOFF
                    val recentMessages = messages.filter { it.timestamp >= oldMessageCutoff }
                    val oldMessages = messages.filter { it.timestamp < oldMessageCutoff }

                    oldMessages.forEach { message ->
                        ref.child("trains/$trainId/messages/${message.timestamp}").removeValue()
                    }

                    callback(recentMessages.sortedByDescending { it.timestamp })
                }.addOnFailureListener {
                    callback(emptyList())
                }
        }

        fun sendMessage(
            trainId: String,
            message: Message,
            callback: (Boolean) -> Unit = {},
        ) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMessageSentTimestamp < MESSAGE_SENDING_COOLDOWN) {
                callback(false)
                return
            }
            lastMessageSentTimestamp = currentTime

            ref.child("trains/$trainId/messages/${message.timestamp}").setValue(message)
                .addOnCompleteListener { callback(it.isSuccessful) }
        }

        fun listenForMessages(
            trainId: String,
            onMessageAdded: (Message) -> Unit,
            onMessageRemoved: (Message) -> Unit = {},
        ) {
            messageListener = object : ChildEventListener {
                override fun onChildAdded(
                    snapshot: com.google.firebase.database.DataSnapshot,
                    previousChildName: String?
                ) {
                    snapshot.getValue(Message::class.java)?.let { message ->
                        onMessageAdded(message)
                    }
                }

                override fun onChildChanged(
                    snapshot: com.google.firebase.database.DataSnapshot,
                    previousChildName: String?
                ) {
                }

                override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {
                    snapshot.getValue(Message::class.java)?.let { message ->
                        onMessageRemoved(message)
                    }
                }

                override fun onChildMoved(
                    snapshot: com.google.firebase.database.DataSnapshot,
                    previousChildName: String?
                ) {
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            ref.child("trains/$trainId/messages")
                .addChildEventListener(messageListener!!)
        }

        fun stopListeningForMessages() {
            messageListener?.let {
                ref.removeEventListener(it)
                messageListener = null
            }
        }
    }
}