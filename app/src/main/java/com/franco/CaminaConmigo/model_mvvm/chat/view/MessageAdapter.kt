package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.databinding.ItemDateHeaderBinding
import com.franco.CaminaConmigo.databinding.ItemLocationMessageBinding
import com.franco.CaminaConmigo.databinding.ItemMessageReceivedBinding
import com.franco.CaminaConmigo.databinding.ItemMessageSentBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.LocationMessage
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(private val currentUserId: String) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_LOCATION = 3
        private const val VIEW_TYPE_DATE_HEADER = 4
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is Message -> {
                when {
                    item.content.startsWith("Ubicación:") -> VIEW_TYPE_LOCATION
                    item.senderId == currentUserId -> VIEW_TYPE_SENT
                    else -> VIEW_TYPE_RECEIVED
                }
            }
            is String -> VIEW_TYPE_DATE_HEADER
            else -> throw IllegalArgumentException("Invalid view type")
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
            VIEW_TYPE_DATE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateHeaderViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        Log.d("MessageAdapter", "Item: $item")
        when (holder) {
            is SentMessageViewHolder -> holder.bind(item as Message)
            is ReceivedMessageViewHolder -> holder.bind(item as Message)
            is LocationMessageViewHolder -> {
                val message = item as Message
                // Parse the latitude and longitude from the content
                val parts = message.content.removePrefix("Ubicación: ").split(", ")
                val latitude = parts[0].toDouble()
                val longitude = parts[1].toDouble()
                val locationMessage = LocationMessage(
                    senderId = message.senderId,
                    timestamp = message.timestamp,
                    latitude = latitude,
                    longitude = longitude,
                    isActive = true
                )
                holder.bind(locationMessage)
            }
            is DateHeaderViewHolder -> holder.bind(item as String)
        }
    }

    fun submitListWithHeaders(messages: List<Message>) {
        val items = mutableListOf<Any>()
        var lastDate: String? = null

        for (message in messages) {
            val messageDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(message.timestamp.toDate())
            if (lastDate != messageDate) {
                val displayDate = if (messageDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())) {
                    "Hoy"
                } else {
                    SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(message.timestamp.toDate())
                }
                items.add(displayDate)
                lastDate = messageDate
            }
            items.add(message)
        }

        submitList(items)
    }
}

class SentMessageViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessage.text = message.content.ifEmpty { "Mensaje vacío" }

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

        message.timestamp?.let {
            val formattedTime = android.text.format.DateFormat.format("hh:mm a", it.toDate())
            binding.tvTimestamp.text = formattedTime
        } ?: run {
            binding.tvTimestamp.text = "Hora no disponible"
        }
    }
}

class LocationMessageViewHolder(private val binding: ItemLocationMessageBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(locationMessage: LocationMessage) {
        binding.tvLocationMessage.text = "Ubicación compartida"
        val mapView = binding.mapView

        mapView.onCreate(null)
        mapView.getMapAsync { googleMap ->
            val location = LatLng(locationMessage.latitude, locationMessage.longitude)
            googleMap.addMarker(MarkerOptions().position(location).title("Ubicación compartida"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
        mapView.onResume()
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

class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(date: String) {
        binding.dateHeaderTextView.text = date
    }
}

class DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return (oldItem is Message && newItem is Message && oldItem.id == newItem.id) ||
                (oldItem is String && newItem is String && oldItem == newItem)
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }
}