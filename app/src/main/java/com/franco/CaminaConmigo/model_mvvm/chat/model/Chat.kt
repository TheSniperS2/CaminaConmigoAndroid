package com.franco.CaminaConmigo.model_mvvm.chat.model

data class Chat(
    var chatId: String = "",
    val userIds: List<String> = listOf(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0,
    val name: String = "" // Campo adicional para el nombre del chat
)
