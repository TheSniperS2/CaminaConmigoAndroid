package com.franco.CaminaConmigo.model_mvvm.novedad.adapter

import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.novedad.model.Reporte
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        private val tiempo: TextView = itemView.findViewById(R.id.txtTiempoReporte)
        private val ubicacion: TextView = itemView.findViewById(R.id.txtUbicacionReporte)
        private val imgReporte: ImageView = itemView.findViewById(R.id.imgReporte)

        fun bind(reporte: Reporte) {
            // Verificar si el reporte tiene imágenes
            if (!reporte.imageUrls.isNullOrEmpty()) {
                // Mostrar la imagen
                imgReporte.visibility = View.VISIBLE
                mapView.visibility = View.GONE
                Glide.with(itemView.context).load(reporte.imageUrls[0]).into(imgReporte)
            } else {
                // Mostrar el mapa
                imgReporte.visibility = View.GONE
                mapView.visibility = View.VISIBLE
                mapView.onCreate(null)
                mapView.getMapAsync { googleMap ->
                    val latLng = LatLng(reporte.latitude, reporte.longitude)
                    googleMap.addMarker(MarkerOptions().position(latLng))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                }
            }

            // El título del reporte es el tipo almacenado en la base de datos
            titulo.text = reporte.type

            // Mostrar la descripción del reporte
            descripcion.text = reporte.description

            // Mostrar cantidad de likes
            likes.text = "${reporte.likes} Me gusta"

            // Mostrar cantidad de comentarios
            comentarios.text = "${reporte.comentarios} Comentarios"

            // Mostrar el tiempo transcurrido desde que se publicó el reporte
            tiempo.text = getTimeAgo(reporte.timestamp)

            // Obtener y mostrar la ubicación
            val geocoder = Geocoder(itemView.context, Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(reporte.latitude, reporte.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    ubicacion.text = "${address.locality}, ${address.adminArea}"
                } else {
                    ubicacion.text = "Ubicación desconocida"
                }
            } catch (e: Exception) {
                ubicacion.text = "Error al obtener ubicación"
            }

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

        private fun getTimeAgo(timestamp: Timestamp): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp.toDate().time

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                seconds < 60 -> "hace un momento"
                minutes < 60 -> "hace $minutes ${if (minutes == 1L) "minuto" else "minutos"}"
                hours < 24 -> "hace $hours ${if (hours == 1L) "hora" else "horas"}"
                days < 7 -> "hace $days ${if (days == 1L) "día" else "días"}"
                days < 30 -> "hace ${days / 7} ${if (days / 7 == 1L) "semana" else "semanas"}"
                days < 365 -> "hace ${days / 30} ${if (days / 30 == 1L) "mes" else "meses"}"
                else -> "hace ${days / 365} ${if (days / 365 == 1L) "año" else "años"}"
            }
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
            "Reunión de hombres" -> R.drawable.icon_reunion
            "Puntos Ciegos" -> R.drawable.icon_puntosciegos
            "Presencia de Bares y Restobares" -> R.drawable.icono_bares
            "Veredas en mal estado" -> R.drawable.icono_veredas
            "Poca Iluminación" -> R.drawable.icono_iluminacion
            "Vegetación Abundante" -> R.drawable.icono_vegetacion
            "Espacios Abandonados" -> R.drawable.icon_abandonados
            "Agresión Fisica" -> R.drawable.icon_agre_fisica
            "Agresión Sexual" -> R.drawable.icon_agre_sexual
            "Agresión Verbal" -> R.drawable.icon_agre_verbal
            "Falta de Baños Públicos" -> R.drawable.icon_faltabanos
            "Mobiliario Inadecuado" -> R.drawable.icon_mobiliario
            "Personas en situación de calle" -> R.drawable.icon_situacioncalle
            else -> R.drawable.ic_anadir
        }
    }
}