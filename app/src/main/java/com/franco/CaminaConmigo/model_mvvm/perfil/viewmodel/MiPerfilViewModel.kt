package com.franco.CaminaConmigo.model_mvvm.perfil.viewmodel

import android.content.Context
import android.util.Log
import com.franco.CaminaConmigo.model_mvvm.perfil.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MiPerfilViewModel(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getUser(callback: (User?) -> Unit) {
        val user = auth.currentUser ?: return callback(null)
        val id = user.uid

        firestore.collection("users").document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: user.displayName ?: "Usuario Desconocido"
                    val username = document.getString("username") ?: ""  // Permite estar vacío
                    val profileType = document.getString("profileType") ?: "Público"
                    val email = document.getString("email") ?: user.email ?: "No Email"
                    val joinDate = document.getTimestamp("joinDate") ?: Timestamp.now()

                    callback(User(name, username, profileType, email, id, joinDate))
                } else {
                    val newUser = User(
                        name = user.displayName ?: "Usuario Desconocido",
                        username = "",  // Se deja vacío por defecto
                        profileType = "Público",
                        email = user.email ?: "No Email",
                        id = id,
                        joinDate = Timestamp.now()
                    )
                    saveNewUserToFirestore(newUser)
                    callback(newUser)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener datos del usuario", e)
                callback(null)
            }
    }

    fun saveUserProfileType(profileType: String) {
        val user = auth.currentUser ?: return

        val userRef = firestore.collection("users").document(user.uid)
        userRef.update("profileType", profileType)
            .addOnSuccessListener { Log.d("Firestore", "Perfil cambiado a: $profileType") }
            .addOnFailureListener { e -> Log.e("Firestore", "Error al actualizar perfil", e) }
    }

    fun saveUserUsername(username: String) {
        val user = auth.currentUser ?: return

        val userRef = firestore.collection("users").document(user.uid)
        userRef.update("username", username)
            .addOnSuccessListener { Log.d("Firestore", "Username actualizado a: $username") }
            .addOnFailureListener { e -> Log.e("Firestore", "Error al actualizar username", e) }
    }

    private fun saveNewUserToFirestore(user: User) {
        val userRef = firestore.collection("users").document(user.id)

        userRef.set(user, SetOptions.merge())
            .addOnSuccessListener { Log.d("Firestore", "Usuario guardado correctamente") }
            .addOnFailureListener { e -> Log.e("Firestore", "Error al guardar usuario", e) }
    }
}
