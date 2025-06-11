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
 * AddCaregiverActivity - aktywność zarządzająca relacjami pacjent-opiekun
 *
 * Architektura relacji "na chłopski rozum":
 * W aplikacji mamy dwustronną relację między pacjentem a opiekunem:
 * - PACJENT może dodać opiekuna do swojego konta
 * - OPIEKUN automatycznie uzyskuje dostęp do leków pacjenta
 * - Relacja many-to-many: 1 pacjent może mieć wielu opiekunów, 1 opiekun może monitorować wielu pacjentów
 *
 * Funkcjonalności główne:
 * - Wprowadzanie emaila opiekuna przez pacjenta
 * - Weryfikacja istnienia konta opiekuna w systemie  
 * - Sprawdzenie czy wprowadzony email należy rzeczywiście do opiekuna (nie pacjenta)
 * - Aktualizacja listy opiekunów w dokumencie pacjenta
 *
 * Walidacje bezpieczeństwa:
 * - Sprawdzenie czy email istnieje w kolekcji "users"
 * - Weryfikacja typu konta (accountType == "opiekun")
 * - Zabezpieczenie przed dodaniem samego siebie jako opiekuna
 * - Obsługa przypadków brzegowych i błędów sieciowych
 *
 * Model danych Firestore:
 * - Tylko dokument PACJENTA zawiera listę "caregivers" 
 * - Opiekun NIE ma listy swoich pacjentów (znalezienie przez zapytanie)
 * - Asymetryczna relacja: pacjent "posiada" opiekuna, nie odwrotnie
 *
 * Integracje zewnętrzne:
 * - Firebase Firestore: sprawdzenie istnienia użytkownika, aktualizacja listy
 * - Firebase Authentication: pobranie emaila aktualnie zalogowanego pacjenta
 * - FieldValue.arrayUnion(): atomowe dodanie do tablicy bez duplikatów
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

    /**
     * Główna metoda dodawania opiekuna - wieloetapowa walidacja i zapis
     * 
     * Proces weryfikacji "na chłopski rozum":
     * 
     * ETAP 1: Walidacja podstawowa
     * - Czy pole nie jest puste?
     * - Czy pacjent nie próbuje dodać siebie? (logicznie bez sensu)
     * 
     * ETAP 2: Sprawdzenie istnienia w bazie
     * - Czy podany email istnieje w kolekcji "users"?
     * - Używamy document(email).get() bo email to unikalny klucz
     * 
     * ETAP 3: Weryfikacja typu konta
     * - Czy znaleziony użytkownik ma accountType == "opiekun"?
     * - Blokujemy dodanie pacjenta jako opiekuna (różne uprawnienia)
     * 
     * ETAP 4: Aktualizacja relacji
     * - Dodanie emaila opiekuna do listy "caregivers" w dokumencie pacjenta
     * - Używamy FieldValue.arrayUnion() - atomowa operacja bez duplikatów
     * 
     * Dlaczego taka skomplikowana walidacja?
     * - Firestore nie ma constraints jak SQL
     * - Musimy sami zapewnić integralność danych
     * - Błędne relacje mogłyby doprowadzić do problemów z uprawnieniami
     */
    private fun addCaregiver(caregiverEmail: String) {
        // ETAP 1: Walidacja podstawowa danych wejściowych
        if (caregiverEmail.isEmpty()) {
            Toast.makeText(this, "Podaj email opiekuna", Toast.LENGTH_SHORT).show()
            return
        }

        // Zabezpieczenie przed logicznym błędem - pacjent nie może być swoim opiekunem
        if (caregiverEmail == currentUserEmail) {
            Toast.makeText(this, "Nie możesz dodać siebie jako opiekuna", Toast.LENGTH_SHORT).show()
            return
        }

        // ETAP 2: Asynchroniczne sprawdzenie istnienia użytkownika w Firestore
        db.collection("users").document(caregiverEmail)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // ETAP 3: Walidacja typu konta znalezionego użytkownika
                    val user = documentSnapshot.toObject(User::class.java)
                    when {
                        user != null && user.isCaregiver() -> {
                            // ✅ Poprawny opiekun - można dodać
                            addCaregiverToPatient(caregiverEmail)
                        }
                        user != null && user.isPatient() -> {
                            // ❌ To pacjent, nie opiekun - błąd logiczny
                            Toast.makeText(this, "Podany email należy do pacjenta, nie opiekuna", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // ❌ Uszkodzone dane w bazie
                            Toast.makeText(this, "Błąd danych użytkownika", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // ❌ Email nie istnieje w systemie
                    Toast.makeText(this, "Nie ma takiego emaila w bazie", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // ❌ Błąd sieciowy lub bazy danych
                Toast.makeText(this, "Błąd sprawdzania emaila: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Finalna aktualizacja relacji pacjent-opiekun w Firestore
     * 
     * Operacja FieldValue.arrayUnion() "na chłopski rozum":
     * 
     * Co to robi?
     * - Dodaje element do tablicy w dokumencie Firestore
     * - Jeśli element już istnieje, nie duplikuje (idempotentność)
     * - Jeśli tablica nie istnieje, tworzy ją automatycznie
     * - Operacja atomowa - albo się uda w 100%, albo wcale
     * 
     * Dlaczego arrayUnion(), a nie zwykły update?
     * - Zwykły update wymagałby: pobrać -> zmodyfikować -> zapisać
     * - To 3 operacje sieciowe zamiast 1
     * - Ryzyka: race conditions gdy dwóch użytkowników jednocześnie modyfikuje
     * - arrayUnion() to jedna atomowa operacja bezpośrednio w bazie
     * 
     * Struktura danych po operacji:
     * Document users/pacjent@email.com:
     * {
     *   "email": "pacjent@email.com",
     *   "accountType": "pacjent", 
     *   "caregivers": ["opiekun1@email.com", "opiekun2@email.com"]
     * }
     * 
     * Dlaczego currentUserEmail bez walidacji?
     * - Jest pobrany z FirebaseAuth.currentUser - już zweryfikowany
     * - Jeśli byłby null, lazy by zwrócił pusty string (bezpieczne)
     * - Dokument może nie istnieć, ale update() utworzy go automatycznie
     */
    private fun addCaregiverToPatient(caregiverEmail: String) {
        // Atomowa aktualizacja tablicy opiekunów w dokumencie pacjenta
        db.collection("users").document(currentUserEmail)
            .update("caregivers", FieldValue.arrayUnion(caregiverEmail))
            .addOnSuccessListener {
                Toast.makeText(this, "Opiekun dodany pomyślnie", Toast.LENGTH_SHORT).show()
                finish() // Zamknij aktywność po pomyślnym dodaniu
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd dodawania opiekuna: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}