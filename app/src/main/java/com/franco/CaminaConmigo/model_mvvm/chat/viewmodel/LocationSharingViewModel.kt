package com.franco.CaminaConmigo.model_mvvm.chat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.franco.CaminaConmigo.model_mvvm.chat.model.LocationMessage
import com.franco.CaminaConmigo.model_mvvm.chat.model.Message
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import java.util.UUID

class LocationSharingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)

    private val _activeLocationSharing = MutableLiveData<Map<String, LocationMessage>>()
    val activeLocationSharing: LiveData<Map<String, LocationMessage>> get() = _activeLocationSharing

    private val locationListeners: MutableMap<String, ListenerRegistration> = mutableMapOf()
    private var currentChatId: String? = null

    fun startSharingLocation(chatId: String) {
        currentChatId = chatId
        val currentUserId = auth.currentUser?.uid ?: return

        val latitude = -29.913299580848747
        val longitude = -71.248106468252

        val locationMessage = LocationMessage(
            id = currentUserId,
            senderId = currentUserId,
            timestamp = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            isActive = true
        )
        db.collection("chats").document(chatId)
            .collection("locationSharing").document(currentUserId)
            .set(locationMessage)

        // Create a placeholder message in the chat messages collection
        val message = Message(
            id = UUID.randomUUID().toString(), // Genera un nuevo UUID para el ID del mensaje
            senderId = currentUserId,
            content = "Ubicación: $latitude, $longitude",
            isRead = false
        )
        db.collection("chats").document(chatId)
            .collection("messages").document(message.id) // Usa el ID generado para el mensaje
            .set(message)


        // Uncomment the following lines to use the real location
        /*
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val locationMessage = LocationMessage(
                    id = currentUserId,
                    senderId = currentUserId,
                    timestamp = System.currentTimeMillis(),
                    latitude = it.latitude,
                    longitude = it.longitude,
                    isActive = true
                )
                db.collection("chats").document(chatId)
                    .collection("locationSharing").document(currentUserId)
                    .set(locationMessage)
            } ?: run {
                // Handle the case when location is null
            }
        }.addOnFailureListener {
            // Handle the failure
        }
        */
    }

    fun stopSharingLocation(chatId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("chats").document(chatId)
            .collection("locationSharing").document(currentUserId)
            .update("isActive", false)
    }

    fun listenToLocationUpdates(chatId: String, userId: String) {
        val listener = db.collection("chats").document(chatId)
            .collection("locationSharing").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val locationMessage = it.toObject<LocationMessage>()
                    locationMessage?.let { message ->
                        _activeLocationSharing.value = _activeLocationSharing.value?.toMutableMap()?.apply {
                            put(userId, message)
                        } ?: mapOf(userId to message)
                    }
                }
            }
        locationListeners[userId] = listener
    }

    fun stopListening(userId: String) {
        locationListeners[userId]?.remove()
        locationListeners.remove(userId)
        _activeLocationSharing.value = _activeLocationSharing.value?.toMutableMap()?.apply {
            remove(userId)
        }
    }
}