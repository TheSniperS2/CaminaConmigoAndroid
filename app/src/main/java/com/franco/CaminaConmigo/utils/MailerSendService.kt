package com.franco.CaminaConmigo.utils

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MailerSendService(private val context: Context) {

    private val apiKey = "mlsn.26eeb53c2e58beecca0eea8dfd0a25fabb71990ced628d5f37472a5d61ce1830"
    private val baseURL = "https://api.mailersend.com/v1/email"
    private val client = OkHttpClient()
    private val gson = Gson()
    private val storageService = StorageService(context)
    private var logoURL: String? = null

    init {
        // Cargar el logo al inicializar el servicio
        runBlocking {
            try {
                logoURL = storageService.uploadLogo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendSuggestion(nombre: String, numero: String, razon: String, mensaje: String, isAnonymous: Boolean) {
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 400px;
                        margin: 0 auto;
                        padding: 15px;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 20px;
                        background-color: #f9f9f9;
                        padding: 15px;
                        border-radius: 8px;
                    }
                    .logo {
                        max-width: 150px;
                        height: auto;
                        margin-bottom: 15px;
                    }
                    .content {
                        background-color: #f9f9f9;
                        padding: 15px;
                        border-radius: 8px;
                    }
                    .field {
                        margin-bottom: 12px;
                    }
                    .label {
                        font-weight: bold;
                        color: #EF6098;
                    }
                    .message-box {
                        background-color: white;
                        padding: 12px;
                        border-radius: 5px;
                        margin-top: 8px;
                    }
                    .title {
                        color: #EF6098;
                        margin-top: 15px;
                        font-size: 1.5em;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    ${logoURL?.let { "<img src='$it' alt='CaminaConmigo Logo' class='logo'>" } ?: ""}
                    <h2 class="title">Nueva Sugerencia Recibida</h2>
                </div>
                <div class="content">
                    <div class="field">
                        <span class="label">Nombre:</span> 
                        <span>${if (isAnonymous) "Anónimo" else nombre}</span>
                    </div>
                    <div class="field">
                        <span class="label">Número:</span>
                        <span>${if (isAnonymous) "No proporcionado" else numero}</span>
                    </div>
                    <div class="field">
                        <span class="label">Razón:</span>
                        <span>$razon</span>
                    </div>
                    <div class="field">
                        <span class="label">Mensaje:</span>
                        <div class="message-box">
                            ${mensaje.replace("\n", "<br>")}
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """

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
            "html" to htmlContent
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