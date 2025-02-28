package com.franco.CaminaConmigo.ayuda

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.franco.CaminaConmigo.model_mvvm.ayuda.model.AyudaModel
import com.franco.CaminaConmigo.model_mvvm.ayuda.viewmodel.AyudaViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class AyudaViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var observer: Observer<AyudaModel>

    private lateinit var viewModel: AyudaViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = AyudaViewModel()
    }

    @Test
    fun testAyudaData() {
        val ayudaModel = AyudaModel()
        viewModel.ayudaData.observeForever(observer)
        verify(observer).onChanged(ayudaModel)
    }
}