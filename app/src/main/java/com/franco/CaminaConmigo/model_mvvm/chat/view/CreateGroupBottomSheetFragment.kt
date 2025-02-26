package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.databinding.FragmentCreateGroupBottomSheetBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.Friend
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.ChatViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class CreateGroupBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCreateGroupBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var groupImageUri: Uri? = null

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGroupBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val friendsAdapter = FriendsAdapter(emptyList()) { friend ->
            // Lógica para manejar la selección de amigos
        }
        binding.recyclerViewFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFriends.adapter = friendsAdapter

        loadFriends(friendsAdapter)

        binding.imageViewGroup.setOnClickListener {
            // Lógica para seleccionar una imagen del dispositivo
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        binding.buttonCreateGroup.setOnClickListener {
            val selectedFriends = friendsAdapter.getSelectedFriends()
            val groupName = binding.editTextGroupName.text.toString().trim()
            if (groupName.isEmpty()) {
                Toast.makeText(requireContext(), "Debes ingresar un nombre para el grupo.", Toast.LENGTH_SHORT).show()
            } else if (selectedFriends.size < 2) {
                Toast.makeText(requireContext(), "Debes seleccionar al menos 2 amigos para crear un grupo.", Toast.LENGTH_SHORT).show()
            } else {
                createGroup(selectedFriends, groupName, groupImageUri)
                dismiss() // Cerrar la ventana inmediatamente
            }
        }

        binding.btnCerrar.setOnClickListener {
            dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            groupImageUri = data?.data
            binding.imageViewGroup.setImageURI(groupImageUri)
        }
    }

    private fun loadFriends(friendsAdapter: FriendsAdapter) {
        val currentUserId = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                val friends = withContext(Dispatchers.IO) {
                    val result = db.collection("users").document(currentUserId).collection("friends").get().await()
                    result.map { document ->
                        val friendId = document.id
                        val friendDoc = db.collection("users").document(friendId).get().await()
                        Friend(
                            id = friendId,
                            name = friendDoc.getString("name") ?: "Amigo sin nombre",
                            imageUrl = friendDoc.getString("photoURL") ?: ""
                        )
                    }
                }
                friendsAdapter.updateFriends(friends)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar amigos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createGroup(selectedFriends: List<Friend>, groupName: String, groupImageUri: Uri?) {
        val currentUserId = auth.currentUser?.uid ?: return
        val groupUserIds = selectedFriends.map { it.id } + currentUserId

        val groupData = hashMapOf(
            "adminIds" to listOf(currentUserId),
            "lastMessage" to "Has sido añadido al grupo $groupName",
            "lastMessageTimestamp" to com.google.firebase.Timestamp.now(),
            "name" to groupName,
            "participants" to groupUserIds,
            "unreadCount" to groupUserIds.associateWith { 0 },
            "isGroup" to true, // Agregar el campo isGroup para identificar que es un grupo
            "groupURL" to "" // Inicializar con un valor vacío
        )

        db.collection("chats").add(groupData)
            .addOnSuccessListener { documentReference ->
                // Subir la imagen en segundo plano y actualizar la URL en Firestore
                if (groupImageUri != null) {
                    uploadGroupImage(documentReference.id, groupImageUri)
                }
                // Notificar a los usuarios del nuevo grupo
                notifyGroupMembers(groupUserIds, documentReference.id, currentUserId, groupName)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al crear el grupo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadGroupImage(groupId: String, imageUri: Uri) {
        lifecycleScope.launch {
            try {
                val storageRef = FirebaseStorage.getInstance().reference.child("group_images/${UUID.randomUUID()}")
                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await()
                db.collection("chats").document(groupId).update("groupURL", downloadUrl.toString())
            } catch (e: Exception) {
                // Manejar error en la carga de la imagen
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al subir la imagen del grupo.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun notifyGroupMembers(groupUserIds: List<String>, chatId: String, createdBy: String, groupName: String) {
        for (userId in groupUserIds) {
            if (userId != createdBy) {
                createGroupInviteNotification(chatId, createdBy, userId, groupName)
            }
        }
    }

    private fun createGroupInviteNotification(chatId: String, createdBy: String, userId: String, groupName: String) {
        val dataMap = mapOf(
            "chatId" to chatId,
            "createdBy" to createdBy,
            "groupName" to groupName
        )
        val notificationData = mapOf(
            "data" to dataMap,
            "userId" to userId,
            "isRead" to false,
            "message" to "Has sido añadido al grupo '$groupName'",
            "title" to "Nuevo grupo",
            "type" to "groupInvite",
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(userId).collection("notifications").add(notificationData)
            .addOnSuccessListener {
                Log.d("CreateGroupBottomSheetFragment", "Notificación de invitación a grupo creada para $userId")
            }
            .addOnFailureListener { e ->
                Log.e("CreateGroupBottomSheetFragment", "Error al crear notificación de invitación a grupo para $userId", e)
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}