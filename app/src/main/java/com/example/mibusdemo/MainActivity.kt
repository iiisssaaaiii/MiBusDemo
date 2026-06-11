package com.example.mibusdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnInicioSesion = findViewById<Button>(R.id.btnInicioSesion)
        val tvRegistrarse = findViewById<TextView>(R.id.tvRegistrarse)

        btnInicioSesion.setOnClickListener {
            val email = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Completa correo y contrasena", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (DemoSession.login(this, email, password)) {
                abrirInicio()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        abrirInicio()
                    } else {
                        Toast.makeText(
                            this,
                            "No se pudo entrar. Revisa tu cuenta o crea una nueva.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        tvRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null || DemoSession.isLoggedIn(this)) {
            abrirInicio()
        }
    }

    private fun abrirInicio() {
        startActivity(Intent(this, ParadasCercanasActivity::class.java))
        finish()
    }
}
