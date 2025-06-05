package com.example.drugreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * BroadcastReceiver obsługujący akcje z powiadomień o lekach
 */
class DrugActionReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_DRUG_TAKEN = "com.example.drugreminder.DRUG_TAKEN"
        const val ACTION_DRUG_NOT_TAKEN = "com.example.drugreminder.DRUG_NOT_TAKEN"
    }
    
    private val db = FirebaseFirestore.getInstance()
    
    override fun onReceive(context: Context, intent: Intent) {
        val drugName = intent.getStringExtra("drug_name") ?: return
        val notificationId = intent.getIntExtra("notification_id", -1)
        val patientEmail = intent.getStringExtra("patient_email") ?: return
        
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
