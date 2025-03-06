package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class AgregarReporteDialogFragment : BottomSheetDialogFragment() {

    private lateinit var tipoReporte: String
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val selectedImageUris = mutableListOf<Uri>()
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedLocationName: String? = null


    private lateinit var txtUbicacion: TextView
    private lateinit var edtDescripcion: EditText
    private lateinit var switchAnonimo: Switch
    private lateinit var btnEnviarReporte: Button
    private lateinit var imgTipoReporte: ImageView
    private lateinit var recyclerViewImages: RecyclerView
    private lateinit var imagesAdapter: ImagesAdapter

    private val IMAGE_PICK_CODE = 102
    private val CAMERA_REQUEST_CODE = 103
    private val LOCATION_PICKER_REQUEST = 104
    private val CAMERA_PERMISSION_CODE = 105

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

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setCanceledOnTouchOutside(false) // Disable closing when touching outside the dialog
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomSheetBehavior(view)
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
        switchAnonimo = view.findViewById(R.id.switchAnonimo)
        switchAnonimo.isChecked = true // Activar por defecto
        btnEnviarReporte = view.findViewById(R.id.btnEnviarReporte)
        imgTipoReporte = view.findViewById(R.id.imgIconoReporte)
        recyclerViewImages = view.findViewById(R.id.recyclerViewImages)

        val btnCerrar = view.findViewById<ImageView>(R.id.btnCerrar)
        val btnTomarFoto = view.findViewById<ImageView>(R.id.btnTomarFoto)
        val btnSeleccionarFoto = view.findViewById<ImageView>(R.id.btnSeleccionarFoto)

        txtTipoReporte.text = tipoReporte
        setImageForTipoReporte(tipoReporte)

        // Configurar colores del Switch basado en el estado
        val colorActive = ContextCompat.getColor(requireContext(), R.color.purple_500)
        val colorInactive = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked), // Estado activado
            intArrayOf(-android.R.attr.state_checked) // Estado desactivado
        )
        val colors = intArrayOf(
            colorActive,
            colorInactive
        )
        val colorStateList = ColorStateList(states, colors)
        switchAnonimo.trackTintList = colorStateList
        switchAnonimo.thumbTintList = colorStateList

        btnTomarFoto.setOnClickListener { checkCameraPermissionAndOpenCamera() }
        btnSeleccionarFoto.setOnClickListener { openGallery() }
        btnEnviarReporte.setOnClickListener { enviarReporte() }
        btnCerrar.setOnClickListener { dismiss() }
        txtUbicacion.setOnClickListener { abrirSelectorUbicacion() }

        // Configurar RecyclerView para las imágenes
        imagesAdapter = ImagesAdapter(selectedImageUris) { uri -> removeImage(uri) }
        recyclerViewImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerViewImages.adapter = imagesAdapter

        // Establecer color inicial del texto de txtUbicacion
        txtUbicacion.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))

        return view
    }

    private fun setupBottomSheetBehavior(view: View) {
        val bottomSheet = view.parent as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        behavior.isFitToContents = true
        behavior.skipCollapsed = true
        behavior.isHideable = true  // Allow hiding the bottom sheet by swiping down
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

    private fun removeImage(uri: Uri) {
        selectedImageUris.remove(uri)
        imagesAdapter.notifyDataSetChanged()
        if (selectedImageUris.isEmpty()) {
            recyclerViewImages.visibility = View.GONE
        }
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
            "Agresión Física" -> R.drawable.icon_agre_fisica
            "Agresión Sexual" -> R.drawable.icon_agre_sexual
            "Agresión Verbal" -> R.drawable.icon_agre_verbal
            "Falta de Baños Públicos" -> R.drawable.icon_faltabanos
            "Mobiliario Inadecuado" -> R.drawable.icon_mobiliario
            "Personas en situación de calle" -> R.drawable.icon_situacioncalle
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
        val isAnonimo = switchAnonimo.isChecked

        if (descripcion.isBlank()) {
            Toast.makeText(requireContext(), "Por favor ingresa una descripción", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLatitude == null || selectedLongitude == null) {
            Toast.makeText(requireContext(), "Por favor selecciona una ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar el botón de enviar para evitar múltiples clics
        btnEnviarReporte.isEnabled = false

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val reporte = hashMapOf(
            "description" to descripcion,
            "type" to tipoReporte,
            "latitude" to selectedLatitude,
            "longitude" to selectedLongitude,
            "timestamp" to FieldValue.serverTimestamp(),
            "senderId" to userId,
            "isAnonymous" to isAnonimo,
            "isRead" to false,
            "imageUrls" to listOf<String>()
        )

        db.collection("reportes")
            .add(reporte)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(requireContext(), "Reporte enviado", Toast.LENGTH_SHORT).show()
                if (selectedImageUris.isNotEmpty()) {
                    for (uri in selectedImageUris) {
                        val storageRef = storage.reference.child("report_images/${UUID.randomUUID()}")
                        storageRef.putFile(uri)
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    documentReference.update("imageUrls", FieldValue.arrayUnion(downloadUri.toString()))
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                if (!isAnonimo) {
                    notifyFriendsAboutReport(userId, documentReference.id, tipoReporte)
                }
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al enviar reporte", Toast.LENGTH_SHORT).show()
                // Rehabilitar el botón de enviar en caso de error
                btnEnviarReporte.isEnabled = true
            }
    }

    private fun notifyFriendsAboutReport(userId: String, reportId: String, reportType: String) {
        db.collection("users").document(userId).collection("friends").get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val friendId = document.id
                    createFriendReportNotification(userId, friendId, reportId, reportType)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al notificar a los amigos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createFriendReportNotification(userId: String, friendId: String, reportId: String, reportType: String) {
        if (!isAdded) {
            return
        }

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val friendName = document.getString("username") ?: "unknown"
            val dataMap = mapOf(
                "friendId" to userId,
                "friendName" to friendName,
                "reportId" to reportId,
                "reportType" to reportType
            )
            val notificationData = mapOf(
                "data" to dataMap,
                "isRead" to false,
                "message" to "$friendName ha reportado un incidente de $reportType",
                "title" to "Nuevo reporte de amigo",
                "type" to "friendReport",
                "userId" to friendId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(friendId).collection("notifications").add(notificationData)
                .addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Notificación de reporte enviada a $friendId", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error al crear notificación de reporte: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }.addOnFailureListener { e ->
            if (isAdded) {
                Toast.makeText(requireContext(), "Error al obtener el nombre de usuario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveReportToFirestore(reporte: HashMap<String, Any?>) {
        db.collection("reportes")
            .add(reporte)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Reporte enviado", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al enviar reporte", Toast.LENGTH_SHORT).show()
                // Rehabilitar el botón de enviar en caso de error
                btnEnviarReporte.isEnabled = true
            }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    data?.let {
                        if (it.clipData != null) {
                            val count = it.clipData!!.itemCount
                            for (i in 0 until count) {
                                val imageUri = it.clipData!!.getItemAt(i).uri
                                selectedImageUris.add(imageUri)
                            }
                            Toast.makeText(requireContext(), "$count imágenes seleccionadas", Toast.LENGTH_SHORT).show()
                        } else if (it.data != null) {
                            val imageUri = it.data!!
                            selectedImageUris.add(imageUri)
                            Toast.makeText(requireContext(), "1 imagen seleccionada", Toast.LENGTH_SHORT).show()
                        }
                        imagesAdapter.notifyDataSetChanged()
                        recyclerViewImages.visibility = View.VISIBLE
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val photo: Bitmap = data?.extras?.get("data") as Bitmap
                    val uri = getImageUriFromBitmap(photo)
                    selectedImageUris.add(uri)
                    Toast.makeText(requireContext(), "Foto tomada", Toast.LENGTH_SHORT).show()
                    imagesAdapter.notifyDataSetChanged()
                    recyclerViewImages.visibility = View.VISIBLE
                }
                LOCATION_PICKER_REQUEST -> {
                    selectedLatitude = data?.getDoubleExtra("latitude", 0.0)
                    selectedLongitude = data?.getDoubleExtra("longitude", 0.0)
                    selectedLocationName = data?.getStringExtra("locationName")
                    txtUbicacion.text = selectedLocationName ?: "Ubicación seleccionada"

                    // Cambiar color del texto de txtUbicacion
                    if (selectedLocationName == null) {
                        txtUbicacion.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                    } else {
                        txtUbicacion.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
                    }
                }
            }
        }
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(requireContext().contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    class ImagesAdapter(
        private val imageUris: List<Uri>,
        private val onRemoveImage: (Uri) -> Unit
    ) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.imageView)
            val btnRemove: ImageView = view.findViewById(R.id.btnRemove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val uri = imageUris[position]
            holder.imageView.setImageURI(uri)
            holder.btnRemove.setOnClickListener {
                onRemoveImage(uri)
            }
        }

        override fun getItemCount(): Int {
            return imageUris.size
        }
    }

}