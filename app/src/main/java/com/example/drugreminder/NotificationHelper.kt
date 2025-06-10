package com.example.drugreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * NotificationHelper - singleton odpowiedzialny za zarządzanie powiadomieniami systemu
 *
 * Architektura:
 * - Implementowany jako object (singleton) dla globalnego dostępu
 * - Wykorzystuje Android Notification API z kompatybilnością wsteczną
 * - Obsługuje kanały powiadomień wymagane od Androida 8.0 (API 26)
 *
 * Funkcjonalności główne:
 * - Tworzenie i konfiguracja kanałów powiadomień
 * - Generowanie powiadomień o przypomnieniach leków
 * - Obsługa akcji interaktywnych w powiadomieniach
 * - Zarządzanie cyklem życia powiadomień (wyświetlanie/anulowanie)
 *
 * Komponenty systemu powiadomień:
 * - NotificationChannel: organizuje powiadomienia w grupy (Android 8.0+)
 * - NotificationCompat.Builder: tworzy powiadomienia kompatybilne z różnymi wersjami
 * - PendingIntent: obsługuje akcje użytkownika z powiadomień
 * - NotificationManagerCompat: zarządza wyświetlaniem powiadomień
 *
 * Integracje zewnętrzne:
 * - DrugActionReceiver: obsługuje akcje "Wzięty"/"Nie wzięty"
 * - MainActivity: otwierana po kliknięciu głównej części powiadomienia
 */
object NotificationHelper {
    
    // Stałe konfiguracyjne kanału powiadomień
    private const val CHANNEL_ID = "drug_reminder_channel"
    private const val CHANNEL_NAME = "Przypomnienia o lekach"
    private const val CHANNEL_DESCRIPTION = "Powiadomienia przypominające o czasie przyjęcia leków"
    
    /**
     * Tworzy kanał powiadomień wymagany dla Androida 8.0 i nowszych
     * Konfiguruje priorytet, wibracje i światła dla powiadomień o lekach
     * 
     * @param context Context aplikacji wymagany dla dostępu do NotificationManager
     */
    fun createNotificationChannel(context: Context) {
        // Sprawdzenie wersji Androida - kanały wymagane tylko od API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH // Wysoki priorytet dla zdrowa
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)  // Włączenie wibracji
                enableLights(true)     // Włączenie diody LED
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDrugReminderNotification(
        context: Context,
        drugName: String,
        dosage: String,
        patientEmail: String,
        notificationId: Int
    ) {
        // Intent do otworzenia aplikacji po kliknięciu powiadomienia
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent dla akcji "Wzięty"
        val takenIntent = Intent(context, DrugActionReceiver::class.java).apply {
            action = DrugActionReceiver.ACTION_DRUG_TAKEN
            putExtra("drug_name", drugName)
            putExtra("patient_email", patientEmail)
            putExtra("notification_id", notificationId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent dla akcji "Nie wzięty"
        val notTakenIntent = Intent(context, DrugActionReceiver::class.java).apply {
            action = DrugActionReceiver.ACTION_DRUG_NOT_TAKEN
            putExtra("drug_name", drugName)
            putExtra("patient_email", patientEmail)
            putExtra("notification_id", notificationId)
        }
        val notTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            notTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Czas na lek!")
            .setContentText("$drugName - $dosage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "Wzięty",
                takenPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Nie wzięty",
                notTakenPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Przypomnienie o przyjęciu leku: $drugName\nDawka: $dosage\n\nKliknij aby otworzyć aplikację lub wybierz akcję poniżej.")
            )
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Obsługa braku uprawnień do powiadomień
            e.printStackTrace()
        }
    }
    
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelNotification(drugId: String) {
        // This method is kept for backwards compatibility but won't work without context
        // Use the other cancelNotification method instead
    }
}
