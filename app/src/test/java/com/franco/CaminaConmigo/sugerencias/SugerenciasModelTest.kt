package com.franco.CaminaConmigo.model_mvvm.sugerencias.model

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SugerenciasModelTest {

    private lateinit var model: SugerenciasModel

    @Before
    fun setUp() {
        model = SugerenciasModel("Nombre", "123456789", "Razon", "Mensaje", false)
    }

    @Test
    fun testModelFields() {
        assertEquals("Nombre", model.nombre)
        assertEquals("123456789", model.numero)
        assertEquals("Razon", model.razon)
        assertEquals("Mensaje", model.mensaje)
        assertEquals(false, model.esAnonimo)
    }
}