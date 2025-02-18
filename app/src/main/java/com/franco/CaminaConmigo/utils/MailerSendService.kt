package com.franco.CaminaConmigo.utils

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MailerSendService {

    private val apiKey = "mlsn.26eeb53c2e58beecca0eea8dfd0a25fabb71990ced628d5f37472a5d61ce1830"
    private val baseURL = "https://api.mailersend.com/v1/email"
    private val client = OkHttpClient()
    private val gson = Gson()

    fun sendSuggestion(nombre: String, numero: String, razon: String, mensaje: String, isAnonymous: Boolean) {
        val emailData = mapOf(
            "from" to mapOf(
                "email" to "sugerencias@trial-zr6ke4n3k53lon12.mlsender.net",
                "name" to "Sistema de Sugerencias"
            ),
            "to" to listOf(
                mapOf(
                    "email" to "camina.conmigo4r@gmail.com",
                    "name" to "Administrador"
                )
            ),
            "subject" to "Nueva Sugerencia: $razon",
            "text" to """
                Nombre: ${if (isAnonymous) "Anónimo" else nombre}
                Número: ${if (isAnonymous) "No proporcionado" else numero}
                Razón: $razon
                
                Mensaje:
                $mensaje
            """.trimIndent()
        )

        val requestBody = gson.toJson(emailData).toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(baseURL)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    throw IOException("Error al enviar el correo: ${response.message}")
                }
            }
        })
    }
}