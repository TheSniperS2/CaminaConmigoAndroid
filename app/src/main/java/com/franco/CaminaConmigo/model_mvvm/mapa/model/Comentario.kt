package com.franco.CaminaConmigo.model_mvvm.mapa.model

import java.util.Date

data class Comentario(
    val id: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val timestamp: Date?,
    val reportId: String
)