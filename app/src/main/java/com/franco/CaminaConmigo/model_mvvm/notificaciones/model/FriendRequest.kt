package com.franco.CaminaConmigo.model_mvvm.notificaciones.model

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserEmail: String = "",
    val toUserId: String = "",
    val status: String = "", // "pending", "accepted", "rejected"
    val createdAt: Long = 0
)