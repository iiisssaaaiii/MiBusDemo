package com.example.mibusdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etCorreoCC)
        val etPassword = findViewById<EditText>(R.id.etPasswordCC)
        val etConfirmarPassword = findViewById<EditText>(R.id.etConfirmarPasswordInput)
        val cbTerminos = findViewById<CheckBox>(R.id.cbTerminosCondiciones)
        val btnCrearCuenta = findViewById<Button>(R.id.btnCrearCuenta)
        val btnTengoCuenta = findViewById<Button>(R.id.btnTengoCuenta)

        btnCrearCuenta.setOnClickListener {
            val name = etNombre.text.toString().trim()
            val email = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmarPassword.text.toString().trim()

            when {
                name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                password != confirmPassword ->
                    Toast.makeText(this, "Las contrasenas no coinciden", Toast.LENGTH_SHORT).show()
                !cbTerminos.isChecked ->
                    Toast.makeText(this, "Acepta terminos y condiciones", Toast.LENGTH_SHORT).show()
                else -> crearCuenta(name, email, password)
            }
        }

        btnTengoCuenta.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun crearCuenta(name: String, email: String, password: String) {
        DemoSession.register(this, name, email, password)
        Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, ParadasCercanasActivity::class.java))
        finish()

        auth.createUserWithEmailAndPassword(email, password)
    }
}
