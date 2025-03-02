package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.franco.CaminaConmigo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmergenciaDialogFragment : DialogFragment() {

    private val db = FirebaseFirestore.getInstance()

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = 0

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

        // Ocultar los contactos personalizados por defecto
        txtContacto1.visibility = View.GONE
        btnContacto1.visibility = View.GONE
        txtContacto2.visibility = View.GONE
        btnContacto2.visibility = View.GONE

        // Obtener el ID del usuario actual
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).collection("emergency_contacts")
                .whereIn("order", listOf(0, 1)) // Filtrar por order 0 y 1
                .orderBy("order") // Ordenar por "order"
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Si no hay contactos de emergencia personalizados, mostrar solo Carabineros
                        txtCarabineros.text = "Carabineros"
                        btnCarabineros.setOnClickListener {
                            realizarLlamada("133")
                        }
                    } else {
                        val contactos = documents.map { doc ->
                            Pair(doc.getString("name") ?: "No disponible", doc.getString("phone") ?: "")
                        }.toMutableList()

                        // Agregar el contacto de Carabineros al principio de la lista
                        contactos.add(0, Pair("Carabineros", "133"))

                        // Asegurarse de que haya exactamente 3 contactos
                        while (contactos.size < 3) {
                            contactos.add(Pair("No disponible", ""))
                        }

                        // Configurar los contactos en la interfaz
                        // Carabineros
                        txtCarabineros.text = contactos[0].first
                        btnCarabineros.setOnClickListener {
                            realizarLlamada(contactos[0].second)
                        }

                        // Contacto 1 (con order 0)
                        if (contactos[1].second.isNotEmpty()) {
                            txtContacto1.text = contactos[1].first
                            txtContacto1.visibility = View.VISIBLE
                            btnContacto1.visibility = View.VISIBLE
                            btnContacto1.setOnClickListener {
                                realizarLlamada(contactos[1].second)
                            }
                        }

                        // Contacto 2 (con order 1)
                        if (contactos[2].second.isNotEmpty()) {
                            txtContacto2.text = contactos[2].first
                            txtContacto2.visibility = View.VISIBLE
                            btnContacto2.visibility = View.VISIBLE
                            btnContacto2.setOnClickListener {
                                realizarLlamada(contactos[2].second)
                            }
                        }
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val params = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        detenerAlarma()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detenerAlarma()
    }

    override fun onStop() {
        super.onStop()
        detenerAlarma()
    }

    private fun detenerAlarma() {
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            Toast.makeText(context, "¡Emergencia desactivada!", Toast.LENGTH_SHORT).show()
        }
    }

    fun setMediaPlayer(mediaPlayer: MediaPlayer, audioManager: AudioManager, originalVolume: Int) {
        this.mediaPlayer = mediaPlayer
        this.audioManager = audioManager
        this.originalVolume = originalVolume
    }
}