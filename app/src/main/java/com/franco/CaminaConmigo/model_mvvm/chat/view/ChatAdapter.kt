package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.franco.CaminaConmigo.databinding.ItemChatBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.Chat
import com.google.firebase.firestore.FirebaseFirestore

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

    private val db = FirebaseFirestore.getInstance()

    fun bind(chat: Chat) {
        try {
            // Asignar el nombre del chat
            val chatName = chat.name ?: "Chat sin nombre"
            binding.chatName.text = chatName

            // Mostrar el último mensaje si existe
            binding.lastMessage.text = chat.lastMessage ?: "Sin mensaje"

            // Formatear la fecha del último mensaje si se tiene el timestamp
            val timestamp = chat.lastMessageTimestamp
            if (timestamp != null) {
                val formattedDate = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm").format(timestamp.toDate())
                binding.lastMessageTimestamp.text = formattedDate
            } else {
                binding.lastMessageTimestamp.text = "Sin fecha"
            }

            // Recuperar y mostrar la imagen de perfil del usuario
            if (chat.userIds.isNotEmpty()) {
                val userId = chat.userIds[0] // Asumiendo que el primer ID es el del usuario con el que se está chateando
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        val photoURL = document.getString("photoURL")
                        if (!photoURL.isNullOrEmpty()) {
                            Glide.with(binding.root.context)
                                .load(photoURL)
                                .circleCrop()
                                .into(binding.profileImage)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatViewHolder", "Error al obtener la imagen de perfil: ${e.message}")
                    }
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