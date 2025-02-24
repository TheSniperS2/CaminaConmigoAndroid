package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CreateGroupBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCreateGroupBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

        binding.buttonCreateGroup.setOnClickListener {
            val selectedFriends = friendsAdapter.getSelectedFriends()
            val groupName = binding.editTextGroupName.text.toString().trim()
            if (groupName.isEmpty()) {
                Toast.makeText(requireContext(), "Debes ingresar un nombre para el grupo.", Toast.LENGTH_SHORT).show()
            } else if (selectedFriends.size < 2) {
                Toast.makeText(requireContext(), "Debes seleccionar al menos 2 amigos para crear un grupo.", Toast.LENGTH_SHORT).show()
            } else {
                createGroup(selectedFriends, groupName)
            }
        }

        binding.btnCerrar.setOnClickListener {
            dismiss()
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

    private fun createGroup(selectedFriends: List<Friend>, groupName: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val groupUserIds = selectedFriends.map { it.id } + currentUserId

        val groupData = mapOf(
            "adminIds" to listOf(currentUserId),
            "lastMessage" to "Has sido añadido al grupo $groupName",
            "lastMessageTimestamp" to com.google.firebase.Timestamp.now(),
            "name" to groupName,
            "participants" to groupUserIds,
            "unreadCount" to groupUserIds.associateWith { 0 },
            "isGroup" to true // Agregar el campo isGroup para identificar que es un grupo
        )

        db.collection("chats").add(groupData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Grupo creado exitosamente.", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al crear el grupo: ${e.message}", Toast.LENGTH_SHORT).show()
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