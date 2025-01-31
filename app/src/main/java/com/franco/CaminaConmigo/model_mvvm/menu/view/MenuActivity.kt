package com.franco.CaminaConmigo.model_mvvm.menu.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.inicio.view.MainActivity
import com.franco.CaminaConmigo.model_mvvm.perfil.view.MiPerfilActivity
import com.franco.CaminaConmigo.model_mvvm.chat.view.ChatActivity
import com.franco.CaminaConmigo.model_mvvm.notificaciones.view.NotificacionesActivity
import com.franco.CaminaConmigo.model_mvvm.invitaramigos.view.InvitarAmigosActivity
import com.franco.CaminaConmigo.model_mvvm.sugerencias.view.SugerenciasActivity
import com.franco.CaminaConmigo.model_mvvm.configuraciones.view.ConfiguracionActivity
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.view.ContactoEmegenciaActivity


class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Referencias a los elementos del menú
        val perfil = findViewById<ImageView>(R.id.imageView6)
        val chat = findViewById<ImageView>(R.id.imageView7)
        val contactoEmergencia = findViewById<ImageView>(R.id.imageView8)
        val notificaciones = findViewById<ImageView>(R.id.imageView9)
        val invitarAmigos = findViewById<ImageView>(R.id.imageView13)
        val sugerencias = findViewById<ImageView>(R.id.imageView14)
        val configuracion = findViewById<ImageView>(R.id.imageView15)
        val btnCerrarSesion = findViewById<Button>(R.id.button2)

        // Navegación entre actividades
        perfil.setOnClickListener {
            startActivity(Intent(this, MiPerfilActivity::class.java))
        }

        chat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        contactoEmergencia.setOnClickListener {
            startActivity(Intent(this, ContactoEmegenciaActivity::class.java))
        }

        notificaciones.setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
        }

        invitarAmigos.setOnClickListener {
            startActivity(Intent(this, InvitarAmigosActivity::class.java))
        }

        sugerencias.setOnClickListener {
            startActivity(Intent(this, SugerenciasActivity::class.java))
        }

        configuracion.setOnClickListener {
            startActivity(Intent(this, ConfiguracionActivity::class.java))
        }

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener {
            // Lógica para cerrar sesión (ejemplo: volver a pantalla de login)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
