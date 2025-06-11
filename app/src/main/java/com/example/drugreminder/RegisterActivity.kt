package com.example.drugreminder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * RegisterActivity - ekran tworzenia nowego konta
 *
 * Nowi użytkownicy mogą założyć konto wpisując email, hasło i wybierając typ konta.
 * Aplikacja sprawdza czy dane są poprawne i czy hasło jest wystarczająco silne.
 * Typ konta (pacjent/opiekun) decyduje o dostępnych funkcjach.
 * 
 * Walidacje:
 * - Email musi zawierać @
 * - Hasło min. 8 znaków, 1 wielka litera, 1 cyfra
 * - Oba hasła muszą być identyczne
 * - Użytkownik musi wybrać typ konta
 *
 * Po udanej rejestracji tworzy konto w Firebase i zapisuje dane w bazie.
 */

class RegisterActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_register)

        findViewById<Button>(R.id.btn_register).setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        // Pobieramy dane z formularza
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString()
        val confirmPassword = findViewById<EditText>(R.id.et_confirm_password).text.toString()
        val radioGroup = findViewById<RadioGroup>(R.id.rg_account_type)

        // Sprawdzamy czy email wygląda jak email
        // Podstawowa walidacja - email musi mieć znak @
        if (!email.contains("@")) {
            Toast.makeText(this, "Nieprawidłowy format emaila", Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdzamy czy hasło jest wystarczająco silne
        // Słabe hasła są łatwe do złamania przez hakerów
        if (!isPasswordValid(password)) {
            Toast.makeText(this, "Hasło musi mieć min. 8 znaków, 1 wielką literę i 1 cyfrę", Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdzamy czy użytkownik wpisał to samo hasło dwa razy
        // Zabezpiecza przed literówkami przy wpisywaniu hasła
        if (password != confirmPassword) {
            Toast.makeText(this, "Hasła nie są identyczne", Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdzamy czy wybrał typ konta
        // Aplikacja musi wiedzieć czy to pacjent czy opiekun
        val selectedId = radioGroup.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "Wybierz typ konta", Toast.LENGTH_SHORT).show()
            return
        }

        val accountType = findViewById<RadioButton>(selectedId).text.toString().lowercase()

        // Tworzymy konto w Firebase
        // Firebase będzie pamiętać login i hasło użytkownika
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Konto stworzone - teraz zapisujemy dodatkowe informacje
                    saveUserToFirestore(email, accountType)
                } else {
                    // Coś poszło nie tak - może email już istnieje
                    Toast.makeText(this, "Błąd rejestracji: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun isPasswordValid(password: String): Boolean {
        // Sprawdzamy 3 rzeczy naraz:
        val hasMinLength = password.length >= 8        // Czy ma przynajmniej 8 znaków
        val hasUpperCase = password.any { it.isUpperCase() }  // Czy ma wielką literę (A,B,C...)
        val hasDigit = password.any { it.isDigit() }          // Czy ma cyfrę (0,1,2...)
        
        // Wszystkie 3 warunki muszą być spełnione
        // Silne hasło chroni konto przed włamaniem
        return hasMinLength && hasUpperCase && hasDigit
    }

    private fun saveUserToFirestore(email: String, accountType: String) {
        // Tworzymy obiekt z danymi użytkownika
        val user = User(email, accountType)

        // Zapisujemy go w bazie danych
        // Dzięki temu aplikacja będzie wiedzieć jaki typ konta ma użytkownik
        db.collection("users").document(email)  // Używamy emaila jako ID
            .set(user)
            .addOnSuccessListener {
                // Udało się! Idziemy do głównego ekranu
                Toast.makeText(this, "Rejestracja zakończona pomyślnie", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish() // Zamykamy ekran rejestracji
            }
            .addOnFailureListener { e ->
                // Nie udało się zapisać do bazy danych
                Toast.makeText(this, "Błąd zapisu danych: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}