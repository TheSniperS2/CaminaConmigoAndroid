package com.franco.CaminaConmigo.model_mvvm.notificaciones.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val userId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val requestId: String = "",
    val isRead: Boolean = false,
    val message: String = "",
    val title: String = "",
    val type: String = "",
    val createdAt: Timestamp? = null
)