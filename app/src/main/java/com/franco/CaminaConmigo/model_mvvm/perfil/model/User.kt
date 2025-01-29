package com.franco.CaminaConmigo.model_mvvm.perfil.model

data class User(
    val name: String,
    val username: String,
    val isPrivate: Boolean,
    val photoUrl: String?
)
