package com.franco.CaminaConmigo.model_mvvm.chat.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val content: String = "",
    val isRead: Boolean = false,
    val timestamp: Long = 0L // Mantén esto como Long
)

