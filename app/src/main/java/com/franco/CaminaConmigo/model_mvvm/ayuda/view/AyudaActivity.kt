package com.franco.CaminaConmigo.model_mvvm.ayuda.view

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.ayuda.viewmodel.AyudaViewModel
import com.franco.CaminaConmigo.model_mvvm.chat.view.ChatActivity
import com.franco.CaminaConmigo.model_mvvm.mapa.view.MapaActivity
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity
import com.franco.CaminaConmigo.model_mvvm.novedad.view.NovedadActivity

class AyudaActivity : AppCompatActivity() {

    private val viewModel: AyudaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ayuda)

        // Observamos los datos del ViewModel
        viewModel.ayudaData.observe(this) { ayudaModel ->
            // Redirigir al cliente de correo
            val emailTextView = findViewById<TextView>(R.id.correo_centro_liwen)
            emailTextView.setOnClickListener {
                val emailIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(ayudaModel.email))
                    putExtra(Intent.EXTRA_SUBJECT, "Consulta sobre Centro de la Mujer Liwen")
                }
                try {
                    startActivity(Intent.createChooser(emailIntent, "Enviar correo usando..."))
                } catch (ex: android.content.ActivityNotFoundException) {
                    Toast.makeText(this, "No hay aplicaciones de correo instaladas.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Configuración del texto interactivo para el Centro de la Mujer Liwen
        val btnCentroLiwen = findViewById<TextView>(R.id.btn_centro_liwen)
        btnCentroLiwen.setOnClickListener {
            val intent = Intent(this, MapaActivity::class.java).apply {
                putExtra("location_name", "Centro de la Mujer Liwen - La Serena")
                putExtra("latitude", -29.9073)
                putExtra("longitude", -71.2540)
                putExtra("zoom_level", 18.0f) // Añadir nivel de zoom aquí
            }
            startActivity(intent)
        }

// Configuración del texto interactivo para el Centro de Atención Especializada en Violencias de Género
        val btnCentroAtencionEspecializada = findViewById<TextView>(R.id.btn_ubicacion_centro_atencion_especializada)
        btnCentroAtencionEspecializada.setOnClickListener {
            val intent = Intent(this, MapaActivity::class.java).apply {
                putExtra("location_name", "Centro de Atención Especializada en Violencias de Género")
                putExtra("latitude", -29.9160)
                putExtra("longitude", -71.2488)
                putExtra("zoom_level", 18.0f) // Añadir nivel de zoom aquí
            }
            startActivity(intent)
        }
        // Funcionalidad de los botones inferiores
        val btnMapa = findViewById<ImageButton>(R.id.imageButton10)
        val btnNovedades = findViewById<ImageButton>(R.id.imageButton11)
        val btnChats = findViewById<ImageButton>(R.id.imageButton12)
        val btnMenu = findViewById<ImageButton>(R.id.imageButton14)

        btnMapa.setOnClickListener {
            startActivity(Intent(this, MapaActivity::class.java))
        }

        btnNovedades.setOnClickListener {
            startActivity(Intent(this, NovedadActivity::class.java))
        }

        btnChats.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        btnMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }
}