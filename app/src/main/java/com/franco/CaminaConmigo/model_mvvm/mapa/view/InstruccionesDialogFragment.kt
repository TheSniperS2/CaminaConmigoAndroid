package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.FragmentInstruccionesBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InstruccionesBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val paginas = listOf(
        R.drawable.instruccion_pagina_1 to "Iniciar reporte",
        R.drawable.instruccion_pagina_2 to "Seleccionar tipo de reporte",
        R.drawable.instruccion_pagina_3 to "Ubicación",
        R.drawable.instruccion_pagina_4 to "Enviar incidente"
    )
    private val descripciones = listOf(
        "Presione el botón 'REPORTE' en la parte inferior de la pantalla.",
        "Elige el tipo de incidente que deseas reportar entre las opciones disponibles.",
        "Selecciona la ubicación exacta donde ocurrió el incidente en el mapa.",
        "Si deseas puedes escribir una descripción detallada del incidente para facilitar su comprensión, agregar fotos y enviar de formar anónima o no. Luego presionar 'REPORTAR' y tu reporte estará a disposición de la comunidad."
    )
    private var paginaActual = 0

    private var _binding: FragmentInstruccionesBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInstruccionesBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheetBehavior(view)
        actualizarVista()

        binding.buttonSiguiente.setOnClickListener {
            if (paginaActual < paginas.size - 1) {
                paginaActual++
                actualizarVista()
            } else {
                dismiss()
            }
        }

        binding.buttonAtras.setOnClickListener {
            if (paginaActual > 0) {
                paginaActual--
                actualizarVista()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setCanceledOnTouchOutside(false) // Disable closing when touching outside the dialog
        return dialog
    }

    private fun setupBottomSheetBehavior(view: View) {
        val bottomSheet = view.parent as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        behavior.isFitToContents = true
        behavior.skipCollapsed = true
        behavior.isHideable = false  // Disable hiding the bottom sheet by swiping down
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // No-op
            }
        })
        behavior.maxHeight = (resources.displayMetrics.heightPixels * 0.9).toInt() // Set the max height to 90% of screen height
    }

    private fun actualizarVista() {
        binding.imageViewInstruccion.setImageResource(paginas[paginaActual].first)
        binding.numeroVista.text = (paginaActual + 1).toString()
        binding.txtTituloInstruccion.text = paginas[paginaActual].second
        binding.txtDescripcionInstruccion.text = descripciones[paginaActual]

        // Actualizar puntos de progreso
        actualizarPuntosDeProgreso()

        if (paginaActual > 0) {
            binding.buttonAtras.isEnabled = true
            binding.buttonAtras.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_purple))
            binding.buttonAtras.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        } else {
            binding.buttonAtras.isEnabled = false
            binding.buttonAtras.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))
            binding.buttonAtras.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }

        if (paginaActual == paginas.size - 1) {
            binding.buttonSiguiente.text = "Finalizar"
            binding.buttonSiguiente.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        } else {
            binding.buttonSiguiente.text = "Siguiente"
            binding.buttonSiguiente.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_right_blanco), null)
        }
    }

    private fun actualizarPuntosDeProgreso() {
        binding.punto1.setBackgroundResource(if (paginaActual == 0) R.drawable.punto_activo else R.drawable.punto_inactivo)
        binding.punto2.setBackgroundResource(if (paginaActual == 1) R.drawable.punto_activo else R.drawable.punto_inactivo)
        binding.punto3.setBackgroundResource(if (paginaActual == 2) R.drawable.punto_activo else R.drawable.punto_inactivo)
        binding.punto4.setBackgroundResource(if (paginaActual == 3) R.drawable.punto_activo else R.drawable.punto_inactivo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}