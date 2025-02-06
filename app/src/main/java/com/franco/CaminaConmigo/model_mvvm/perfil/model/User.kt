package com.franco.CaminaConmigo.model_mvvm.perfil.model

import com.google.firebase.Timestamp

data class User(
    val name: String,
    val username: String,  // Ahora puede ser vacío
    val profileType: String,
    val email: String,
    val id: String,
    val joinDate: Timestamp
)
