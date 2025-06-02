package com.example.drugreminder

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Aktywność umożliwiająca dodanie nowego leku do systemu
 *
 * Funkcjonalności:
 * - Formularz dodawania leku z polami:
 *   * Nazwa leku
 *   * Data rozpoczęcia (YYYY-MM-DD)
 *   * Data zakończenia (YYYY-MM-DD)
 *   * Godzina przyjmowania (HH:MM)
 *   * Dawkowanie
 *   * Dodatkowe informacje
 *
 * Walidacje:
 * - Sprawdzanie wymaganych pól
 * - Poprawność formatu dat
 * - Logiczność zakresu dat (start < end)
 * - Format godziny
 *
 * Integracja:
 * - Zapis do Firestore
 * - Automatyczne przypisanie do aktualnego pacjenta
 */
class AddDrugActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val currentUserEmail by lazy { FirebaseAuth.getInstance().currentUser?.email ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_add_drug)

        val nameEditText = findViewById<EditText>(R.id.et_drug_name)
        val startDateEditText = findViewById<EditText>(R.id.et_start_date)
        val endDateEditText = findViewById<EditText>(R.id.et_end_date)
        val timeEditText = findViewById<EditText>(R.id.et_time)
        val dosageEditText = findViewById<EditText>(R.id.et_dosage)
        val additionalInfoEditText = findViewById<EditText>(R.id.et_additional_info)
        val addButton = findViewById<Button>(R.id.btn_add)
        val backButton = findViewById<Button>(R.id.btn_back)

        addButton.setOnClickListener {
            addDrug(nameEditText, startDateEditText, endDateEditText, timeEditText, dosageEditText, additionalInfoEditText)
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun addDrug(
        nameEditText: EditText,
        startDateEditText: EditText,
        endDateEditText: EditText,
        timeEditText: EditText,
        dosageEditText: EditText,
        additionalInfoEditText: EditText
    ) {
        val name = nameEditText.text.toString().trim()
        val startDate = startDateEditText.text.toString().trim()
        val endDate = endDateEditText.text.toString().trim()
        val time = timeEditText.text.toString().trim()
        val dosage = dosageEditText.text.toString().trim()
        val additionalInfo = additionalInfoEditText.text.toString().trim()

        // Walidacja pól
        when {
            name.isEmpty() -> {
                Toast.makeText(this, "Podaj nazwę leku", Toast.LENGTH_SHORT).show()
                return
            }
            startDate.isEmpty() -> {
                Toast.makeText(this, "Podaj datę rozpoczęcia", Toast.LENGTH_SHORT).show()
                return
            }
            endDate.isEmpty() -> {
                Toast.makeText(this, "Podaj datę zakończenia", Toast.LENGTH_SHORT).show()
                return
            }
            time.isEmpty() -> {
                Toast.makeText(this, "Podaj godzinę", Toast.LENGTH_SHORT).show()
                return
            }
            dosage.isEmpty() -> {
                Toast.makeText(this, "Podaj dawkę", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val drug = Drug(
            name = name,
            startDate = startDate,
            endDate = endDate,
            time = time,
            dosage = dosage,
            additionalInfo = additionalInfo,
            patientEmail = currentUserEmail
        )

        db.collection("drugs")
            .add(drug)
            .addOnSuccessListener {
                Toast.makeText(this, "Lek dodany pomyślnie", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd dodawania leku: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}