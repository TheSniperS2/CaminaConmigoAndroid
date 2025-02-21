package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.chat.view.ChatActivity
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity
import com.franco.CaminaConmigo.model_mvvm.novedad.view.NovedadActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MapaActivity : AppCompatActivity(), OnMapReadyCallback, TipoReporteDialogFragment.OnTipoReporteSeleccionadoListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = "reports"
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializa Places
        Places.initialize(applicationContext, getString(R.string.google_map_api_key))

        // Configura el fragmento de autocompletado
        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // Mueve la cámara a la ubicación seleccionada
                place.latLng?.let {
                    mMap.addMarker(MarkerOptions().position(it).title(place.name))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                // Maneja el error
                Toast.makeText(this@MapaActivity, "Error al seleccionar el lugar: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })

        findViewById<Button>(R.id.btnReportar).setOnClickListener {
            val dialogFragment = TipoReporteDialogFragment()
            dialogFragment.show(supportFragmentManager, "TipoReporteDialogFragment")
        }

        // Inicializar el AudioManager para controlar el volumen
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Guardar el volumen original
        originalVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

// Configurar el botón para activar la alarma de emergencia
        findViewById<Button>(R.id.btnSOS).setOnClickListener {
            try {
                // Liberar el MediaPlayer si ya existe
                mediaPlayer?.release()
                mediaPlayer = null

                // Crear una nueva instancia del MediaPlayer
                mediaPlayer = MediaPlayer.create(this, R.raw.emergency_alarm).apply {
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                }

                // Verificar si mediaPlayer no es nulo antes de llamar a isPlaying
                mediaPlayer?.let { player ->
                    if (!player.isPlaying) {
                        audioManager?.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0,
                            0
                        )
                        player.start()
                        Toast.makeText(this, "¡Emergencia activada!", Toast.LENGTH_SHORT).show()

                        // Mostrar el diálogo de emergencia
                        val emergenciaDialog = EmergenciaDialogFragment().apply {
                            setMediaPlayer(player, audioManager!!, originalVolume)
                        }
                        emergenciaDialog.show(supportFragmentManager, "EmergenciaDialogFragment")
                    }
                }
            } catch (e: IllegalStateException) {
                // Manejar la excepción y mostrar un mensaje de error
                Toast.makeText(this, "Error al activar la emergencia: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Botones de navegación
        findViewById<ImageButton>(R.id.imageButton11).setOnClickListener {
            startActivity(Intent(this, NovedadActivity::class.java))
        }
        findViewById<ImageButton>(R.id.imageButton12).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        findViewById<ImageButton>(R.id.imageButton13).setOnClickListener {
            startActivity(Intent(this, AyudaActivity::class.java))
        }
        findViewById<ImageButton>(R.id.imageButton14).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        cargarReportes()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        mMap.isMyLocationEnabled = true

        // Coordenadas de La Serena, Chile
        val laSerena = LatLng(-29.9027, -71.2519)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(laSerena, 12f))

        mMap.setOnMarkerClickListener { marker ->
            val reporteId = marker.tag as? String
            if (reporteId != null) {
                // Recuperar los demás datos desde Firestore
                db.collection("reportes").document(reporteId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            // Extraer los datos del reporte
                            val type = document.getString("type") ?: "Tipo desconocido"
                            val description = document.getString("description") ?: "Descripción desconocida"
                            val timestamp = document.getDate("timestamp") // Obtener la fecha de Firestore
                            val likes = document.getLong("likes")?.toInt() ?: 0  // Convertir likes a Int

                            // Crear la instancia del fragmento con los datos
                            val dialogFragment = DetallesReporteDialogFragment.newInstance(
                                reporteId, type, description, timestamp, likes
                            )
                            dialogFragment.show(supportFragmentManager, "DetallesReporteDialogFragment")
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al obtener los detalles del reporte", Toast.LENGTH_SHORT).show()
                    }
            }
            true
        }

        /*
        // CÓDIGO COMENTADO PARA GPS EN TIEMPO REAL (Activar cuando sea necesario)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val miUbicacion = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 15f))
            }
        }
        */
    }

    private fun cargarReportes() {
        db.collection("reportes").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val lat = document.getDouble("latitude") ?: continue
                val lng = document.getDouble("longitude") ?: continue
                val description = document.getString("description") ?: "Sin descripción"
                val type = document.getString("type") ?: "Sin tipo"

                val ubicacion = LatLng(lat, lng)
                val icon = obtenerIconoPorTipo(type)

                val marker = mMap.addMarker(
                    MarkerOptions()
                        .position(ubicacion)
                        .title(type)
                        .snippet(description)
                        .icon(icon)
                )
                marker?.tag = document.id
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar reportes", Toast.LENGTH_SHORT).show()
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
            "Falta de Baños Públicos" -> R.drawable.icon_faltabanos
            "Mobiliario Inadecuado" -> R.drawable.i_mobiliario_inadecuado
            "Puntos Ciegos" -> R.drawable.i_puntos_ciegos
            "Personas en situación de calle" -> R.drawable.i_personas_en_situacion_de_calle
            else -> return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        }

        return BitmapDescriptorFactory.fromBitmap(redimensionarIcono(resId, 100, 100))
    }


    private fun redimensionarIcono(resId: Int, width: Int, height: Int): Bitmap {
        val imageBitmap = BitmapFactory.decodeResource(resources, resId)
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false)
    }

    private fun reportarUbicacion(tipo: String, descripcion: String, imageUrl: String?) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latitud = it.latitude
                val longitud = it.longitude

                val reporte = hashMapOf(
                    "description" to descripcion,
                    "type" to tipo,
                    "latitude" to latitud,
                    "longitude" to longitud,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "userId" to (FirebaseAuth.getInstance().currentUser?.uid ?: "Anónimo"),
                    "isAnonymous" to (FirebaseAuth.getInstance().currentUser == null),
                    "imageUrl" to imageUrl,
                    "likes" to 0 // Se agrega el campo de likes con valor inicial 0
                )

                db.collection(reportsCollection)
                    .add(reporte)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reporte enviado", Toast.LENGTH_SHORT).show()
                        cargarReportes() // Recargar los marcadores con la nueva imagen
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al enviar reporte", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Liberar el recurso del MediaPlayer cuando la actividad se destruye
        mediaPlayer?.release()
        mediaPlayer = null

        // Restaurar el volumen original al cerrar la actividad
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
    }

    override fun onTipoReporteSeleccionado(tipoReporte: String) {
        val descripcionEditText = findViewById<EditText>(R.id.edtDescripcion)
        descripcionEditText.setText(tipoReporte) // Establece el tipo de reporte como descripción
    }
}