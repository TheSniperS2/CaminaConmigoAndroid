package com.franco.CaminaConmigo.model_mvvm.ayuda.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AyudaModelTest {

    private lateinit var model: AyudaModel

    @Before
    fun setUp() {
        model = AyudaModel()
    }

    @Test
    fun testAyudaModelCreation() {
        assertNotNull(model)
    }

    @Test
    fun testDefaultEmail() {
        assertEquals("centroliwen@laserena.cl", model.email)
    }

    @Test
    fun testDefaultPhoneNumbers() {
        val expectedPhoneNumbers = listOf("51-2641850", "51-2427844", "961244738")
        assertEquals(expectedPhoneNumbers, model.phoneNumbers)
    }
}