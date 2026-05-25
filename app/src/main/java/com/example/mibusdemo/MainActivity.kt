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


        // Declaramos las variables para el inicio de sesión
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnInicioSesion = findViewById<Button>(R.id.btnInicioSesion)
        val tvRegistrarse = findViewById<TextView>(R.id.tvRegistrarse)

        // Logica del boton de inicio de sesion
        btnInicioSesion.setOnClickListener {
            val email = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Intent para ir a la pantalla principal desde el inicio de sesion
                            val intent = Intent(this, principal::class.java)
                            startActivity(intent)
                            finish() // Cerramos login para que no se pueda volver a atras
                        } else {
                            // else para mostrar errores -- Debug :´c
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                // Mostar mensaje el usuario para completar los campos
                Toast.makeText(this, "Por favor, completa los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Logica del boton de registrarse
        tvRegistrarse.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Si el usuario ya inició sesión, que entre directo
    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            val intent = Intent(this, principal::class.java)
            startActivity(intent)
            finish()
        }
    }
}