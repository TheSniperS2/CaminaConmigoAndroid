package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ActivityChatDetailBinding
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.ChatViewModel
import com.franco.CaminaConmigo.model_mvvm.mapa.view.MapaActivity
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity
import com.franco.CaminaConmigo.model_mvvm.novedad.view.NovedadActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private val viewModel: ChatViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isSharingLocation = false
    private var isGroupChat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chatId = intent.getStringExtra("CHAT_ID")
        if (chatId == null) {
            Log.e("ChatDetailActivity", "Chat ID es nulo, no se pueden cargar mensajes")
            return
        }

        Log.d("ChatDetailActivity", "Cargando mensajes para chat ID: $chatId")

        // Configura RecyclerView
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)

        // Actualizar la imagen y el nombre del contacto
        updateContactInfo(chatId)

        val currentUserId = auth.currentUser?.uid ?: return
        val btnMapa = findViewById<ImageButton>(R.id.imageButton10)
        val btnNovedades = findViewById<ImageButton>(R.id.imageButton11)
        val btnAyuda = findViewById<ImageButton>(R.id.imageButton13)
        val btnMenu = findViewById<ImageButton>(R.id.imageButton14)
        val btnCall = findViewById<ImageButton>(R.id.btnCall)

        btnMapa.setOnClickListener {
            startActivity(Intent(this, MapaActivity::class.java))
        }

        btnNovedades.setOnClickListener {
            startActivity(Intent(this, NovedadActivity::class.java))
        }

        btnAyuda.setOnClickListener {
            startActivity(Intent(this, AyudaActivity::class.java))
        }

        btnMenu.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        // Observa los userNames y crea el adaptador cuando estén disponibles


            // Observa los mensajes y actualiza el adaptador
            viewModel.messages.observe(this, Observer { messages ->
                Log.d("ChatDetailActivity", "Mensajes actualizados en UI: ${messages.size}")
                val adapter = MessageAdapter(auth.currentUser?.uid ?: "")
                binding.recyclerViewMessages.adapter = adapter
                adapter.submitList(messages)
                binding.recyclerViewMessages.scrollToPosition(messages.size - 1) // Desplazar al último mensaje
            })


        // Carga los mensajes y los userNames
        viewModel.loadMessages(chatId)
        viewModel.loadLocationMessages(chatId) // Cargar mensajes de ubicación
        viewModel.loadChats()

        // Verificar el estado de compartir ubicación
        checkLocationSharingStatus()

        // Enviar mensaje
        binding.btnSend.setOnClickListener {
            val messageContent = binding.etMessage.text.toString()
            if (messageContent.isNotEmpty()) {
                val message = Message(
                    senderId = auth.currentUser?.uid ?: "",
                    content = messageContent,
                    timestamp = Timestamp.now(),
                    isActive = true
                )
                viewModel.sendMessage(chatId, message)
                binding.etMessage.text.clear()
            }
        }

        // Configurar el menú desplegable para btnOptions
        val btnOptions = findViewById<ImageButton>(R.id.btnOptions)
        btnOptions.setOnClickListener { view ->
            val chatId = intent.getStringExtra("CHAT_ID") ?: return@setOnClickListener
            viewModel.isGroupChat(chatId) { isGroup ->
                isGroupChat = isGroup
                showPopupMenu(view, chatId)
            }
        }



        // Configurar el botón para compartir ubicación
        btnCall.setOnClickListener {
            showLocationSharingDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.recyclerViewMessages.adapter?.let { adapter ->
            for (i in 0 until adapter.itemCount) {
                val holder = binding.recyclerViewMessages.findViewHolderForAdapterPosition(i)
                if (holder is LocationMessageViewHolder) {
                    holder.onResume()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.recyclerViewMessages.adapter?.let { adapter ->
            for (i in 0 until adapter.itemCount) {
                val holder = binding.recyclerViewMessages.findViewHolderForAdapterPosition(i)
                if (holder is LocationMessageViewHolder) {
                    holder.onPause()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.recyclerViewMessages.adapter?.let { adapter ->
            for (i in 0 until adapter.itemCount) {
                val holder = binding.recyclerViewMessages.findViewHolderForAdapterPosition(i)
                if (holder is LocationMessageViewHolder) {
                    holder.onDestroy()
                }
            }
        }
    }

    private fun showPopupMenu(view: View, chatId: String) {
        val popupMenu = PopupMenu(this, view)
        viewModel.isGroupChat(chatId) { isGroup ->
            if (isGroup) {
                popupMenu.menuInflater.inflate(R.menu.menu_group_options, popupMenu.menu)
            } else {
                popupMenu.menuInflater.inflate(R.menu.menu_options, popupMenu.menu)
            }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_change_nickname -> {
                        if (!isGroup) {
                            showChangeNicknameDialog(chatId)
                        } else {
                            Toast.makeText(this, "Esta opción no está disponible para chats de grupo", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.action_edit_group_name -> {
                        if (isGroup) {
                            showEditGroupNameDialog(chatId)
                        } else {
                            Toast.makeText(this, "Esta opción solo está disponible para chats de grupo", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.action_add_participants -> {
                        if (isGroup) {
                            showAddParticipantsDialog(chatId)
                        } else {
                            Toast.makeText(this, "Esta opción solo está disponible para chats de grupo", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.action_manage_members -> {
                        if (isGroup) {
                            showManageMembersDialog(chatId)
                        } else {
                            Toast.makeText(this, "Esta opción solo está disponible para chats de grupo", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    private fun showEditGroupNameDialog(chatId: String) {
        val editText = EditText(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar nombre del grupo")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotEmpty()) {
                    viewModel.updateGroupName(chatId, newName)
                    binding.tvContactName.text = newName
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()

        editText.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showAddParticipantsDialog(chatId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_participants, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextParticipants)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Añadir participantes")
            .setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                val newParticipants = editText.text.toString().split(",").map { it.trim() }
                if (newParticipants.isNotEmpty()) {
                    viewModel.addParticipants(chatId, newParticipants)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()

        editText.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showManageMembersDialog(chatId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manage_members, null)
        val builder = AlertDialog.Builder(this)
            .setTitle("Administrar miembros")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .create()

        val listView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewMembers)
        listView.layoutManager = LinearLayoutManager(this)

        val adapter = MembersAdapter(viewModel, db, chatId)
        listView.adapter = adapter

        viewModel.loadChatById(chatId) { chat ->
            if (chat != null) {
                adapter.submitList(chat.participants)
            }
        }

        dialogView.findViewById<Button>(R.id.btnAddAdmin).setOnClickListener {
            val editText = EditText(this)
            val addAdminDialog = AlertDialog.Builder(this)
                .setTitle("Añadir administrador")
                .setView(editText)
                .setPositiveButton("Añadir") { _, _ ->
                    val username = editText.text.toString().trim()
                    if (username.isNotEmpty()) {
                        viewModel.addAdmin(chatId, username)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .create()
            addAdminDialog.show()
        }

        dialogView.findViewById<Button>(R.id.btnRemoveAdmin).setOnClickListener {
            val editText = EditText(this)
            val removeAdminDialog = AlertDialog.Builder(this)
                .setTitle("Remover administrador")
                .setView(editText)
                .setPositiveButton("Remover") { _, _ ->
                    val username = editText.text.toString().trim()
                    if (username.isNotEmpty()) {
                        viewModel.removeAdmin(chatId, username)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .create()
            removeAdminDialog.show()
        }

        builder.show()
    }

    class MembersAdapter(
        private val viewModel: ChatViewModel,
        private val db: FirebaseFirestore,
        private val chatId: String
    ) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

        private var membersList: List<String> = emptyList()

        fun submitList(members: List<String>) {
            membersList = members
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
            return MemberViewHolder(view, db, chatId, viewModel)
        }

        override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
            val memberId = membersList[position]
            holder.bind(memberId)
        }

        override fun getItemCount(): Int = membersList.size

        inner class MemberViewHolder(
            itemView: View,
            private val db: FirebaseFirestore,
            private val chatId: String,
            private val viewModel: ChatViewModel
        ) : RecyclerView.ViewHolder(itemView) {
            private val memberNameTextView: TextView = itemView.findViewById(R.id.memberNameTextView)
            private val memberImageView: ImageView = itemView.findViewById(R.id.memberImageView)
            private val removeButton: Button = itemView.findViewById(R.id.removeButton)

            fun bind(memberId: String) {
                db.collection("users").document(memberId).get()
                    .addOnSuccessListener { document ->
                        val username = document.getString("username") ?: memberId
                        val photoURL = document.getString("photoURL")
                        memberNameTextView.text = username
                        if (!photoURL.isNullOrEmpty()) {
                            Glide.with(itemView.context)
                                .load(photoURL)
                                .circleCrop()
                                .into(memberImageView)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MembersAdapter", "Error al obtener nombre de usuario: ${e.message}")
                        memberNameTextView.text = memberId
                    }

                removeButton.setOnClickListener {
                    viewModel.removeParticipant(chatId, memberId)
                }
            }
        }
    }

    // Método para actualizar la imagen y el nombre del contacto
    private fun updateContactInfo(chatId: String) {
        db.collection("chats").document(chatId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val isGroup = document.getBoolean("isGroup") ?: false

                    if (isGroup) {
                        val groupName = document.getString("name") ?: "Grupo"
                        binding.tvContactName.text = groupName
                        binding.profileImage.setImageResource(R.drawable.ic_imagen) // Imagen de grupo predeterminada
                    } else {
                        val participants = document.get("participants") as? List<String> ?: emptyList()
                        val currentUserId = auth.currentUser?.uid ?: return@addOnSuccessListener
                        val friendId = participants.firstOrNull { it != currentUserId } ?: return@addOnSuccessListener
                        db.collection("users").document(friendId).get()
                            .addOnSuccessListener { userDocument ->
                                val contactName = userDocument.getString("username") ?: "Desconocido"
                                val photoURL = userDocument.getString("photoURL")

                                binding.tvContactName.text = contactName
                                if (!photoURL.isNullOrEmpty()) {
                                    Glide.with(this)
                                        .load(photoURL)
                                        .circleCrop()
                                        .into(binding.profileImage)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ChatDetailActivity", "Error al obtener información del usuario: ${e.message}")
                            }
                    }
                } else {
                    Log.e("ChatDetailActivity", "No se encontró el documento del chat")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatDetailActivity", "Error al obtener información del chat: ${e.message}")
            }
    }


    private fun showChangeNicknameDialog(friendId: String) {
        val editText = EditText(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Cambiar apodo")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newNickname = editText.text.toString()
                if (newNickname.isNotEmpty()) {
                    viewModel.updateNickname(friendId, newNickname)
                    binding.tvContactName.text = newNickname
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()

        editText.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showLocationSharingDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_location_sharing, null)
        builder.setView(dialogView)

        val title = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val description = dialogView.findViewById<TextView>(R.id.dialogDescription)
        val positiveButton = dialogView.findViewById<Button>(R.id.positiveButton)
        val negativeButton = dialogView.findViewById<Button>(R.id.negativeButton)

        if (isSharingLocation) {
            title.text = "Compartir ubicación"
            description.text = "¿Deseas dejar de compartir tu ubicación actual?"
            positiveButton.text = "Dejar de compartir"
            positiveButton.setTextColor(Color.RED)
            negativeButton.text = "Cancelar"
            negativeButton.setTextColor(Color.BLUE)
        } else {
            title.text = "Compartir ubicación"
            description.text = "¿Desea compartir tu ubicación actual? Se compartirá hasta que decidas detenerla."
            positiveButton.text = "Compartir"
            positiveButton.setTextColor(Color.BLUE)
            negativeButton.text = "Cancelar"
            negativeButton.setTextColor(Color.BLUE)
        }

        val dialog = builder.create()

        // 👇 Aquí forzamos los bordes redondeados
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        positiveButton.setOnClickListener {
            if (isSharingLocation) {
                isSharingLocation = false
                stopSharingLocation()
                Toast.makeText(this, "Dejó de compartir su ubicación", Toast.LENGTH_SHORT).show()
            } else {
                isSharingLocation = true
                shareLocation()
                Toast.makeText(this, "Comenzó a compartir su ubicación", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        negativeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun stopSharingLocation() {
        val chatId = intent.getStringExtra("CHAT_ID") ?: return
        val currentUser = auth.currentUser ?: return

        val locationSharingRef = db.collection("chats").document(chatId).collection("locationSharing")
        locationSharingRef.whereEqualTo("senderId", currentUser.uid)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshots ->
                for (document in snapshots.documents) {
                    locationSharingRef.document(document.id).update("isActive", false)
                        .addOnSuccessListener {
                            Log.d("ChatDetailActivity", "Ubicación dejada de compartir con éxito")
                            addLocationSharingMessage(chatId, "Dejó de compartir su ubicación")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatDetailActivity", "Error al dejar de compartir ubicación: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatDetailActivity", "Error al obtener mensajes de ubicación: ${e.message}")
            }
    }

    private fun addLocationSharingMessage(chatId: String, content: String) {
        val currentUser = auth.currentUser ?: return
        val message = hashMapOf(
            "content" to content,
            "isRead" to false,
            "senderId" to currentUser.uid,
            "timestamp" to Timestamp.now()
        )
        val messagesRef = db.collection("chats").document(chatId).collection("messages")
        messagesRef.add(message)
            .addOnSuccessListener {
                Log.d("ChatDetailActivity", "Mensaje de compartir ubicación agregado con éxito")
            }
            .addOnFailureListener { e ->
                Log.e("ChatDetailActivity", "Error al agregar mensaje de compartir ubicación: ${e.message}")
            }
    }

    private fun checkLocationSharingStatus() {
        val chatId = intent.getStringExtra("CHAT_ID") ?: return
        val currentUser = auth.currentUser ?: return

        val locationSharingRef = db.collection("chats").document(chatId).collection("locationSharing")
        locationSharingRef.whereEqualTo("senderId", currentUser.uid)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshots ->
                isSharingLocation = !snapshots.isEmpty
            }
            .addOnFailureListener { e ->
                Log.e("ChatDetailActivity", "Error al verificar estado de compartir ubicación: ${e.message}")
            }
    }

    private fun shareLocation() {
        val latitude = -29.913299580848747
        val longitude = -71.248106468252
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val chatId = intent.getStringExtra("CHAT_ID") ?: return
            val locationSharingRef = db.collection("chats").document(chatId).collection("locationSharing")

            locationSharingRef.whereEqualTo("senderId", currentUser.uid)
                .get()
                .addOnSuccessListener { snapshots ->
                    if (snapshots.isEmpty) {
                        // Crear un nuevo documento si no existe
                        val locationMessage = hashMapOf(
                            "isActive" to true,
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "senderId" to currentUser.uid,
                            "timestamp" to Timestamp.now()
                        )
                        locationSharingRef.add(locationMessage)
                            .addOnSuccessListener {
                                Log.d("ChatDetailActivity", "Ubicación compartida con éxito")
                                addLocationSharingMessage(chatId, "Comenzó a compartir su ubicación")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ChatDetailActivity", "Error al compartir ubicación: ${e.message}")
                            }
                    } else {
                        // Actualizar el documento existente
                        for (document in snapshots.documents) {
                            locationSharingRef.document(document.id).update(
                                mapOf(
                                    "isActive" to true,
                                    "latitude" to latitude,
                                    "longitude" to longitude,
                                    "timestamp" to Timestamp.now()
                                )
                            ).addOnSuccessListener {
                                Log.d("ChatDetailActivity", "Ubicación actualizada con éxito")
                                addLocationSharingMessage(chatId, "Comenzó a compartir su ubicación")
                            }.addOnFailureListener { e ->
                                Log.e("ChatDetailActivity", "Error al actualizar ubicación: ${e.message}")
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ChatDetailActivity", "Error al obtener mensajes de ubicación: ${e.message}")
                }
        } else {
            Log.e("ChatDetailActivity", "Usuario no autenticado, no se puede compartir la ubicación")
        }
    }
}