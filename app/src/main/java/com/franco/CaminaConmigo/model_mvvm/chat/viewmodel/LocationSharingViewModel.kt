package com.franco.CaminaConmigo.model_mvvm.chat.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.franco.CaminaConmigo.model_mvvm.chat.model.LocationMessage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LocationSharingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)

    private val _activeLocationSharing = MutableLiveData<Map<String, LocationMessage>>()
    val activeLocationSharing: LiveData<Map<String, LocationMessage>> get() = _activeLocationSharing

    private val _isActive = MutableLiveData<Boolean>(false)
    val isActive: LiveData<Boolean> get() = _isActive

    private val locationListeners: MutableMap<String, ListenerRegistration> = mutableMapOf()
    private var locationCallback: LocationCallback? = null
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("LocationSharingPrefs", Context.MODE_PRIVATE)
    private val isSharingKey = "isLocationSharing"
    private val activeChatIdKey = "activeLocationChatId"

    fun restoreSharingStateIfNeeded() {
        if (sharedPreferences.getBoolean(isSharingKey, false)) {
            val chatId = sharedPreferences.getString(activeChatIdKey, null)
            if (chatId != null) {
                startSharingLocation(chatId)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startSharingLocation(chatId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    val locationMessage = LocationMessage(
                        senderId = currentUserId,
                        timestamp = Timestamp.now(),
                        latitude = location.latitude,
                        longitude = location.longitude,
                        isActive = true
                    )
                    db.collection("chats").document(chatId)
                        .collection("locationSharing").document(currentUserId)
                        .set(locationMessage)
                }
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 30000 // 30 seconds
            fastestInterval = 15000 // 15 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        _isActive.value = true

        sharedPreferences.edit().putBoolean(isSharingKey, true).putString(activeChatIdKey, chatId).apply()
    }

    fun stopSharingLocation(chatId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        sharedPreferences.edit().putBoolean(isSharingKey, false).remove(activeChatIdKey).apply()

        db.collection("chats").document(chatId)
            .collection("locationSharing").document(currentUserId)
            .update("isActive", false)

        _isActive.value = false
    }

    fun listenToLocationUpdates(chatId: String, userId: String) {
        val listener = db.collection("chats").document(chatId)
            .collection("locationSharing").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val locationMessage = it.toObject(LocationMessage::class.java)
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

    fun setActive(isActive: Boolean) {
        _isActive.value = isActive
    }
}