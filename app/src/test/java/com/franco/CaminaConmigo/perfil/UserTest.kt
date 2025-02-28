package com.franco.CaminaConmigo.model_mvvm.perfil.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class UserTest {

    private lateinit var user: User

    @Before
    fun setUp() {
        user = User(
            name = "John Doe",
            username = "johndoe",
            profileType = "Público",
            email = "johndoe@example.com",
            id = "12345",
            joinDate = Timestamp.now(),
            photoURL = "http://example.com/photo.jpg"
        )
    }

    @Test
    fun testUserFields() {
        assertEquals("John Doe", user.name)
        assertEquals("johndoe", user.username)
        assertEquals("Público", user.profileType)
        assertEquals("johndoe@example.com", user.email)
        assertEquals("12345", user.id)
        assertNotNull(user.joinDate)
        assertEquals("http://example.com/photo.jpg", user.photoURL)
    }
}