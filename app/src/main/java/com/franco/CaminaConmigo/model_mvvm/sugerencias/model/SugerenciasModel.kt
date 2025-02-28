package com.franco.CaminaConmigo.model_mvvm.sugerencias.model

data class SugerenciasModel(
    val nombre: String,
    val numero: String,
    val razon: String,
    val mensaje: String,
    val esAnonimo: Boolean
)