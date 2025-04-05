package com.franco.CaminaConmigo.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveUserSession(userId: String) {
        val editor = prefs.edit()
        editor.putString("user_id", userId)
        editor.apply()
    }

    fun getUserSession(): String? {
        return prefs.getString("user_id", null)
    }

    fun clearUserSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }

    fun isUserLoggedIn(): Boolean {
        return getUserSession() != null
    }
}