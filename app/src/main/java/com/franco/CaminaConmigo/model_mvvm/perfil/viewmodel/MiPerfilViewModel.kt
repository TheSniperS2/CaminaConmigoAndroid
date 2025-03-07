package com.franco.CaminaConmigo.model_mvvm.perfil.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.franco.CaminaConmigo.model_mvvm.perfil.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class MiPerfilViewModel(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

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
                    var photoURL = document.getString("photoURL")

                    // Asignar la imagen de Google si no existe una en la base de datos
                    if (photoURL.isNullOrEmpty()) {
                        photoURL = user.photoUrl.toString()
                        saveUserPhotoURL(photoURL)
                    }

                    callback(User(name, username, profileType, email, id, joinDate, photoURL))
                } else {
                    val newUser = User(
                        name = user.displayName ?: "Usuario Desconocido",
                        username = "",  // Se deja vacío por defecto
                        profileType = "Público",
                        email = user.email ?: "No Email",
                        id = id,
                        joinDate = Timestamp.now(),
                        photoURL = user.photoUrl.toString()
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

    fun saveUserPhotoURL(photoURL: String) {
        val user = auth.currentUser ?: return

        val userRef = firestore.collection("users").document(user.uid)
        userRef.update("photoURL", photoURL)
            .addOnSuccessListener { Log.d("Firestore", "PhotoURL actualizado a: $photoURL") }
            .addOnFailureListener { e -> Log.e("Firestore", "Error al actualizar photoURL", e) }
    }

    fun uploadProfileImage(imageUri: Uri, callback: (String?) -> Unit) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val imageRef: StorageReference = storage.reference.child("profile_images/$userId.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val photoURL = uri.toString()
                    saveUserPhotoURL(photoURL)
                    callback(photoURL)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Storage", "Error al subir la imagen", e)
                callback(null)
            }
    }

    private fun saveNewUserToFirestore(user: User) {
        val userRef = firestore.collection("users").document(user.id)

        userRef.set(user, SetOptions.merge())
            .addOnSuccessListener { Log.d("Firestore", "Usuario guardado correctamente") }
            .addOnFailureListener { e -> Log.e("Firestore", "Error al guardar usuario", e) }
    }

    fun isUsernameAvailable(username: String, callback: (Boolean) -> Unit) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.isEmpty)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al verificar disponibilidad del username", e)
                callback(false)
            }
    }

    suspend fun generateUniqueUsername(baseUsername: String): String {
        var username = baseUsername
        var counter = 1

        while (true) {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (snapshot.documents.isEmpty()) {
                return username
            }

            username = "$baseUsername$counter"
            counter += 1
        }
    }
}