package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
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
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SelectorUbicacionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLatLng: LatLng? = null
    private val db = FirebaseFirestore.getInstance() // Instancia de Firestore
    private lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selector_ubicacion)

        // Inicializar Places
        Places.initialize(applicationContext, getString(R.string.google_map_api_key))
        placesClient = Places.createClient(this)

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
                obtenerNombreUbicacion(it) { locationName ->
                    val intent = Intent().apply {
                        putExtra("latitude", it.latitude)
                        putExtra("longitude", it.longitude)
                        putExtra("locationName", locationName)
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            } ?: run {
                Toast.makeText(this, "Por favor selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val defaultLocation = LatLng(-29.9027, -71.2519) // La Serena, Chile
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        // Verificar si el sistema está en modo oscuro
        val isNightMode = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (isNightMode) {
            try {
                val success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_night)
                )
                if (!success) {
                    Log.e("SelectorUbicacionActivity", "Error al aplicar el estilo del mapa.")
                }
            } catch (e: Resources.NotFoundException) {
                Log.e("SelectorUbicacionActivity", "No se encontró el estilo del mapa. Error: ", e)
            }
        }


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
            "Reunión de hombres" -> R.drawable.i_reunion_de_hombre
            "Poca Iluminación" -> R.drawable.i_poca_iluminacion
            "Presencia de Bares y Restobares" -> R.drawable.i_presencia_de_bares_y_restobares
            "Veredas en mal estado" -> R.drawable.i_veredas_en_mal_estado
            "Vegetación Abundante" -> R.drawable.i_vegetacion_abundante
            "Espacios Abandonados" -> R.drawable.i_espacios_abandonados
            "Agresión Física" -> R.drawable.i_agresion_fisica
            "Agresión Sexual" -> R.drawable.i_agresion_sexual
            "Agresión Verbal" -> R.drawable.i_agresion_verbal
            "Falta de Baños Públicos" -> R.drawable.i_falta_de_banos_publicos
            "Mobiliario Inadecuado" -> R.drawable.i_mobiliario_inadecuado
            "Puntos Ciegos" -> R.drawable.i_puntos_ciegos
            "Personas en situación de calle" -> R.drawable.i_personas_en_situacion_de_calle
            else -> return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        }

        return BitmapDescriptorFactory.fromBitmap(redimensionarIcono(resId, 60, 60))
    }

    private fun redimensionarIcono(resId: Int, width: Int, height: Int): Bitmap {
        val imageBitmap = BitmapFactory.decodeResource(resources, resId)
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false)
    }

    // Modifica el método obtenerNombreUbicacion
    private fun obtenerNombreUbicacion(latLng: LatLng, callback: (String) -> Unit) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val streetAddress = address.thoroughfare ?: "Ubicación seleccionada"
                val streetNumber = address.subThoroughfare ?: ""
                val locationName = if (streetNumber.isNotEmpty()) "$streetAddress $streetNumber" else streetAddress
                callback(locationName)
            } else {
                callback("Ubicación seleccionada")
            }
        } catch (e: Exception) {
            Log.e("SelectorUbicacionActivity", "Error al obtener el nombre de la ubicación", e)
            callback("Ubicación seleccionada")
        }
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