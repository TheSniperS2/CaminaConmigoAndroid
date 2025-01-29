package com.franco.CaminaConmigo.model_mvvm.inicio.model

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class GoogleSignInModel {

    // Configuración de Google Sign-In
    fun googleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("925265916348-8l7e2dotbohh2g0bc37bntbs60h7qh67.apps.googleusercontent.com")  // Reemplaza con tu Client ID
            .requestEmail()
            .build()
    }

    // Manejo del resultado de Google Sign-In
    fun handleSignInResult(task: Task<GoogleSignInAccount>, callback: (GoogleSignInAccount?) -> Unit) {
        try {
            val account = task.getResult(ApiException::class.java) // Obtén GoogleSignInAccount
            callback(account)  // Pasa la cuenta al callback
        } catch (e: ApiException) {
            callback(null)  // Si ocurre un error, se pasa null
        }
    }
}
