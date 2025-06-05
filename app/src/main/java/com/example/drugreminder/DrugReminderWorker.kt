package com.example.drugreminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * WorkManager Worker odpowiedzialny za wysyłanie powiadomień o lekach
 *
 * Funkcjonalności:
 * - Sprawdzanie aktywnych leków w Firestore
 * - Porównywanie czasu z harmonogramem leków
 * - Wysyłanie powiadomień dla leków w odpowiednim czasie
 * - Obsługa błędów i logowanie
 *
 * Harmonogram:
 * - Uruchamiany periodycznie przez WorkManager
 * - Sprawdza wszystkie leki dla wszystkich pacjentów
 * - Wysyła powiadomienia tylko w odpowiednim czasie
 *
 * Integracja:
 * - Firebase Firestore do pobierania leków
 * - NotificationHelper do wyświetlania powiadomień
 * - DrugHistory do sprawdzania czy lek już został wzięty
 */
class DrugReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override suspend fun doWork(): Result {
        return try {
            checkAndSendDrugReminders()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkAndSendDrugReminders() {
        val currentDate = dateFormat.format(Date())
        val currentTime = timeFormat.format(Date())
        
        // Pobierz wszystkie aktywne leki
        val drugsSnapshot = db.collection("drugs").get().await()
        
        for (document in drugsSnapshot.documents) {
            val drug = document.toObject(Drug::class.java) ?: continue
            drug.id = document.id
            
            // Sprawdź czy lek jest aktywny w tym dniu
            if (isDrugActiveToday(drug, currentDate)) {
                // Sprawdź czy to czas na lek (z tolerancją ±5 minut)
                if (isTimeForDrug(drug.time, currentTime)) {
                    // Sprawdź czy lek nie został już wzięty dzisiaj
                    val alreadyTaken = checkIfDrugTakenToday(drug.patientEmail, drug.name, currentDate)
                    
                    if (!alreadyTaken) {
                        // Wyślij powiadomienie
                        val notificationId = generateNotificationId(drug.id, currentDate)
                        NotificationHelper.showDrugReminderNotification(
                            applicationContext,
                            drug.name,
                            drug.dosage,
                            drug.patientEmail,
                            notificationId
                        )
                    }
                }
            }
        }
    }

    private fun isDrugActiveToday(drug: Drug, currentDate: String): Boolean {
        return try {
            val startDate = dateFormat.parse(drug.startDate)
            val endDate = dateFormat.parse(drug.endDate)
            val today = dateFormat.parse(currentDate)
            
            today != null && startDate != null && endDate != null &&
                    !today.before(startDate) && !today.after(endDate)
        } catch (e: Exception) {
            false
        }
    }

    private fun isTimeForDrug(drugTime: String, currentTime: String): Boolean {
        return try {
            val drugTimeObj = timeFormat.parse(drugTime)
            val currentTimeObj = timeFormat.parse(currentTime)
            
            if (drugTimeObj != null && currentTimeObj != null) {
                val diffInMillis = kotlin.math.abs(currentTimeObj.time - drugTimeObj.time)
                val diffInMinutes = diffInMillis / (1000 * 60)
                
                // Tolerancja ±5 minut
                diffInMinutes <= 5
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkIfDrugTakenToday(
        patientEmail: String,
        drugName: String,
        currentDate: String
    ): Boolean {
        return try {
            val historySnapshot = db.collection("drug_history")
                .whereEqualTo("patientEmail", patientEmail)
                .whereEqualTo("drugName", drugName)
                .whereEqualTo("date", currentDate)
                .whereEqualTo("taken", true)
                .get()
                .await()
            
            !historySnapshot.isEmpty
        } catch (e: Exception) {
            false // W przypadku błędu, wyślij powiadomienie
        }
    }

    private fun generateNotificationId(drugId: String, date: String): Int {
        return (drugId + date).hashCode()
    }
}
