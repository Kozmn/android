package com.example.drugreminder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * StartActivity - pierwszy ekran po uruchomieniu aplikacji
 *
 * Sprawdza czy użytkownik jest już zalogowany w Firebase.
 * Jeśli tak, od razu przechodzi do głównego ekranu z lekami.
 * Jeśli nie, pokazuje przyciski do logowania i rejestracji.
 *
 * Dzięki temu nie trzeba logować się za każdym razem gdy otwiera się aplikację.
 */
class StartActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_start)

        // Sprawdzamy czy ktoś jest już zalogowany
        // Firebase pamięta sesję więc nie trzeba logować się za każdym razem
        auth.currentUser?.let {
            // Jeśli ktoś jest zalogowany, od razu idziemy do głównego ekranu
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Zamykamy ten ekran żeby nie można było wrócić
            return
        }

        // Jeśli nikt nie jest zalogowany, pokazujemy przyciski
        setupButtons()
    }

    private fun setupButtons() {
        // Przycisk "Zaloguj się" otwiera ekran logowania
        findViewById<Button>(R.id.btn_login).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        
        // Przycisk "Zarejestruj się" otwiera ekran rejestracji
        findViewById<Button>(R.id.btn_register).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}