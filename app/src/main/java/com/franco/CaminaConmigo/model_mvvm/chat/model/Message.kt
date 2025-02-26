package com.franco.CaminaConmigo.model_mvvm.chat.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Message(
    val id: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false
)