package com.franco.CaminaConmigo.model_mvvm.contactoemegencia.view

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ActivityContactoemergenciaBinding
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.viewmodel.ContactoEmergenciaViewModel
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity

class ContactoEmegenciaActivity : AppCompatActivity() {

    lateinit var binding: ActivityContactoemergenciaBinding
    val viewModel: ContactoEmergenciaViewModel = ContactoEmergenciaViewModel()
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
                contactos.toMutableList(),
                viewModel,
                ::editarContacto
            )
            binding.recyclerViewContactos.adapter = adapter

            // Configurar ItemTouchHelper
            val callback = ItemTouchHelperCallback(adapter) { fromPosition, toPosition ->
                adapter.swapItems(fromPosition, toPosition)
                viewModel.moverContacto(fromPosition, toPosition)
            }
            val itemTouchHelper = ItemTouchHelper(callback)
            itemTouchHelper.attachToRecyclerView(binding.recyclerViewContactos)
        }

        // Configurar el botón de agregar contacto
        binding.textViewAgregarContacto.setOnClickListener { agregarContacto() }

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
        val contacto = viewModel.contactos.value?.get(index)
        contacto?.let {
            val dialog = EditContactDialogFragment(it, index)
            dialog.show(supportFragmentManager, "EditContactDialogFragment")
        }
    }

    private fun agregarContacto() {
        val dialog = AddContactDialogFragment()
        dialog.show(supportFragmentManager, "AddContactDialogFragment")
    }
}