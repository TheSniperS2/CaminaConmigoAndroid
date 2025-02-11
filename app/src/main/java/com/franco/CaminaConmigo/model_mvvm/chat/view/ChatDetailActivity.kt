package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.databinding.ActivityChatDetailBinding
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.ChatViewModel
import com.google.firebase.firestore.FirebaseFirestore

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private val viewModel: ChatViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chatId = intent.getStringExtra("CHAT_ID") ?: return

        // Configura RecyclerView para mostrar los mensajes del chat
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        val adapter = MessageAdapter()
        binding.recyclerViewMessages.adapter = adapter

        // Cargar los mensajes cuando la actividad se inicie
        viewModel.loadMessages(chatId)

        // Observa los mensajes disponibles
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages)
        }

        // Enviar mensaje
        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString()
            if (messageText.isNotEmpty()) {
                viewModel.sendMessage(chatId, messageText)
                binding.etMessage.text.clear()
            }
        }
    }
}
