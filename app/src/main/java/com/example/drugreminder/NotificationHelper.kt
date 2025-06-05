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
 * Helper class dla zarządzania powiadomieniami o lekach
 *
 * Funkcjonalności:
 * - Tworzenie kanałów powiadomień
 * - Wyświetlanie powiadomień o czasie przyjęcia leku
 * - Obsługa akcji powiadomień (wzięty/nie wzięty)
 *
 * Integracja:
 * - Android Notification API
 * - PendingIntent dla akcji
 * - NotificationChannel dla Android 8.0+
 */
object NotificationHelper {
    
    private const val CHANNEL_ID = "drug_reminder_channel"
    private const val CHANNEL_NAME = "Przypomnienia o lekach"
    private const val CHANNEL_DESCRIPTION = "Powiadomienia przypominające o czasie przyjęcia leków"
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
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
