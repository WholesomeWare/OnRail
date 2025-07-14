package com.csakitheone.onrail.data.sources

import android.util.Log
import com.csakitheone.onrail.data.model.EMMAVehiclePosition
import com.csakitheone.onrail.data.model.Message
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RTDB {

    enum class ChatRoomType(
        val path: String,
    ) {
        TERRITORY("territory"),
        TRAIN("train"),
    }

    companion object {

        val CONFIG_KEY_ACTIVE_FILTER_DURATION_MILLIS = "ACTIVE_FILTER_DURATION_MILLIS"
        val CONFIG_KEY_EMMA_API_CALL_COOLDOWN = "EMMA_API_CALL_COOLDOWN"
        val CONFIG_KEY_MOTD = "MOTD"
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

        fun getChatRelevances(
            callback: (Map<ChatRoomType, Map<String, Long>>) -> Unit,
        ) {
            ref.child("stats/relevance").get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        callback(emptyMap())
                        return@addOnSuccessListener
                    }
                    val relevanceMap = mutableMapOf<ChatRoomType, Map<String, Long>>()
                    if (snapshot.hasChild(ChatRoomType.TRAIN.path)) {
                        relevanceMap[ChatRoomType.TRAIN] = snapshot.child(ChatRoomType.TRAIN.path)
                            .children.associate {
                                (it.key ?: "(empty)") to (it.getValue(Long::class.java) ?: 0L)
                            }
                    }
                    if (snapshot.hasChild(ChatRoomType.TERRITORY.path)) {
                        relevanceMap[ChatRoomType.TERRITORY] =
                            snapshot.child(ChatRoomType.TERRITORY.path)
                                .children.associate {
                                    (it.key ?: "(empty)") to (it.getValue(Long::class.java) ?: 0L)
                                }
                    }
                    callback(relevanceMap)
                }.addOnFailureListener {
                    callback(emptyMap())
                }
        }

        fun updateVehicleData(
            vehicleData: List<EMMAVehiclePosition>
        ) {
            if (vehicleData.isEmpty()) {
                Log.w("RTDB", "updateVehicleData called with empty vehicleData list")
                return
            }

            ref.child("vehiclePositions")
                .setValue(vehicleData.associateBy { vehicleData -> vehicleData.trip.tripShortName.ifBlank { "(empty)" } })
            ref.child("stats/relevance/vehiclePositions").setValue(ServerValue.TIMESTAMP)
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

        fun sendMessage(
            chatRoomType: ChatRoomType,
            chatRoomId: String,
            message: Message,
            callback: (Boolean) -> Unit = {},
        ) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMessageSentTimestamp < MESSAGE_SENDING_COOLDOWN) {
                callback(false)
                return
            }
            lastMessageSentTimestamp = currentTime

            val messagePath = "chats/${chatRoomType.path}/$chatRoomId/${message.timestamp}"
            val relevancePath = "stats/relevance/${chatRoomType.path}/$chatRoomId"

            ref.child(messagePath)
                .setValue(message)
                .addOnCompleteListener {
                    callback(it.isSuccessful)
                    ref.child("$messagePath/timestamp")
                        .setValue(ServerValue.TIMESTAMP)
                }

            ref.child(relevancePath).setValue(ServerValue.TIMESTAMP)
        }

        fun removeMessage(
            chatRoomType: ChatRoomType,
            chatRoomId: String,
            message: Message,
            callback: (Boolean) -> Unit = {},
        ) {
            val messagePath = "chats/${chatRoomType.path}/$chatRoomId/${message.key}"

            ref.child(messagePath).removeValue()
                .addOnCompleteListener { callback(it.isSuccessful) }
        }

        fun listenForMessages(
            chatRoomType: ChatRoomType,
            chatRoomId: String,
            onMessageAdded: (Message) -> Unit,
            onMessageRemoved: (Message) -> Unit = {},
        ) {
            messageListener = object : ChildEventListener {
                override fun onChildAdded(
                    snapshot: com.google.firebase.database.DataSnapshot,
                    previousChildName: String?
                ) {
                    snapshot.getValue(Message::class.java)?.let { message ->
                        onMessageAdded(message.copy(key = snapshot.key ?: ""))
                    }
                }

                override fun onChildChanged(
                    snapshot: com.google.firebase.database.DataSnapshot,
                    previousChildName: String?
                ) {
                }

                override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {
                    snapshot.getValue(Message::class.java)?.let { message ->
                        onMessageRemoved(message.copy(key = snapshot.key ?: ""))
                    }
                }

                override fun onChildMoved(
                    snapshot: com.google.firebase.database.DataSnapshot,
                    previousChildName: String?
                ) {
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            ref.child("chats/${chatRoomType.path}/$chatRoomId")
                .addChildEventListener(messageListener!!)
        }

        fun stopListeningForMessages() {
            messageListener?.let {
                ref.removeEventListener(it)
                messageListener = null
            }
        }

        /**
         * Clears messages older than the OLD_MESSAGE_CUTOFF.
         * This function should only be called with an unmetered network connection
         * and should only output its results to the console.
         */
        fun clearOldMessages() {
            ref.child("chats").get()
                .addOnSuccessListener { chatsSnapshot ->
                    val oldMessageCutoff = System.currentTimeMillis() - OLD_MESSAGE_CUTOFF

                    chatsSnapshot.children.forEach { roomTypes ->
                        roomTypes.children.forEach { room ->
                            room.children.forEach { messageSnapshot ->
                                val message = messageSnapshot.getValue(Message::class.java)
                                    ?.copy(key = messageSnapshot.key ?: "")
                                if (message != null && message.timestamp < oldMessageCutoff) {
                                    ref.child("chats/${roomTypes.key}/${room.key}/${messageSnapshot.key}")
                                        .removeValue()
                                        .addOnSuccessListener {
                                            Log.d(
                                                "RTDB",
                                                "Removed old message: ${message.content} from: ${roomTypes.key}/${room.key}"
                                            )
                                        }
                                }
                            }
                        }
                    }
                }
        }
    }
}