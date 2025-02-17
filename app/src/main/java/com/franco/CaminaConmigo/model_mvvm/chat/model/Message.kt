package com.franco.CaminaConmigo.model_mvvm.chat.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null,  // Usa Timestamp en lugar de Long
    val isRead: Boolean = false  // Asegúrate de que este campo esté presente
)