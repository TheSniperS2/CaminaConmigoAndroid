package com.franco.CaminaConmigo.model_mvvm.novedad.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.firestore.FirebaseFirestore

class NovedadActivity : AppCompatActivity() {

    private val viewModel: NovedadViewModel by viewModels()
    private lateinit var adapter: ReporteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novedad)

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
        val btnFilterTendencias = findViewById<Button>(R.id.btnFilterTendencias)
        val btnFilterRecientes = findViewById<Button>(R.id.btnFilterRecientes)

        btnFilterTendencias.setOnClickListener {
            viewModel.filtrarPorTendencias()
        }

        btnFilterRecientes.setOnClickListener {
            viewModel.cargarReportes()
        }

        // Funcionalidad de los botones inferiores
        configurarBotonesInferiores()
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

    private fun configurarBotonesInferiores() {
        val btnMapa = findViewById<ImageButton>(R.id.imageButton10)
        val btnAyuda = findViewById<ImageButton>(R.id.imageButton13)
        val btnChats = findViewById<ImageButton>(R.id.imageButton12)
        val btnMenu = findViewById<ImageButton>(R.id.imageButton14)

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