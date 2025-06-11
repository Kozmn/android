package com.example.drugr        // Wyciągamy informacje o leku z otrzymanego sygnału
        // Sprawdzamy czy dane nie są puste - zabezpieczenie przed błędami
        val drugId = intent.getStringExtra("drug_id") ?: return
        val drugName = intent.getStringExtra("drug_name") ?: return
        val drugDosage = intent.getStringExtra("drug_dosage") ?: return
        val patientEmail = intent.getStringExtra("patient_email") ?: return
        
        // Tworzymy unikalny numer dla powiadomienia
        // Każde powiadomienie musi mieć inny numer żeby się nie nadpisywały
        val notificationId = (drugId + System.currentTimeMillis().toString()).hashCode()
        
        // Pokazujemy powiadomienie użytkownikowi
        // To jest moment gdy użytkownik zobaczy "Czas na lek!"
        NotificationHelper.showDrugReminderNotification(t android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

/**
 * NotificationReceiver - odbiera sygnały o czasie wzięcia leku
 *
 * Gdy użytkownik doda lek z godziną 14:30, Android zapamięta to i o tej godzinie
 * uruchomi ten kod żeby pokazać powiadomienie.
 * 
 * Działa nawet gdy aplikacja jest zamknięta - Android budzi ten kod w tle.
 * Każde powiadomienie ma unikalny numer żeby się nie nadpisywały.
 * 
 * Przepływ:
 * 1. Użytkownik dodaje lek z godziną
 * 2. Aplikacja planuje alarm w Androidzie  
 * 3. O określonej godzinie Android uruchamia ten kod
 * 4. Kod pokazuje powiadomienie "Czas na [nazwa leku]!"
 */

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Wyciągamy informacje o leku z otrzymanego sygnału
        // Po co sprawdzamy czy nie są puste: żeby nie pokazać powiadomienia "null null"
        val drugId = intent.getStringExtra("drug_id") ?: return
        val drugName = intent.getStringExtra("drug_name") ?: return
        val drugDosage = intent.getStringExtra("drug_dosage") ?: return
        val patientEmail = intent.getStringExtra("patient_email") ?: return
        
        // Tworzymy unikalny numer dla powiadomienia
        // Po co: żeby każde powiadomienie miało inny numer i nie nadpisywały się
        val notificationId = (drugId + System.currentTimeMillis().toString()).hashCode()
        
        // Pokazujemy powiadomienie użytkownikowi
        // Po co: żeby użytkownik zobaczył że czas wziąć lek
        NotificationHelper.showDrugReminderNotification(
            context,
            drugName,
            drugDosage,
            patientEmail,
            notificationId
        )
    }
}
