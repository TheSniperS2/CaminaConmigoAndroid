package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.franco.CaminaConmigo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmergenciaDialogFragment : DialogFragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_emergencia_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCarabineros = view.findViewById<ImageButton>(R.id.btnCarabineros)
        val btnContacto1 = view.findViewById<ImageButton>(R.id.btnContacto1)
        val btnContacto2 = view.findViewById<ImageButton>(R.id.btnContacto2)
        val txtCarabineros = view.findViewById<TextView>(R.id.txtCarabineros)
        val txtContacto1 = view.findViewById<TextView>(R.id.txtContacto1)
        val txtContacto2 = view.findViewById<TextView>(R.id.txtContacto2)
        val btnCerrar = view.findViewById<TextView>(R.id.btnCerrar)

        // Obtener los 3 contactos de emergencia: Carabineros + 2 contactos personalizados (con order 0 y 1)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).collection("emergency_contacts")
                .whereIn("order", listOf(0, 1)) // Filtrar por order 0 y 1
                .orderBy("order") // Ordenar por "order"
                .limit(2) // Obtener solo los dos contactos personalizados
                .get()
                .addOnSuccessListener { documents ->
                    val contactos = documents.map { doc ->
                        Pair(doc.getString("name") ?: "No disponible", doc.getString("phone") ?: "")
                    }.toMutableList()

                    // Agregar el contacto de Carabineros al principio de la lista
                    contactos.add(0, Pair("Carabineros", "133"))

                    // Asegurarse de que haya exactamente 3 contactos
                    if (contactos.size < 3) {
                        contactos.add(Pair("No disponible", ""))
                    }

                    // Configurar los contactos en la interfaz
                    // Carabineros
                    txtCarabineros.text = contactos[0].first
                    btnCarabineros.setOnClickListener {
                        realizarLlamada(contactos[0].second)
                    }

                    // Contacto 1 (con order 0)
                    txtContacto1.text = contactos[1].first
                    btnContacto1.setOnClickListener {
                        if (contactos[1].second.isNotEmpty()) realizarLlamada(contactos[1].second)
                    }

                    // Contacto 2 (con order 1)
                    txtContacto2.text = contactos[2].first
                    btnContacto2.setOnClickListener {
                        if (contactos[2].second.isNotEmpty()) realizarLlamada(contactos[2].second)
                    }
                }
                .addOnFailureListener {
                    txtContacto1.text = "Error"
                    txtContacto2.text = "Error"
                    btnContacto1.isEnabled = false
                    btnContacto2.isEnabled = false
                }
        }

        // Cerrar el diálogo
        btnCerrar.setOnClickListener {
            dismiss()
        }
    }

    private fun realizarLlamada(numero: String) {
        if (numero.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))
            startActivity(intent)
        }
    }
}
