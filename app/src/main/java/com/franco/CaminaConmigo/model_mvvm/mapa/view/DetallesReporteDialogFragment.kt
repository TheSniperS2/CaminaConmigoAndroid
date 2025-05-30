package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.inicio.view.MainActivity
import com.franco.CaminaConmigo.model_mvvm.mapa.model.Comentario
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class DetallesReporteDialogFragment : BottomSheetDialogFragment() {

    // Declaración de variables para la vista
    private lateinit var txtTipo: TextView
    private lateinit var txtDescripcion: TextView
    private lateinit var txtFechaHora: TextView
    private lateinit var txtLikes: TextView
    private lateinit var txtCompartir: TextView
    private lateinit var recyclerComentarios: RecyclerView
    private lateinit var edtComentario: EditText
    private lateinit var btnEnviarComentario: TextView
    private lateinit var mapView: MapView
    private lateinit var imgLike: ImageView
    private lateinit var imgIconoReporte: ImageView
    private lateinit var likeContainer: LinearLayout
    private lateinit var imgReporte: ImageView
    private lateinit var imgAnterior: ImageView
    private lateinit var imgSiguiente: ImageView
    private lateinit var imageContainer: FrameLayout
    private var imageUrls: List<String> = emptyList()
    private var currentImageIndex = 0

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val comentarios = mutableListOf<Comentario>()
    private lateinit var comentariosAdapter: ComentariosAdapter

    // Variables para los detalles del reporte
    private lateinit var reportId: String
    private lateinit var type: String
    private lateinit var description: String
    private var timestamp: Date? = null
    private var likes: Int = 0

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    companion object {
        fun newInstance(reportId: String, type: String, description: String, timestamp: Date?, likes: Int): DetallesReporteDialogFragment {
            val fragment = DetallesReporteDialogFragment()
            val args = Bundle()
            args.putString("reportId", reportId)
            args.putString("type", type)
            args.putString("description", description)
            args.putSerializable("timestamp", timestamp)
            args.putInt("likes", likes)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            reportId = it.getString("reportId") ?: ""
            type = it.getString("type") ?: ""
            description = it.getString("description") ?: ""
            timestamp = it.getSerializable("timestamp") as Date?
            likes = it.getInt("likes")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detalles_reporte, container, false)

        // Inicialización de vistas
        txtTipo = view.findViewById(R.id.txtTipo)
        txtDescripcion = view.findViewById(R.id.txtDescripcion)
        txtFechaHora = view.findViewById(R.id.txtFechaHora)
        txtLikes = view.findViewById(R.id.txtLikes)
        txtCompartir = view.findViewById(R.id.txtCompartir)
        recyclerComentarios = view.findViewById(R.id.recyclerComentarios)
        edtComentario = view.findViewById(R.id.edtComentario)
        btnEnviarComentario = view.findViewById(R.id.btnEnviarComentario)
        mapView = view.findViewById(R.id.mapView)
        imgIconoReporte = view.findViewById(R.id.imgIconoReporte)
        imgLike = view.findViewById(R.id.imgLike)
        likeContainer = view.findViewById(R.id.likeContainer)
        imgReporte = view.findViewById(R.id.imgReporte)

        // Asigna los valores a los elementos de la vista
        txtTipo.text = type
        txtDescripcion.text = description
        txtFechaHora.text = timestamp?.let { getTimeAgo(Timestamp(it)) } ?: "Fecha desconocida"
        txtLikes.text = "$likes Me gusta"

        // Verificar el estado del "like" al cargar el reporte
        verificarEstadoLike()

        // Configuración del RecyclerView
        recyclerComentarios.layoutManager = LinearLayoutManager(requireContext())
        comentariosAdapter = ComentariosAdapter(requireContext(), comentarios)
        recyclerComentarios.adapter = comentariosAdapter

        // Inicializar el mapa
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { googleMap ->
            val reportRef = db.collection("reportes").document(reportId)

            // Obtener los datos de latitud y longitud del reporte
            reportRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val latitude = document.getDouble("latitude") ?: return@addOnSuccessListener
                    val longitude = document.getDouble("longitude") ?: return@addOnSuccessListener

                    val location = LatLng(latitude, longitude)

                    // Mover la cámara a la ubicación del reporte
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))

                    // Agregar un marcador en la ubicación del reporte
                    googleMap.addMarker(MarkerOptions().position(location).title("Ubicación del reporte"))
                }
            }
        }

        // Verificar si el reporte tiene imágenes
