package com.franco.CaminaConmigo.model_mvvm.contactoemegencia.view

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.franco.CaminaConmigo.R

class AddContactDialogFragment : DialogFragment() {

    private lateinit var editTextNombre: EditText
    private lateinit var editTextNumero: EditText
    private lateinit var btnAgregar: TextView
    private lateinit var btnCancelar: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_agregar_contacto, container, false)

        editTextNombre = view.findViewById(R.id.editTextNombre)
        editTextNumero = view.findViewById(R.id.editTextNumero)
        btnAgregar = view.findViewById(R.id.btnAgregar)
        btnCancelar = view.findViewById(R.id.btnCancelar)

        // Prellenar el campo del número con +569 y hacerlo fijo
        editTextNumero.setText("+569")
        editTextNumero.setSelection(editTextNumero.text.length)
        editTextNumero.filters = arrayOf(InputFilter.LengthFilter(12))

        // Añadir TextWatcher para evitar que se borre el prefijo +569
        editTextNumero.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.toString().startsWith("+569")) {
                    editTextNumero.setText("+569")
                    editTextNumero.setSelection(editTextNumero.text.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Deshabilitar el botón de agregar inicialmente
        btnAgregar.isEnabled = false

        // Añadir TextWatcher para habilitar/deshabilitar el botón de agregar
        editTextNumero.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 12) {
                    btnAgregar.isEnabled = true
                    btnAgregar.setBackgroundResource(R.drawable.button_background_enabled)
                } else {
                    btnAgregar.isEnabled = false
                    btnAgregar.setBackgroundResource(R.drawable.button_background_disabled)
                }
            }
        })

        btnAgregar.setOnClickListener {
            val nombre = editTextNombre.text.toString()
            val numero = editTextNumero.text.toString()
            if (nombre.isNotEmpty() && numero.startsWith("+569") && numero.length == 12) {
                (activity as ContactoEmegenciaActivity).viewModel.agregarContacto(nombre, numero)
                dismiss()
            } else {
                // Mostrar error si el número no es válido
                Toast.makeText(activity, "Número inválido. Debe empezar con +569 y tener 12 dígitos en total.", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
    }
}