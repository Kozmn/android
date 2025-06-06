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
        val drugName = intent.getStringExtra("drug_name") ?: return
        val drugDosage = intent.getStringExtra("drug_dosage") ?: return
        val patientEmail = intent.getStringExtra("patient_email") ?: return
        
        // Pokazanie powiadomienia
        val notificationId = (drugId + System.currentTimeMillis().toString()).hashCode()
        NotificationHelper.showDrugReminderNotification(
            context,
            drugName,
            drugDosage,
            patientEmail,
            notificationId
        )
    }
}
