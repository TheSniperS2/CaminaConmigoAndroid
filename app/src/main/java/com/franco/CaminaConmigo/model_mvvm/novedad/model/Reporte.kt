package com.franco.CaminaConmigo.model_mvvm.novedad.model

import com.google.firebase.Timestamp

data class Reporte(
    val id: String = "",
    val type: String = "",  // Este es el título del reporte (tipo)
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var likes: Int = 0,
    var comentarios: Int = 0, // Puedes ajustar esto si los comentarios están en otro lugar
    val description: String = "", // Descripción del reporte
    val timestamp: Timestamp = Timestamp.now() // Marca de tiempo del reporte
)