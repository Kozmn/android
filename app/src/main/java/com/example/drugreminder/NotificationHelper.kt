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
 * Klasa pomocnicza do zarządzania powiadomieniami o lekach
 *
 * Funkcjonalności:
 * - Tworzenie kanału powiadomień (Android 8.0+)
 * - Wyświetlanie powiadomień o lekach
 * - Anulowanie powiadomień
 *
 * Implementacja:
 * - Wykorzystanie NotificationCompat dla kompatybilności wstecznej
 * - Obsługa uprawnień do powiadomień
 * - Konfiguracja PendingIntent dla akcji
 *
 * Stałe:
 * - CHANNEL_ID: identyfikator kanału powiadomień
 * - CHANNEL_NAME: nazwa wyświetlana w ustawieniach
 * - CHANNEL_DESCRIPTION: opis funkcji powiadomień
 */
class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "drug_reminder_channel"
        private const val CHANNEL_NAME = "Drug Reminder"
        private const val CHANNEL_DESCRIPTION = "Powiadomienia o lekach"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDrugReminder(drug: Drug) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Czas na lek!")
            .setContentText("Czas wziąć lek: ${drug.name}, dawka: ${drug.dosage}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(drug.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Obsługa braku uprawnień do powiadomień
        }
    }

    fun cancelNotification(drugId: String) {
        NotificationManagerCompat.from(context).cancel(drugId.hashCode())
    }
}
