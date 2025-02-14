package com.franco.CaminaConmigo.model_mvvm.notificaciones.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.databinding.ItemNotificationBinding
import com.franco.CaminaConmigo.model_mvvm.notificaciones.model.Notification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val onAcceptClicked: (Notification) -> Unit,
    private val onRejectClicked: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

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

            // Formatear la fecha de creación
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val date = Date(notification.createdAt)
            binding.tvNotificationDate.text = dateFormat.format(date)

            // Mostrar botones de aceptar y rechazar solo para solicitudes de amistad
            if (notification.type == "friendRequest") {
                binding.btnAccept.visibility = View.VISIBLE
                binding.btnReject.visibility = View.VISIBLE

                binding.btnAccept.setOnClickListener { onAcceptClicked(notification) }
                binding.btnReject.setOnClickListener { onRejectClicked(notification) }
            } else {
                binding.btnAccept.visibility = View.GONE
                binding.btnReject.visibility = View.GONE
            }
        }
    }
}