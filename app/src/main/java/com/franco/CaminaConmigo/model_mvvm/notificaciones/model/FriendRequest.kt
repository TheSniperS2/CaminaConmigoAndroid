package com.franco.CaminaConmigo.model_mvvm.notificaciones.model

import com.google.firebase.Timestamp

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserEmail: String = "",
    val fromUserName: String = "",
    val toUserId: String = "",
    val status: String = "",
    val createdAt: Timestamp? = null
) {
    enum class RequestStatus {
        pending, accepted, rejected
    }
}