package com.example.drugreminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Worker do obsługi przypomnień o lekach
 *
 * Funkcje:
 * - Sprawdzanie czy lek powinien być wzięty
 * - Wyświetlanie powiadomień
 * - Aktualizacja statusu w Firestore
 */
class DrugReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationHelper = NotificationHelper(context)
    private val db = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        try {
            val drugId = inputData.getString("drug_id") ?: return Result.failure()
            val drugName = inputData.getString("drug_name") ?: return Result.failure()
            val drugTime = inputData.getString("drug_time") ?: return Result.failure()
            val patientEmail = inputData.getString("patient_email") ?: return Result.failure()

            // Sprawdź czy lek nie został już wzięty
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val historyRef = db.collection("drug_history")
                .whereEqualTo("drugId", drugId)
                .whereEqualTo("date", currentDate)
                .get()
                .await()

            if (historyRef.documents.isEmpty()) {
                // Lek nie został jeszcze wzięty dzisiaj - pokaż powiadomienie
                val drug = Drug(id = drugId, name = drugName, time = drugTime, patientEmail = patientEmail)
                notificationHelper.showDrugReminder(drug)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}
