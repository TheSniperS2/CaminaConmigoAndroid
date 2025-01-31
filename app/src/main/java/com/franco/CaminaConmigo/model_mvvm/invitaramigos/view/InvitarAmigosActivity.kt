package com.franco.CaminaConmigo.model_mvvm.invitaramigos.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.R

class InvitarAmigosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invitaramigos)

        // Inicializando los elementos de la UI
        val imageViewBack = findViewById<ImageView>(R.id.imageView)
        val buttonInvitar = findViewById<Button>(R.id.button)

        // Acción para el botón de retroceder
        imageViewBack.setOnClickListener {
            onBackPressed()
        }

        // Acción para el botón de invitar amigos
        buttonInvitar.setOnClickListener {
            enviarInvitacion()
        }
    }

    private fun enviarInvitacion() {
        val numeroTelefono = "+123456789"  // Número de teléfono al que deseas enviar el mensaje
        val mensaje = "¡Hola! Te invito a unirte a la app CaminaConmigo. Descárgala ahora desde Google Play: https://play.google.com/store/apps/details?id=com.franco.CaminaConmigo"

        // Intent para abrir WhatsApp con el mensaje predefinido
        val uri = Uri.parse("https://wa.me/$numeroTelefono?text=${Uri.encode(mensaje)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        // Verificamos si WhatsApp está instalado en el dispositivo
        if (isAppInstalled("com.whatsapp")) {
            startActivity(intent)
        } else {
            // Si WhatsApp no está instalado, redirigir a Google Play para que lo descargue
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp"))
            startActivity(playStoreIntent)
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true  // Si la app está instalada
        } catch (e: Exception) {
            false  // Si la app no está instalada
        }
    }
}
