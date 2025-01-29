package com.franco.CaminaConmigo.model_mvvm.inicio.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.franco.CaminaConmigo.databinding.ActivityMainBinding
import com.franco.CaminaConmigo.model_mvvm.inicio.viewmodel.GoogleSignInViewModel
import com.franco.CaminaConmigo.model_mvvm.perfil.view.MiPerfilActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val googleSignInViewModel: GoogleSignInViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración de Google Sign-In
        val gso = googleSignInViewModel.getSignInOptions()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Vincula el botón para iniciar sesión con Google
        binding.button2.setOnClickListener {
            signInWithGoogle()
        }

        // Observa los cambios en la cuenta de usuario
        googleSignInViewModel.accountLiveData.observe(this) { account ->
            account?.let {
                // Redirige al perfil del usuario
                val intent = Intent(this, MiPerfilActivity::class.java)
                startActivity(intent)
                finish()  // Cierra la actividad principal
            }
        }
    }

    /* Método para iniciar sesión con Google */
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                googleSignInViewModel.handleSignInResult(task) { success ->
                    if (success) {
                        // El inicio de sesión fue exitoso, no se necesita mostrar un mensaje.
                    } else {
                        Toast.makeText(this, "Autenticación fallida. Por favor intenta nuevamente.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Inicio de sesión cancelado. Por favor intenta nuevamente.", Toast.LENGTH_SHORT).show()
            }
        }
}
