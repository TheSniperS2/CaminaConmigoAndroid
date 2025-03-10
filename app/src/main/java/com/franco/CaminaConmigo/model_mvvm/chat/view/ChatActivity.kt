package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ActivityChatBinding
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.ChatViewModel
import com.franco.CaminaConmigo.model_mvvm.mapa.view.MapaActivity
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity
import com.franco.CaminaConmigo.model_mvvm.novedad.view.NovedadActivity
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
        val btnMapa = findViewById<LinearLayout>(R.id.MapaContainer)
        val btnNovedades = findViewById<LinearLayout>(R.id.NovedadContainer)
        val btnAyuda = findViewById<LinearLayout>(R.id.AyudaContainer)
        val btnMenu = findViewById<LinearLayout>(R.id.MenuContainer)

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
            val addFriendBottomSheetFragment = AddFriendBottomSheetFragment()
            addFriendBottomSheetFragment.show(supportFragmentManager, addFriendBottomSheetFragment.tag)
        }

        binding.textView57.setOnClickListener {
            showCreateGroupBottomSheet()
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

    private fun showCreateGroupBottomSheet() {
        val createGroupBottomSheetFragment = CreateGroupBottomSheetFragment()
        createGroupBottomSheetFragment.show(supportFragmentManager, createGroupBottomSheetFragment.tag)
    }

}