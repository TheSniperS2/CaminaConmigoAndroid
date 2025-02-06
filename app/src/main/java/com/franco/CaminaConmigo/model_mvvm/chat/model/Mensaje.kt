package com.franco.CaminaConmigo.model_mvvm.chat.model

data class Message(
    val text: String,
    val isSentByUser: Boolean = false
)
