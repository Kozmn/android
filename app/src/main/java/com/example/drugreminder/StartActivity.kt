package com.example.drugreminder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Ekran startowy aplikacji (Launcher Activity)
 *
 * Funkcjonalności:
 * - Sprawdzanie stanu logowania użytkownika
 * - Przekierowanie do MainActivity dla zalogowanych użytkowników
 * - Przyciski do rejestracji i logowania dla nowych użytkowników
 *
 * Przepływ:
 * 1. Sprawdzenie stanu auth w Firebase
 * 2. Jeśli użytkownik zalogowany -> MainActivity
 * 3. Jeśli niezalogowany -> wyświetlenie przycisków:
 *    - Zaloguj się -> LoginActivity
 *    - Zarejestruj się -> RegisterActivity
 *
 * Design:
 * - Prosty, przejrzysty interfejs
 * - Logo aplikacji
 * - Opis funkcjonalności
 */
class StartActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_start)

        // Sprawdzenie czy użytkownik jest zalogowany
        auth.currentUser?.let {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupButtons()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_login).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_register).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}