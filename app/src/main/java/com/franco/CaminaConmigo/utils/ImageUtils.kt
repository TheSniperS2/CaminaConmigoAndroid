package com.franco.CaminaConmigo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {

    fun getCompressedBitmap(context: Context, imageUri: Uri, maxWidth: Int, maxHeight: Int): ByteArray {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        val width = originalBitmap.width
        val height = originalBitmap.height

        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth = if (width > height) maxWidth else (maxHeight * aspectRatio).toInt()
        val newHeight = if (height > width) maxHeight else (maxWidth / aspectRatio).toInt()

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        return outputStream.toByteArray()
    }
}