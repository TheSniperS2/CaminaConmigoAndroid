package com.franco.CaminaConmigo.model_mvvm.configuraciones.view

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.franco.CaminaConmigo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var switchNotificacionesGrupos: Switch
    private lateinit var switchNotificacionesReporte: Switch
    private lateinit var switchModoOscuro: Switch
    private lateinit var switchShakeAlerta: Switch
    private lateinit var sharedPreferences: SharedPreferences



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuraciones)

        // Inicializar elementos de la interfaz
        val btnRetroceder: ImageView = findViewById(R.id.btnRetroceder)
        switchNotificacionesGrupos = findViewById(R.id.switch3)
        switchNotificacionesReporte = findViewById(R.id.switch2)
        switchModoOscuro = findViewById(R.id.switch4)
        switchShakeAlerta = findViewById(R.id.switch5)

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("configuraciones", MODE_PRIVATE)

        // Cargar estado de los switches desde SharedPreferences
        switchNotificacionesGrupos.isChecked = sharedPreferences.getBoolean("notificaciones_grupos", false)
        switchNotificacionesReporte.isChecked = sharedPreferences.getBoolean("notificaciones_reporte", false)
        switchModoOscuro.isChecked = sharedPreferences.getBoolean("modo_oscuro", false)
        switchShakeAlerta.isChecked = sharedPreferences.getBoolean("shake_alerta", false)

        // Listener para el botón de retroceso
        btnRetroceder.setOnClickListener {
            finish() // Cierra la actividad y vuelve atrás
        }

        // Listeners para los switches
        switchNotificacionesGrupos.setOnCheckedChangeListener { _, isChecked ->
            guardarConfiguracion("notificaciones_grupos", isChecked)
        }

        switchNotificacionesReporte.setOnCheckedChangeListener { _, isChecked ->
            guardarConfiguracion("notificaciones_reporte", isChecked)
            actualizarNotificacionEnFirebase(isChecked)
        }

        switchModoOscuro.setOnCheckedChangeListener { _, isChecked ->
            guardarConfiguracion("modo_oscuro", isChecked)
            aplicarModoOscuro(isChecked)
        }

        switchShakeAlerta.setOnCheckedChangeListener { _, isChecked ->
            guardarConfiguracion("shake_alerta", isChecked)
            enviarBroadcastShakeAlerta(isChecked)
        }

        // Aplicar modo oscuro si está activado
        aplicarModoOscuro(switchModoOscuro.isChecked)
    }

    private fun actualizarNotificacionEnFirebase(isChecked: Boolean) {
        val userId = obtenerUserId()
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId)
                .update("reportNotifications", isChecked)
                .addOnSuccessListener {
                    Log.d("Firebase", "Notificaciones de reporte actualizadas: $isChecked")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error al actualizar notificaciones de reporte", e)
                }
        } else {
            Log.e("Firebase", "Usuario no autenticado")
        }
    }


    private fun obtenerUserId(): String? {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid
    }


    private fun guardarConfiguracion(clave: String, valor: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(clave, valor)
        editor.apply()
    }

    private fun aplicarModoOscuro(activar: Boolean) {
        val modo = if (activar) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(modo)

        // Enviar broadcast para actualizar el modo del mapa
        val intent = Intent("com.franco.CaminaConmigo.MODO_OSCURO")
        intent.putExtra("activar", activar)
        sendBroadcast(intent)
    }

    private fun enviarBroadcastShakeAlerta(activar: Boolean) {
        val intent = Intent("com.franco.CaminaConmigo.SHAKE_ALERTA")
        intent.putExtra("activar", activar)
        sendBroadcast(intent)
    }
}