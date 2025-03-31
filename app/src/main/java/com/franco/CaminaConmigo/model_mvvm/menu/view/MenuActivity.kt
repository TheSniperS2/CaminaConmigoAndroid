package com.franco.CaminaConmigo.model_mvvm.menu.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.chat.view.ChatActivity
import com.franco.CaminaConmigo.model_mvvm.configuraciones.view.ConfiguracionActivity
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.view.ContactoEmegenciaActivity
import com.franco.CaminaConmigo.model_mvvm.inicio.view.MainActivity
import com.franco.CaminaConmigo.model_mvvm.invitaramigos.view.InvitarAmigosActivity
import com.franco.CaminaConmigo.model_mvvm.mapa.view.MapaActivity
import com.franco.CaminaConmigo.model_mvvm.notificaciones.view.NotificationsActivity
import com.franco.CaminaConmigo.model_mvvm.novedad.view.NovedadActivity
import com.franco.CaminaConmigo.model_mvvm.perfil.view.MiPerfilActivity
import com.franco.CaminaConmigo.model_mvvm.sugerencias.view.SugerenciasActivity
import com.google.firebase.auth.FirebaseAuth


class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Referencias a los elementos del menú
        val perfil = findViewById<LinearLayout>(R.id.miPerfilContainer)
        val chat = findViewById<LinearLayout>(R.id.chatContainer)
        val contactoEmergencia = findViewById<LinearLayout>(R.id.contactosEmergenciaContainer)
        val notificaciones = findViewById<LinearLayout>(R.id.notificacionesContainer)
        val invitarAmigos = findViewById<LinearLayout>(R.id.invitarAmigosContainer)
        val sugerencias = findViewById<LinearLayout>(R.id.sugerenciasContainer)
        val configuracion = findViewById<LinearLayout>(R.id.configuracionContainer)
        val btnCerrarSesion = findViewById<Button>(R.id.button2)

        val Mapa = findViewById<LinearLayout>(R.id.MapaContainer)
        val Novedad = findViewById<LinearLayout>(R.id.NovedadContainer)
        val chat_friend = findViewById<LinearLayout>(R.id.ChatContainer2)
        val Ayuda = findViewById<LinearLayout>(R.id.AyudaContainer)

        // Verificar si el usuario está logueado y cambiar el texto del botón
        if (!isUserAuthenticated()) {
            btnCerrarSesion.text = "Iniciar sesión"
        }

        // Navegación entre actividades
        perfil.setOnClickListener {
            if (isUserAuthenticated()) {
                startActivity(Intent(this, MiPerfilActivity::class.java))
            } else {
                showSignInDialog()
            }
        }

        chat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        contactoEmergencia.setOnClickListener {
            if (isUserAuthenticated()) {
                startActivity(Intent(this, ContactoEmegenciaActivity::class.java))
            } else {
                showSignInDialog()
            }
        }

        notificaciones.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        invitarAmigos.setOnClickListener {
            startActivity(Intent(this, InvitarAmigosActivity::class.java))
        }

        sugerencias.setOnClickListener {
            if (isUserAuthenticated()) {
                startActivity(Intent(this, SugerenciasActivity::class.java))
            } else {
                showSignInDialog()
            }
        }

        configuracion.setOnClickListener {
            if (isUserAuthenticated()) {
                startActivity(Intent(this, ConfiguracionActivity::class.java))
            } else {
                showSignInDialog()
            }
        }

        Mapa.setOnClickListener {
            startActivity(Intent(this, MapaActivity::class.java))
        }

        Novedad.setOnClickListener {
            startActivity(Intent(this, NovedadActivity::class.java))
        }

        chat_friend.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        Ayuda.setOnClickListener {
            startActivity(Intent(this, AyudaActivity::class.java))
        }

        // Cerrar sesión o iniciar sesión
        btnCerrarSesion.setOnClickListener {
            if (isUserAuthenticated()) {
                // Lógica para cerrar sesión (ejemplo: volver a pantalla de login)
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // Redirigir a la pantalla de inicio de sesión
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
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
    }}
