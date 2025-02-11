package com.franco.CaminaConmigo.model_mvvm.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.chat.model.Chat
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message
import com.google.firebase.firestore.FirebaseFirestore

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Para manejar los mensajes del chat
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    // Para manejar los chats
    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> get() = _chats

    // Cargar mensajes de un chat específico
    fun loadMessages(chatId: String) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")  // Asegúrate de tener un campo 'timestamp' para ordenar los mensajes
            .get()
            .addOnSuccessListener { result ->
                val messageList = result.documents.map { it.toObject(Message::class.java) ?: Message() }
                _messages.value = messageList
            }
            .addOnFailureListener { exception ->
                // Maneja el error aquí si es necesario
                _messages.value = emptyList()
            }
    }

    // Enviar un mensaje a un chat
    fun sendMessage(chatId: String, text: String) {
        val message = Message(
            chatId = chatId,
            senderId = "currentUserId",  // Reemplaza con el ID real del usuario
            text = text,
            timestamp = System.currentTimeMillis()
        )

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                // Después de enviar el mensaje, podrías cargar los mensajes nuevamente si lo deseas
                loadMessages(chatId)
            }
    }

    // Cargar la lista de chats
    fun loadChats() {
        db.collection("chats")
            .get()
            .addOnSuccessListener { result ->
                val chatList = result.documents.mapNotNull { it.toObject(Chat::class.java) }
                _chats.value = chatList
            }
            .addOnFailureListener { exception ->
                // Maneja el error aquí si es necesario
                _chats.value = emptyList()
            }
    }
}
