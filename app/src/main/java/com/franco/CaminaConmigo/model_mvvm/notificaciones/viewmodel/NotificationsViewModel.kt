package com.franco.CaminaConmigo.model_mvvm.notificaciones.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.notificaciones.model.FriendRequest
import com.franco.CaminaConmigo.model_mvvm.notificaciones.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _friendRequests = MutableLiveData<List<FriendRequest>>()
    val friendRequests: LiveData<List<FriendRequest>> get() = _friendRequests
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> get() = _notifications

    fun loadFriendRequests() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("NotificationsViewModel", "Usuario no autenticado")
            return
        }

        val userId = currentUser.uid
        Log.d("NotificationsViewModel", "Cargando solicitudes de amistad para el usuario: $userId")

        db.collection("friendRequests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendRequest.RequestStatus.pending.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationsViewModel", "Error al obtener solicitudes de amistad", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { document ->
                        document.toObject(FriendRequest::class.java)?.copy(id = document.id)
                    }
                    _friendRequests.value = requests
                }
            }
    }

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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationsViewModel", "Error al obtener notificaciones", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { document ->
                        document.toObject(Notification::class.java)?.copy(id = document.id)
                    }
                    _notifications.value = notifications
                }
            }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("NotificationsViewModel", "Usuario no autenticado")
            return
        }

        val requestId = request.id
        val fromUserId = request.fromUserId
        val toUserId = request.toUserId

        if (requestId.isEmpty() || fromUserId.isEmpty() || toUserId.isEmpty()) {
            Log.e("NotificationsViewModel", "Datos de solicitud incompletos")
            return
        }

        val friendRequestRef = db.collection("friendRequests").document(requestId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(friendRequestRef)
            if (snapshot.exists()) {
                transaction.update(friendRequestRef, "status", "accepted")
                addFriendToUser(currentUser.uid, fromUserId)
                addFriendToUser(fromUserId, currentUser.uid)
                createChatBetweenFriends(currentUser.uid, fromUserId)
                createFriendRequestAcceptedNotification(currentUser.uid, fromUserId)
            }
        }.addOnSuccessListener {
            Log.d("NotificationsViewModel", "Solicitud de amistad aceptada y amigo añadido")
        }.addOnFailureListener { e ->
            Log.e("NotificationsViewModel", "Error al aceptar la solicitud", e)
        }
    }

    private fun addFriendToUser(userId: String, friendId: String) {
        db.collection("users").document(friendId).get().addOnSuccessListener { document ->
            val friendUsername = document.getString("username") ?: "unknown"
            val friendsRef = db.collection("users").document(userId).collection("friends").document(friendId)
            friendsRef.set(mapOf(
                "nickname" to friendUsername,
                "addedAt" to FieldValue.serverTimestamp()
            )).addOnSuccessListener {
                Log.d("NotificationsViewModel", "Amigo añadido a $userId")
            }.addOnFailureListener { e ->
                Log.e("NotificationsViewModel", "Error al añadir amigo a $userId", e)
            }
        }.addOnFailureListener { e ->
            Log.e("NotificationsViewModel", "Error al obtener username de $friendId", e)
        }
    }

    private fun createFriendRequestAcceptedNotification(userId: String, friendId: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val username = document.getString("username") ?: "unknown"
            val notificationData = mapOf(
                "userId" to friendId,
                "fromUserId" to userId,
                "fromUsername" to username,
                "isRead" to false,
                "message" to "$username ha aceptado tu solicitud de amistad",
                "title" to "Solicitud aceptada",
                "type" to "friendRequestAccepted",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(friendId).collection("notifications").add(notificationData)
                .addOnSuccessListener {
                    Log.d("NotificationsViewModel", "Notificación de solicitud de amistad aceptada creada para $friendId")
                }
                .addOnFailureListener { e ->
                    Log.e("NotificationsViewModel", "Error al crear notificación para $friendId", e)
                }
        }.addOnFailureListener { e ->
            Log.e("NotificationsViewModel", "Error al obtener username de $userId", e)
        }
    }

    private fun createChatBetweenFriends(userId1: String, userId2: String) {
        db.collection("users").document(userId1).get().addOnSuccessListener { document1 ->
            val user1Nickname = document1.getString("username") ?: "unknown"
            db.collection("users").document(userId2).get().addOnSuccessListener { document2 ->
                val user2Nickname = document2.getString("username") ?: "unknown"

                val chatRef = db.collection("chats").document()
                val chatData = mapOf(
                    "participants" to listOf(userId1, userId2),
                    "lastMessage" to "¡Hola! Ahora somos amigos",
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "unreadCount" to mapOf(userId1 to 0, userId2 to 0),
                    "userNames" to mapOf(userId1 to user1Nickname, userId2 to user2Nickname)
                )
                chatRef.set(chatData).addOnSuccessListener {
                    Log.d("NotificationsViewModel", "Chat creado entre $userId1 y $userId2")
                }.addOnFailureListener { e ->
                    Log.e("NotificationsViewModel", "Error al crear chat entre $userId1 y $userId2", e)
                }
            }.addOnFailureListener { e ->
                Log.e("NotificationsViewModel", "Error al obtener username de $userId2", e)
            }
        }.addOnFailureListener { e ->
            Log.e("NotificationsViewModel", "Error al obtener username de $userId1", e)
        }
    }

    fun rejectFriendRequest(request: FriendRequest) {
        val requestId = request.id

        if (requestId.isEmpty()) {
            Log.e("NotificationsViewModel", "requestId está vacío")
            return
        }

        val friendRequestRef = db.collection("friendRequests").document(requestId)
        friendRequestRef.update("status", "rejected").addOnSuccessListener {
            Log.d("NotificationsViewModel", "Solicitud de amistad rechazada")
        }.addOnFailureListener { e ->
            Log.e("NotificationsViewModel", "Error al rechazar la solicitud", e)
        }
    }
}