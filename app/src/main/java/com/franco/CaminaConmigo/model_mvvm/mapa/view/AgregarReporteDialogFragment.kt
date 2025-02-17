package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AgregarReporteDialogFragment : BottomSheetDialogFragment() {

    private lateinit var tipoReporte: String
    private val db = FirebaseFirestore.getInstance()
    private var selectedImageUri: Uri? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    private lateinit var txtUbicacion: TextView
    private lateinit var edtDescripcion: EditText
    private lateinit var chkAnonimo: CheckBox
    private lateinit var btnEnviarReporte: Button
    private lateinit var imgTipoReporte: ImageView

    private val IMAGE_PICK_CODE = 102
    private val CAMERA_REQUEST_CODE = 103
    private val LOCATION_PICKER_REQUEST = 104

    companion object {
        fun newInstance(tipoReporte: String): AgregarReporteDialogFragment {
            val fragment = AgregarReporteDialogFragment()
            val args = Bundle()
            args.putString("tipoReporte", tipoReporte)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tipoReporte = arguments?.getString("tipoReporte") ?: "Sin Tipo"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_agregar_reporte, container, false)

        // Inicializar vistas
        val txtTipoReporte = view.findViewById<TextView>(R.id.txtTipoReporte)
        txtUbicacion = view.findViewById(R.id.txtUbicacion)
        edtDescripcion = view.findViewById(R.id.edtDescripcion)
        chkAnonimo = view.findViewById(R.id.chkAnonimo)
        btnEnviarReporte = view.findViewById(R.id.btnEnviarReporte)
        imgTipoReporte = view.findViewById(R.id.imgIconoReporte)

        val btnCerrar = view.findViewById<ImageView>(R.id.btnCerrar)
        val btnTomarFoto = view.findViewById<ImageView>(R.id.btnTomarFoto)
        val btnSeleccionarFoto = view.findViewById<ImageView>(R.id.btnSeleccionarFoto)

        txtTipoReporte.text = tipoReporte
        setImageForTipoReporte(tipoReporte)

        btnTomarFoto.setOnClickListener { openCamera() }
        btnSeleccionarFoto.setOnClickListener { openGallery() }
        btnEnviarReporte.setOnClickListener { enviarReporte() }
        btnCerrar.setOnClickListener { dismiss() }
        txtUbicacion.setOnClickListener { abrirSelectorUbicacion() }

        return view
    }

    private fun setImageForTipoReporte(tipo: String) {
        val imageRes = when (tipo) {
            "Reunión de hombres" -> R.drawable.icon_reunion
            "Puntos Ciegos" -> R.drawable.icon_puntosciegos
            "Presencia de Bares y Restobares" -> R.drawable.icono_bares
            "Veredas en mal estado" -> R.drawable.icono_veredas
            "Poca Iluminación" -> R.drawable.icono_iluminacion
            "Vegetación Abundante" -> R.drawable.icono_vegetacion
            "Espacios Abandonados" -> R.drawable.icon_abandonados
            "Agresión Fisica" -> R.drawable.icon_agre_fisica
            "Agresión Sexual" -> R.drawable.icon_agre_sexual
            "Agresión Verbal" -> R.drawable.icon_agre_verbal
            "Falta de Baños Públicos" -> R.drawable.icon_faltabanos
            "Mobiliario Inadecuado" -> R.drawable.icon_mobiliario
            "Persona en situación de calle" -> R.drawable.icon_situacioncalle
            else -> R.drawable.ic_anadir
        }
        imgTipoReporte.setImageResource(imageRes)
    }

    private fun abrirSelectorUbicacion() {
        val intent = Intent(requireContext(), SelectorUbicacionActivity::class.java)
        startActivityForResult(intent, LOCATION_PICKER_REQUEST)
    }

    private fun enviarReporte() {
        val descripcion = edtDescripcion.text.toString().trim()
        val isAnonimo = chkAnonimo.isChecked

        if (descripcion.isBlank()) {
            Toast.makeText(requireContext(), "Por favor ingresa una descripción", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLatitude == null || selectedLongitude == null) {
            Toast.makeText(requireContext(), "Por favor selecciona una ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        val reporte = hashMapOf(
            "description" to descripcion,
            "type" to tipoReporte,
            "latitude" to selectedLatitude,
            "longitude" to selectedLongitude,
            "timestamp" to FieldValue.serverTimestamp(),
            "userId" to (FirebaseAuth.getInstance().currentUser?.uid ?: "Anónimo"),
            "isAnonymous" to isAnonimo,
            "likes" to 0
        )

        selectedImageUri?.let { uri -> reporte["imageUri"] = uri.toString() }

        db.collection("reportes")
            .add(reporte)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "com.franco.CaminaConmigo.model_mvvm.novedad.model.Reporte enviado", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al enviar reporte", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    selectedImageUri = data?.data
                    Toast.makeText(requireContext(), "Imagen seleccionada", Toast.LENGTH_SHORT).show()
                }
                CAMERA_REQUEST_CODE -> {
                    val photo: Bitmap = data?.extras?.get("data") as Bitmap
                    Toast.makeText(requireContext(), "Foto tomada", Toast.LENGTH_SHORT).show()
                }
                LOCATION_PICKER_REQUEST -> {
                    selectedLatitude = data?.getDoubleExtra("latitude", 0.0)
                    selectedLongitude = data?.getDoubleExtra("longitude", 0.0)
                    txtUbicacion.text = "Ubicación seleccionada"
                }
            }
        }
    }
}
