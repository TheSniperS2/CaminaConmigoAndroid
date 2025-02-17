package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.databinding.ItemMessageReceivedBinding
import com.franco.CaminaConmigo.databinding.ItemMessageSentBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message

class MessageAdapter(private val userNames: Map<String, String>, private val currentUserId: String) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentMessageViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedMessageViewHolder(binding, userNames)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        Log.d("MessageAdapter", "Mensaje: ${message.content}, Timestamp: ${message.timestamp}")
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }
}

class SentMessageViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessage.text = message.content.ifEmpty { "Mensaje vacío" }

        // Verificar si el timestamp es válido antes de formatearlo
        message.timestamp?.let {
            val formattedDate = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm:ss", it.toDate())
            binding.tvTimestamp.text = formattedDate
        } ?: run {
            binding.tvTimestamp.text = "Fecha no disponible"
        }
    }
}

class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding, private val userNames: Map<String, String>) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessage.text = message.content.ifEmpty { "Mensaje vacío" }

        // Mostrar el nombre del remitente usando userNames
        val senderName = userNames[message.senderId] ?: "Remitente desconocido"
        binding.tvSender.text = senderName

        // Verificar si el timestamp es válido antes de formatearlo
        message.timestamp?.let {
            val formattedDate = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm:ss", it.toDate())
            binding.tvTimestamp.text = formattedDate
        } ?: run {
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