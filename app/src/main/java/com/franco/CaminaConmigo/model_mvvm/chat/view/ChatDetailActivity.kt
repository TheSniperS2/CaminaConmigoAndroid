package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter

    // Lista de mensajes en memoria (ejemplo simple)
    private val messagesList = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_detail)

        // Obtén el nombre del chat que se pasó desde el Intent
        val chatName = intent.getStringExtra("CHAT_NAME")
        // Puedes usar este valor para setear el título o hacer lógica adicional
        title = chatName ?: "Detalle del Chat"

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        val editTextMessage = findViewById<EditText>(R.id.editTextMessage)
        val buttonSend = findViewById<Button>(R.id.buttonSend)

        // Inicializamos el Adapter y RecyclerView
        chatAdapter = ChatAdapter(messagesList)
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatDetailActivity)
            adapter = chatAdapter
        }

        // Ejemplo: Agregamos un mensaje inicial (dummy)
        messagesList.add(Message("Bienvenido al chat de $chatName"))
        chatAdapter.notifyDataSetChanged()

        // Manejar evento de enviar mensaje
        buttonSend.setOnClickListener {
            val newMessageText = editTextMessage.text.toString().trim()
            if (newMessageText.isNotEmpty()) {
                // Agregamos el mensaje a la lista
                messagesList.add(Message(newMessageText, isSentByUser = true))
                // Notificamos cambios al Adapter
                chatAdapter.notifyItemInserted(messagesList.size - 1)
                // Hacemos scroll al final para ver el último mensaje
                messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                // Limpiamos el EditText
                editTextMessage.setText("")
            }
        }
    }
}
