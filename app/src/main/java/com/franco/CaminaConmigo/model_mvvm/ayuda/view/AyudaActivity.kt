package com.franco.CaminaConmigo.model_mvvm.ayuda.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.databinding.ActivityAyudaBinding

class AyudaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAyudaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAyudaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Redirigir al cliente de correo
        binding.textView48.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:centroliwen@laserena.cl")
            }
            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(emailIntent)
            } else {
                Toast.makeText(this, "No hay aplicaciones de correo instaladas", Toast.LENGTH_SHORT).show()
            }
        }

        // Redirigir a los números de teléfono
        binding.textView47.setOnClickListener {
            val phoneNumbers = listOf("51-2641850", "51-2427844", "961244738")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phoneNumbers.first()}")
            }
            startActivity(intent)
        }

        // Funcionalidad de los botones inferiores
        binding.imageButton10.setOnClickListener {
            Toast.makeText(this, "Abrir Mapa", Toast.LENGTH_SHORT).show()
            // Agregar lógica para abrir la actividad de mapas aquí
        }

        binding.imageButton11.setOnClickListener {
            Toast.makeText(this, "Abrir Novedades", Toast.LENGTH_SHORT).show()
            // Agregar lógica para abrir la actividad de novedades aquí
        }

        binding.imageButton12.setOnClickListener {
            Toast.makeText(this, "Abrir Chats", Toast.LENGTH_SHORT).show()
            // Agregar lógica para abrir la actividad de chats aquí
        }

        binding.imageButton13.setOnClickListener {
            Toast.makeText(this, "Ayuda seleccionada", Toast.LENGTH_SHORT).show()
            // Este botón puede ser el actual
        }

        binding.imageButton14.setOnClickListener {
            Toast.makeText(this, "Abrir Menú", Toast.LENGTH_SHORT).show()
            // Agregar lógica para abrir el menú aquí
        }
    }
}
