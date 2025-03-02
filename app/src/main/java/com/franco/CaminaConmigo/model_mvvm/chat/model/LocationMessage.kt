package com.franco.CaminaConmigo.model_mvvm.chat.model

import android.os.Parcel
import android.os.Parcelable

data class LocationMessage(
    val id: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isActive: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(senderId)
        parcel.writeLong(timestamp)
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
    }
}