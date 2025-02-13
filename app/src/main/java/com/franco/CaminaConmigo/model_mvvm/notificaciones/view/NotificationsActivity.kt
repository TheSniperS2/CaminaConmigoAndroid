package com.franco.CaminaConmigo.model_mvvm.notificaciones.view

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.databinding.ActivityNotificationsBinding
import com.franco.CaminaConmigo.model_mvvm.notificaciones.viewmodel.NotificationsViewModel

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val viewModel: NotificationsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(this)
        val adapter = NotificationsAdapter { notification ->
            // Llama a la función de aceptar solicitud cuando se presiona un botón
            viewModel.acceptFriendRequest(notification)
        }
        binding.recyclerViewNotifications.adapter = adapter

        viewModel.notifications.observe(this) { notifications ->
            Log.d("NotificationsActivity", "Se han recibido ${notifications.size} notificaciones")

            if (notifications.isEmpty()) {
                Log.d("NotificationsActivity", "No hay notificaciones para mostrar.")
            }

            adapter.submitList(notifications)
        }

        viewModel.loadNotifications()
    }
}