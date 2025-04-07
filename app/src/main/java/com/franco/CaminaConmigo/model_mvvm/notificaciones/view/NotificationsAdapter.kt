package com.franco.CaminaConmigo.model_mvvm.notificaciones.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ItemNotificationBinding
import com.franco.CaminaConmigo.model_mvvm.notificaciones.model.Notification
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

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

            // Asignar icono según el título de la notificación
            val iconResId = when (notification.title) {
                "Nuevo grupo" -> R.drawable.grouppp_24px
                "Nueva solicitud de amistad", "Solicitud aceptada" -> R.drawable.ic_person
                "Nuevo reporte de amigo" -> R.drawable.report_24px
                "Nuevo comentario" -> R.drawable.feedback_24px
                else -> R.drawable.ic_anadir // Icono por defecto
            }
            binding.ivNotificationIcon.setImageResource(iconResId)

            // Formatear la fecha de creación si no es nula
            notification.createdAt?.let {
                binding.tvNotificationDate.text = getTimeAgo(it)
            } ?: run {
                binding.tvNotificationDate.text = "Fecha no disponible"
            }
        }

        private fun getTimeAgo(timestamp: Timestamp): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp.toDate().time

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                seconds < 60 -> "hace un momento"
                minutes < 60 -> "hace $minutes ${if (minutes == 1L) "minuto" else "minutos"}"
                hours < 24 -> "hace $hours ${if (hours == 1L) "hora" else "horas"}"
                days < 7 -> "hace $days ${if (days == 1L) "día" else "días"}"
                days < 30 -> "hace ${days / 7} ${if (days / 7 == 1L) "semana" else "semanas"}"
                days < 365 -> "hace ${days / 30} ${if (days / 30 == 1L) "mes" else "meses"}"
                else -> "hace ${days / 365} ${if (days / 365 == 1L) "año" else "años"}"
            }
        }
    }
}