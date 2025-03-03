package com.franco.CaminaConmigo.model_mvvm.chat.model

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class LocationMessage(
    val id: String = "",
    val senderId: String = "",
    val timestamp: Timestamp = Timestamp(0, 0),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isActive: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readParcelable(Timestamp::class.java.classLoader) ?: Timestamp(0, 0),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(senderId)
        parcel.writeParcelable(timestamp, flags)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeByte(if (isActive) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocationMessage> {
        override fun createFromParcel(parcel: Parcel): LocationMessage {
            return LocationMessage(parcel)
        }

        override fun newArray(size: Int): Array<LocationMessage?> {
            return arrayOfNulls(size)
        }

        fun fromDocumentSnapshot(document: DocumentSnapshot): LocationMessage {
            return LocationMessage(
                id = document.id,
                senderId = document.getString("senderId") ?: "",
                timestamp = document.getTimestamp("timestamp") ?: Timestamp(0, 0),
                latitude = document.getDouble("latitude") ?: 0.0,
                longitude = document.getDouble("longitude") ?: 0.0,
                isActive = document.getBoolean("isActive") ?: false
            )
        }
    }
}