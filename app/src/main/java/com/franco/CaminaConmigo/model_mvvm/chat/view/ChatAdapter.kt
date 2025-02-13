package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.franco.CaminaConmigo.databinding.ItemChatBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.Chat

class ChatAdapter(private val onChatClick: (String) -> Unit) : ListAdapter<Chat, ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding, onChatClick)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = getItem(position)
        Log.d("ChatAdapter", "Mostrando chat con: ${chat.name} (ID: ${chat.chatId})")
        holder.bind(chat)
    }
}

class ChatViewHolder(
    private val binding: ItemChatBinding,
    private val onChatClick: (String) -> Unit
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

    fun bind(chat: Chat) {
        try {
            // Asignar el nombre del chat
            val chatName = chat.name ?: "Chat sin nombre"
            binding.chatName.text = chatName

            // Mostrar el último mensaje si existe
            binding.lastMessage.text = chat.lastMessage ?: "Sin mensaje"

            // Formatear la fecha del último mensaje si se tiene el timestamp
            if (chat.lastMessageTimestamp != 0L) {
                val timestamp = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm").format(java.util.Date(chat.lastMessageTimestamp))
                binding.lastMessageTimestamp.text = timestamp
            } else {
                binding.lastMessageTimestamp.text = "Sin fecha"
            }

            // Manejar el clic en el chat para abrirlo
            binding.root.setOnClickListener {
                chat.chatId?.let {
                    onChatClick(it)
                } ?: Log.e("ChatViewHolder", "Chat ID nulo")
            }
        } catch (e: Exception) {
            Log.e("ChatViewHolder", "Error al bindear el chat: ${e.message}")
        }
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
    override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
        return oldItem.chatId == newItem.chatId
    }

    override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
        return oldItem == newItem
    }
}
