package com.franco.CaminaConmigo.model_mvvm.perfil.view

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.franco.CaminaConmigo.databinding.ActivityMiperfilBinding
import com.franco.CaminaConmigo.model_mvvm.menu.view.MenuActivity
import com.franco.CaminaConmigo.model_mvvm.perfil.viewmodel.MiPerfilViewModel
import jp.wasabeef.glide.transformations.CropCircleTransformation

class MiPerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMiperfilBinding
    private lateinit var viewModel: MiPerfilViewModel
    private var isUpdatingSwitch = false  // Evita que se active el listener al recuperar datos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMiperfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = MiPerfilViewModel(this)
        setupUI()
    }

    private fun setupUI() {
        viewModel.getUser { user ->
            if (user != null) {
                runOnUiThread {
                    binding.textView14.text = user.name
                    binding.textView16.text = user.username ?: ""  // Permite que esté vacío
                    binding.textView18.text = user.profileType
                    // Usar Glide para cargar la imagen desde la URL y hacerla circular
                    Glide.with(this)
                        .load(user.photoURL)
                        .transform(CropCircleTransformation())
                        .into(binding.imageView23)

                    // Recuperar el estado del switch sin activar el listener
                    isUpdatingSwitch = true
                    binding.switch1.isChecked = user.profileType == "Privado"
                    isUpdatingSwitch = false
                }
            }
        }

        binding.atras.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.imageButton12.setOnClickListener {
            showEditDialog("Editar Nombre de Usuario", binding.textView16) { newValue ->
                binding.textView16.text = newValue
                viewModel.saveUserUsername(newValue)
            }
        }

        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSwitch) {
                val newProfileType = if (isChecked) "Privado" else "Público"
                binding.textView18.text = newProfileType
                viewModel.saveUserProfileType(newProfileType)
                Toast.makeText(this, "Modo cambiado a $newProfileType", Toast.LENGTH_SHORT).show()
            }
        }

        binding.imageView23.setOnClickListener {
            // Lógica para cambiar la imagen de perfil
            selectImageFromGallery()
        }

        binding.textView26.setOnClickListener {
            // Lógica para cambiar la imagen de perfil
            selectImageFromGallery()
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            val imageUri = data?.data ?: return
            binding.imageView23.setImageURI(imageUri)
            viewModel.uploadProfileImage(imageUri) { photoURL ->
                if (photoURL != null) {
                    Glide.with(this)
                        .load(photoURL)
                        .transform(CropCircleTransformation())
                        .into(binding.imageView23)
                } else {
                    Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                }
            }
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

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
    }
}