// Verificar si el reporte tiene imágenes
        val reportRef = db.collection("reportes").document(reportId)
        reportRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                imageUrls = document.get("imageUrls") as? List<String> ?: emptyList()
                if (imageUrls.isNotEmpty()) {
                    // Mostrar la primera imagen
                    imgReporte.visibility = View.VISIBLE
                    imageContainer.visibility = View.VISIBLE
                    mapView.visibility = View.GONE
                    Glide.with(this).load(imageUrls[0]).into(imgReporte)

                    // Configurar visibilidad de botones de navegación
                    if (imageUrls.size > 1) {
                        imgAnterior.visibility = View.VISIBLE
                        imgSiguiente.visibility = View.VISIBLE
                    } else {
                        imgAnterior.visibility = View.GONE
                        imgSiguiente.visibility = View.GONE
                    }
                } else {
                    // Mostrar el mapa
                    imgReporte.visibility = View.GONE
                    imageContainer.visibility = View.GONE
                    mapView.visibility = View.VISIBLE
                }
            }
        }

        // Asigna el ícono según el tipo de reporte
        when (type) {
            "Reunión de hombres" -> imgIconoReporte.setImageResource(R.drawable.icon_reunion)
            "Puntos Ciegos" -> imgIconoReporte.setImageResource(R.drawable.icon_puntosciegos)
            "Presencia de Bares y Restobares" -> imgIconoReporte.setImageResource(R.drawable.icono_bares)
            "Veredas en mal estado" -> imgIconoReporte.setImageResource(R.drawable.icono_veredas)
            "Poca Iluminación" -> imgIconoReporte.setImageResource(R.drawable.icono_iluminacion)
            "Vegetación Abundante" -> imgIconoReporte.setImageResource(R.drawable.icono_vegetacion)
            "Espacios Abandonados" -> imgIconoReporte.setImageResource(R.drawable.icon_abandonados)
            "Agresión Física" -> imgIconoReporte.setImageResource(R.drawable.icon_agre_fisica)
            "Agresión Sexual" -> imgIconoReporte.setImageResource(R.drawable.icon_agre_sexual)
            "Agresión Verbal" -> imgIconoReporte.setImageResource(R.drawable.icon_agre_verbal)
            "Falta de Baños Públicos" -> imgIconoReporte.setImageResource(R.drawable.icon_faltabanos)
            "Mobiliario Inadecuado" -> imgIconoReporte.setImageResource(R.drawable.icon_mobiliario)
            "Personas en situación de calle" -> imgIconoReporte.setImageResource(R.drawable.icon_situacioncalle)
            else -> imgIconoReporte.setImageResource(R.drawable.ic_anadir)
        }

        // Configuración de los botones
        likeContainer.setOnClickListener { darLike() }
        txtCompartir.setOnClickListener { compartirReporte() }
        btnEnviarComentario.setOnClickListener { enviarComentario() }
        imgAnterior = view.findViewById(R.id.imgAnterior)
        imgSiguiente = view.findViewById(R.id.imgSiguiente)
        imageContainer = view.findViewById(R.id.imageContainer)
        imgAnterior.setOnClickListener { mostrarImagenAnterior() }
        imgSiguiente.setOnClickListener { mostrarImagenSiguiente() }

        // Botón cerrar
        val btnCerrar: TextView = view.findViewById(R.id.btnCerrar)
        btnCerrar.setOnClickListener {
            dismiss()  // Cierra el DialogFragment
        }

        // Cargar los comentarios
        cargarComentarios()

        // Ajustar la vista cuando el teclado se muestra
        adjustViewForKeyboard(view)

        return view
    }

    private fun adjustViewForKeyboard(view: View) {
        val rootView = view.findViewById<View>(R.id.rootLayout)
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - rect.bottom
                if (keypadHeight > screenHeight * 0.15) { // Teclado visible
                    rootView.findViewById<LinearLayout>(R.id.commentSection).translationY = -keypadHeight.toFloat()
                } else { // Teclado oculto
                    rootView.findViewById<LinearLayout>(R.id.commentSection).translationY = 0f
                }
            }
        })
    }

    private fun mostrarImagenAnterior() {
        if (imageUrls.isNotEmpty()) {
            currentImageIndex = (currentImageIndex - 1 + imageUrls.size) % imageUrls.size
            Glide.with(this).load(imageUrls[currentImageIndex]).into(imgReporte)
        }
    }

    private fun mostrarImagenSiguiente() {
        if (imageUrls.isNotEmpty()) {
            currentImageIndex = (currentImageIndex + 1) % imageUrls.size
            Glide.with(this).load(imageUrls[currentImageIndex]).into(imgReporte)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val layoutParams = it.layoutParams as CoordinatorLayout.LayoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.topMargin = 50 // Ajusta este valor según el margen que desees
                it.layoutParams = layoutParams

                bottomSheetBehavior = BottomSheetBehavior.from(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                bottomSheetBehavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels - layoutParams.topMargin
                bottomSheetBehavior.isFitToContents = true
                bottomSheetBehavior.skipCollapsed = true
            }
        }
        return dialog
    }

    private fun cargarComentarios() {
        db.collection("reportes").document(reportId).collection("comentarios")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Ordenar en orden descendente
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val nuevosComentarios = snapshot.documents.map { document ->
                        com.franco.CaminaConmigo.model_mvvm.mapa.model.Comentario(
                            id = document.id, // Asegúrate de obtener el ID del documento
                            authorId = document.getString("authorId") ?: "",
                            authorName = document.getString("authorName") ?: "Anónimo",
                            text = document.getString("text") ?: "",
                            timestamp = document.getDate("timestamp"),
                            reportId = reportId // Asegúrate de pasar el reportId
                        )
                    }
                    comentariosAdapter.actualizarLista(nuevosComentarios)
                }
            }
    }

    private fun enviarComentario() {
        if (isUserAuthenticated()) {
            val text = edtComentario.text.toString().trim()
            if (text.isEmpty()) return

            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            val comentario = hashMapOf(
                "authorId" to (user?.uid ?: "Anónimo"),
                "authorName" to (user?.displayName ?: "Anónimo"),
                "reportId" to reportId,
                "text" to text,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            db.collection("reportes").document(reportId).collection("comentarios")
                .add(comentario)
                .addOnSuccessListener {
                    edtComentario.text.clear()
                    Toast.makeText(requireContext(), "Comentario agregado", Toast.LENGTH_SHORT).show()
                    // Enviar notificación
                    user?.let {
                        db.collection("reportes").document(reportId).get().addOnSuccessListener { document ->
                            val reportOwnerId = document.getString("userId") ?: ""
                            db.collection("users").document(it.uid).get().addOnSuccessListener { userDoc ->
                                val commentAuthorUsername = userDoc.getString("username") ?: "Anónimo"
                                createCommentNotification(it.uid, commentAuthorUsername, text, reportId, reportOwnerId)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al agregar comentario", Toast.LENGTH_SHORT).show()
                }
        } else {
            showSignInDialog()
        }
    }

    private fun createCommentNotification(commentAuthorId: String, commentAuthorName: String, commentText: String, reportId: String, reportOwnerId: String) {
        // Verificar si el autor del comentario es el mismo que el dueño del reporte
        if (commentAuthorId == reportOwnerId) {
            return // No enviar notificación si el autor del comentario es el mismo que el dueño del reporte
        }

        val dataMap = mapOf(
            "commentAuthorId" to commentAuthorId,
            "commentAuthorName" to commentAuthorName,
            "commentText" to commentText,
            "reportId" to reportId
        )
        val notificationData = mapOf(
            "data" to dataMap,
            "isRead" to false,
            "message" to "$commentAuthorName comentó en tu reporte: $commentText",
            "title" to "Nuevo comentario",
            "type" to "reportComment",
            "userId" to reportOwnerId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(reportOwnerId).collection("notifications").add(notificationData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Notificación enviada al autor del reporte", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al enviar notificación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun darLike() {
        if (isUserAuthenticated()) {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            // Verificar si el usuario ya ha dado like
            db.collection("reportes").document(reportId).collection("likes")
                .document(user!!.uid) // Usamos el ID del usuario para identificar si ya dio like
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Si el usuario ya dio like, lo eliminamos
                        eliminarLike(user.uid)
                    } else {
                        // Si el usuario no ha dado like, lo agregamos
                        agregarLike(user.uid)
                    }
                }
        } else {
            showSignInDialog()
        }
    }

    private fun isUserAuthenticated(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun showSignInDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Iniciar Sesión Requerido")
        builder.setMessage("Para acceder a esta funcionalidad, por favor inicia sesión.")
        builder.setPositiveButton("Iniciar Sesión") { _, _ ->
            // Redirigir a la pantalla de inicio de sesión
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun agregarLike(userId: String) {
        // Agregar un like a la subcolección de likes
        db.collection("reportes").document(reportId).collection("likes")
            .document(userId)
            .set(hashMapOf("userId" to userId))
            .addOnSuccessListener {
                // Actualizar el contador de likes y cambiar el ícono del corazón
                likes++
                txtLikes.text = "$likes Me gusta"
                imgLike.setImageResource(R.drawable.ic_corazon_lleno)
                actualizarContadorLikes()
            }
    }

    private fun eliminarLike(userId: String) {
        // Eliminar el like del usuario
        db.collection("reportes").document(reportId).collection("likes")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                // Actualizar el contador de likes y cambiar el ícono del corazón
                likes--
                txtLikes.text = "$likes Me gusta"
                imgLike.setImageResource(R.drawable.ic_corazon_vacio)
                actualizarContadorLikes()
            }
    }

    private fun actualizarContadorLikes() {
        db.collection("reportes").document(reportId)
            .update("likes", likes)
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al actualizar el contador de likes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verificarEstadoLike() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            db.collection("reportes").document(reportId).collection("likes")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        imgLike.setImageResource(R.drawable.ic_corazon_lleno)
                    } else {
                        imgLike.setImageResource(R.drawable.ic_corazon_vacio)
                    }
                }
        }
    }

    private fun compartirReporte() {
        val shareText = "com.franco.CaminaConmigo.model_mvvm.novedad.model.Reporte: $type\nDescripción: $description\nPublicado el: ${txtFechaHora.text}"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir reporte"))
    }

    private fun getTimeAgo(timestamp: Timestamp): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp.toDate().time

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 60 -> "hace un momento"
            minutes < 60 -> "hace $minutes ${if (minutes == 1L) "minuto" else "minutos"}"
            hours < 24 -> "hace $hours ${if (hours == 1L) "hora" else "horas"}"
            days < 7 -> "hace $days ${if (days == 1L) "día" else "días"}"
            days < 30 -> "hace ${days / 7} ${if (days / 7 == 1L) "semana" else "semanas"}"
            days < 365 -> "hace ${days / 30} ${if (days / 30 == 1L) "mes" else "meses"}"
            else -> "hace ${days / 365} ${if (days / 365 == 1L) "año" else "años"}"
        }
    }
}

