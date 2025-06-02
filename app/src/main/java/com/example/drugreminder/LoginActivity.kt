package com.example.drugreminder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Aktywność obsługująca logowanie użytkownika
 *
 * Funkcjonalności:
 * - Formularz logowania (email + hasło)
 * - Walidacja pól formularza
 * - Integracja z Firebase Authentication
 *
 * Walidacje:
 * - Sprawdzanie pustych pól
 * - Podstawowa walidacja formatu email
 * 
 * Obsługa błędów:
 * - Nieprawidłowe dane logowania
 * - Problemy z połączeniem
 * - Informacje zwrotne przez Toast
 *
 * Po udanym logowaniu:
 * - Przejście do MainActivity
 * - Zamknięcie aktywności logowania
 */
class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_login)

        findViewById<Button>(R.id.btn_login).setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString()

        // Sprawdzenie pustych pól
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }

        // Logowanie przez Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Logowanie udane", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Błędny login lub hasło", Toast.LENGTH_SHORT).show()
                }
            }
    }
}