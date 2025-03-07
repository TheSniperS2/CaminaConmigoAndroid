package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ItemChatBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(private val onChatClick: (String) -> Unit) : ListAdapter<Chat, ChatViewHolder>(ChatDiffCallback()) {

    private val auth = FirebaseAuth.getInstance()

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
) : RecyclerView.ViewHolder(binding.root) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun bind(chat: Chat) {
        try {
            // Asignar el nombre del chat
            val chatName = chat.name ?: "Chat sin nombre"
            binding.chatName.text = chatName

            // Mostrar el último mensaje si existe
            binding.lastMessage.text = chat.lastMessage ?: "Sin mensaje"

            // Formatear la hora del último mensaje si se tiene el timestamp
            val timestamp = chat.lastMessageTimestamp
            if (timestamp != null) {
                val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp.toDate())
                binding.lastMessageTimestamp.text = formattedTime
            } else {
                binding.lastMessageTimestamp.text = "Sin hora"
            }

            // Recuperar y mostrar la imagen de perfil del usuario o del grupo
            if (chat.isGroup) {
                if (chat.groupURL.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(chat.groupURL)
                        .circleCrop()
                        .into(binding.profileImage)
                } else {
                    binding.profileImage.setImageResource(R.drawable.ic_imagen) // Imagen de grupo predeterminada
                }
            } else if (chat.participants.size == 2) {
                val friendId = chat.participants.first { it != auth.currentUser?.uid }
                db.collection("users").document(friendId).get()
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
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_imagen)
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