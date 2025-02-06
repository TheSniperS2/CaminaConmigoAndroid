package com.franco.CaminaConmigo.model_mvvm.contactoemegencia.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.model.ContactoEmergencia
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ContactoEmergenciaViewModel : ViewModel() {

    private val _contactos = MutableLiveData<List<ContactoEmergencia>>()
    val contactos: LiveData<List<ContactoEmergencia>> = _contactos

    private val listaContactos = mutableListOf<ContactoEmergencia>()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        _contactos.value = listaContactos
        cargarContactosDeFirestore()
    }

    private fun cargarContactosDeFirestore() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .orderBy("order") // Asegura que los contactos se obtengan en el orden correcto
                .get()
                .addOnSuccessListener { documents ->
                    listaContactos.clear()
                    for (document in documents) {
                        val contacto = ContactoEmergencia(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            phone = document.getString("phone") ?: "",
                            order = document.getLong("order")?.toInt() ?: listaContactos.size
                        )
                        listaContactos.add(contacto)
                    }
                    _contactos.value = listaContactos
                }
                .addOnFailureListener { exception ->
                    Log.w("ContactoEmergencia", "Error obteniendo contactos: ", exception)
                }
        }
    }

    fun agregarContacto(name: String, phone: String) {
        val userId = auth.currentUser?.uid ?: return
        val order = listaContactos.size

        val nuevoContacto = ContactoEmergencia(
            id = "", name = name, phone = phone, order = order
        )

        firestore.collection("users")
            .document(userId)
            .collection("emergency_contacts")
            .add(mapOf("name" to name, "phone" to phone, "order" to order))
            .addOnSuccessListener { documentReference ->
                nuevoContacto.id = documentReference.id
                listaContactos.add(nuevoContacto)
                _contactos.value = listaContactos
            }
            .addOnFailureListener { e ->
                Log.w("ContactoEmergencia", "Error al agregar contacto", e)
            }
    }

    fun priorizarContacto(index: Int) {
        if (index > 0) {
            val contacto = listaContactos.removeAt(index)
            listaContactos.add(0, contacto)

            actualizarOrdenEnFirestore()
            _contactos.value = listaContactos
        }
    }

    fun moverContacto(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= listaContactos.size || toPosition >= listaContactos.size) {
            return
        }

        val contacto = listaContactos.removeAt(fromPosition)
        listaContactos.add(toPosition, contacto)

        actualizarOrdenEnFirestore()
        _contactos.value = listaContactos
    }

    fun editarNumero(index: Int, nuevoNumero: String) {
        if (index < 0 || index >= listaContactos.size) return // Evita errores de índice

        val contacto = listaContactos[index]
        contacto.phone = nuevoNumero
        _contactos.value = listaContactos.toList() // Notifica cambios a la UI

        // Actualizar en Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contacto.id)
                .update("phone", nuevoNumero)
                .addOnSuccessListener {
                    Log.d("ContactoEmergencia", "Número actualizado correctamente.")
                }
                .addOnFailureListener { e ->
                    Log.w("ContactoEmergencia", "Error al actualizar el número", e)
                }
        }
    }


    private fun actualizarOrdenEnFirestore() {
        val userId = auth.currentUser?.uid ?: return

        listaContactos.forEachIndexed { index, contacto ->
            contacto.order = index
            firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contacto.id)
                .update("order", index)
        }
    }
}
