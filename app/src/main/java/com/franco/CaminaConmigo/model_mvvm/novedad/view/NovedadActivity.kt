package com.franco.CaminaConmigo.model_mvvm.novedad.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.chat.view.ChatActivity
import com.franco.CaminaConmigo.model_mvvm.mapa.view.DetallesReporteDialogFragment
import com.franco.CaminaConmigo.model_mvvm.mapa.view.MapaActivity
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity
import com.franco.CaminaConmigo.model_mvvm.novedad.adapter.ReporteAdapter
import com.franco.CaminaConmigo.model_mvvm.novedad.model.Reporte
import com.franco.CaminaConmigo.model_mvvm.novedad.viewmodel.NovedadViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore

class NovedadActivity : AppCompatActivity() {

    private val viewModel: NovedadViewModel by viewModels()
    private lateinit var adapter: ReporteAdapter
    private lateinit var btnFilterTendencias: Button
    private lateinit var btnFilterRecientes: Button
    private lateinit var btnFilterCiudad: Button
    private lateinit var btnSearch: ImageButton
    private lateinit var edtSearch: EditText
    private lateinit var btnCancel: TextView
    private lateinit var searchLayout: LinearLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novedad)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        obtenerUbicacion()

        // Configuración del RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewNovedades)
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.reportes.observe(this) { reportes ->
            adapter = ReporteAdapter(reportes) { reporte ->
                abrirDetalles(reporte)
            }
            recyclerView.adapter = adapter
        }

        // Configuración de los botones de filtro
        btnFilterTendencias = findViewById(R.id.btnFilterTendencias)
        btnFilterRecientes = findViewById(R.id.btnFilterRecientes)
        btnFilterCiudad = findViewById(R.id.btnFilterCiudad)

        // Marcar el filtro de "Recientes" por defecto
        actualizarFiltroSeleccionado(btnFilterRecientes)
        viewModel.cargarReportes()

        btnFilterTendencias.setOnClickListener {
            if (btnFilterTendencias.isSelected) return@setOnClickListener
            viewModel.filtrarPorTendencias()
            actualizarFiltroSeleccionado(btnFilterTendencias)
        }

        btnFilterRecientes.setOnClickListener {
            if (btnFilterRecientes.isSelected) return@setOnClickListener
            viewModel.cargarReportes()
            actualizarFiltroSeleccionado(btnFilterRecientes)
        }

        btnFilterCiudad.setOnClickListener {
            if (btnFilterCiudad.isSelected) return@setOnClickListener
            currentLocation?.let { location ->
                viewModel.filtrarPorCiudad(location.latitude, location.longitude)
                actualizarFiltroSeleccionado(btnFilterCiudad)
            } ?: run {
                Toast.makeText(this, "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuración del botón de búsqueda
        btnSearch = findViewById(R.id.btnSearch)
        edtSearch = findViewById(R.id.edtSearch)
        btnCancel = findViewById(R.id.btnCancel)
        searchLayout = findViewById(R.id.searchLayout)

        btnSearch.setOnClickListener {
            mostrarBarraBusqueda()
        }

        btnCancel.setOnClickListener {
            edtSearch.text.clear()
            ocultarBarraBusqueda()
        }

        // Añadir TextWatcher para búsqueda en tiempo real
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                viewModel.buscarReportes(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Funcionalidad de los botones inferiores
        configurarBotonesInferiores()
    }

    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
            }
        }
    }

    private fun abrirDetalles(reporte: Reporte) {
        // Recuperar los demás datos desde Firestore si es necesario
        val db = FirebaseFirestore.getInstance()
        db.collection("reportes").document(reporte.id) // Asegúrate de que reporte.id contiene el ID correcto
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Extraer los datos del reporte
                    val type = document.getString("type") ?: "Tipo desconocido"
                    val description = document.getString("description") ?: "Descripción desconocida"
                    val timestamp = document.getDate("timestamp")
                    val likes = document.getLong("likes")?.toInt() ?: 0

                    // Crear la instancia del fragmento con los datos
                    val dialogFragment = DetallesReporteDialogFragment.newInstance(
                        reporte.id, type, description, timestamp, likes
                    )
                    dialogFragment.show(supportFragmentManager, "DetallesReporteDialogFragment")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener los detalles del reporte", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarFiltroSeleccionado(botonSeleccionado: Button) {
        val botones = listOf(btnFilterTendencias, btnFilterRecientes, btnFilterCiudad)
        botones.forEach { boton ->
            boton.isSelected = boton == botonSeleccionado
            if (boton.isSelected) {
                boton.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
                boton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                boton.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_500))
                boton.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }
        }
    }

    private fun mostrarBarraBusqueda() {
        searchLayout.visibility = View.VISIBLE
        btnSearch.visibility = View.GONE
        btnFilterTendencias.visibility = View.GONE
        btnFilterRecientes.visibility = View.GONE
        btnFilterCiudad.visibility = View.GONE
    }

    private fun ocultarBarraBusqueda() {
        searchLayout.visibility = View.GONE
        btnSearch.visibility = View.VISIBLE
        btnFilterTendencias.visibility = View.VISIBLE
        btnFilterRecientes.visibility = View.VISIBLE
        btnFilterCiudad.visibility = View.VISIBLE
    }

    private fun configurarBotonesInferiores() {
        val btnMapa = findViewById<LinearLayout>(R.id.MapaContainer)
        val btnChats = findViewById<LinearLayout>(R.id.ChatContainer2)
        val btnAyuda = findViewById<LinearLayout>(R.id.AyudaContainer)
        val btnMenu = findViewById<LinearLayout>(R.id.MenuContainer)

        btnMapa.setOnClickListener {
            startActivity(Intent(this, MapaActivity::class.java))
        }

        btnChats.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        btnAyuda.setOnClickListener {
            startActivity(Intent(this, AyudaActivity::class.java))
        }

        btnMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }
}