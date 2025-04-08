package com.franco.CaminaConmigo.model_mvvm.chat.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.chat.model.Chat
import com.franco.CaminaConmigo.model_mvvm.chat.model.LocationMessage
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

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
                        val locationMessage = doc.toObject(LocationMessage::class.java)?.copy(id = doc.id)
                        if (locationMessage != null) {
                            val message = Message(
                                id = locationMessage.id,
                                senderId = locationMessage.senderId,
                                content = "Ubicación: ${locationMessage.latitude}, ${locationMessage.longitude}",
                                timestamp = Timestamp.now(),
                                isRead = false
                            )
                            Log.d("ChatViewModel", "Mensaje de ubicación cargado: ${message.content}, Timestamp: ${message.timestamp}")
                            message
                        } else {
                            null
                        }
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
                        val chatName = doc.getString("name") ?: run {
                            // Si no hay un nombre de grupo, construye un nombre basado en los amigos
                            userNames.filterKeys { it != currentUserId }.values.joinToString(", ")
                        }
                        val isGroup = doc.getBoolean("isGroup") ?: false // Obtener el campo isGroup
                        val groupURL = doc.getString("groupURL") ?: "" // Obtener el campo groupURL

                        Log.d("ChatViewModel", "Chat encontrado - ID: $chatId, Participantes: $participants")

                        Chat(
                            chatId = chatId,
                            name = chatName,
                            lastMessage = lastMessage,
                            lastMessageTimestamp = lastMessageTimestamp,
                            participants = participants,
                            isGroup = isGroup, // Asegurarse de pasar el valor de isGroup
                            groupURL = groupURL // Asegurarse de pasar el valor de groupURL
                        ).also {
                            _userNames.value = userNames
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error al convertir documento: ${e.message}")
                        null
                    }
                }

                Log.d("ChatViewModel", "Se cargaron ${chatList.size} chats.")

                // Ordenar los chats por la fecha del último mensaje en orden descendente
                val sortedChatList = chatList.sortedByDescending { it.lastMessageTimestamp }

                if (sortedChatList.isNotEmpty()) {
                    _chats.value = sortedChatList
                    Log.d("ChatViewModel", "Total de chats cargados: ${sortedChatList.size}")
                } else {
                    Log.w("ChatViewModel", "La lista de chats está vacía después de procesar los documentos")
                    _chats.value = emptyList()
                }
            }
    }

    fun createChat(friendUsername: String) {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .whereEqualTo("username", friendUsername)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("ChatViewModel", "No se encontró usuario con el nombre de usuario: $friendUsername")
                    return@addOnSuccessListener
                }

                val friendId = documents.documents.first().id
                val friendName = documents.documents.first().getString("name") ?: ""

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
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al buscar usuario: ${e.message}")
            }
    }

    fun sendMessage(chatId: String, message: Message) {
        val currentUser = auth.currentUser ?: return
        val chatRef = db.collection("chats").document(chatId)
        val messagesRef = chatRef.collection("messages")

        val newMessageRef = messagesRef.document()

        val messageData = hashMapOf(
            "content" to message.content,
            "isRead" to message.isRead,
            "senderId" to message.senderId,
            "timestamp" to message.timestamp
        )

        newMessageRef.set(messageData)
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

    fun updateNickname(friendId: String, newNickname: String, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser ?: return
        val userRef = db.collection("users").document(currentUser.uid)
        val friendRef = userRef.collection("friends").whereEqualTo("id", friendId).limit(1)

        friendRef.get().addOnSuccessListener { querySnapshot ->
            if (querySnapshot != null && !querySnapshot.isEmpty) {
                val friendDocument = querySnapshot.documents[0]
                friendDocument.reference.update("nickname", newNickname)
                    .addOnSuccessListener {
                        Log.d("ChatViewModel", "Apodo actualizado con éxito en la subcolección friends")
                        updateChatNickname(friendId, newNickname, callback)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatViewModel", "Error al actualizar apodo en la subcolección friends: ${e.message}")
                        callback(false, e.message)
                    }
            } else {
                Log.e("ChatViewModel", "El documento del amigo no existe")
                callback(false, "El documento del amigo no existe")
            }
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Error al obtener el documento del amigo: ${e.message}")
            callback(false, e.message)
        }
    }

    private fun updateChatNickname(friendId: String, newNickname: String, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("chats")
            .whereArrayContains("participants", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    val chatId = document.id
                    val participants = document.get("participants") as? List<String> ?: continue
                    if (participants.contains(friendId)) {
                        val userNames = document.get("userNames") as? MutableMap<String, String> ?: mutableMapOf()
                        userNames[friendId] = newNickname
                        val chatRef = db.collection("chats").document(chatId)
                        batch.update(chatRef, "userNames", userNames)
                    }
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("ChatViewModel", "Apodo actualizado con éxito en la colección chats")
                        callback(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatViewModel", "Error al actualizar apodo en la colección chats: ${e.message}")
                        callback(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al obtener los chats: ${e.message}")
                callback(false, e.message)
            }
    }

    fun isGroupChat(chatId: String, callback: (Boolean) -> Unit) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val isGroup = document.getBoolean("isGroup") ?: false // Usar el campo isGroup
                callback(isGroup)
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun leaveGroup(chatId: String) {
        val currentUser = auth.currentUser ?: return
        val currentUserId = currentUser.uid

        db.collection("chats").document(chatId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val participants = document.get("participants") as? List<String> ?: emptyList()
                val adminIds = document.get("adminIds") as? List<String> ?: emptyList()

                if (participants.size == 1 && participants.contains(currentUserId)) {
                    // Eliminar el grupo si el usuario es el único miembro restante
                    db.collection("chats").document(chatId).delete()
                        .addOnSuccessListener {
                            Log.d("ChatViewModel", "Grupo eliminado ya que el usuario era el único miembro")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatViewModel", "Error al eliminar el grupo: ${e.message}")
                        }
                } else {
                    if (adminIds.contains(currentUserId) && adminIds.size == 1) {
                        // Si el usuario es el único admin, pasar el rol de admin a otro participante
                        val remainingParticipants = participants.filter { it != currentUserId }
                        if (remainingParticipants.isNotEmpty()) {
                            val newAdminId = remainingParticipants.random()
                            db.collection("chats").document(chatId).update("adminIds", FieldValue.arrayUnion(newAdminId))
                                .addOnSuccessListener {
                                    Log.d("ChatViewModel", "Nuevo administrador asignado: $newAdminId")
                                    removeParticipantById(chatId, currentUserId)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ChatViewModel", "Error al asignar nuevo administrador: ${e.message}")
                                }
                        }
                    } else {
                        // Si el usuario no es el único admin, simplemente salir del grupo
                        removeParticipantById(chatId, currentUserId)
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Error al obtener el documento del chat: ${e.message}")
        }
    }

    // Método para cargar amigos que no están en el chat
    fun loadFriendsNotInChat(chatId: String, callback: (List<String>) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(currentUserId).collection("friends").get()
            .addOnSuccessListener { documents ->
                val friends = documents.mapNotNull { it.id }
                db.collection("chats").document(chatId).get()
                    .addOnSuccessListener { chatDocument ->
                        val participants = chatDocument.get("participants") as? List<String> ?: emptyList()
                        val friendsNotInChat = friends.filterNot { it in participants }
                        callback(friendsNotInChat)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatViewModel", "Error al cargar participantes del chat: ${e.message}")
                        callback(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al cargar amigos: ${e.message}")
                callback(emptyList())
            }
    }

    private fun removeParticipantById(chatId: String, userId: String) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.update(
            mapOf(
                "participants" to FieldValue.arrayRemove(userId),
                "unreadCount.$userId" to FieldValue.delete(),
                "adminIds" to FieldValue.arrayRemove(userId)
            )
        ).addOnSuccessListener {
            Log.d("ChatViewModel", "Participante removido con éxito")
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Error al remover participante: ${e.message}")
        }
    }

    fun updateGroupName(chatId: String, newName: String) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.update("name", newName)
            .addOnSuccessListener { Log.d("ChatViewModel", "Nombre del grupo actualizado con éxito") }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al actualizar nombre del grupo: ${e.message}") }
    }

    fun addAdmin(chatId: String, username: String) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("ChatViewModel", "No se encontró usuario con el nombre de usuario: $username")
                    return@addOnSuccessListener
                }

                val userId = documents.documents.first().id

                db.collection("chats").document(chatId)
                    .update("adminIds", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener {
                        Log.d("ChatViewModel", "Admin agregado con éxito")
                    }.addOnFailureListener { e ->
                        Log.e("ChatViewModel", "Error al agregar admin: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al buscar usuario: ${e.message}")
            }
    }


    fun removeParticipant(chatId: String, username: String) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("ChatViewModel", "No se encontró usuario con el nombre de usuario: $username")
                    return@addOnSuccessListener
                }

                val userId = documents.documents.first().id
                val chatRef = db.collection("chats").document(chatId)

                chatRef.update(
                    mapOf(
                        "participants" to FieldValue.arrayRemove(userId),
                        "unreadCount.$userId" to FieldValue.delete(),
                        "adminIds" to FieldValue.arrayRemove(userId)
                    )
                ).addOnSuccessListener {
                    Log.d("ChatViewModel", "Participante removido con éxito")
                }.addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Error al remover participante: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al buscar usuario: ${e.message}")
            }
    }

    fun isAdmin(chatId: String, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: return
        val chatRef = db.collection("chats").document(chatId)
        chatRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val adminIds = document.get("adminIds") as? List<String> ?: emptyList()
                callback(adminIds.contains(currentUser.uid))
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun isAdmin(chatId: String, username: String, callback: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("ChatViewModel", "No se encontró usuario con el nombre de usuario: $username")
                    callback(false)
                    return@addOnSuccessListener
                }

                val userId = documents.documents.first().id

                val chatRef = db.collection("chats").document(chatId)
                chatRef.get().addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val adminIds = document.get("adminIds") as? List<String> ?: emptyList()
                        callback(adminIds.contains(userId))
                    } else {
                        callback(false)
                    }
                }.addOnFailureListener {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun uploadGroupImage(chatId: String, imageUri: Uri, callback: (Boolean) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("group_images/${UUID.randomUUID()}")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateGroupImageUrl(chatId, uri.toString(), callback)
                }.addOnFailureListener {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun updateGroupImageUrl(chatId: String, imageUrl: String, callback: (Boolean) -> Unit) {
        db.collection("chats").document(chatId).update("groupURL", imageUrl)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun removeAdmin(chatId: String, username: String) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("ChatViewModel", "No se encontró usuario con el nombre de usuario: $username")
                    return@addOnSuccessListener
                }

                val userId = documents.documents.first().id

                db.collection("chats").document(chatId)
                    .update("adminIds", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener {
                        Log.d("ChatViewModel", "Admin removido con éxito")
                    }.addOnFailureListener { e ->
                        Log.e("ChatViewModel", "Error al remover admin: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al buscar usuario: ${e.message}")
            }
    }

    // Método para añadir participantes al chat
    fun addParticipants(chatId: String, newParticipants: List<String>) {
        db.collection("chats").document(chatId).update("participants", FieldValue.arrayUnion(*newParticipants.toTypedArray()))
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Participantes añadidos correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al añadir participantes: ${e.message}")
            }
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