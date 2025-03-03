package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ActivityChatDetailBinding
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.ChatViewModel
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.LocationSharingViewModel
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
    private var selectedChatId: String? = null
    private var groupImageUri: Uri? = null
    private val locationSharingViewModel: LocationSharingViewModel by viewModels()

    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("CHAT_ID") ?: run {
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

        // Observa el estado de isActive y actualiza el icono de btnCall en consecuencia
        locationSharingViewModel.isActive.observe(this, Observer { isActive ->
            val iconRes = if (isActive) R.drawable.location_on_24px else R.drawable.location_off_24px
            btnCall.setImageResource(iconRes)
        })

        // Verificar el estado de compartir ubicación
        checkLocationSharingStatus(chatId)

        // Restaurar el estado de compartir ubicación si es necesario
        locationSharingViewModel.restoreSharingStateIfNeeded()

        // Enviar mensaje
        binding.btnSend.setOnClickListener {
            val messageContent = binding.etMessage.text.toString()
            if (messageContent.isNotEmpty()) {
                val message = Message(
                    senderId = auth.currentUser?.uid ?: "",
                    content = messageContent,
                    timestamp = Timestamp.now(),
                )
                viewModel.sendMessage(chatId, message)
                binding.etMessage.text.clear()
            }
        }


// Agrega un TextWatcher para ajustar el tamaño dinámicamente
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // No se necesita implementación aquí
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No se necesita implementación aquí
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val minLines = 2
                val maxLines = 5
                val lineHeight = binding.etMessage.lineHeight
                val lines = binding.etMessage.lineCount

                if (lines in minLines..maxLines) {
                    binding.etMessage.layoutParams.height = lineHeight * lines
                } else if (lines > maxLines) {
                    binding.etMessage.layoutParams.height = lineHeight * maxLines
                    binding.etMessage.isVerticalScrollBarEnabled = true
                } else {
                    binding.etMessage.layoutParams.height = lineHeight * minLines
                }
                binding.etMessage.requestLayout()
            }
        })

        // Configurar el menú desplegable para btnOptions
        val btnOptions = findViewById<ImageButton>(R.id.btnOptions)
        btnOptions.setOnClickListener { view ->
            viewModel.isGroupChat(chatId) { isGroup ->
                isGroupChat = isGroup
                showPopupMenu(view, chatId)
            }
        }

        // Configurar el botón para compartir ubicación
        btnCall.setOnClickListener {
            showLocationSharingDialog(chatId)
        }
    }


    private fun startSharingLocationWithPermission(chatId: String) {
        locationSharingViewModel.startSharingLocation(chatId)
        Toast.makeText(this, "Comenzó a compartir su ubicación", Toast.LENGTH_SHORT).show()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startSharingLocationWithPermission(chatId)
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
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
                viewModel.isAdmin(chatId) { isAdmin ->
                    popupMenu.menuInflater.inflate(R.menu.menu_group_options, popupMenu.menu)

                    // Solo permitir opciones adicionales para administradores
                    if (!isAdmin) {
                        popupMenu.menu.removeItem(R.id.action_edit_group_name)
                        popupMenu.menu.removeItem(R.id.action_add_participants)
                        popupMenu.menu.removeItem(R.id.action_manage_members)
                        popupMenu.menu.removeItem(R.id.action_change_group_image)
                    }

                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.action_change_nickname -> {
                                Toast.makeText(this, "Esta opción no está disponible para chats de grupo", Toast.LENGTH_SHORT).show()
                                true
                            }
                            R.id.action_edit_group_name -> {
                                showEditGroupNameDialog(chatId)
                                true
                            }
                            R.id.action_add_participants -> {
                                showAddParticipantsDialog(chatId)
                                true
                            }
                            R.id.action_manage_members -> {
                                showManageMembersDialog(chatId)
                                true
                            }
                            R.id.action_change_group_image -> {
                                selectGroupImage(chatId)
                                true
                            }
                            R.id.action_leave_group -> {
                                leaveGroup(chatId)
                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                }
            } else {
                popupMenu.menuInflater.inflate(R.menu.menu_options, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_change_nickname -> {
                            showChangeNicknameDialog(chatId)
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            }
        }
    }

    private fun selectGroupImage(chatId: String) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 2)
        selectedChatId = chatId
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            groupImageUri = data?.data
            binding.profileImage.setImageURI(groupImageUri)
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null && selectedChatId != null) {
                viewModel.uploadGroupImage(selectedChatId!!, imageUri) { success ->
                    if (success) {
                        Toast.makeText(this, "Imagen del grupo actualizada exitosamente.", Toast.LENGTH_SHORT).show()
                        // Recuperar y actualizar la URL de la imagen del grupo desde Firestore
                        db.collection("chats").document(selectedChatId!!).get()
                            .addOnSuccessListener { document ->
                                val groupURL = document.getString("groupURL") ?: ""
                                if (groupURL.isNotEmpty()) {
                                    Glide.with(this)
                                        .load(groupURL)
                                        .circleCrop()
                                        .into(binding.profileImage)
                                }
                            }
                    } else {
                        Toast.makeText(this, "Error al subir la imagen del grupo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    private fun leaveGroup(chatId: String) {
        AlertDialog.Builder(this)
            .setTitle("Salir del grupo")
            .setMessage("¿Estás seguro de que deseas salir del grupo?")
            .setPositiveButton("Salir") { _, _ ->
                viewModel.leaveGroup(chatId)
                finish() // Cerrar la actividad después de salir del grupo
            }
            .setNegativeButton("Cancelar", null)
            .create()
            .show()
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

        builder.show()
    }

    class MembersAdapter(
        private val viewModel: ChatViewModel,
        private val db: FirebaseFirestore,
        private val chatId: String
    ) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

        private var membersList: List<String> = emptyList()
        private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        fun submitList(members: List<String>) {
            membersList = members
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
            return MemberViewHolder(view, db, chatId, viewModel, this)
        }

        override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
            val memberId = membersList[position]
            holder.bind(memberId)
        }

        override fun getItemCount(): Int = membersList.size

        class MemberViewHolder(
            itemView: View,
            private val db: FirebaseFirestore,
            private val chatId: String,
            private val viewModel: ChatViewModel,
            private val adapter: MembersAdapter
        ) : RecyclerView.ViewHolder(itemView) {
            private val memberNameTextView: TextView = itemView.findViewById(R.id.memberNameTextView)
            private val memberImageView: ImageView = itemView.findViewById(R.id.memberImageView)
            private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

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

                itemView.setOnClickListener {
                    showMemberOptions(memberId, memberNameTextView.text.toString())
                }
            }

            private fun showMemberOptions(memberId: String, username: String) {
                if (memberId == currentUserId) {
                    // No permitir eliminar al usuario actual
                    return
                }

                val popupMenu = PopupMenu(itemView.context, itemView)
                popupMenu.menuInflater.inflate(R.menu.menu_member_options, popupMenu.menu)

                viewModel.isAdmin(chatId) { isAdmin ->
                    viewModel.isAdmin(chatId, username) { isMemberAdmin ->
                        popupMenu.menu.findItem(R.id.action_give_admin).isVisible = isAdmin && !isMemberAdmin
                        popupMenu.menu.findItem(R.id.action_remove_admin).isVisible = isAdmin && isMemberAdmin

                        popupMenu.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.action_give_admin -> {
                                    viewModel.addAdmin(chatId, username)
                                    adapter.reloadMembers()
                                    true
                                }
                                R.id.action_remove_admin -> {
                                    viewModel.removeAdmin(chatId, username)
                                    adapter.reloadMembers()
                                    true
                                }
                                R.id.action_remove_member -> {
                                    viewModel.removeParticipant(chatId, username)
                                    adapter.reloadMembers()
                                    true
                                }
                                else -> false
                            }
                        }
                        popupMenu.show()
                    }
                }
            }

            private fun MembersAdapter.reloadMembers() {
                viewModel.loadChatById(chatId) { chat ->
                    if (chat != null) {
                        submitList(chat.participants)
                    }
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
                        val groupURL = document.getString("groupURL") ?: ""
                        if (groupURL.isNotEmpty()) {
                            Glide.with(this)
                                .load(groupURL)
                                .circleCrop()
                                .into(binding.profileImage)
                        } else {
                            binding.profileImage.setImageResource(R.drawable.ic_imagen) // Imagen de grupo predeterminada
                        }
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

    // Métodos adicionales para gestionar el estado de compartir ubicación
    private fun showLocationSharingDialog(chatId: String) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_location_sharing, null)
        builder.setView(dialogView)

        val title = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val description = dialogView.findViewById<TextView>(R.id.dialogDescription)
        val positiveButton = dialogView.findViewById<Button>(R.id.positiveButton)
        val negativeButton = dialogView.findViewById<Button>(R.id.negativeButton)

        val isActive = locationSharingViewModel.isActive.value ?: false

        if (isActive) {
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
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        positiveButton.setOnClickListener {
            if (isActive) {
                locationSharingViewModel.stopSharingLocation(chatId)
                Toast.makeText(this, "Dejó de compartir su ubicación", Toast.LENGTH_SHORT).show()
            } else {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startSharingLocationWithPermission(chatId)
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
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
        locationSharingViewModel.stopSharingLocation(chatId)
        addLocationSharingMessage(chatId, "Dejó de compartir su ubicación")
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

    private fun checkLocationSharingStatus(chatId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        val locationSharingRef = db.collection("chats").document(chatId).collection("locationSharing")
        locationSharingRef.whereEqualTo("senderId", currentUserId)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshots ->
                isSharingLocation = !snapshots.isEmpty
                locationSharingViewModel.setActive(isSharingLocation)
                val iconRes = if (isSharingLocation) R.drawable.location_on_24px else R.drawable.location_off_24px
                findViewById<ImageButton>(R.id.btnCall).setImageResource(iconRes)
            }
            .addOnFailureListener { e ->
                Log.e("ChatDetailActivity", "Error al verificar estado de compartir ubicación: ${e.message}")
            }
    }

    private fun shareLocation() {
        val chatId = intent.getStringExtra("CHAT_ID") ?: return
        locationSharingViewModel.startSharingLocation(chatId)
        addLocationSharingMessage(chatId, "Comenzó a compartir su ubicación")
    }
}