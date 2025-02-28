package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.databinding.ItemLocationMessageBinding
import com.franco.CaminaConmigo.databinding.ItemMessageReceivedBinding
import com.franco.CaminaConmigo.databinding.ItemMessageSentBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MessageAdapter(private val currentUserId: String) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_LOCATION = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.latitude != null && message.longitude != null -> VIEW_TYPE_LOCATION
            message.senderId == currentUserId -> VIEW_TYPE_SENT
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceivedMessageViewHolder(binding)
            }
            VIEW_TYPE_LOCATION -> {
                val binding = ItemLocationMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LocationMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        Log.d("MessageAdapter", "Mensaje: ${message.content}, Timestamp: ${message.timestamp}")
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is LocationMessageViewHolder -> holder.bind(message)
        }
    }
}

class SentMessageViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessage.text = message.content.ifEmpty { "Mensaje vacío" }

        // Verificar si el timestamp es válido antes de formatearlo
        message.timestamp?.let {
            val formattedTime = android.text.format.DateFormat.format("hh:mm a", it.toDate())
            binding.tvTimestamp.text = formattedTime
        } ?: run {
            binding.tvTimestamp.text = "Hora no disponible"
        }
    }
}

class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessage.text = message.content.ifEmpty { "Mensaje vacío" }

        // Verificar si el timestamp es válido antes de formatearlo
        message.timestamp?.let {
            val formattedTime = android.text.format.DateFormat.format("hh:mm a", it.toDate())
            binding.tvTimestamp.text = formattedTime
        } ?: run {
            binding.tvTimestamp.text = "Hora no disponible"
        }
    }
}

class LocationMessageViewHolder(private val binding: ItemLocationMessageBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvLocationMessage.text = message.content
        val mapView = binding.mapView

        mapView.onCreate(null)
        mapView.getMapAsync { googleMap ->
            val location = LatLng(message.latitude!!, message.longitude!!)
            googleMap.addMarker(MarkerOptions().position(location).title("Ubicación compartida"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
        mapView.onResume() // Asegúrate de llamar a onResume para que el mapa se muestre correctamente
    }

    fun onResume() {
        binding.mapView.onResume()
    }

    fun onPause() {
        binding.mapView.onPause()
    }

    fun onDestroy() {
        binding.mapView.onDestroy()
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