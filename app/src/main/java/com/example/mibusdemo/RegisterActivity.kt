package com.example.mibusdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log // Lo importe para ver errores en el logcat -- Debug :´c
import android.widget.Button
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

        // Inicializar botones
        val etCorreo = findViewById<EditText>(R.id.etCorreoCC)
        val etPassword = findViewById<EditText>(R.id.etPasswordCC)
        val btnCrearCuenta = findViewById<Button>(R.id.btnCrearCuenta)
        val btnTengoCuenta = findViewById<Button>(R.id.btnTengoCuenta)

        // Logica de crear cuenta
        btnCrearCuenta.setOnClickListener {
            val email = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Cuenta creada con éxito", Toast.LENGTH_SHORT)
                                .show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val error = task.exception?.message ?: "Error desconocido"
                            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                            Log.e("MiBusAuth", "Fallo al registrar: $error")
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Logica del boton de ya tengo una cuenta
        btnTengoCuenta.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}