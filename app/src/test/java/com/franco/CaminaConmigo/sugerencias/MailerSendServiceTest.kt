package com.franco.CaminaConmigo.utils

import android.content.Context
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.IOException

class MailerSendServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var client: OkHttpClient

    @Mock
    private lateinit var call: Call

    private lateinit var mailerSendService: MailerSendService
    private val gson = Gson()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mailerSendService = MailerSendService(context)
    }

    @Test
    fun testSendSuggestionSuccess() {
        val response = Response.Builder()
            .request(Request.Builder().url("https://api.mailersend.com/v1/email").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create("application/json".toMediaTypeOrNull(), "{}"))
            .build()

        `when`(client.newCall(any(Request::class.java))).thenReturn(call)
        `when`(call.execute()).thenReturn(response)

        mailerSendService.sendSuggestion("Nombre", "123456789", "Razon", "Mensaje", false)

        verify(call).execute()
    }

    @Test(expected = IOException::class)
    fun testSendSuggestionFailure() {
        val response = Response.Builder()
            .request(Request.Builder().url("https://api.mailersend.com/v1/email").build())
            .protocol(Protocol.HTTP_1_1)
            .code(400)
            .message("Bad Request")
            .body(ResponseBody.create("application/json".toMediaTypeOrNull(), "{}"))
            .build()

        `when`(client.newCall(any(Request::class.java))).thenReturn(call)
        `when`(call.execute()).thenReturn(response)

        mailerSendService.sendSuggestion("Nombre", "123456789", "Razon", "Mensaje", false)
    }
}