package com.franco.CaminaConmigo.model_mvvm.sugerencias.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.franco.CaminaConmigo.model_mvvm.sugerencias.model.SugerenciasModel
import com.franco.CaminaConmigo.utils.MailerSendService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SugerenciasViewModel(private val mailerSendService: MailerSendService) : ViewModel() {

    private val _suggestionSent = MutableLiveData<Boolean>()
    val suggestionSent: LiveData<Boolean> get() = _suggestionSent

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun enviarSugerencia(model: SugerenciasModel) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    mailerSendService.sendSuggestion(
                        model.nombre,
                        model.numero,
                        model.razon,
                        model.mensaje,
                        model.esAnonimo
                    )
                }
                _suggestionSent.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.postValue("Hubo un problema al enviar la sugerencia. Inténtalo de nuevo.")
            }
        }
    }
}