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
    private val onEditar: (Int) -> Unit,
    private val onMoverArriba: (Int) -> Unit,
    private val onMoverAbajo: (Int) -> Unit
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
        holder.textPrioridad.text = (contacto.order + 1).toString() // Mostrar el número de prioridad, sumando 1

        // Asignar evento de editar solo
        holder.btnEditar.setOnClickListener { onEditar(position) }

        holder.btnMoverArriba.setOnClickListener { onMoverArriba(position) }
        holder.btnMoverAbajo.setOnClickListener { onMoverAbajo(position) }
    }

    override fun getItemCount() = contactos.size

    class ContactoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNombre: TextView = view.findViewById(R.id.textViewNombre)
        val textNumero: TextView = view.findViewById(R.id.textViewNumero)
        val textPrioridad: TextView = view.findViewById(R.id.textViewPrioridad)
        val btnEditar: ImageView = view.findViewById(R.id.imageViewEditar)
        val btnMoverArriba: ImageView = view.findViewById(R.id.imageViewMoverArriba)
        val btnMoverAbajo: ImageView = view.findViewById(R.id.imageViewMoverAbajo)
    }
}
