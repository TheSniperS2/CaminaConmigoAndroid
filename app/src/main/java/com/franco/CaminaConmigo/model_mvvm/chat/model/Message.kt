package com.franco.CaminaConmigo.model_mvvm.chat.model

data class Message(
    val id: String = "",  // Este es el identificador único del mensaje
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
