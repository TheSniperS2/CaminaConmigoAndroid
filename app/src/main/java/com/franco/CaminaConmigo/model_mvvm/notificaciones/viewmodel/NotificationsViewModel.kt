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
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                Log.d("NotificationsViewModel", "Se encontraron ${result.size()} notificaciones")

                val notificationList = result.documents.mapNotNull { document ->
                    try {
                        val fromUserId = document.getString("fromUserId") ?: ""
                        val toUserId = document.getString("userId") ?: ""
                        val fromUsername = document.getString("fromUsername") ?: ""
                        val requestId = document.getString("requestId") ?: ""
                        val isRead = document.getBoolean("isRead") ?: false
                        val message = document.getString("message") ?: ""
                        val title = document.getString("title") ?: ""
                        val type = document.getString("type") ?: ""
                        val createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L

                        Log.d("NotificationsViewModel", "fromUserId: $fromUserId, toUserId: $toUserId, fromUsername: $fromUsername, requestId: $requestId, isRead: $isRead, message: $message, title: $title, type: $type, createdAt: $createdAt")

                        Notification(
                            createdAt = createdAt,
                            fromUserId = fromUserId,
                            fromUsername = fromUsername,
                            requestId = requestId,
                            isRead = isRead,
                            message = message,
                            title = title,
                            type = type,
                            userId = toUserId
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

        val fromUserId = notification.fromUserId
        val toUserId = notification.userId

        if (fromUserId.isEmpty() || toUserId.isEmpty()) {
            Log.e("NotificationsViewModel", "fromUserId o toUserId está vacío")
            return
        }

        Log.d("NotificationsViewModel", "Usuario autenticado: ${currentUser.uid}")
        Log.d("NotificationsViewModel", "Aceptando solicitud de amistad de: $fromUserId")

        val friendRequestQuery = db.collection("friendRequests")
            .whereEqualTo("fromUserId", fromUserId)
            .whereEqualTo("toUserId", toUserId)
            .whereEqualTo("status", "pending")

        friendRequestQuery.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val friendRequestDoc = querySnapshot.documents[0]
                val friendRequestRef = friendRequestDoc.reference
                val friendsRef = db.collection("users").document(currentUser.uid).collection("friends").document(fromUserId)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(friendRequestRef)
                    if (snapshot.exists()) {
                        transaction.update(friendRequestRef, "status", "accepted")
                        transaction.set(friendsRef, mapOf(
                            "nickname" to notification.fromUsername,
                            "addedAt" to Date()
                        ))
                    }
                }.addOnSuccessListener {
                    Log.d("NotificationsViewModel", "Solicitud de amistad aceptada y amigo añadido")
                    removeNotification(notification)
                }.addOnFailureListener { e ->
                    Log.e("NotificationsViewModel", "Error al aceptar la solicitud", e)
                }
            } else {
                Log.e("NotificationsViewModel", "No se encontró la solicitud de amistad pendiente")
            }
        }.addOnFailureListener { e ->
            Log.e("NotificationsViewModel", "Error al buscar la solicitud de amistad", e)
        }
    }

    fun rejectFriendRequest(notification: Notification) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("NotificationsViewModel", "Usuario no autenticado")
            return
        }

        Log.d("NotificationsViewModel", "Usuario autenticado: ${currentUser.uid}")
        Log.d("NotificationsViewModel", "Rechazando solicitud de amistad de: ${notification.fromUserId}")

        val friendRequestRef = db.collection("friendRequests").document(notification.requestId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(friendRequestRef)
            if (snapshot.exists()) {
                transaction.update(friendRequestRef, "status", "rejected")
            }
        }.addOnSuccessListener {
            Log.d("NotificationsViewModel", "Solicitud de amistad rechazada")
            removeNotification(notification)
        }.addOnFailureListener { e ->
            Log.e("NotificationsViewModel", "Error al rechazar la solicitud", e)
        }
    }

    fun removeNotification(notification: Notification) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("NotificationsViewModel", "Usuario no autenticado")
            return
        }

        if (notification.requestId.isEmpty()) {
            Log.e("NotificationsViewModel", "requestId está vacío")
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