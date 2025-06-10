package com.example.drugreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * DrugActionReceiver - BroadcastReceiver obsługujący akcje z powiadomień o lekach
 *
 * Architektura:
 * - Dziedziczy z BroadcastReceiver dla obsługi intencji systemowych
 * - Implementuje wzorzec Command dla różnych akcji użytkownika
 * - Komunikuje się z Firebase Firestore dla persystencji danych
 *
 * Funkcjonalności główne:
 * - Obsługa akcji "Lek wzięty" z powiadomienia push
 * - Obsługa akcji "Lek nie wzięty" z powiadomienia push
 * - Automatyczne zapisywanie historii przyjmowania leków
 * - Zarządzanie cyklem życia powiadomień (anulowanie po akcji)
 *
 * Stałe akcji:
 * - ACTION_DRUG_TAKEN: identyfikator akcji oznaczenia leku jako wzięty
 * - ACTION_DRUG_NOT_TAKEN: identyfikator akcji oznaczenia leku jako pominięty
 *
 * Przepływ danych:
 * 1. Użytkownik klika akcję w powiadomieniu
 * 2. System wywołuje onReceive z odpowiednim Intent
 * 3. Receiver zapisuje historię do Firestore
 * 4. Wyświetla potwierdzenie użytkownikowi
 * 5. Anuluje powiadomienie
 */
class DrugActionReceiver : BroadcastReceiver() {
    
    companion object {
        // Stałe identyfikujące rodzaje akcji obsługiwanych przez receiver
        const val ACTION_DRUG_TAKEN = "com.example.drugreminder.DRUG_TAKEN"
        const val ACTION_DRUG_NOT_TAKEN = "com.example.drugreminder.DRUG_NOT_TAKEN"
    }
    
    // Instancja Firebase Firestore do zapisu historii
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Główna metoda obsługi otrzymanych intencji broadcast
     * Analizuje typ akcji i wykonuje odpowiednie operacje
     * 
     * @param context Context aplikacji
     * @param intent Intent zawierający dane o akcji i leku
     */
    override fun onReceive(context: Context, intent: Intent) {
        // Pobranie danych z Intent - walidacja obecności wymaganych parametrów
        val drugName = intent.getStringExtra("drug_name") ?: return
        val notificationId = intent.getIntExtra("notification_id", -1)
        val patientEmail = intent.getStringExtra("patient_email") ?: return
        
        // Obsługa różnych typów akcji użytkownika
        when (intent.action) {
            ACTION_DRUG_TAKEN -> {
                saveDrugHistory(context, drugName, patientEmail, true)
                Toast.makeText(context, "Lek oznaczony jako wzięty", Toast.LENGTH_SHORT).show()
            }
            ACTION_DRUG_NOT_TAKEN -> {
                saveDrugHistory(context, drugName, patientEmail, false)
                Toast.makeText(context, "Lek oznaczony jako nie wzięty", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Anuluj powiadomienie
        if (notificationId != -1) {
            NotificationHelper.cancelNotification(context, notificationId)
        }
    }
    
    private fun saveDrugHistory(context: Context, drugName: String, patientEmail: String, taken: Boolean) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        val history = DrugHistory(
            drugName = drugName,
            patientEmail = patientEmail,
            date = currentDate,
            timeTaken = currentTime,
            taken = taken
        )
        
        db.collection("drug_history")
            .add(history)
            .addOnSuccessListener {
                // Historia zapisana pomyślnie
            }
            .addOnFailureListener {
                // Błąd zapisywania historii
            }
    }
}
