package com.franco.testing4.model_mvvm.perfil.viewmodel

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.franco.testing4.model_mvvm.perfil.model.User
import kotlin.random.Random

class MiPerfilViewModel(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getUser(): User? {
        val user = auth.currentUser ?: return null

        val name = sharedPreferences.getString("user_name", user.displayName) ?: "Usuario Desconocido"
        val username = sharedPreferences.getString("user_username", generateUsername(name))
        val isPrivate = sharedPreferences.getBoolean("user_is_private", false)
        val photoUrl = user.photoUrl?.toString()

        return User(name, username!!, isPrivate, photoUrl)
    }

    fun saveUserName(name: String) {
        sharedPreferences.edit().putString("user_name", name).apply()
    }

    fun saveUserUsername(username: String) {
        sharedPreferences.edit().putString("user_username", username).apply()
    }

    fun setPrivacy(isPrivate: Boolean) {
        sharedPreferences.edit().putBoolean("user_is_private", isPrivate).apply()
    }

    fun saveProfileImage(imageUri: String) {
        sharedPreferences.edit().putString("user_profile_image", imageUri).apply()
    }

    private fun generateUsername(name: String): String {
        val cleanName = name.lowercase().replace(" ", "").replace("[^a-zA-Z0-9]".toRegex(), "")
        val randomSuffix = Random.nextInt(100, 999)
        return "$cleanName$randomSuffix"
    }
}
