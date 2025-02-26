package com.franco.CaminaConmigo.model_mvvm.notificaciones.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.databinding.ItemNotificationBinding
import com.franco.CaminaConmigo.model_mvvm.notificaciones.model.Notification
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsAdapter : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    private var notifications: List<Notification> = emptyList()

    fun submitList(newList: List<Notification>) {
        notifications = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            binding.tvNotificationTitle.text = notification.title
            binding.tvNotificationMessage.text = notification.message

            // Formatear la fecha de creación si no es nula
            notification.createdAt?.let {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val date = it.toDate()
                binding.tvNotificationDate.text = dateFormat.format(date)
            } ?: run {
                binding.tvNotificationDate.text = "Fecha no disponible"
            }
        }
    }
}