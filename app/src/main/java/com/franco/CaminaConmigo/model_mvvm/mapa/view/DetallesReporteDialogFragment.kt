package com.franco.CaminaConmigo.model_mvvm.mapa.view

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class DetallesReporteDialogFragment : DialogFragment() {

    // Declaración de variables para la vista
    private lateinit var txtTipo: TextView
    private lateinit var txtDescripcion: TextView
    private lateinit var txtFechaHora: TextView
    private lateinit var btnLike: Button
    private lateinit var btnCompartir: Button
    private lateinit var recyclerComentarios: RecyclerView
    private lateinit var edtComentario: EditText
    private lateinit var btnEnviarComentario: Button
    private lateinit var mapView: MapView
    private lateinit var imgIconoReporte: ImageView

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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            reportId = it.getString("reportId") ?: ""
            type = it.getString("type") ?: ""
            description = it.getString("description") ?: ""
            timestamp = it.getSerializable("timestamp") as Date?
            likes = it.getInt("likes")
        }

        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_detalles_reporte, null)

        // Inicialización de vistas
        txtTipo = view.findViewById(R.id.txtTipo)
        txtDescripcion = view.findViewById(R.id.txtDescripcion)
        txtFechaHora = view.findViewById(R.id.txtFechaHora)
        btnLike = view.findViewById(R.id.btnLike)
        btnCompartir = view.findViewById(R.id.btnCompartir)
        recyclerComentarios = view.findViewById(R.id.recyclerComentarios)
        edtComentario = view.findViewById(R.id.edtComentario)
        btnEnviarComentario = view.findViewById(R.id.btnEnviarComentario)
        mapView = view.findViewById(R.id.mapView)
        imgIconoReporte = view.findViewById(R.id.imgIconoReporte)

        // Asigna los valores a los elementos de la vista
        txtTipo.text = type
        txtDescripcion.text = description
        txtFechaHora.text = timestamp?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) } ?: "Fecha desconocida"
        btnLike.text = "Me gusta ($likes)"

        // Configuración del RecyclerView
        recyclerComentarios.layoutManager = LinearLayoutManager(requireContext())
        comentariosAdapter = ComentariosAdapter(comentarios)
        recyclerComentarios.adapter = comentariosAdapter

        // Configuración del MapView
        mapView.onCreate(savedInstanceState)
        // Asegúrate de que mapView esté inicializado en el método onCreateDialog
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


        // Asigna el ícono según el tipo de reporte
        when (type) {
            "Reunión de Hombres" -> imgIconoReporte.setImageResource(R.drawable.icon_reunion)
            "Puntos ciegos" -> imgIconoReporte.setImageResource(R.drawable.icon_puntosciegos)
            "Presencia de bares" -> imgIconoReporte.setImageResource(R.drawable.icono_bares)
            "Veredas en mal estado" -> imgIconoReporte.setImageResource(R.drawable.icono_veredas)
            "Poca Iluminación" -> imgIconoReporte.setImageResource(R.drawable.icono_iluminacion)
            "Vegetación abundante" -> imgIconoReporte.setImageResource(R.drawable.icono_vegetacion)
            "Espacios Abandonados" -> imgIconoReporte.setImageResource(R.drawable.icon_abandonados)
            "Agresión fisica" -> imgIconoReporte.setImageResource(R.drawable.icon_agre_fisica)
            "Agresión Sexual" -> imgIconoReporte.setImageResource(R.drawable.icon_agre_sexual)
            "Agresión verbal" -> imgIconoReporte.setImageResource(R.drawable.icon_agre_verbal)
            "Falta de baños" -> imgIconoReporte.setImageResource(R.drawable.icon_faltabanos)
            "Mobiliario inadecuado" -> imgIconoReporte.setImageResource(R.drawable.icon_mobiliario)
            "Persona en situación de calle" -> imgIconoReporte.setImageResource(R.drawable.icon_situacioncalle)
            else -> imgIconoReporte.setImageResource(R.drawable.ic_anadir)
        }


        // Configuración de los botones
        btnLike.setOnClickListener { darLike() }
        btnCompartir.setOnClickListener { compartirReporte() }
        btnEnviarComentario.setOnClickListener { enviarComentario() }

        // Botón cerrar
        val btnCerrar: TextView = view.findViewById(R.id.btnCerrar)
        btnCerrar.setOnClickListener {
            dismiss()  // Cierra el DialogFragment
        }

        // Cargar los comentarios
        cargarComentarios()

        builder.setView(view)
        return builder.create()
    }

    private fun cargarComentarios() {
        db.collection("reportes").document(reportId).collection("comentarios")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val nuevosComentarios = snapshot.documents.map { document ->
                        Comentario(
                            authorId = document.getString("authorId") ?: "",
                            authorName = document.getString("authorName") ?: "Anónimo",
                            text = document.getString("text") ?: "",
                            timestamp = document.getDate("timestamp")
                        )
                    }
                    comentariosAdapter.actualizarLista(nuevosComentarios)
                }
            }
    }

    private fun enviarComentario() {
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
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al agregar comentario", Toast.LENGTH_SHORT).show()
            }
    }

    private fun darLike() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Inicia sesión para dar like", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si el usuario ya ha dado like
        db.collection("reportes").document(reportId).collection("likes")
            .document(user.uid) // Usamos el ID del usuario para identificar si ya dio like
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
    }

    private fun agregarLike(userId: String) {
        // Agregar un like a la subcolección de likes
        db.collection("reportes").document(reportId).collection("likes")
            .document(userId)
            .set(hashMapOf("userId" to userId))
            .addOnSuccessListener {
                // Actualizar el contador de likes
                db.collection("reportes").document(reportId)
                    .update("likes", likes + 1)
                    .addOnSuccessListener {
                        likes++
                        btnLike.text = "Me gusta ($likes)"
                    }
            }
    }

    private fun eliminarLike(userId: String) {
        // Eliminar el like del usuario
        db.collection("reportes").document(reportId).collection("likes")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                // Actualizar el contador de likes
                db.collection("reportes").document(reportId)
                    .update("likes", likes - 1)
                    .addOnSuccessListener {
                        likes--
                        btnLike.text = "Me gusta ($likes)"
                    }
            }
    }

    private fun compartirReporte() {
        val shareText = "Reporte: $type\nDescripción: $description\nPublicado el: ${txtFechaHora.text}"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir reporte"))
    }
}

data class Comentario(val authorId: String, val authorName: String, val text: String, val timestamp: Date?)

class ComentariosAdapter(private val comentarios: MutableList<Comentario>) : RecyclerView.Adapter<ComentariosAdapter.ComentarioViewHolder>() {

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
    }

    override fun getItemCount() = comentarios.size

    class ComentarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtAutor: TextView = itemView.findViewById(R.id.txtAutor)
        private val txtComentario: TextView = itemView.findViewById(R.id.txtComentario)
        private val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)

        fun bind(comentario: Comentario) {
            txtAutor.text = comentario.authorName
            txtComentario.text = comentario.text
            txtFecha.text = comentario.timestamp?.let {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
            } ?: "Fecha desconocida"
        }
    }
}
