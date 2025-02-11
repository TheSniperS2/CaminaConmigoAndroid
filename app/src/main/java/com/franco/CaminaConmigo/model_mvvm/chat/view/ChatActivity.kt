package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.databinding.ActivityChatBinding
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.ChatViewModel
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

        // Configura RecyclerView para mostrar la lista de amigos/chat
        binding.recyclerViewChats.layoutManager = LinearLayoutManager(this)
        val adapter = ChatAdapter { chatId -> // Este lambda nos permite manejar el click en el chat
            openChat(chatId)
        }
        binding.recyclerViewChats.adapter = adapter

        // Verificar si el usuario tiene amigos antes de mostrar los chats
        verifyFriendship()

        // Cargar los chats cuando la actividad se inicie
        viewModel.loadChats()
        try {
            // Código para cargar chats
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar chats: ${e.message}", Toast.LENGTH_LONG).show()
        }


        // Observa los chats disponibles
        viewModel.chats.observe(this) { chats ->
            adapter.submitList(chats)
        }

        // Maneja el click para agregar un amigo
        binding.textView55.setOnClickListener {
            startActivity(Intent(this, AddFriendActivity::class.java))
        }
    }

    private fun verifyFriendship() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "No estás autenticado", Toast.LENGTH_LONG).show()
            return
        }
        // Verificar si el usuario tiene amigos
        db.collection("friendRequests")
            .whereEqualTo("requestTo", currentUserId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    // Si no tiene amigos, mostrar un mensaje y no mostrar los chats
                    Toast.makeText(this, "No tienes amigos. Agrega amigos para iniciar chats.", Toast.LENGTH_LONG).show()
                    binding.recyclerViewChats.visibility = android.view.View.GONE  // Oculta la lista de chats
                } else {
                    binding.recyclerViewChats.visibility = android.view.View.VISIBLE  // Muestra la lista de chats
                }
            }
    }

    // Este método se usa para abrir una conversación con el amigo
    private fun openChat(chatId: String) {
        val intent = Intent(this, ChatDetailActivity::class.java).apply {
            putExtra("CHAT_ID", chatId)
        }
        startActivity(intent)
    }
}
