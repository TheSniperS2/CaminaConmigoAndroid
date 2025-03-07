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
import com.franco.CaminaConmigo.model_mvvm.contactoemegencia.model.ContactoEmergencia

class EditContactDialogFragment(
    private val contacto: ContactoEmergencia,
    private val index: Int
) : DialogFragment() {

    private lateinit var editTextNuevoNombre: EditText
    private lateinit var editTextNuevoNumero: EditText
    private lateinit var btnGuardar: TextView
    private lateinit var btnCancelar: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_editar_numero, container, false)

        editTextNuevoNombre = view.findViewById(R.id.editTextNuevoNombre)
        editTextNuevoNumero = view.findViewById(R.id.editTextNuevoNumero)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        btnCancelar = view.findViewById(R.id.btnCancelar)

        // Rellenar los campos con los datos actuales y hacer el prefijo +569 fijo
        editTextNuevoNombre.setText(contacto.name)
        editTextNuevoNumero.setText(contacto.phone)

        // Hacer el prefijo +569 fijo
        editTextNuevoNumero.filters = arrayOf(InputFilter.LengthFilter(12))

        // Añadir TextWatcher para evitar que se borre el prefijo +569
        editTextNuevoNumero.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.toString().startsWith("+569")) {
                    editTextNuevoNumero.setText("+569")
                    editTextNuevoNumero.setSelection(editTextNuevoNumero.text.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnGuardar.setOnClickListener {
            val nuevoNombre = editTextNuevoNombre.text.toString()
            val nuevoNumero = editTextNuevoNumero.text.toString()
            if (nuevoNombre.isNotEmpty() && nuevoNumero.startsWith("+569") && nuevoNumero.length == 12) {
                (activity as ContactoEmegenciaActivity).viewModel.editarContacto(index, nuevoNombre, nuevoNumero)
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