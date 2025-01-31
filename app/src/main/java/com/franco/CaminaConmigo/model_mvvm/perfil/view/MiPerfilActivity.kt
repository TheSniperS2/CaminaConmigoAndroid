package com.franco.CaminaConmigo.model_mvvm.perfil.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.franco.CaminaConmigo.databinding.ActivityMiperfilBinding
import com.franco.CaminaConmigo.model_mvvm.perfil.viewmodel.MiPerfilViewModel
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity

class MiPerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMiperfilBinding
    private lateinit var viewModel: MiPerfilViewModel
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMiperfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = MiPerfilViewModel(this)
        setupUI()
    }

    private fun setupUI() {
        val user = viewModel.getUser()

        if (user != null) {
            binding.textView14.text = user.name
            binding.textView16.text = user.username
            binding.textView18.text = "Privado" // Texto fijo
            binding.switch1.isChecked = user.isPrivate

            if (user.photoUrl != null) {
                Glide.with(this)
                    .load(user.photoUrl)
                    .circleCrop()
                    .into(binding.imageView23)
            }
        }

// Listeners
        binding.atras.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish() // Opcional, si quieres cerrar la actividad actual
        }


        binding.imageButton11.setOnClickListener {
            showEditDialog("Editar Nombre", binding.textView14) { newValue ->
                binding.textView14.text = newValue
                viewModel.saveUserName(newValue)
            }
        }

        binding.imageButton12.setOnClickListener {
            showEditDialog("Editar Nombre de Usuario", binding.textView16) { newValue ->
                binding.textView16.text = newValue
                viewModel.saveUserUsername(newValue)
            }
        }

        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setPrivacy(isChecked)
        }

        binding.imageView23.setOnClickListener {
            openImageChooser()
        }
    }

    private fun showEditDialog(title: String, textView: TextView, onSave: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)

        val input = EditText(this)
        input.setText(textView.text.toString())
        builder.setView(input)

        builder.setPositiveButton("Guardar") { _, _ ->
            val newValue = input.text.toString()
            onSave(newValue)
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri = data.data!!

            viewModel.saveProfileImage(imageUri.toString())
            Glide.with(this)
                .load(imageUri)
                .circleCrop()
                .into(binding.imageView23)
        }
    }
}
