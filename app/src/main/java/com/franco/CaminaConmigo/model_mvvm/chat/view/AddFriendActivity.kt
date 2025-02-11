package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.databinding.ActivityAddFriendBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddFriendActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFriendBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFriendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón de enviar solicitud
        binding.btnSendRequest.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                sendFriendRequest(email)
            } else {
                Toast.makeText(this, "Por favor ingresa un correo electrónico", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendFriendRequest(username: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Debes estar autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = currentUser.uid

        // Obtener los datos del usuario actual desde Firestore
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDocument ->
                val currentUserName = userDocument.getString("name") ?: "Usuario"
                val currentUserEmail = userDocument.getString("email") ?: "Sin email"
                val currentUserUsername = userDocument.getString("username") ?: "Sin username" // Nuevo

                // Buscar el usuario por username en Firestore
                db.collection("users")
                    .whereEqualTo("username", username) // Buscar por username en lugar de email
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val receiverUser = documents.documents[0]
                            val receiverId = receiverUser.id // Obtener el UID del receptor

                            // Verificar si ya hay una solicitud pendiente
                            db.collection("friendRequests")
                                .whereEqualTo("fromUserId", currentUserId)
                                .whereEqualTo("toUserId", receiverId)
                                .whereEqualTo("status", "pending")
                                .get()
                                .addOnSuccessListener { requestDocs ->
                                    if (!requestDocs.isEmpty) {
                                        Toast.makeText(this, "Ya enviaste una solicitud a este usuario", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Crear la solicitud de amistad
                                        val requestData = mapOf(
                                            "fromUserId" to currentUserId,
                                            "fromUserName" to currentUserName, // Usar el username
                                            "fromUserEmail" to currentUserEmail,
                                            "toUserId" to receiverId,
                                            "status" to "pending",
                                            "createdAt" to Timestamp.now() // Guarda la fecha correctamente
                                        )

                                        // Agregar solicitud a Firestore
                                        db.collection("friendRequests")
                                            .add(requestData)
                                            .addOnSuccessListener { requestRef ->
                                                val requestId = requestRef.id // Obtener el ID del request

                                                // Crear la estructura del mapa "data"
                                                val dataMap = mapOf(
                                                    "fromUserId" to currentUserId,
                                                    "fromUsername" to currentUserUsername, // Usar el username
                                                    "requestId" to requestId // ID de la solicitud
                                                )

                                                // Crear la notificación con `data`
                                                val notificationData = mapOf(
                                                    "data" to dataMap, // Mapa con datos de la solicitud
                                                    "userId" to receiverId,
                                                    "message" to "$currentUserUsername quiere ser tu amigo",
                                                    "title" to "Nueva solicitud de amistad",
                                                    "type" to "friendRequest",
                                                    "isRead" to false, // Notificación no leída
                                                    "createdAt" to Timestamp.now() // Fecha de la notificación
                                                )

                                                // Guardar la notificación en `users/notifications`
                                                db.collection("users")
                                                    .document(receiverId)
                                                    .collection("notifications")
                                                    .add(notificationData)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(this, "Solicitud enviada a $username", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(this, "Error al enviar notificación", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this, "Error al enviar solicitud", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                        } else {
                            Toast.makeText(this, "El usuario no existe", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al buscar usuario", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
            }
    }
}
