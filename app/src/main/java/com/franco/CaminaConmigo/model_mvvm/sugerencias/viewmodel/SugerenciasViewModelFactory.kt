package com.franco.CaminaConmigo.model_mvvm.sugerencias.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.franco.CaminaConmigo.utils.MailerSendService

class SugerenciasViewModelFactory(private val mailerSendService: MailerSendService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SugerenciasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SugerenciasViewModel(mailerSendService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}