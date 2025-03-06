package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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
            TipoReporte("Reunión de hombres", R.drawable.icon_reunion),
            TipoReporte("Puntos Ciegos", R.drawable.icon_puntosciegos),
            TipoReporte("Presencia de Bares y Restobares", R.drawable.icono_bares),
            TipoReporte("Veredas en mal estado", R.drawable.icono_veredas),
            TipoReporte("Poca Iluminación", R.drawable.icono_iluminacion),
            TipoReporte("Vegetación Abundante", R.drawable.icono_vegetacion),
            TipoReporte("Espacios Abandonados", R.drawable.icon_abandonados),
            TipoReporte("Agresión Física", R.drawable.icon_agre_fisica),
            TipoReporte("Agresión Sexual", R.drawable.icon_agre_sexual),
            TipoReporte("Agresión Verbal", R.drawable.icon_agre_verbal),
            TipoReporte("Falta de Baños Públicos", R.drawable.icon_faltabanos),
            TipoReporte("Mobiliario Inadecuado", R.drawable.icon_mobiliario),
            TipoReporte("Personas en situación de calle", R.drawable.icon_situacioncalle)
        )

        val adapter = TipoReporteAdapter(tipos) { tipo ->
            val dialog = AgregarReporteDialogFragment.newInstance(tipo.nombre)
            dialog.show(parentFragmentManager, "AgregarReporteDialog")
            dismiss()
        }
        recyclerView.adapter = adapter

        // Agregamos espacio entre los elementos
        recyclerView.addItemDecoration(SpaceItemDecoration(24))

        // Configurar el botón Cerrar
        view.findViewById<TextView>(R.id.btnCerrar).setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
                behavior.isFitToContents = true
                behavior.skipCollapsed = true
            }
        }
        return dialog
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
                    onClick(tipo)
                }
            }
        }
    }

    // Clase para agregar espacio entre elementos en el RecyclerView
    class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State
        ) {
            outRect.left = space / 2
            outRect.right = space / 2
            outRect.bottom = space

            if (parent.getChildAdapterPosition(view) < 2) {
                outRect.top = space
            }
        }
    }
}