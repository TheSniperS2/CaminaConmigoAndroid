package com.franco.CaminaConmigo.model_mvvm.novedad.view

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
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

        // Funcionalidad de los botones inferiores
        configurarBotonesInferiores()
    }

    private fun abrirDetalles(reporte: Reporte) {
        val intent = Intent(this, DetallesReporteDialogFragment::class.java)
        intent.putExtra("idReporte", reporte.id)
        startActivity(intent)
    }

    private fun configurarBotonesInferiores() {
        val btnMapa = findViewById<ImageButton>(R.id.imageButton10)
        val btnNovedades = findViewById<ImageButton>(R.id.imageButton11)
        val btnChats = findViewById<ImageButton>(R.id.imageButton12)
        val btnMenu = findViewById<ImageButton>(R.id.imageButton14)

        btnMapa.setOnClickListener {
            startActivity(Intent(this, MapaActivity::class.java))
        }

        btnChats.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        btnNovedades.setOnClickListener {
            startActivity(Intent(this, AyudaActivity::class.java))
        }

        btnMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }
}
