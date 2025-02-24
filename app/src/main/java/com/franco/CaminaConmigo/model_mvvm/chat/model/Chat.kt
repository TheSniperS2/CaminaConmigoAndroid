package com.franco.CaminaConmigo.model_mvvm.chat.model

import com.google.firebase.Timestamp

data class Chat(
    val chatId: String = "",
    val name: String = "",
    val participants: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null, // Cambiar a Timestamp
    val userIds: List<String> = emptyList()
)