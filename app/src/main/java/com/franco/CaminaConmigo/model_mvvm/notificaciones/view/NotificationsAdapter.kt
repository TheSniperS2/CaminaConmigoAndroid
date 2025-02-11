package com.franco.CaminaConmigo.model_mvvm.notificaciones.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.databinding.ItemNotificationBinding
import com.franco.CaminaConmigo.model_mvvm.notificaciones.model.Notification

class NotificationsAdapter(private val onAcceptClicked: (Notification) -> Unit) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

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
            binding.txtTitle.text = notification.title
            binding.txtMessage.text = notification.message

            // Botón de aceptar solicitud
            binding.btnAccept.setOnClickListener {
                onAcceptClicked(notification)
            }
        }
    }
}
