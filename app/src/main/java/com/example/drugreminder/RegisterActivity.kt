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
 * Aktywność rejestracji użytkownika
 */
/**
 * Aktywność obsługująca rejestrację nowego użytkownika
 *
 * Funkcjonalności:
 * - Formularz rejestracji (email, hasło, potwierdzenie hasła)
 * - Wybór typu konta (pacjent/opiekun)
 * - Tworzenie konta w Firebase Auth
 * - Zapis danych użytkownika w Firestore
 *
 * Walidacje:
 * - Format email (obecność @)
 * - Siła hasła (min. 8 znaków, wielka litera, cyfra)
 * - Zgodność haseł
 * - Wybór typu konta
 *
 * Bezpieczeństwo:
 * - Szyfrowanie hasła (Firebase Auth)
 * - Unikalność adresu email
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
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<EditText>(R.id.et_password).text.toString()
        val confirmPassword = findViewById<EditText>(R.id.et_confirm_password).text.toString()
        val radioGroup = findViewById<RadioGroup>(R.id.rg_account_type)

        // Walidacja emaila
        if (!email.contains("@")) {
            Toast.makeText(this, "Nieprawidłowy format emaila", Toast.LENGTH_SHORT).show()
            return
        }

        // Walidacja hasła
        if (!isPasswordValid(password)) {
            Toast.makeText(this, "Hasło musi mieć min. 8 znaków, 1 wielką literę i 1 cyfrę", Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdzenie zgodności haseł
        if (password != confirmPassword) {
            Toast.makeText(this, "Hasła nie są identyczne", Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdzenie wyboru typu konta
        val selectedId = radioGroup.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "Wybierz typ konta", Toast.LENGTH_SHORT).show()
            return
        }

        val accountType = findViewById<RadioButton>(selectedId).text.toString().lowercase()

        // Rejestracja w Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToFirestore(email, accountType)
                } else {
                    Toast.makeText(this, "Błąd rejestracji: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun isPasswordValid(password: String): Boolean {
        val hasMinLength = password.length >= 8
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasMinLength && hasUpperCase && hasDigit
    }

    private fun saveUserToFirestore(email: String, accountType: String) {
        val user = User(email, accountType)

        db.collection("users").document(email)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Rejestracja zakończona pomyślnie", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd zapisu danych: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}