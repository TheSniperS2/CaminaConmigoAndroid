package com.franco.CaminaConmigo.model_mvvm.notificaciones.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.notificaciones.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

class NotificationsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> get() = _notifications

    fun loadNotifications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("NotificationsViewModel", "Usuario no autenticado")
            return
        }

        val userId = currentUser.uid
        Log.d("NotificationsViewModel", "Cargando notificaciones para el usuario: $userId")

        db.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING) // Ordena de más reciente a más antiguo
            .get()
            .addOnSuccessListener { result ->
                Log.d("NotificationsViewModel", "Se encontraron ${result.size()} notificaciones")

                val notificationList = result.documents.mapNotNull { document ->
                    try {
                        Notification(
                            createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            fromUserId = document.getString("fromUserId") ?: "",
                            fromUsername = document.getString("fromUsername") ?: "",
                            requestId = document.getString("requestId") ?: "",
                            isRead = document.getBoolean("isRead") ?: false,
                            message = document.getString("message") ?: "",
                            title = document.getString("title") ?: "",
                            type = document.getString("type") ?: "",
                            userId = document.getString("userId") ?: ""
                        ).also {
                            Log.d("NotificationsViewModel", "Notificación cargada: $it")
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationsViewModel", "Error al convertir notificación", e)
                        null
                    }
                }

                _notifications.value = notificationList
            }
            .addOnFailureListener { exception ->
                Log.e("NotificationsViewModel", "Error al obtener notificaciones", exception)
            }
    }

    fun acceptFriendRequest(notification: Notification) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("NotificationsViewModel", "Usuario no autenticado")
            return
        }

        Log.d("NotificationsViewModel", "Usuario autenticado: ${currentUser.uid}")
        Log.d("NotificationsViewModel", "Aceptando solicitud de amistad de: ${notification.fromUserId}")

        val friendsRef = db.collection("users")
            .document(currentUser.uid)
            .collection("friends")
            .document(notification.fromUserId)

        val friendData = mapOf(
            "nickname" to notification.fromUsername,
            "addedAt" to Date() // Fecha de aceptación
        )

        friendsRef.set(friendData)
            .addOnSuccessListener {
                Log.d("NotificationsViewModel", "Solicitud de amistad aceptada")
                removeNotification(notification)
            }
            .addOnFailureListener { e ->
                Log.e("NotificationsViewModel", "Error al aceptar la solicitud", e)
            }
    }

    fun removeNotification(notification: Notification) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("NotificationsViewModel", "Usuario no autenticado")
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .collection("notifications")
            .document(notification.requestId)
            .delete()
            .addOnSuccessListener {
                Log.d("NotificationsViewModel", "Notificación eliminada correctamente")
                loadNotifications() // Recarga las notificaciones después de eliminar
            }
            .addOnFailureListener { e ->
                Log.e("NotificationsViewModel", "Error al eliminar la notificación", e)
            }
    }
}
