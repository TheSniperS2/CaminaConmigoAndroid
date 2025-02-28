package com.franco.CaminaConmigo.model_mvvm.ayuda.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.franco.CaminaConmigo.model_mvvm.ayuda.model.AyudaModel

class AyudaViewModel : ViewModel() {

    private val _ayudaData = MutableLiveData<AyudaModel>()
    val ayudaData: LiveData<AyudaModel> get() = _ayudaData

    init {
        // Inicializamos con datos por defecto
        _ayudaData.value = AyudaModel()
    }
}