package com.franco.CaminaConmigo.model_mvvm.chat.view

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
        holder.bind(chat)
    }
}

class ChatViewHolder(
    private val binding: ItemChatBinding,
    private val onChatClick: (String) -> Unit
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

    fun bind(chat: Chat) {
        binding.chatName.text = chat.name  // Asegúrate de que estás mostrando el nombre correctamente
        binding.root.setOnClickListener { onChatClick(chat.chatId) }  // Usar chatId para abrir el chat
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
    override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
        return oldItem.chatId == newItem.chatId  // Comparar por el ID del chat
    }

    override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
        return oldItem == newItem
    }
}
