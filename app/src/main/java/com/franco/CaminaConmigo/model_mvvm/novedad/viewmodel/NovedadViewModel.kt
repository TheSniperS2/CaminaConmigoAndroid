package com.franco.CaminaConmigo.model_mvvm.novedad.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.novedad.model.Reporte
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.math.pow
import kotlin.math.sqrt

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

    fun buscarReportes(query: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("reportes")
            .whereGreaterThanOrEqualTo("type", query)
            .whereLessThanOrEqualTo("type", query + '\uf8ff')
            .get()
            .addOnSuccessListener { documents ->
                val reportesList = mutableListOf<Reporte>()
                for (document in documents) {
                    val reporte = document.toObject(Reporte::class.java)
                    reportesList.add(reporte)
                }
                _reportes.value = reportesList
            }
            .addOnFailureListener {
                _reportes.value = emptyList()
            }
    }

    fun filtrarPorCiudad(userLatitude: Double, userLongitude: Double) {
        val db = FirebaseFirestore.getInstance()
        db.collection("reportes")
            .get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.documents.mapNotNull { it.toObject(Reporte::class.java) }
                val listaOrdenada = lista.sortedBy { reporte ->
                    val distancia = calcularDistancia(userLatitude, userLongitude, reporte.latitude, reporte.longitude)
                    distancia
                }
                _reportes.value = listaOrdenada
            }
            .addOnFailureListener {
                _reportes.value = emptyList() // Si falla, se envía una lista vacía
            }
    }

    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return sqrt((lat1 - lat2).pow(2) + (lon1 - lon2).pow(2))
    }
}