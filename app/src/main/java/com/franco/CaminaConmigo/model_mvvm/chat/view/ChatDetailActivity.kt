package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ActivityChatDetailBinding
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.ChatViewModel
import com.franco.CaminaConmigo.model_mvvm.mapa.view.MapaActivity
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity
import com.franco.CaminaConmigo.model_mvvm.novedad.view.NovedadActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private val viewModel: ChatViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

        val currentUserId = auth.currentUser?.uid ?: return
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


        // Observa los userNames y crea el adaptador cuando estén disponibles
        viewModel.userNames.observe(this, Observer { userNames ->
            val adapter = MessageAdapter(userNames, currentUserId)
            binding.recyclerViewMessages.adapter = adapter

            // Observa los mensajes y actualiza el adaptador
            viewModel.messages.observe(this, Observer { messages ->
                Log.d("ChatDetailActivity", "Mensajes actualizados en UI: ${messages.size}")
                adapter.submitList(messages)
                binding.recyclerViewMessages.scrollToPosition(messages.size - 1) // Desplazar al último mensaje
            })
        })

        // Carga los mensajes y los userNames
        viewModel.loadMessages(chatId)
        viewModel.loadChats()

        // Enviar mensaje
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(chatId, message)
                binding.etMessage.text.clear()
            }
        }
    }
}