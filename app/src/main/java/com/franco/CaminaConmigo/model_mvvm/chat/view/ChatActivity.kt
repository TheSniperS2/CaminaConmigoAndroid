package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.model_mvvm.ayuda.view.AyudaActivity
import com.franco.CaminaConmigo.model_mvvm.mapa.view.MapaActivity
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity
import com.franco.CaminaConmigo.model_mvvm.novedad.view.NovedadActivity

class ChatActivity : AppCompatActivity() {

    // Ejemplo de CardViews para cada chat
    private lateinit var cardViewVictor: CardView
    private lateinit var cardViewJane: CardView
    private lateinit var cardViewFamilia: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Inicializamos los CardViews
        cardViewVictor = findViewById(R.id.fragmentContainerView2)
        cardViewJane = findViewById(R.id.fragmentContainerView4)
        cardViewFamilia = findViewById(R.id.fragmentContainerView5)

        // Configuramos clics para cada CardView y redireccionamos al detalle del chat
        cardViewVictor.setOnClickListener {
            openChatDetail("Victor")
        }

        cardViewJane.setOnClickListener {
            openChatDetail("Jane Doe")
        }

        cardViewFamilia.setOnClickListener {
            openChatDetail("Familia")
        }

        // TextViews para "Añadir Amigo" y "Crear Grupo"
        val textViewAddFriend = findViewById<TextView>(R.id.textView55)
        val textViewCreateGroup = findViewById<TextView>(R.id.textView57)

        textViewAddFriend.setOnClickListener {
            // Ejemplo: redireccionar a AddFriendActivity (asegúrate de tenerla creada y declarada en el manifest)
            //val intent = Intent(this, AddFriendActivity::class.java)
            //startActivity(intent)
        }

        textViewCreateGroup.setOnClickListener {
            // Ejemplo: redireccionar a CreateGroupActivity
            //val intent = Intent(this, CreateGroupActivity::class.java)
            //startActivity(intent)
        }

        // Configuramos la barra de navegación inferior
        val btnMapa = findViewById<ImageButton>(R.id.imageButton10)
        val btnNovedad = findViewById<ImageButton>(R.id.imageButton11)
        val btnChat = findViewById<ImageButton>(R.id.imageButton12)
        val btnAyuda = findViewById<ImageButton>(R.id.imageButton13)
        val btnMenu = findViewById<ImageButton>(R.id.imageButton14)

        btnMapa.setOnClickListener {
            // Ejemplo: redireccionar a MapaActivity
            val intent = Intent(this, MapaActivity::class.java)
            startActivity(intent)
        }

        btnNovedad.setOnClickListener {
            // Ejemplo: redireccionar a NovedadActivity
            val intent = Intent(this, NovedadActivity::class.java)
            startActivity(intent)
        }

        btnChat.setOnClickListener {
            // Ya estás en ChatActivity, pero podrías refrescar la pantalla o mostrar un mensaje.
        }

        btnAyuda.setOnClickListener {
            // Ejemplo: redireccionar a AyudaActivity
            val intent = Intent(this, AyudaActivity::class.java)
            startActivity(intent)
        }

        btnMenu.setOnClickListener {
            // Ejemplo: redireccionar a MenuActivity
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Método para abrir el detalle del chat.
     */
    private fun openChatDetail(chatName: String) {
        val intent = Intent(this, ChatDetailActivity::class.java)
        intent.putExtra("CHAT_NAME", chatName)
        startActivity(intent)
    }
}
