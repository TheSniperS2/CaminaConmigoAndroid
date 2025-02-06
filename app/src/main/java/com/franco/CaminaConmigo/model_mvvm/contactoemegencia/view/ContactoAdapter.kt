package com.franco.CaminaConmigo.model_mvvm.contactoemegencia.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.model.ContactoEmergencia

class ContactoAdapter(
    private val contactos: List<ContactoEmergencia>,
    private val onPriorizar: (Int) -> Unit,
    private val onEditar: (Int) -> Unit
) : RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contacto, parent, false)
        return ContactoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactoViewHolder, position: Int) {
        val contacto = contactos[position]
        holder.textNombre.text = contacto.name
        holder.textNumero.text = contacto.phone

        holder.btnPriorizar.setOnClickListener {
            onPriorizar(position)
        }

        holder.btnEditar.setOnClickListener {
            onEditar(position)
        }
    }

    override fun getItemCount() = contactos.size

    class ContactoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNombre: TextView = view.findViewById(R.id.textViewNombre)
        val textNumero: TextView = view.findViewById(R.id.textViewNumero)
        val btnEditar: ImageView = view.findViewById(R.id.imageViewEditar)
        val btnPriorizar: ImageView = view.findViewById(R.id.imageViewPriorizar)
    }
}
