package com.franco.CaminaConmigo.model_mvvm.contactoemegencia.view

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.databinding.ActivityContactoemergenciaBinding
import com.franco.CaminaConmigo.R

class ContactoEmegenciaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactoemergenciaBinding
    private val contactos = mutableListOf<Pair<String, String>>() // Lista de contactos (nombre, número)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactoemergenciaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Agregar contactos de prueba
        contactos.add("Mamá" to "+569 12345678")
        contactos.add("Papá" to "+569 12345679")

        // Mostrar los contactos iniciales
        actualizarListaContactos()

        // Funcionalidad del botón para agregar un nuevo contacto
        binding.imageViewAgregar.setOnClickListener {
            agregarContacto()
        }
    }

    // Método para actualizar la lista de contactos en la UI
    private fun actualizarListaContactos() {
        binding.contactosLayout.removeAllViews()

        for ((index, contacto) in contactos.withIndex()) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_contacto, binding.contactosLayout, false)

            val textNombre = view.findViewById<TextView>(R.id.textViewNombre)
            val textNumero = view.findViewById<TextView>(R.id.textViewNumero)
            val btnEditar = view.findViewById<ImageView>(R.id.imageViewEditar)
            val btnPriorizar = view.findViewById<ImageView>(R.id.imageViewPriorizar)

            textNombre.text = contacto.first
            textNumero.text = contacto.second

            // Editar número
            btnEditar.setOnClickListener {
                editarNumero(index)
            }

            // Priorizar (Mover hacia arriba)
            btnPriorizar.setOnClickListener {
                if (index > 0) {
                    val temp = contactos[index]
                    contactos[index] = contactos[index - 1]
                    contactos[index - 1] = temp
                    actualizarListaContactos() // Actualizamos la lista después de mover
                }
            }

            binding.contactosLayout.addView(view)
        }
    }

    // Método para agregar un nuevo contacto
    private fun agregarContacto() {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_contacto, null)

        val editTextNombre = view.findViewById<EditText>(R.id.editTextNombre)
        val editTextNumero = view.findViewById<EditText>(R.id.editTextNumero)

        builder.setView(view)
        builder.setPositiveButton("Agregar") { _, _ ->
            val nombre = editTextNombre.text.toString()
            val numero = editTextNumero.text.toString()

            if (nombre.isNotEmpty() && numero.isNotEmpty()) {
                contactos.add(nombre to numero)
                actualizarListaContactos() // Actualizamos la lista después de agregar
                Toast.makeText(this, "Contacto agregado correctamente.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Por favor, ingrese un nombre y un número.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    // Método para editar el número de un contacto
    private fun editarNumero(index: Int) {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_editar_numero, null)
        val editText = view.findViewById<EditText>(R.id.editTextNuevoNumero)

        builder.setView(view)
        builder.setPositiveButton("Guardar") { _, _ ->
            val nuevoNumero = editText.text.toString()
            if (nuevoNumero.isNotEmpty()) {
                contactos[index] = contactos[index].first to nuevoNumero
                actualizarListaContactos() // Actualizamos la lista después de editar
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    // Método para eliminar un contacto
    private fun eliminarContacto(index: Int) {
        AlertDialog.Builder(this)
            .setMessage("¿Estás seguro de eliminar este contacto?")
            .setPositiveButton("Sí") { _, _ ->
                contactos.removeAt(index)
                actualizarListaContactos() // Actualizamos la lista después de eliminar
                Toast.makeText(this, "Contacto eliminado.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
