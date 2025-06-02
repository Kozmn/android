package com.example.drugreminder

import android.app.Application
import androidx.work.Configuration
import com.google.firebase.FirebaseApp

/**
 * Klasa aplikacji inicjalizująca główne komponenty
 */
class DrugReminderApp : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        // Inicjalizacja Firebase
        FirebaseApp.initializeApp(this)
        
        // Tworzenie indeksów Firestore
        FirestoreHelper.createIndexes()
    }

    // Konfiguracja WorkManager
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
