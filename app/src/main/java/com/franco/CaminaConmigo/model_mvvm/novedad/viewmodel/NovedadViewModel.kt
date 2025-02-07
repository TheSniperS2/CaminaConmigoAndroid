package com.franco.CaminaConmigo.model_mvvm.novedad.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.novedad.model.Reporte
import com.google.firebase.firestore.FirebaseFirestore

class NovedadViewModel : ViewModel() {

    private val _reportes = MutableLiveData<List<Reporte>>()
    val reportes: LiveData<List<Reporte>> get() = _reportes

    init {
        cargarReportes()
    }

    private fun cargarReportes() {
        val db = FirebaseFirestore.getInstance()
        db.collection("reportes").get().addOnSuccessListener { snapshot ->
            val lista = snapshot.documents.mapNotNull { it.toObject(Reporte::class.java) }
            _reportes.value = lista
        }.addOnFailureListener {
            _reportes.value = emptyList() // Si falla, se envía una lista vacía
        }
    }
}
