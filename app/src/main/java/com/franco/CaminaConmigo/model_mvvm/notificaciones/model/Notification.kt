package com.franco.CaminaConmigo.model_mvvm.notificaciones.model

import com.google.firebase.Timestamp

data class Notification(
    var createdAt: Long = 0L,  // Este campo se usará para almacenar el timestamp en formato Long
    val data: Map<String, Any>? = null,
    val fromUserId: String = "",
    val fromUsername: String = "",
    val requestId: String = "",
    val isRead: Boolean = false,
    val message: String = "",
    val title: String = "",
    val type: String = "",
    val userId: String = ""
) {
    // Puedes tener una función para convertir Timestamp a Long si quieres que se haga de forma automática
    fun setCreatedAt(timestamp: Timestamp?) {
        createdAt = timestamp?.toDate()?.time ?: 0L
    }
}