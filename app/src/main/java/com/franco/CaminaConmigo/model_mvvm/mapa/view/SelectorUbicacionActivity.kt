package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class SelectorUbicacionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLatLng: LatLng? = null
    private val db = FirebaseFirestore.getInstance() // Instancia de Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selector_ubicacion)

        try {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al cargar el mapa", Toast.LENGTH_LONG).show()
            finish()
        }

        findViewById<Button>(R.id.btnConfirmarUbicacion).setOnClickListener {
            selectedLatLng?.let {
                val intent = Intent().apply {
                    putExtra("latitude", it.latitude)
                    putExtra("longitude", it.longitude)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } ?: run {
                Toast.makeText(this, "Por favor selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val defaultLocation = LatLng(-29.9027, -71.2519) // La Serena, Chile
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        // Cargar reportes desde Firebase Firestore
        cargarReportesDesdeFirestore()

        // Permitir selección de ubicación en el mapa
        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            cargarReportesDesdeFirestore() // Recargar los reportes después de limpiar
            mMap.addMarker(MarkerOptions().position(latLng).title("Ubicación seleccionada"))
            selectedLatLng = latLng
        }
    }

    private fun cargarReportesDesdeFirestore() {
        db.collection("reportes").get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Toast.makeText(this, "No hay reportes disponibles", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            for (document in documents) {
                val lat = document.getDouble("latitude")
                val lng = document.getDouble("longitude")
                val description = document.getString("description") ?: "Sin descripción"
                val type = document.getString("type") ?: "Tipo desconocido"
                // No necesitamos los likes en el snippet
                Log.d("Reportes", "Lat: $lat, Lng: $lng, Descripción: $description, Tipo: $type")

                if (lat != null && lng != null) {
                    val reportLocation = LatLng(lat, lng)
                    val icon = obtenerIconoPorTipo(type)

                    mMap.addMarker(
                        MarkerOptions()
                            .position(reportLocation)
                            .title(type)  // Solo el título, sin los likes ni descripción en el snippet
                            .icon(icon)  // Ícono personalizado
                    )
                } else {
                    Log.e("Firestore", "Documento inválido en Firestore: ${document.id}")
                }
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Error al cargar los reportes", e)
            Toast.makeText(this, "Error al cargar los reportes: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun obtenerIconoPorTipo(type: String): BitmapDescriptor {
        val resId = when (type) {
            "Reunión de Hombres" -> R.drawable.i_reunion_de_hombre
            "Poca Iluminación" -> R.drawable.i_poca_iluminacion
            "Presencia de bares" -> R.drawable.i_presencia_de_bares_y_restobares
            "Veredas en mal estado" -> R.drawable.i_veredas_en_mal_estado
            "Vegetación abundante" -> R.drawable.i_vegetacion_abundante
            "Espacios Abandonados" -> R.drawable.i_espacios_abandonados
            "Agresión física" -> R.drawable.i_agresion_fisica
            "Agresión Sexual" -> R.drawable.i_agresion_sexual
            "Agresión verbal" -> R.drawable.i_agresion_verbal
            "Falta de Baños Públicos" -> R.drawable.icon_faltabanos
            "Mobiliario inadecuado" -> R.drawable.i_mobiliario_inadecuado
            "Puntos ciegos" -> R.drawable.i_puntos_ciegos
            "Personas en situación de calle" -> R.drawable.i_personas_en_situacion_de_calle
            else -> return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        }

        return BitmapDescriptorFactory.fromBitmap(redimensionarIcono(resId, 100, 100))
    }

    private fun redimensionarIcono(resId: Int, width: Int, height: Int): Bitmap {
        val imageBitmap = BitmapFactory.decodeResource(resources, resId)
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false)
    }

    // Método para agregar un nuevo reporte a la base de datos
    fun agregarReporte(description: String, type: String, latitude: Double, longitude: Double) {
        val reporte = hashMapOf(
            "description" to description,
            "type" to type,
            "latitude" to latitude,
            "longitude" to longitude,
            "likes" to 0,  // Los likes inician en 0
            "timestamp" to System.currentTimeMillis()  // Timestamp del reporte
        )

        db.collection("reportes")  // Guardar en la colección 'reportes'
            .add(reporte)
            .addOnSuccessListener {
                Toast.makeText(this, "com.franco.CaminaConmigo.model_mvvm.novedad.model.Reporte agregado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al agregar reporte", e)
                Toast.makeText(this, "Error al agregar reporte: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
