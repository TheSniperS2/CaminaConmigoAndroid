package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ActivityChatBinding
import com.franco.CaminaConmigo.databinding.DialogCreateGroupBinding
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.chat.model.Friend
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.ChatViewModel
import com.franco.CaminaConmigo.model_mvvm.mapa.view.MapaActivity
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity
import com.franco.CaminaConmigo.model_mvvm.novedad.view.NovedadActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerViewChats.layoutManager = LinearLayoutManager(this)
        val adapter = ChatAdapter { chatId -> openChat(chatId) }
        binding.recyclerViewChats.adapter = adapter

        // Agregar manejo de errores en verifyFriendship()
        try {
            verifyFriendship()
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error en verifyFriendship: ${e.message}")
        }

        // Agregar control de excepciones en loadChats()
        try {
            viewModel.loadChats()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar chats: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ChatActivity", "Error en loadChats: ${e.message}")
        }

        // Observador con control de errores
        viewModel.chats.observe(this) { chats ->
            if (chats != null) {
                adapter.submitList(chats)
            } else {
                Log.w("ChatActivity", "Lista de chats nula")
            }
        }

        // Funcionalidad de los botones inferiores
        val btnMapa = findViewById<ImageButton>(R.id.imageButton10)
        val btnNovedades = findViewById<ImageButton>(R.id.imageButton11)
        val btnAyuda = findViewById<ImageButton>(R.id.imageButton13)
        val btnMenu = findViewById<ImageButton>(R.id.imageButton14)

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

        binding.textView55.setOnClickListener {
            startActivity(Intent(this, AddFriendActivity::class.java))
        }

        binding.textView57.setOnClickListener {
            showCreateGroupDialog()
        }
    }

    private fun verifyFriendship() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "No estás autenticado", Toast.LENGTH_LONG).show()
            return
        }

        // Verifica la subcolección 'friends' del usuario actual
        db.collection("users")  // Asegúrate de que 'users' es la colección de usuarios
            .document(currentUserId)  // Obtén el documento del usuario actual
            .collection("friends")  // Revisa la subcolección 'friends'
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    // No hay amigos en la subcolección
                    Toast.makeText(this, "No tienes amigos. Agrega amigos para iniciar chats.", Toast.LENGTH_LONG).show()
                    binding.recyclerViewChats.visibility = android.view.View.GONE
                } else {
                    // Hay amigos en la subcolección, puedes mostrar los chats
                    binding.recyclerViewChats.visibility = android.view.View.VISIBLE
                    loadChats()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ChatActivity", "Error al verificar amigos: ${exception.message}")
                Toast.makeText(this, "Error al verificar amigos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadChats() {
        try {
            viewModel.loadChats()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar chats: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ChatActivity", "Error en loadChats: ${e.message}")
        }
    }

    private fun openChat(chatId: String) {
        val intent = Intent(this, ChatDetailActivity::class.java).apply {
            putExtra("CHAT_ID", chatId)
        }
        startActivity(intent)
    }

    private fun showCreateGroupDialog() {
        val dialogBinding = DialogCreateGroupBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val friendsAdapter = FriendsAdapter(emptyList()) { friend ->
            // Lógica para manejar la selección de amigos
        }
        dialogBinding.recyclerViewFriends.layoutManager = LinearLayoutManager(this)
        dialogBinding.recyclerViewFriends.adapter = friendsAdapter

        // Cargar amigos del usuario actual
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUserId).collection("friends").get()
            .addOnSuccessListener { result ->
                val friends = result.map { document ->
                    Friend(
                        id = document.id,
                        name = document.getString("nickname") ?: "Amigo sin nombre"
                    )
                }
                friendsAdapter.updateFriends(friends)
            }
            .addOnFailureListener { e ->
                Log.e("ChatActivity", "Error al cargar amigos: ${e.message}")
            }

        dialogBinding.buttonCreateGroup.setOnClickListener {
            val selectedFriends = friendsAdapter.getSelectedFriends()
            if (selectedFriends.size < 2) {
                Toast.makeText(this, "Debes seleccionar al menos 2 amigos para crear un grupo.", Toast.LENGTH_SHORT).show()
            } else {
                createGroup(selectedFriends)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun createGroup(selectedFriends: List<Friend>) {
        val currentUserId = auth.currentUser?.uid ?: return
        val groupUserIds = selectedFriends.map { it.id } + currentUserId

        val groupData = mapOf(
            "adminIds" to listOf(currentUserId),
            "lastMessage" to "Hola",
            "lastMessageTimestamp" to Timestamp.now(),
            "name" to "Nuevo Grupo",
            "participants" to groupUserIds,
            "unreadCount" to groupUserIds.associateWith { 0 }
        )

        db.collection("chats").add(groupData)
            .addOnSuccessListener {
                Toast.makeText(this, "Grupo creado exitosamente.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("ChatActivity", "Error al crear el grupo: ${e.message}")
                Toast.makeText(this, "Error al crear el grupo.", Toast.LENGTH_SHORT).show()
            }
    }
}