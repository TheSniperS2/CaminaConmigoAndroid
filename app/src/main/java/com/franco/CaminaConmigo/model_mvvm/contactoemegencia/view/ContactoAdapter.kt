package com.franco.CaminaConmigo.model_mvvm.contactoemegencia.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.model.ContactoEmergencia
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.viewmodel.ContactoEmergenciaViewModel

class ContactoAdapter(
    private val contactos: MutableList<ContactoEmergencia>,
    private val viewModel: ContactoEmergenciaViewModel,
    private val onEditar: (Int) -> Unit
) : RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactoViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_contacto, parent, false)
        return ContactoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactoViewHolder, position: Int) {
        val contacto = contactos[position]
        holder.textNombre.text = contacto.name
        holder.textNumero.text = contacto.phone
        holder.textPrioridad.text = (position + 1).toString() // Mostrar el número de prioridad basado en la posición

        // Asignar evento de editar
        holder.btnEditar.setOnClickListener { onEditar(position) }

        // No cambiar la apariencia del primer contacto de emergencia
        holder.itemView.setBackgroundResource(android.R.color.transparent)
        holder.textNombre.textSize = 16f
        holder.textNumero.textSize = 14f
    }

    override fun getItemCount() = contactos.size

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val fromItem = contactos.removeAt(fromPosition)
        contactos.add(toPosition, fromItem)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun updateOrder() {
        for ((index, contacto) in contactos.withIndex()) {
            contacto.order = index
        }
        notifyDataSetChanged()
    }

    fun updateOrderInDatabase() {
        updateOrder()
        viewModel.updateContactsOrder(contactos)
    }

    class ContactoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNombre: TextView = view.findViewById(R.id.textViewNombre)
        val textNumero: TextView = view.findViewById(R.id.textViewNumero)
        val textPrioridad: TextView = view.findViewById(R.id.textViewPrioridad)
        val btnEditar: ImageView = view.findViewById(R.id.imageViewEditar)
        val btnMover: ImageView = view.findViewById(R.id.imageViewMover)
    }
}