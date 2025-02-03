package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TipoReporteDialogFragment : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView

    interface OnTipoReporteSeleccionadoListener {
        fun onTipoReporteSeleccionado(tipo: String)
    }

    private var listener: OnTipoReporteSeleccionadoListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnTipoReporteSeleccionadoListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tipo_reporte, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewTipos)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val tipos = listOf(
            TipoReporte("Reunión de Hombres", R.drawable.icon_reunion),
            TipoReporte("Puntos ciegos", R.drawable.icon_puntosciegos),
            TipoReporte("Presencia de bares", R.drawable.icono_bares),
            TipoReporte("Veredas en mal estado", R.drawable.icono_veredas),
            TipoReporte("Poca iluminación", R.drawable.icono_iluminacion),
            TipoReporte("Vegetación abundante", R.drawable.icono_vegetacion),
            TipoReporte("Lugar abandonado", R.drawable.icon_abandonados),
            TipoReporte("Agresión fisica", R.drawable.icon_agre_fisica),
            TipoReporte("Agresión sexual", R.drawable.icon_agre_sexual),
            TipoReporte("Agresión verbal", R.drawable.icon_agre_verbal),
            TipoReporte("Falta de baños", R.drawable.icon_faltabanos),
            TipoReporte("Mobiliario inadecuado", R.drawable.icon_mobiliario),
            TipoReporte("Persona en situación de calle", R.drawable.icon_situacioncalle)
        )

        val adapter = TipoReporteAdapter(tipos) { tipo ->
            // Al seleccionar el tipo de reporte, abrir la nueva actividad para agregar el reporte
            val intent = Intent(context, AgregarReporteActivity::class.java)
            intent.putExtra("TIPO_REPORTE", tipo.nombre)  // Pasamos el tipo de reporte seleccionado
            startActivity(intent)
            dismiss()  // Cierra el diálogo
        }
        recyclerView.adapter = adapter

        return view
    }

    data class TipoReporte(val nombre: String, val imagen: Int)

    class TipoReporteAdapter(
        private val tipos: List<TipoReporte>,
        private val onClick: (TipoReporte) -> Unit
    ) : RecyclerView.Adapter<TipoReporteAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tipo_reporte, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tipo = tipos[position]
            holder.bind(tipo)
        }

        override fun getItemCount() = tipos.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tipoTextView: TextView = view.findViewById(R.id.txtTipo)
            private val tipoImageView: ImageView = view.findViewById(R.id.imgTipo)

            fun bind(tipo: TipoReporte) {
                tipoTextView.text = tipo.nombre
                tipoImageView.setImageResource(tipo.imagen)

                itemView.setOnClickListener {
                    onClick(tipo)  // Ejecuta la acción cuando se selecciona un tipo
                }
            }
        }
    }
}
