package com.franco.CaminaConmigo.model_mvvm.chat.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.chat.model.Chat
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> get() = _chats

    private val _userNames = MutableLiveData<Map<String, String>>()
    val userNames: LiveData<Map<String, String>> get() = _userNames

    fun loadMessages(chatId: String) {
        Log.d("ChatViewModel", "Iniciando loadMessages para chatId: $chatId")

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Error al obtener mensajes: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.w("ChatViewModel", "No hay mensajes en este chat")
                    _messages.postValue(emptyList())
                    return@addSnapshotListener
                }

                Log.d("ChatViewModel", "Mensajes encontrados: ${snapshots.size()}")

                val messageList = snapshots.documents.mapNotNull { doc ->
                    try {
                        val message = doc.toObject(Message::class.java)?.copy(
                            id = doc.id
                        )
                        Log.d("ChatViewModel", "Mensaje cargado: ${message?.content}, Timestamp: ${message?.timestamp}")
                        message
                    } catch (ex: Exception) {
                        Log.e("ChatViewModel", "Error al procesar mensaje: ${ex.message}")
                        null
                    }
                }

                Log.d("ChatViewModel", "Total de mensajes cargados: ${messageList.size}")
                _messages.postValue(messageList)
            }
    }

    fun loadLocationMessages(chatId: String) {
        val locationSharingRef = db.collection("chats").document(chatId).collection("locationSharing")

        locationSharingRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Error al obtener mensajes de ubicación: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.w("ChatViewModel", "No hay mensajes de ubicación en este chat")
                    return@addSnapshotListener
                }

                val locationMessages = snapshots.documents.mapNotNull { doc ->
                    try {
                        val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                        Log.d("ChatViewModel", "Mensaje de ubicación cargado: ${message?.content}, Timestamp: ${message?.timestamp}")
                        message
                    } catch (ex: Exception) {
                        Log.e("ChatViewModel", "Error al procesar mensaje de ubicación: ${ex.message}")
                        null
                    }
                }

                val combinedMessages = _messages.value.orEmpty() + locationMessages
                _messages.postValue(combinedMessages.sortedBy { it.timestamp })
            }
    }

    fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error al obtener chats: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.w("ChatViewModel", "No hay chats disponibles")
                    _chats.value = emptyList()
                    return@addSnapshotListener
                }

                Log.d("ChatViewModel", "Se encontraron ${snapshots.size()} chats.")

                val chatList = snapshots.documents.mapNotNull { doc ->
                    try {
                        val chatId = doc.id
                        val participants = doc.get("participants") as? List<String> ?: emptyList()
                        val userNames = doc.get("userNames") as? Map<String, String> ?: emptyMap()
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val lastMessageTimestamp = doc.getTimestamp("lastMessageTimestamp")

                        Log.d("ChatViewModel", "Chat encontrado - ID: $chatId, Participantes: $participants")

                        val chatName = userNames.filterKeys { it != currentUserId }.values.firstOrNull() ?: "Chat"

                        Chat(
                            chatId = chatId,
                            name = chatName,
                            lastMessage = lastMessage,
                            lastMessageTimestamp = lastMessageTimestamp
                        ).also {
                            _userNames.value = userNames
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error al convertir documento: ${e.message}")
                        null
                    }
                }

                Log.d("ChatViewModel", "Se cargaron ${chatList.size} chats.")

                if (chatList.isNotEmpty()) {
                    _chats.value = chatList
                    Log.d("ChatViewModel", "Total de chats cargados: ${chatList.size}")
                } else {
                    Log.w("ChatViewModel", "La lista de chats está vacía después de procesar los documentos")
                    _chats.value = emptyList()
                }
            }
    }

    fun createChat(friendId: String, friendName: String) {
        val currentUser = auth.currentUser ?: return

        val chatRef = db.collection("chats").document()
        val userNames = mapOf(
            currentUser.uid to "Tú",
            friendId to friendName
        )

        val newChat = hashMapOf(
            "participants" to listOf(currentUser.uid, friendId),
            "userNames" to userNames,
            "lastMessage" to "",
            "lastMessageTimestamp" to null
        )

        chatRef.set(newChat)
            .addOnSuccessListener { Log.d("ChatViewModel", "Chat creado con éxito: ${chatRef.id}") }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al crear chat: ${e.message}") }
    }

    fun sendLocationMessage(chatId: String, locationMessage: Map<String, Any>) {
        val chatRef = db.collection("chats").document(chatId)
        val locationSharingRef = chatRef.collection("locationSharing")

        val newLocationMessageRef = locationSharingRef.document()

        newLocationMessageRef.set(locationMessage)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Mensaje de ubicación enviado con ID: ${newLocationMessageRef.id}")
            }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al enviar mensaje de ubicación: ${e.message}") }
    }

    fun sendMessage(chatId: String, message: Message) {
        val currentUser = auth.currentUser ?: return
        val chatRef = db.collection("chats").document(chatId)
        val messagesRef = chatRef.collection("messages")

        val newMessageRef = messagesRef.document()

        newMessageRef.set(message)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Mensaje enviado con ID: ${newMessageRef.id}")

                chatRef.update(
                    mapOf(
                        "lastMessage" to message.content,
                        "lastMessageTimestamp" to message.timestamp
                    )
                ).addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Error al actualizar el último mensaje: ${e.message}")
                }
            }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al enviar mensaje: ${e.message}") }
    }

    private fun markMessagesAsRead(messages: List<Message>, chatId: String) {
        val currentUser = auth.currentUser ?: return
        val batch = db.batch()

        val unreadMessages = messages.filter { !it.isRead && it.senderId != currentUser.uid }

        for (message in unreadMessages) {
            val messageRef = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(message.id)

            batch.update(messageRef, "isRead", true)
        }

        if (unreadMessages.isNotEmpty()) {
            val chatRef = db.collection("chats").document(chatId)
            batch.update(chatRef, "unreadCount.${currentUser.uid}", 0)

            batch.commit().addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al marcar mensajes como leídos: ${e.message}")
            }
        }
    }

    fun updateNickname(chatId: String, userId: String, newNickname: String) {
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nicknames = document.get("nicknames") as? MutableMap<String, String> ?: mutableMapOf()
                nicknames[userId] = newNickname

                chatRef.update("nicknames", nicknames)
                    .addOnSuccessListener {
                        Log.d("ChatViewModel", "Apodo actualizado con éxito en la base de datos")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatViewModel", "Error al actualizar apodo en la base de datos: ${e.message}")
                    }
            } else {
                Log.e("ChatViewModel", "El documento del chat no existe")
            }
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Error al obtener el documento del chat: ${e.message}")
        }
    }

    fun isGroupChat(chatId: String, callback: (Boolean) -> Unit) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val participants = document.get("participants") as? List<String> ?: emptyList()
                val isGroup = participants.size > 2 // Si hay más de 2 personas, es grupo
                callback(isGroup)
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun updateGroupName(chatId: String, newName: String) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.update("name", newName)
            .addOnSuccessListener { Log.d("ChatViewModel", "Nombre del grupo actualizado con éxito") }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al actualizar nombre del grupo: ${e.message}") }
    }

    fun addAdmin(chatId: String, userId: String) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.update("adminIds", FieldValue.arrayUnion(userId))
            .addOnSuccessListener { Log.d("ChatViewModel", "Administrador agregado con éxito") }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al agregar administrador: ${e.message}") }
    }

    fun removeParticipant(chatId: String, userId: String) {
        isGroupChat(chatId) { isGroup ->
            if (isGroup) {
                val chatRef = db.collection("chats").document(chatId)
                chatRef.update("participants", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener { Log.d("ChatViewModel", "Participante eliminado con éxito") }
                    .addOnFailureListener { Log.e("ChatViewModel", "Error al eliminar participante: ${it.message}") }
            } else {
                Log.e("ChatViewModel", "No se pueden eliminar participantes en un chat individual")
            }
        }
    }

    fun removeAdmin(chatId: String, userId: String) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.update("adminIds", FieldValue.arrayRemove(userId))
            .addOnSuccessListener { Log.d("ChatViewModel", "Administrador removido con éxito") }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al remover administrador: ${e.message}") }
    }

    fun addParticipants(chatId: String, newParticipants: List<String>) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.update("participants", FieldValue.arrayUnion(*newParticipants.toTypedArray()))
            .addOnSuccessListener { Log.d("ChatViewModel", "Participantes añadidos con éxito") }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al añadir participantes: ${e.message}") }
    }

    fun loadChatById(chatId: String, callback: (Chat?) -> Unit) {
        db.collection("chats").document(chatId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val chat = document.toObject(Chat::class.java)?.copy(chatId = document.id)
                    callback(chat)
                } else {
                    Log.w("ChatViewModel", "No se encontró el chat con ID: $chatId")
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al cargar chat: ${e.message}")
                callback(null)
            }
    }

    fun markMessagesAsRead(chatId: String) {
        val currentUser = auth.currentUser ?: return
        db.collection("chats").document(chatId).collection("messages")
            .whereEqualTo("isRead", false)
            .whereNotEqualTo("senderId", currentUser.uid)
            .get()
            .addOnSuccessListener { snapshots ->
                val batch = db.batch()
                for (document in snapshots.documents) {
                    val messageRef = document.reference
                    batch.update(messageRef, "isRead", true)
                }
                batch.commit()
                    .addOnSuccessListener { Log.d("ChatViewModel", "Mensajes marcados como leídos") }
                    .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al marcar mensajes como leídos: ${e.message}") }
            }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al obtener mensajes no leídos: ${e.message}") }
    }
}