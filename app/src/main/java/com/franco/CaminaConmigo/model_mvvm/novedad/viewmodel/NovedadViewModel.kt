package com.franco.CaminaConmigo.model_mvvm.novedad.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.novedad.model.Reporte
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NovedadViewModel : ViewModel() {

    private val _reportes = MutableLiveData<List<Reporte>>()
    val reportes: LiveData<List<Reporte>> get() = _reportes

    init {
        cargarReportes()
    }

    fun cargarReportes() {
        val db = FirebaseFirestore.getInstance()
        db.collection("reportes")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Ordenar por timestamp descendente
            .get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.documents.mapNotNull { it.toObject(Reporte::class.java) }
                _reportes.value = lista
            }
            .addOnFailureListener {
                _reportes.value = emptyList() // Si falla, se envía una lista vacía
            }
    }

    fun filtrarPorTendencias() {
        val db = FirebaseFirestore.getInstance()
        db.collection("reportes")
            .get()
            .addOnSuccessListener { snapshot ->
                val reportes = snapshot.documents.mapNotNull { it.toObject(Reporte::class.java) }
                val reportesConTendencias = mutableListOf<Reporte>()

                for (document in snapshot.documents) {
                    val reporte = document.toObject(Reporte::class.java)
                    if (reporte != null) {
                        val likesRef = document.reference.collection("likes")
                        val comentariosRef = document.reference.collection("comentarios")

                        likesRef.get().addOnSuccessListener { likesSnapshot ->
                            val likesCount = likesSnapshot.size()

                            comentariosRef.get().addOnSuccessListener { comentariosSnapshot ->
                                val comentariosCount = comentariosSnapshot.size()

                                reporte.likes = likesCount
                                reporte.comentarios = comentariosCount

                                reportesConTendencias.add(reporte)

                                if (reportesConTendencias.size == reportes.size) {
                                    reportesConTendencias.sortWith(compareByDescending<Reporte> { it.likes + it.comentarios })
                                    _reportes.value = reportesConTendencias
                                }
                            }.addOnFailureListener {
                                // Manejar errores de comentarios
                                if (reportesConTendencias.size == reportes.size) {
                                    reportesConTendencias.sortWith(compareByDescending<Reporte> { it.likes + it.comentarios })
                                    _reportes.value = reportesConTendencias
                                }
                            }
                        }.addOnFailureListener {
                            // Manejar errores de likes
                            if (reportesConTendencias.size == reportes.size) {
                                reportesConTendencias.sortWith(compareByDescending<Reporte> { it.likes + it.comentarios })
                                _reportes.value = reportesConTendencias
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                _reportes.value = emptyList() // Si falla, se envía una lista vacía
            }
    }
}