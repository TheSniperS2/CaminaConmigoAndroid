package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.os.Bundle
import android.util.Log
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

        val chatId = intent.getStringExtra("CHAT_ID")
        if (chatId == null) {
            Log.e("ChatDetailActivity", "Chat ID es nulo, no se pueden cargar mensajes")
            return
        }

        Log.d("ChatDetailActivity", "Cargando mensajes para chat ID: $chatId")
        viewModel.loadMessages(chatId)


        // Configura RecyclerView para mostrar los mensajes del chat
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        val adapter = MessageAdapter()
        binding.recyclerViewMessages.adapter = adapter

        // Cargar los mensajes cuando la actividad se inicie
        viewModel.loadMessages(chatId)

        // Observar los cambios en los mensajes
        viewModel.messages.observe(this) { messages ->
            Log.d("ChatDetailActivity", "Mensajes actualizados en UI: ${messages.size}")
            adapter.submitList(messages)
        }


        // Enviar mensaje cuando el usuario lo escriba
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(chatId, message)
                binding.etMessage.text.clear()
            }
        }
    }
}
