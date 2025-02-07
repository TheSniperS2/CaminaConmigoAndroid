package com.franco.CaminaConmigo.model_mvvm.novedad.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.novedad.model.Reporte
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ReporteAdapter(
    private val reportes: List<Reporte>,
    private val onClick: (Reporte) -> Unit
) : RecyclerView.Adapter<ReporteAdapter.ReporteViewHolder>() {

    inner class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icono: ImageView = itemView.findViewById(R.id.imgIconoReporte)
        private val titulo: TextView = itemView.findViewById(R.id.txtTituloReporte)
        private val descripcion: TextView = itemView.findViewById(R.id.txtDescripcionReporte)
        private val mapView: MapView = itemView.findViewById(R.id.mapView)
        private val likes: TextView = itemView.findViewById(R.id.txtLikes)
        private val comentarios: TextView = itemView.findViewById(R.id.txtComments)

        fun bind(reporte: Reporte) {
            // Inicialización del MapView
            mapView.onCreate(null)
            mapView.getMapAsync { googleMap ->
                val latLng = LatLng(reporte.latitude, reporte.longitude)
                googleMap.addMarker(MarkerOptions().position(latLng))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }

            // El título del reporte es el tipo almacenado en la base de datos
            titulo.text = reporte.type

            // Mostrar la descripción del reporte
            descripcion.text = reporte.description

            // Mostrar cantidad de likes
            likes.text = reporte.likes.toString()

            // Se necesita una consulta para obtener el número de comentarios
            comentarios.text = "0" // Reemplazar por la cantidad real de comentarios

            // Cargar el icono según el tipo de reporte
            val iconoRes = obtenerIconoPorTipo(reporte.type)
            icono.setImageResource(iconoRes)

            itemView.setOnClickListener { onClick(reporte) }
        }

        fun onDestroy() {
            mapView.onDestroy()
        }

        fun onResume() {
            mapView.onResume()
        }

        fun onPause() {
            mapView.onPause()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reporte, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        holder.bind(reportes[position])
    }

    override fun getItemCount(): Int = reportes.size

    // Devuelve el icono correspondiente según el tipo de reporte
    private fun obtenerIconoPorTipo(type: String): Int {
        return when (type) {
            "Reunión de Hombres" -> R.drawable.icon_reunion
            "Puntos ciegos" -> R.drawable.icon_puntosciegos
            "Presencia de bares" -> R.drawable.icono_bares
            "Veredas en mal estado" -> R.drawable.icono_veredas
            "Poca Iluminación" -> R.drawable.icono_iluminacion
            "Vegetación abundante" -> R.drawable.icono_vegetacion
            "Espacios Abandonados" -> R.drawable.icon_abandonados
            "Agresión fisica" -> R.drawable.icon_agre_fisica
            "Agresión Sexual" -> R.drawable.icon_agre_sexual
            "Agresión verbal" -> R.drawable.icon_agre_verbal
            "Falta de baños" -> R.drawable.icon_faltabanos
            "Mobiliario inadecuado" -> R.drawable.icon_mobiliario
            "Personas en situación de calle" -> R.drawable.icon_situacioncalle
            else -> R.drawable.ic_anadir
        }
    }
}
