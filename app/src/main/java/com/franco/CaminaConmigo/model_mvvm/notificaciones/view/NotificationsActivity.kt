package com.franco.CaminaConmigo.model_mvvm.notificaciones.view

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.ActivityNotificationsBinding
import com.franco.CaminaConmigo.model_mvvm.notificaciones.viewmodel.NotificationsViewModel

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val viewModel: NotificationsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerViewFriendRequests.layoutManager = LinearLayoutManager(this)
        val friendRequestsAdapter = FriendRequestsAdapter(
            onAcceptClicked = { request ->
                viewModel.acceptFriendRequest(request)
            },
            onRejectClicked = { request ->
                viewModel.rejectFriendRequest(request)
            }
        )
        binding.recyclerViewFriendRequests.adapter = friendRequestsAdapter

        val btnRetroceder = findViewById<ImageView>(R.id.btnRetroceder)

        // Acción para el botón de retroceder
        btnRetroceder.setOnClickListener {
            onBackPressed()
        }

        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(this)
        val notificationsAdapter = NotificationsAdapter()
        binding.recyclerViewNotifications.adapter = notificationsAdapter

        viewModel.friendRequests.observe(this) { friendRequests ->
            Log.d("NotificationsActivity", "Se han recibido ${friendRequests.size} solicitudes de amistad")

            if (friendRequests.isEmpty()) {
                Log.d("NotificationsActivity", "No hay solicitudes de amistad para mostrar.")
                binding.tvFriendRequests.visibility = View.GONE
                binding.recyclerViewFriendRequests.visibility = View.GONE
            } else {
                binding.tvFriendRequests.visibility = View.VISIBLE
                binding.recyclerViewFriendRequests.visibility = View.VISIBLE
            }

            friendRequestsAdapter.submitList(friendRequests)
        }

        viewModel.notifications.observe(this) { notifications ->
            Log.d("NotificationsActivity", "Se han recibido ${notifications.size} notificaciones")

            if (notifications.isEmpty()) {
                Log.d("NotificationsActivity", "No hay notificaciones para mostrar.")
            }

            notificationsAdapter.submitList(notifications)
        }

        viewModel.loadFriendRequests()
        viewModel.loadNotifications()
    }
}