package com.franco.CaminaConmigo.model_mvvm.sugerencias.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.franco.CaminaConmigo.model_mvvm.sugerencias.model.SugerenciasModel
import com.franco.CaminaConmigo.utils.MailerSendService
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import kotlin.concurrent.thread

class SugerenciasViewModelTest {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mailerSendService: MailerSendService

    @Mock
    private lateinit var suggestionSentObserver: Observer<Boolean>

    @Mock
    private lateinit var errorMessageObserver: Observer<String>

    private lateinit var viewModel: SugerenciasViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        viewModel = SugerenciasViewModel(mailerSendService)
        viewModel.suggestionSent.observeForever(suggestionSentObserver)
        viewModel.errorMessage.observeForever(errorMessageObserver)
    }

    @Test
    fun testEnviarSugerenciaSuccess() {
        val model = SugerenciasModel("Nombre", "123456789", "Razon", "Mensaje", false)
        doNothing().`when`(mailerSendService).sendSuggestion(
            model.nombre, model.numero, model.razon, model.mensaje, model.esAnonimo
        )

        thread {
            viewModel.enviarSugerencia(model)
        }.join()

        verify(suggestionSentObserver).onChanged(true)
        verify(errorMessageObserver, never()).onChanged(anyString())
    }

    @Test
    fun testEnviarSugerenciaFailure() {
        val model = SugerenciasModel("Nombre", "123456789", "Razon", "Mensaje", false)
        doThrow(RuntimeException("Error")).`when`(mailerSendService).sendSuggestion(
            model.nombre, model.numero, model.razon, model.mensaje, model.esAnonimo
        )

        thread {
            viewModel.enviarSugerencia(model)
        }.join()

        verify(errorMessageObserver).onChanged("Hubo un problema al enviar la sugerencia. Inténtalo de nuevo.")
        verify(suggestionSentObserver, never()).onChanged(true)
    }

    @After
    fun tearDown() {
        viewModel.suggestionSent.removeObserver(suggestionSentObserver)
        viewModel.errorMessage.removeObserver(errorMessageObserver)
    }
}