package com.example.drugreminder

import android.app.Application
import androidx.work.Configuration
import com.google.firebase.FirebaseApp

/**
 * DrugReminderApp - główna klasa aplikacji uruchamiana przed wszystkimi ekranami
 *
 * Przygotowuje aplikację do działania:
 * - Łączy się z Firebase (nasza baza danych w chmurze)
 * - Tworzy optymalizacje dla szybszego pobierania danych
 * - Konfiguruje system powiadomień działający w tle
 *
 * Android uruchamia tę klasę jako pierwszą, jeszcze zanim użytkownik cokolwiek zobaczy.
 * Tutaj robimy wszystkie przygotowania które muszą być gotowe od początku.
 */
class DrugReminderApp : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        
        // Startujemy połączenie z Firebase (nasza baza danych w chmurze)
        FirebaseApp.initializeApp(this)
        
        // Tworzymy optymalizacje dla szybszego pobierania danych z bazy
        FirestoreHelper.createIndexes()
    }

    // Konfigurujemy WorkManager - system odpowiedzialny za powiadomienia w tle
    // Dzięki temu telefon przypomina o lekach nawet gdy aplikacja nie jest włączona
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO) // Zapisuje logi dla debugowania
            .build()
}
