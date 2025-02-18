package com.franco.CaminaConmigo.model_mvvm.sugerencias.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.utils.MailerSendService
import kotlin.concurrent.thread

class SugerenciasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sugerencias)

        // Inicializamos los elementos de la UI
        val imageViewBack = findViewById<ImageView>(R.id.imageView)
        val buttonEnviar = findViewById<Button>(R.id.button)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etNumero = findViewById<EditText>(R.id.etNombre2)
        val etRazon = findViewById<EditText>(R.id.etNombre3)
        val etMensaje = findViewById<EditText>(R.id.etNombre4)
        val cbAnonimo = findViewById<CheckBox>(R.id.cbAnonimo)

        // Acción para el botón de retroceder
        imageViewBack.setOnClickListener {
            onBackPressed()
        }

        // Listener para el checkbox "Enviar de forma Anónima"
        cbAnonimo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Si está marcado, deshabilitar y vaciar los campos de nombre y número
                etNombre.setText("")
                etNumero.setText("")
                etNombre.isEnabled = false
                etNumero.isEnabled = false
            } else {
                // Si no está marcado, habilitar los campos de nombre y número
                etNombre.isEnabled = true
                etNumero.isEnabled = true
            }
        }

        // Agregamos un TextWatcher para que el campo de número solo acepte números
        etNumero.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Verificar si el texto contiene caracteres no numéricos
                if (s != null && s.toString().matches(Regex("[^0-9]"))) {
                    etNumero.setText(s.toString().replace(Regex("[^0-9]"), ""))  // Remover cualquier carácter no numérico
                    etNumero.setSelection(etNumero.length())  // Poner el cursor al final
                }
            }

            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Acción para el botón de enviar
        buttonEnviar.setOnClickListener {
            // Enviamos la sugerencia, incluso si está en modo anónimo
            val nombre = etNombre.text.toString()
            val numeroStr = etNumero.text.toString()
            val razon = etRazon.text.toString()
            val mensaje = etMensaje.text.toString()

            if (!cbAnonimo.isChecked && (nombre.isEmpty() || numeroStr.isEmpty())) {
                // Si el campo nombre o número está vacío y no se ha seleccionado el modo anónimo
                Toast.makeText(this, "Por favor, rellena todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Enviamos la sugerencia en un hilo separado para no bloquear la UI
            thread {
                enviarSugerencia(nombre, numeroStr, razon, mensaje, cbAnonimo.isChecked)
            }

            // Después de enviar, limpiamos todos los campos
            etNombre.setText("")
            etNumero.setText("")
            etRazon.setText("")
            etMensaje.setText("")
            cbAnonimo.isChecked = false

            // Rehabilitamos los campos de nombre y número en caso de haber sido deshabilitados
            etNombre.isEnabled = true
            etNumero.isEnabled = true
        }
    }

    private fun enviarSugerencia(nombre: String, numero: String, razon: String, mensaje: String, esAnonimo: Boolean) {
        try {
            MailerSendService().sendSuggestion(nombre, numero, razon, mensaje, esAnonimo)
            runOnUiThread {
                Toast.makeText(this, "Sugerencia enviada correctamente.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Hubo un problema al enviar la sugerencia. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
            }
        }
    }
}