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
        val btnRetroceder = findViewById<ImageView>(R.id.btnRetroceder)
        val buttonInvitar = findViewById<Button>(R.id.button)

        // Acción para el botón de retroceder
        btnRetroceder.setOnClickListener {
            onBackPressed()
        }

        // Acción para el botón de invitar amigos
        buttonInvitar.setOnClickListener {
            enviarInvitacion()
        }
    }

    private fun enviarInvitacion() {
        val mensaje = "¡Hola! Te invito a unirte a la app CaminaConmigo. Descárgala ahora desde Google Play: https://play.google.com/store/apps/details?id=com.franco.CaminaConmigo"

        // Intent para compartir el mensaje con diferentes aplicaciones
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensaje)
        }

        // Verificar si hay aplicaciones que puedan manejar el Intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, "Compartir invitación con"))
        } else {
            // Manejar el caso donde no hay aplicaciones disponibles para manejar el Intent
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp"))
            startActivity(playStoreIntent)
        }
    }
}