package com.franco.CaminaConmigo.model_mvvm.inicio.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.inicio.model.GoogleSignInModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleSignInViewModel : ViewModel() {

    private val googleSignInModel = GoogleSignInModel()
    private val _accountLiveData = MutableLiveData<GoogleSignInAccount?>()
    val accountLiveData: LiveData<GoogleSignInAccount?> = _accountLiveData

    // Firebase Auth
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // Obtiene las opciones de inicio de sesión de Google
    fun getSignInOptions() = googleSignInModel.googleSignInOptions()

    // Manejo del resultado de inicio de sesión
    fun handleSignInResult(task: Task<GoogleSignInAccount>, callback: (Boolean) -> Unit) {
        googleSignInModel.handleSignInResult(task) { account ->
            if (account != null) {
                // Autenticación con Firebase
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            _accountLiveData.postValue(account)
                            callback(true)
                        } else {
                            callback(false)
                        }
                    }
            } else {
                callback(false)
            }
        }
    }
}
