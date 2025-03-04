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
        cargarContactosDeFirestore()
    }

    private fun cargarContactosDeFirestore() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("emergency_contacts")
            .orderBy("order")
            .get()
            .addOnSuccessListener { documents ->
                listaContactos.clear()
                for (document in documents) {
                    listaContactos.add(
                        ContactoEmergencia(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            phone = document.getString("phone") ?: "",
                            order = document.getLong("order")?.toInt() ?: listaContactos.size
                        )
                    )
                }
                _contactos.value = listaContactos
            }
            .addOnFailureListener { exception ->
                Log.w("ContactoEmergencia", "Error obteniendo contactos: ", exception)
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
    }

    fun moverContacto(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                listaContactos[i] = listaContactos.set(i + 1, listaContactos[i])
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                listaContactos[i] = listaContactos.set(i - 1, listaContactos[i])
            }
        }
        listaContactos[toPosition].order = toPosition
        actualizarOrdenEnFirestore()
    }

    fun eliminarContacto(index: Int) {
        val userId = auth.currentUser?.uid ?: return
        val contacto = listaContactos.removeAt(index)

        firestore.collection("users")
            .document(userId)
            .collection("emergency_contacts")
            .document(contacto.id)
            .delete()
            .addOnSuccessListener {
                actualizarOrdenEnFirestore()
            }
    }

    fun editarContacto(index: Int, nuevoNombre: String, nuevoNumero: String) {
        val userId = auth.currentUser?.uid ?: return
        val contacto = listaContactos[index]
        contacto.name = nuevoNombre
        contacto.phone = nuevoNumero

        firestore.collection("users")
            .document(userId)
            .collection("emergency_contacts")
            .document(contacto.id)
            .update("name", nuevoNombre, "phone", nuevoNumero)

        _contactos.value = listaContactos
    }

    fun updateContactsOrder(contactos: List<ContactoEmergencia>) {
        val userId = auth.currentUser?.uid ?: return
        val batch = firestore.batch()
        for (contacto in contactos) {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contacto.id)
            batch.update(docRef, "order", contacto.order)
        }
        batch.commit()
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
        _contactos.value = listaContactos
    }
}