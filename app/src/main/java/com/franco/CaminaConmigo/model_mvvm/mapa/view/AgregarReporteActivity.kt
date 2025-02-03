package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.franco.CaminaConmigo.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException

class AgregarReporteActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = "reports"
    private var tipoReporte: String? = null
    private var selectedImageUri: Uri? = null
    private lateinit var imageView: ImageView
    private lateinit var txtTituloReporte: TextView

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val GALLERY_PERMISSION_REQUEST_CODE = 101
    private val IMAGE_PICK_CODE = 102
    private val CAMERA_REQUEST_CODE = 103

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_reporte)

        tipoReporte = intent.getStringExtra("TIPO_REPORTE")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val edtDescripcion = findViewById<EditText>(R.id.edtDescripcion)
        val btnAgregarImagen = findViewById<ImageView>(R.id.btnAgregarImagen)
        val btnEnviarReporte = findViewById<Button>(R.id.btnEnviarReporte)
        val chkAnonimo = findViewById<CheckBox>(R.id.chkAnonimo)
        imageView = findViewById(R.id.imagenSeleccionada)
        txtTituloReporte = findViewById(R.id.txtTituloReporte)

        // Mostrar el tipo de reporte en el TextView
        findViewById<TextView>(R.id.txtTipoReporte).text = tipoReporte

        // Agregar imagen desde la galería o cámara
        btnAgregarImagen.setOnClickListener {
            // Crear un menú de selección (galería o cámara)
            val options = arrayOf("Tomar Foto", "Seleccionar de Galería")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission() // Tomar foto
                    1 -> checkGalleryPermission() // Seleccionar de la galería
                }
            }
            builder.show()
        }

        // Enviar reporte
        btnEnviarReporte.setOnClickListener {
            val descripcion = edtDescripcion.text.toString()
            if (descripcion.isBlank()) {
                Toast.makeText(this, "Por favor ingresa una descripción", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar permisos de ubicación
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                return@setOnClickListener
            }

            // Obtener la ubicación y enviar el reporte
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latitud = it.latitude
                    val longitud = it.longitude
                    val reporte = hashMapOf(
                        "description" to descripcion,
                        "type" to tipoReporte,
                        "latitude" to latitud,
                        "longitude" to longitud,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "userId" to (FirebaseAuth.getInstance().currentUser?.uid ?: "Anónimo"),
                        "isAnonymous" to chkAnonimo.isChecked
                    )

                    // Agregar la imagen a Firestore si existe
                    selectedImageUri?.let { uri ->
                        reporte["imageUri"] = uri.toString()
                    }

                    // Guardar reporte en Firestore
                    db.collection(reportsCollection)
                        .add(reporte)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Reporte enviado", Toast.LENGTH_SHORT).show()
                            finish()  // Volver a la actividad anterior
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al enviar reporte", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                GALLERY_PERMISSION_REQUEST_CODE
            )
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
            GALLERY_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    selectedImageUri = data?.data
                    imageView.setImageURI(selectedImageUri)
                    txtTituloReporte.text = "Imagen Seleccionada"
                }
                CAMERA_REQUEST_CODE -> {
                    val photo: Bitmap = data?.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(photo)
                    txtTituloReporte.text = "Foto Tomada"
                }
            }
        }
    }
}
