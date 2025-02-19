package com.franco.CaminaConmigo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.franco.CaminaConmigo.R
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class StorageService(private val context: Context) {
    private val storage: StorageReference = FirebaseStorage.getInstance().reference

    suspend fun uploadLogo(): String {
        val image = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val imageData = baos.toByteArray()

        val logoRef = storage.child("drawable/logo.png")

        logoRef.putBytes(imageData).await()
        val downloadURL = logoRef.downloadUrl.await()

        return downloadURL.toString()
    }
}