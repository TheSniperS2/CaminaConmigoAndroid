package com.franco.CaminaConmigo.model_mvvm.chat.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.chat.model.Chat
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> get() = _chats

    fun loadMessages(chatId: String) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Error al obtener mensajes: ${e.message}")
                    return@addSnapshotListener
                }

                val messageList = snapshots?.documents?.mapNotNull { doc ->
                    try {
                        val message = doc.toObject(Message::class.java)
                        // Convertir Timestamp a Long
                        val timestamp = doc.getTimestamp("timestamp")?.seconds?.times(1000) ?: 0L
                        message?.copy(timestamp = timestamp)
                    } catch (ex: Exception) {
                        Log.e("ChatViewModel", "Error al procesar mensaje: ${ex.message}")
                        null
                    }
                } ?: emptyList()

                _messages.postValue(messageList)
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

                // Verificar si los snapshots están vacíos
                if (snapshots == null || snapshots.isEmpty) {
                    Log.w("ChatViewModel", "No hay chats disponibles")
                    // Actualizar la UI o manejar la lógica de "no hay chats"
                    // Ejemplo: Notificar que no hay chats.
                    _chats.value = emptyList()
                    return@addSnapshotListener
                }

                // Depuración: Ver si el tamaño de los snapshots es el esperado
                Log.d("ChatViewModel", "Se encontraron ${snapshots.size()} chats.")

                // Cargar los chats
                val chatList = snapshots.documents.mapNotNull { doc ->
                    try {
                        val chatId = doc.id
                        val participants = doc.get("participants") as? List<String> ?: emptyList()
                        val userNames = doc.get("userNames") as? Map<String, String> ?: emptyMap()
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val lastMessageTimestamp = doc.getTimestamp("lastMessageTimestamp")?.toDate()?.time ?: 0L

                        Log.d("ChatViewModel", "Chat encontrado - ID: $chatId, Participantes: $participants")

                        val chatName = userNames.filterKeys { it != currentUserId }.values.firstOrNull() ?: "Chat"

                        // Crear el objeto Chat
                        Chat(
                            chatId = chatId,
                            name = chatName,
                            lastMessage = lastMessage,
                            lastMessageTimestamp = lastMessageTimestamp
                        )
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error al convertir documento: ${e.message}")
                        null
                    }
                }

                // Depuración: Ver si chatList tiene elementos
                Log.d("ChatViewModel", "Se cargaron ${chatList.size} chats.")

                // Solo actualizamos la lista de chats si no está vacía
                if (chatList.isNotEmpty()) {
                    _chats.value = chatList
                    Log.d("ChatViewModel", "Total de chats cargados: ${chatList.size}")
                } else {
                    Log.w("ChatViewModel", "La lista de chats está vacía después de procesar los documentos")
                    // Notificar a la UI que la lista está vacía
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

    fun sendMessage(chatId: String, message: String) {
        val currentUser = auth.currentUser ?: return
        val chatRef = db.collection("chats").document(chatId)
        val messagesRef = chatRef.collection("messages")

        // Crea un nuevo documento para obtener el ID
        val newMessageRef = messagesRef.document()

        val messageData = hashMapOf(
            "senderId" to currentUser.uid,
            "content" to message,
            "isRead" to false,
            "timestamp" to Timestamp.now() // Se usa un Timestamp real
        )

        // Guarda el mensaje con su propio ID
        newMessageRef.set(messageData)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Mensaje enviado con ID: ${newMessageRef.id}")

                // Actualiza el último mensaje del chat
                chatRef.update(
                    mapOf(
                        "lastMessage" to message,
                        "lastMessageTimestamp" to Timestamp.now()
                    )
                ).addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Error al actualizar el último mensaje: ${e.message}")
                }
            }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al enviar mensaje: ${e.message}") }
    }

}
