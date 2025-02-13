package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ItemMessageBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message

class MessageAdapter : ListAdapter<Message, MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message)
    }
}

class MessageViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessage.text = message.content ?: "No content"

        // Cambiar color de fondo si el mensaje está leído o no
        if (message.isRead) {
            binding.root.setBackgroundColor(binding.root.context.getColor(R.color.read_message_background))
        } else {
            binding.root.setBackgroundColor(binding.root.context.getColor(R.color.unread_message_background))
        }

        // Verificar si el timestamp es válido antes de formatearlo
        if (message.timestamp > 0) {
            val formattedDate = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm:ss", message.timestamp)
            binding.tvTimestamp.text = formattedDate
        } else {
            binding.tvTimestamp.text = "Fecha no disponible"
        }
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}
