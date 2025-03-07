package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.franco.CaminaConmigo.databinding.FragmentAddFriendBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddFriendBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddFriendBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFriendBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón de enviar solicitud
        binding.btnSendRequest.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            if (username.isNotEmpty()) {
                sendFriendRequest(username)
            } else {
                Toast.makeText(requireContext(), "Por favor ingresa un nombre de usuario", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCerrar.setOnClickListener {
            dismiss()
        }
    }

    private fun sendFriendRequest(username: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Debes estar autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = currentUser.uid

        // Obtener los datos del usuario actual desde Firestore
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDocument ->
                val currentUserName = userDocument.getString("name") ?: "Usuario"
                val currentUserEmail = userDocument.getString("email") ?: "Sin email"
                val currentUserUsername = userDocument.getString("username") ?: "Sin username" // Nuevo

                // Buscar el usuario por username en Firestore
                db.collection("users")
                    .whereEqualTo("username", username) // Buscar por username en lugar de email
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val receiverUser = documents.documents[0]
                            val receiverId = receiverUser.id // Obtener el UID del receptor

                            // Verificar si ya son amigos
                            db.collection("users").document(currentUserId)
                                .collection("friends").document(receiverId)
                                .get()
                                .addOnSuccessListener { friendDoc ->
                                    if (friendDoc.exists()) {
                                        Toast.makeText(requireContext(), "Ya tienes agregado a este usuario como amigo", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Verificar si ya hay una solicitud pendiente
                                        db.collection("friendRequests")
                                            .whereEqualTo("fromUserId", currentUserId)
                                            .whereEqualTo("toUserId", receiverId)
                                            .whereEqualTo("status", "pending")
                                            .get()
                                            .addOnSuccessListener { requestDocs ->
                                                if (!requestDocs.isEmpty) {
                                                    Toast.makeText(requireContext(), "Ya enviaste una solicitud a este usuario", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    // Crear la solicitud de amistad
                                                    val requestData = mapOf(
                                                        "fromUserId" to currentUserId,
                                                        "fromUserName" to currentUserName, // Usar el username
                                                        "fromUserEmail" to currentUserEmail,
                                                        "toUserId" to receiverId,
                                                        "status" to "pending",
                                                        "createdAt" to Timestamp.now() // Guarda la fecha correctamente
                                                    )

                                                    // Agregar solicitud a Firestore
                                                    db.collection("friendRequests")
                                                        .add(requestData)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(requireContext(), "Solicitud enviada a $username", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .addOnFailureListener {
                                                            Toast.makeText(requireContext(), "Error al enviar solicitud", Toast.LENGTH_SHORT).show()
                                                        }
                                                }
                                            }
                                    }
                                }
                        } else {
                            Toast.makeText(requireContext(), "El usuario no existe", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al buscar usuario", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
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