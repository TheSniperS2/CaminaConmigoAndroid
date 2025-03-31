package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.chat.view.ChatActivity
import com.franco.CaminaConmigo.model_mvvm.inicio.view.MainActivity
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
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MapaActivity : AppCompatActivity(), OnMapReadyCallback, TipoReporteDialogFragment.OnTipoReporteSeleccionadoListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = "reports"
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = 0
    private val markersList = mutableListOf<Marker>()
    private var searchMarker: Marker? = null
    private var isAlarmActive: Boolean = false
    private lateinit var modoOscuroReceiver: BroadcastReceiver
    private lateinit var refreshMapReceiver: BroadcastReceiver
    private var reportListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        modoOscuroReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val activar = intent?.getBooleanExtra("activar", false) ?: return
                aplicarModoOscuroEnMapa(activar)
            }
        }
        val filter = IntentFilter("com.franco.CaminaConmigo.MODO_OSCURO")
        registerReceiver(modoOscuroReceiver, filter, RECEIVER_NOT_EXPORTED)

        // Dentro del método onCreate
        refreshMapReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                cargarReportes()
            }
        }
        val refreshFilter = IntentFilter("com.franco.CaminaConmigo.REFRESH_MAP")
        registerReceiver(refreshMapReceiver, refreshFilter, RECEIVER_NOT_EXPORTED)

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
                    // Eliminar el marcador de búsqueda anterior si existe
                    searchMarker?.remove()

                    if (!mostrarMarcadorExistente(it)) {
                        // Agregar un nuevo marcador de búsqueda
                        searchMarker = mMap.addMarker(MarkerOptions().position(it).title(place.name))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 12f))
                    }
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                // Maneja el error
                Toast.makeText(this@MapaActivity, "Error al seleccionar el lugar: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })

        findViewById<AppCompatButton>(R.id.btnAyuda).setOnClickListener {
            val instruccionesDialog = InstruccionesBottomSheetDialogFragment()
            instruccionesDialog.show(supportFragmentManager, "InstruccionesDialogFragment")
        }

        findViewById<Button>(R.id.btnReportar).setOnClickListener {
            if (isUserAuthenticated()) {
                val dialogFragment = TipoReporteDialogFragment()
                dialogFragment.show(supportFragmentManager, "TipoReporteDialogFragment")
            } else {
                showSignInDialog()
            }
        }

        // Inicializar el AudioManager para controlar el volumen
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Guardar el volumen original
        originalVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

        // Configurar el botón para activar la alarma de emergencia
        findViewById<Button>(R.id.btnSOS).setOnClickListener {
            if (isAlarmActive) {
                // Si la alarma ya está activa, no hacer nada
                return@setOnClickListener
            }

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
                        isAlarmActive = true
                        Toast.makeText(this, "¡Emergencia activada!", Toast.LENGTH_SHORT).show()

                        // Mostrar el diálogo de emergencia
                        val emergenciaDialog = EmergenciaDialogFragment().apply {
                            setMediaPlayer(player, audioManager!!, originalVolume)
                            setOnDismissListener(object : EmergenciaDialogFragment.OnDismissListener {
                                override fun onDismiss() {
                                    isAlarmActive = false
                                }
                            })
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
        findViewById<LinearLayout>(R.id.NovedadContainer).setOnClickListener {
            startActivity(Intent(this, NovedadActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.ChatContainer2).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.AyudaContainer).setOnClickListener {
            startActivity(Intent(this, AyudaActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.MenuContainer).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        cargarReportes()

        // Obtener las coordenadas del intent
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val locationName = intent.getStringExtra("location_name")
        val zoomLevel = intent.getFloatExtra("zoom_level", 15.0f) // Nivel de zoom predeterminado

        // Añadir marcador en la ubicación especificada y mover la cámara
        val location = LatLng(latitude, longitude)
        mMap.addMarker(MarkerOptions().position(location).title(locationName))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))

        // Aplicar el estilo del mapa basado en el modo oscuro
        val sharedPreferences = getSharedPreferences("configuraciones", Context.MODE_PRIVATE)
        val modoOscuroActivado = sharedPreferences.getBoolean("modo_oscuro", false)
        aplicarModoOscuroEnMapa(modoOscuroActivado)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        mMap.isMyLocationEnabled = true

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

        // Obtener ubicación en tiempo real
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val miUbicacion = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 14f))
            }
        }

        // Escuchar cambios en Firestore para actualizar el mapa en tiempo real
        reportListener = db.collection(reportsCollection)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("MapaActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // Usar un Handler para asegurarnos de que las actualizaciones se realicen en el hilo principal
                    val mainHandler = Handler(Looper.getMainLooper())
                    mainHandler.post {
                        for (dc in snapshots.documentChanges) {
                            when (dc.type) {
                                DocumentChange.Type.ADDED -> {
                                    val document = dc.document
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
                                    marker?.let { markersList.add(it) }
                                    marker?.tag = document.id
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    val document = dc.document
                                    val marker = markersList.find { it.tag == document.id }
                                    marker?.let {
                                        val lat = document.getDouble("latitude") ?: return@let
                                        val lng = document.getDouble("longitude") ?: return@let
                                        val description = document.getString("description") ?: "Sin descripción"
                                        val type = document.getString("type") ?: "Sin tipo"

                                        val ubicacion = LatLng(lat, lng)
                                        val icon = obtenerIconoPorTipo(type)

                                        it.position = ubicacion
                                        it.title = type
                                        it.snippet = description
                                        it.setIcon(icon)
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val document = dc.document
                                    val marker = markersList.find { it.tag == document.id }
                                    marker?.remove()
                                    markersList.remove(marker)
                                }
                            }
                        }
                    }
                }
            }
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
                marker?.let { markersList.add(it) }
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

    private fun mostrarMarcadorExistente(latLng: LatLng): Boolean {
        for (marker in markersList) {
            if (marker.position == latLng) {
                marker.showInfoWindow()
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                return true
            }
        }
        return false
    }

    private fun isUserAuthenticated(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun showSignInDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Iniciar Sesión Requerido")
        builder.setMessage("Para acceder a esta funcionalidad, por favor inicia sesión.")
        builder.setPositiveButton("Iniciar Sesión") { _, _ ->
            // Redirigir a la pantalla de inicio de sesión
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshMapReceiver)

        // Liberar el recurso del MediaPlayer cuando la actividad se destruye
        mediaPlayer?.release()
        mediaPlayer = null

        // Restaurar el volumen original al cerrar la actividad
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
    }

    // Nueva función para aplicar el modo oscuro en el mapa
    private fun aplicarModoOscuroEnMapa(activar: Boolean) {
        val style = if (activar) {
            R.raw.map_style_night
        } else {
            R.raw.map_style_standard
        }
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, style))
    }

    override fun onTipoReporteSeleccionado(tipoReporte: String) {
        val descripcionEditText = findViewById<EditText>(R.id.edtDescripcion)
        descripcionEditText.setText(tipoReporte) // Establece el tipo de reporte como descripción
    }
}