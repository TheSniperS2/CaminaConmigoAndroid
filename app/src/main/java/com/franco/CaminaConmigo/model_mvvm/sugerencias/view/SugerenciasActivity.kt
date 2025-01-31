package com.franco.CaminaConmigo.model_mvvm.sugerencias.view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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

class SugerenciasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sugerencias)

        // Inicializamos los elementos de la UI
        val imageViewBack = findViewById<ImageView>(R.id.imageView)
        val buttonEnviar = findViewById<Button>(R.id.button)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etNombre2 = findViewById<EditText>(R.id.etNombre2)
        val etNombre3 = findViewById<EditText>(R.id.etNombre3)
        val etNombre4 = findViewById<EditText>(R.id.etNombre4)
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
                etNombre2.setText("")
                etNombre.isEnabled = false
                etNombre2.isEnabled = false
            } else {
                // Si no está marcado, habilitar los campos de nombre y número
                etNombre.isEnabled = true
                etNombre2.isEnabled = true
            }
        }

        // Agregamos un TextWatcher para que el campo de número solo acepte números
        etNombre2.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Verificar si el texto contiene caracteres no numéricos
                if (s != null && s.toString().matches(Regex("[^0-9]"))) {
                    etNombre2.setText(s.toString().replace(Regex("[^0-9]"), ""))  // Remover cualquier carácter no numérico
                    etNombre2.setSelection(etNombre2.length())  // Poner el cursor al final
                }
            }

            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Acción para el botón de enviar
        buttonEnviar.setOnClickListener {
            // Enviamos la sugerencia, incluso si está en modo anónimo
            val nombre = etNombre.text.toString()
            val numeroStr = etNombre2.text.toString()
            val razon = etNombre3.text.toString()
            val mensaje = etNombre4.text.toString()

            if (nombre.isEmpty() && !cbAnonimo.isChecked) {
                // Si el campo nombre está vacío y no se ha seleccionado el modo anónimo
                Toast.makeText(this, "Por favor ingrese su nombre", Toast.LENGTH_SHORT).show()
            } else {
                // Intentamos convertir el número a un valor entero
                val numero = try {
                    numeroStr.toInt()
                } catch (e: NumberFormatException) {
                    // Si no es un número válido, mostramos un error
                    Toast.makeText(this, "Por favor ingrese un número válido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Enviamos la sugerencia
                enviarSugerencia(nombre, numero, razon, mensaje, cbAnonimo.isChecked)

                // Después de enviar, limpiamos todos los campos
                etNombre.setText("")
                etNombre2.setText("")
                etNombre3.setText("")
                etNombre4.setText("")
                cbAnonimo.isChecked = false

                // Rehabilitamos los campos de nombre y número en caso de haber sido deshabilitados
                etNombre.isEnabled = true
                etNombre2.isEnabled = true
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun enviarSugerencia(nombre: String, numero: Int, razon: String, mensaje: String, esAnonimo: Boolean) {
        val email = "franco.munoz@alumnos.ucentral.cl"
        val asunto = "Sugerencia de $nombre"
        val mensajeCompleto = """
            Nombre: $nombre
            Número: $numero
            Razón: $razon
            Mensaje: $mensaje
            Anónimo: ${if (esAnonimo) "Sí" else "No"}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            putExtra(Intent.EXTRA_TEXT, mensajeCompleto)
        }

        // Verificamos si hay una aplicación para enviar correos
        if (intent.resolveActivity(packageManager) != null) {
            try {
                startActivity(intent)
                Toast.makeText(this, "Sugerencia enviada correctamente", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Si ocurre algún error al intentar enviar el correo
                Toast.makeText(this, "Hubo un problema al enviar la sugerencia. Inténtalo de nuevo", Toast.LENGTH_LONG).show()
            }
        } else {
            // Si no hay una aplicación para enviar correos
            Toast.makeText(this, "No se encontró una aplicación de correo electrónico. Instale una para enviar sugerencias", Toast.LENGTH_LONG).show()
        }
    }
}
