package com.franco.CaminaConmigo.model_mvvm.contactoemegencia.view

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ActivityContactoemergenciaBinding
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.viewmodel.ContactoEmergenciaViewModel

class ContactoEmegenciaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactoemergenciaBinding
    private val viewModel: ContactoEmergenciaViewModel = ContactoEmergenciaViewModel()
    private lateinit var adapter: ContactoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactoemergenciaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerViewContactos.layoutManager = LinearLayoutManager(this)

        viewModel.contactos.observe(this) { contactos ->
            adapter = ContactoAdapter(contactos, ::priorizarContacto, ::editarNumero)
            binding.recyclerViewContactos.adapter = adapter
        }

        configurarItemTouchHelper()

        binding.imageViewAgregar.setOnClickListener { agregarContacto() }
    }

    private fun configurarItemTouchHelper() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                viewModel.moverContacto(fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewContactos)
    }

    private fun priorizarContacto(index: Int) {
        viewModel.priorizarContacto(index)
    }

    private fun editarNumero(index: Int) {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_editar_numero, null)
        val editText = view.findViewById<EditText>(R.id.editTextNuevoNumero)

        builder.setView(view)
        builder.setPositiveButton("Guardar") { _, _ ->
            val nuevoNumero = editText.text.toString()
            if (nuevoNumero.isNotEmpty()) {
                viewModel.editarNumero(index, nuevoNumero)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
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
