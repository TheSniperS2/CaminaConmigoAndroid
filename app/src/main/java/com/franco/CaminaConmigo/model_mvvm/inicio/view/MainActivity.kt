package com.franco.CaminaConmigo.model_mvvm.inicio.view

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.franco.CaminaConmigo.databinding.ActivityMainBinding
import com.franco.CaminaConmigo.model_mvvm.inicio.viewmodel.GoogleSignInViewModel
import com.franco.CaminaConmigo.model_mvvm.mapa.view.InstruccionesBottomSheetDialogFragment
import com.franco.CaminaConmigo.model_mvvm.mapa.view.MapaActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth

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

        // Vincula el botón para continuar como invitado
        binding.button.setOnClickListener {
            continueAsGuest()
        }

        // Observa los cambios en la cuenta de usuario
        googleSignInViewModel.accountLiveData.observe(this) { account ->
            account?.let {
                // Verificar si la cuenta es nueva
                val user = FirebaseAuth.getInstance().currentUser
                val isNewUser = user?.metadata?.creationTimestamp == user?.metadata?.lastSignInTimestamp

                // Si es un usuario nuevo, muestra el tutorial y configura las notificaciones
                if (isNewUser) {
                    setupDefaultNotifications()
                    checkAndShowTutorial(this)
                }

                // Redirige al perfil del usuario (MapaActivity)
                val intent = Intent(this, MapaActivity::class.java)
                startActivity(intent)
                finish()  // Cierra la actividad principal
            }
        }
    }

    // Función para continuar como invitado
    private fun continueAsGuest() {
        googleSignInViewModel.continueAsGuest()
        val intent = Intent(this, MapaActivity::class.java)
        startActivity(intent)
        finish()  // Cierra la actividad principal
    }

    // Función para configurar las notificaciones por defecto
    private fun setupDefaultNotifications() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("configuraciones", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("notificaciones_grupos", true)
            putBoolean("notificaciones_reporte", true)
            apply()
        }
    }

    // Función para verificar y mostrar el tutorial
    private fun checkAndShowTutorial(context: FragmentActivity) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val hasSeenTutorial = sharedPreferences.getBoolean("hasSeenTutorial", false)

        if (!hasSeenTutorial) {
            // Mostrar el tutorial
            val tutorialDialog = InstruccionesBottomSheetDialogFragment()
            tutorialDialog.show(context.supportFragmentManager, "TutorialDialog")

            // Guardar que el usuario ya lo vio para que no se repita
            sharedPreferences.edit().putBoolean("hasSeenTutorial", true).apply()
        }
    }

    // Método para iniciar sesión con Google
    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }
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