package com.example.drugreminder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * LoginActivity - ekran logowania dla istniejących użytkowników  
 *
 * Użytkownicy którzy już mają konto mogą się zalogować wpisując email i hasło.
 * Firebase sprawdza czy dane są poprawne i czy konto istnieje.
 * 
 * Podstawowe walidacje:
 * - Sprawdza czy pola nie są puste
 * - Firebase weryfikuje poprawność danych logowania
 *
 * Po udanym logowaniu przechodzi do głównego ekranu i zapamięta sesję.
 * Po nieudanym pokazuje komunikat o błędzie.
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
        // Pobieramy dane z formularza
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString()

        // Sprawdzamy czy użytkownik wpisał cokolwiek
        // Nie wysyłamy pustych danych do Firebase
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }

        // Próbujemy zalogować przez Firebase
        // Firebase sprawdzi czy email i hasło się zgadzają z bazą danych
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Udało się! Dane są poprawne
                    Toast.makeText(this, "Logowanie udane", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Zamykamy ekran logowania
                } else {
                    // Nie udało się - błędny email lub hasło
                    Toast.makeText(this, "Błędny login lub hasło", Toast.LENGTH_SHORT).show()
                }
            }
    }
}