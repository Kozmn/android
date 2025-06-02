package com.example.drugreminder

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Aktywność umożliwiająca dodanie opiekuna do konta pacjenta
 *
 * Funkcjonalności:
 * - Wprowadzanie adresu email opiekuna
 * - Weryfikacja istnienia konta opiekuna
 * - Przypisanie opiekuna do pacjenta
 *
 * Walidacje:
 * - Sprawdzenie czy email istnieje w systemie
 * - Weryfikacja typu konta (musi być opiekun)
 * - Zabezpieczenie przed dodaniem samego siebie
 *
 * Bezpieczeństwo:
 * - Sprawdzanie uprawnień
 * - Aktualizacja listy opiekunów w Firestore
 * - Obsługa błędów i komunikaty
 */
class AddCaregiverActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val currentUserEmail by lazy { FirebaseAuth.getInstance().currentUser?.email ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_add_caregiver)

        val caregiverEmailEditText = findViewById<EditText>(R.id.et_caregiver_email)
        val addButton = findViewById<Button>(R.id.btn_add)
        val backButton = findViewById<Button>(R.id.btn_back)

        addButton.setOnClickListener {
            addCaregiver(caregiverEmailEditText.text.toString().trim())
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun addCaregiver(caregiverEmail: String) {
        // Walidacja pola
        if (caregiverEmail.isEmpty()) {
            Toast.makeText(this, "Podaj email opiekuna", Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdzenie czy nie dodaje siebie
        if (caregiverEmail == currentUserEmail) {
            Toast.makeText(this, "Nie możesz dodać siebie jako opiekuna", Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdzenie czy email istnieje w bazie
        db.collection("users").document(caregiverEmail)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)
                    when {
                        user != null && user.isCaregiver() -> {
                            addCaregiverToPatient(caregiverEmail)
                        }
                        user != null && user.isPatient() -> {
                            Toast.makeText(this, "Podany email należy do pacjenta, nie opiekuna", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Błąd danych użytkownika", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Nie ma takiego emaila w bazie", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd sprawdzania emaila: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addCaregiverToPatient(caregiverEmail: String) {
        db.collection("users").document(currentUserEmail)
            .update("caregivers", FieldValue.arrayUnion(caregiverEmail))
            .addOnSuccessListener {
                Toast.makeText(this, "Opiekun dodany pomyślnie", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd dodawania opiekuna: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}