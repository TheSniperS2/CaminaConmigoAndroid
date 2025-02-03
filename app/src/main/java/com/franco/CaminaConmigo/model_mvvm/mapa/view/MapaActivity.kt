package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MapaActivity : AppCompatActivity(), OnMapReadyCallback, TipoReporteDialogFragment.OnTipoReporteSeleccionadoListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = "reports"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnReportar = findViewById<Button>(R.id.btnReportar)
        btnReportar.setOnClickListener {
            // Mostrar el diálogo de selección de tipo de reporte
            val dialogFragment = TipoReporteDialogFragment()
            dialogFragment.show(supportFragmentManager, "TipoReporteDialogFragment")
        }

        // Agregar listeners para los ImageButtons
        findViewById<ImageButton>(R.id.imageButton11).setOnClickListener {
            // Redirigir a otra vista
            val intent = Intent(this, NovedadActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.imageButton12).setOnClickListener {
            // Redirigir a otra vista
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.imageButton13).setOnClickListener {
            // Redirigir a otra vista
            val intent = Intent(this, AyudaActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.imageButton14).setOnClickListener {
            // Redirigir a otra vista
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        cargarReportes()

        // Solicitar permisos de ubicación si no se han concedido
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        mMap.isMyLocationEnabled = true

        // Obtener ubicación actual y mover la cámara hacia ella
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val miUbicacion = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 15f))
            }
        }
    }

    private fun cargarReportes() {
        db.collection(reportsCollection).get().addOnSuccessListener { documents ->
            for (document in documents) {
                val lat = document.getDouble("latitude") ?: continue
                val lng = document.getDouble("longitude") ?: continue
                val description = document.getString("description") ?: "Sin descripción"
                val type = document.getString("type") ?: "Sin tipo"

                val ubicacion = LatLng(lat, lng)
                mMap.addMarker(MarkerOptions().position(ubicacion).title(type).snippet(description))
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar reportes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reportarUbicacion(tipo: String, descripcion: String) {
        // Verificar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener la ubicación y guardar el reporte
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
                    "isAnonymous" to (FirebaseAuth.getInstance().currentUser == null)
                )

                // Guardar reporte en Firestore
                db.collection(reportsCollection)
                    .add(reporte)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(this, "Reporte enviado", Toast.LENGTH_SHORT).show()

                        // Agregar el marcador en el mapa
                        mMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitud, longitud))
                                .title(tipo)
                                .snippet(descripcion)
                        )
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al enviar reporte", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onTipoReporteSeleccionado(tipoReporte: String) {
        // Una vez que el usuario seleccione un tipo, mostramos un formulario o el diálogo
        // Abre los campos de descripción para agregar más detalles.
        val edtDescripcion = findViewById<EditText>(R.id.edtDescripcion)
    }
}
