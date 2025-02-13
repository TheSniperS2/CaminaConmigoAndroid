package com.franco.CaminaConmigo.model_mvvm.contactoemegencia.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ActivityContactoemergenciaBinding
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.viewmodel.ContactoEmergenciaViewModel
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity

class ContactoEmegenciaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactoemergenciaBinding
    private val viewModel: ContactoEmergenciaViewModel = ContactoEmergenciaViewModel()
    private lateinit var adapter: ContactoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactoemergenciaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración del RecyclerView
        binding.recyclerViewContactos.layoutManager = LinearLayoutManager(this)

        // Observando los contactos en el ViewModel
        viewModel.contactos.observe(this) { contactos ->
            adapter = ContactoAdapter(
                contactos,
                ::editarContacto,
                ::moverArriba,
                ::moverAbajo
            )
            binding.recyclerViewContactos.adapter = adapter
        }

        // Configurar el botón de agregar contacto
        binding.imageViewAgregar.setOnClickListener { agregarContacto() }

        // Configurar el botón de retroceder
        val btnRetroceder: ImageView = findViewById(R.id.btnRetroceder)
        btnRetroceder.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java) // Crear el Intent para redirigir
            startActivity(intent) // Iniciar la actividad
            finish() // Finalizar la actividad actual (opcional)
        }
    }

    private fun eliminarContacto(index: Int) {
        viewModel.eliminarContacto(index)
        adapter.notifyItemRemoved(index)
    }

    private fun editarContacto(index: Int) {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_editar_numero, null)
        val editTextNombre = view.findViewById<EditText>(R.id.editTextNuevoNombre)
        val editTextNumero = view.findViewById<EditText>(R.id.editTextNuevoNumero)

        // Rellenar los campos con los datos actuales
        editTextNombre.setText(viewModel.contactos.value?.get(index)?.name)
        editTextNumero.setText(viewModel.contactos.value?.get(index)?.phone)

        builder.setView(view)
        builder.setPositiveButton("Guardar") { _, _ ->
            val nuevoNombre = editTextNombre.text.toString()
            val nuevoNumero = editTextNumero.text.toString()
            if (nuevoNombre.isNotEmpty() && nuevoNumero.isNotEmpty()) {
                viewModel.editarContacto(index, nuevoNombre, nuevoNumero)
            }
        }

        builder.setNegativeButton("Cancelar", null)

        builder.setNeutralButton("Eliminar") { _, _ ->
            eliminarContacto(index)
        }

        builder.show()
    }

    private fun moverArriba(index: Int) {
        if (index > 0) {
            viewModel.moverContacto(index, index - 1)
            adapter.notifyItemMoved(index, index - 1)
        }
    }

    private fun moverAbajo(index: Int) {
        if (index < viewModel.contactos.value?.size?.minus(1) ?: 0) {
            viewModel.moverContacto(index, index + 1)
            adapter.notifyItemMoved(index, index + 1)
        }
    }

    private fun agregarContacto() {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_contacto, null)
        val editTextNombre = view.findViewById<EditText>(R.id.editTextNombre)
        val editTextNumero = view.findViewById<EditText>(R.id.editTextNumero)

        builder.setView(view)
        builder.setPositiveButton("Agregar") { _, _ ->
            if (editTextNombre.text.isNotEmpty() && editTextNumero.text.isNotEmpty()) {
                viewModel.agregarContacto(editTextNombre.text.toString(), editTextNumero.text.toString())
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
}
