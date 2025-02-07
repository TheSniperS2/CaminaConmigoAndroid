package com.franco.CaminaConmigo.model_mvvm.novedad.model

data class Reporte(
    val id: String = "",
    val type: String = "",  // Este es el título del reporte (tipo)
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val likes: Int = 0,
    val comentarios: Int = 0, // Puedes ajustar esto si los comentarios están en otro lugar
    val description: String = "", // Descripción del reporte
)
