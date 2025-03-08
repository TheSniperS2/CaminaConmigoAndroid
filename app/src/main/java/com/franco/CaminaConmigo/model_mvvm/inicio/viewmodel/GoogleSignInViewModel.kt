@file:Suppress("DEPRECATION")

package com.franco.CaminaConmigo.model_mvvm.inicio.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.inicio.model.GoogleSignInModel
import com.franco.CaminaConmigo.model_mvvm.perfil.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class GoogleSignInViewModel : ViewModel() {

    private val googleSignInModel = GoogleSignInModel()
    private val _accountLiveData = MutableLiveData<GoogleSignInAccount?>()
    val accountLiveData: LiveData<GoogleSignInAccount?> = _accountLiveData

    // Firebase Auth
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // Obtiene las opciones de inicio de sesión de Google
    fun getSignInOptions() = googleSignInModel.googleSignInOptions()

    fun handleSignInResult(task: Task<GoogleSignInAccount>, callback: (Boolean) -> Unit) {
        googleSignInModel.handleSignInResult(task) { account ->
            if (account != null) {
                // Autenticación con Firebase
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            _accountLiveData.postValue(account)

                            // Verificar si el usuario ya existe en Firestore
                            val userId = firebaseAuth.currentUser?.uid
                            val firestore = FirebaseFirestore.getInstance()

                            firestore.collection("users").document(userId!!).get()
                                .addOnSuccessListener { document ->
                                    if (!document.exists()) {
                                        // Crear el usuario en Firestore con un nombre de usuario único
                                        val email = account.email ?: "No Email"
                                        val baseUsername = email.substringBefore("@")
                                        generateUniqueUsername(baseUsername) { uniqueUsername ->
                                            val newUser = User(
                                                name = account.displayName ?: "Usuario Desconocido",
                                                username = uniqueUsername,
                                                profileType = "Público",
                                                email = email,
                                                id = userId,
                                                joinDate = Timestamp.now(),
                                                photoURL = account.photoUrl.toString()
                                            )
                                            saveNewUserToFirestore(newUser)
                                        }
                                    }
                                    callback(true)
                                }
                                .addOnFailureListener {
                                    callback(false)
                                }
                        } else {
                            callback(false)
                        }
                    }
            } else {
                callback(false)
            }
        }
    }

    private fun generateUniqueUsername(baseUsername: String, callback: (String) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        var username = baseUsername
        var suffix = 1

        firestore.collection("users").whereEqualTo("username", username).get()
            .addOnSuccessListener { documents ->
                while (!documents.isEmpty) {
                    username = "$baseUsername$suffix"
                    suffix++
                    firestore.collection("users").whereEqualTo("username", username).get()
                }
                callback(username)
            }
    }

    private fun saveNewUserToFirestore(user: User) {
        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(user.id)

        userRef.set(user)
            .addOnSuccessListener { Log.d("Firestore", "Usuario guardado correctamente") }
            .addOnFailureListener { e -> Log.e("Firestore", "Error al guardar usuario", e) }
    }
}