data class Comentario(val authorId: String, val authorName: String, val text: String, val timestamp: Date?)

class ComentariosAdapter(
    private val context: Context,
    private val comentarios: MutableList<Comentario>
) : RecyclerView.Adapter<ComentariosAdapter.ComentarioViewHolder>() {

    fun actualizarLista(nuevosComentarios: List<Comentario>) {
        comentarios.clear()
        comentarios.addAll(nuevosComentarios)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comentario, parent, false)
        return ComentarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        val comentario = comentarios[position]
        holder.bind(comentario)

        // Configurar el botón de borrar
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null && user.uid == comentario.authorId) {
            holder.imgBorrar.visibility = View.VISIBLE
            holder.imgBorrar.setOnClickListener {
                borrarComentario(comentario)
            }
        } else {
            holder.imgBorrar.visibility = View.GONE
        }
    }

    override fun getItemCount() = comentarios.size

    class ComentarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtAutor: TextView = itemView.findViewById(R.id.txtAutor)
        private val txtComentario: TextView = itemView.findViewById(R.id.txtComentario)
        private val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        val imgBorrar: ImageView = itemView.findViewById(R.id.imgBorrar)

        fun bind(comentario: Comentario) {
            txtAutor.text = comentario.authorName
            txtComentario.text = comentario.text
            txtFecha.text = comentario.timestamp?.let {
                getTimeAgo(Timestamp(it))
            } ?: "Fecha desconocida"
        }
        private fun getTimeAgo(timestamp: Timestamp): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp.toDate().time

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                seconds < 60 -> "hace un momento"
                minutes < 60 -> "hace $minutes ${if (minutes == 1L) "minuto" else "minutos"}"
                hours < 24 -> "hace $hours ${if (hours == 1L) "hora" else "horas"}"
                days < 7 -> "hace $days ${if (days == 1L) "día" else "días"}"
                days < 30 -> "hace ${days / 7} ${if (days / 7 == 1L) "semana" else "semanas"}"
                days < 365 -> "hace ${days / 30} ${if (days / 30 == 1L) "mes" else "meses"}"
                else -> "hace ${days / 365} ${if (days / 365 == 1L) "año" else "años"}"
            }
        }
    }

    private fun borrarComentario(comentario: Comentario) {
        // Lógica para borrar el comentario de la base de datos
        val db = FirebaseFirestore.getInstance()
        db.collection("reportes").document(comentario.reportId).collection("comentarios")
            .document(comentario.id)
            .delete()
            .addOnSuccessListener {
                comentarios.remove(comentario)
                notifyDataSetChanged()
                Toast.makeText(context, "Comentario borrado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al borrar comentario", Toast.LENGTH_SHORT).show()
            }
    }
}