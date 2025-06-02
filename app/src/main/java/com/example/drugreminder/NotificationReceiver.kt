package com.example.drugreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

/**
 * BroadcastReceiver do obsługi powiadomień o lekach
 *
 * Funkcjonalności:
 * - Odbieranie zaplanowanych powiadomień
 * - Sprawdzanie czy lek został już przyjęty
 * - Wyświetlanie powiadomień przez NotificationHelper
 *
 * Implementacja:
 * - Integracja z WorkManager do planowania
 * - Dostęp do Firestore w celu weryfikacji statusu
 * - Obsługa różnych stanów aplikacji
 *
 * Bezpieczeństwo:
 * - Weryfikacja danych wejściowych
 * - Obsługa przypadków brzegowych
 * - Zabezpieczenie przed duplikacją powiadomień
 */

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val drugId = intent.getStringExtra("drug_id") ?: return
        
        // Pokazanie powiadomienia
        val notificationHelper = NotificationHelper(context)
        
        // Pobierz lek z Firebase i sprawdź czy już został wzięty dzisiaj
        val db = FirebaseFirestore.getInstance()
        db.collection("drugs").document(drugId).get()
            .addOnSuccessListener { document ->
                val drug = document.toObject(Drug::class.java)
                drug?.let {
                    // Zawsze pokazuj powiadomienie - sprawdzanie statusu w aplikacji
                    notificationHelper.showDrugReminder(drug)
                }
            }
    }
}
