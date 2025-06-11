package com.example.drugreminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * DrugReminderWorker - WorkManager Worker odpowiedzialny za automatyczne powiadomienia o lekach
 *
 * Architektura:
 * - Dziedziczy z CoroutineWorker dla asynchronicznego przetwarzania w tle
 * - Implementuje wzorzec Background Job dla długotrwałych operacji
 * - Wykorzystuje Kotlin Coroutines dla nieblokujących operacji I/O
 *
 * Cykl działania:
 * 1. Pobieranie wszystkich aktywnych leków z Firestore
 * 2. Filtrowanie leków aktywnych w bieżącym dniu
 * 3. Sprawdzanie czy nadszedł czas na powiadomienie (±5 min tolerancja)
 * 4. Weryfikacja czy lek nie został już wzięty dzisiaj
 * 5. Wysyłanie powiadomienia przez NotificationHelper
 *
 * Logika czasowa:
 * - Uruchamiany co 15 minut przez WorkManager
 * - Sprawdza harmonogram wszystkich pacjentów jednocześnie
 * - Tolerancja ±5 minut dla czasu powiadomienia
 * - Respektuje daty rozpoczęcia i zakończenia kuracji
 *
 * Integracje zewnętrzne:
 * - Firebase Firestore: pobieranie leków i historii
 * - NotificationHelper: wyświetlanie powiadomień
 * - WorkManager: planowanie wykonania w tle
 */
class DrugReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Instancje do komunikacji z Firebase i formatowania dat
    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Główna metoda wykonywana przez WorkManager w tle
     * 
     * WorkManager Result "na chłopski rozum":
     * - Result.success() = zadanie zakończone pomyślnie, nie uruchamiaj ponownie
     * - Result.failure() = zadanie nie powiodło się, nie próbuj ponownie
     * - Result.retry() = wystąpił błąd, spróbuj ponownie za jakiś czas
     * 
     * Dlaczego try-catch?
     * - Firestore może być niedostępny (brak internetu)
     * - Parsing dat może się nie powieść (uszkodzone dane)
     * - NotificationHelper może wyrzucić SecurityException (brak uprawnień)
     * - Lepiej retry niż całkowite zepsute powiadomienia
     */
    override suspend fun doWork(): Result {
        return try {
            checkAndSendDrugReminders()
            Result.success()
        } catch (e: Exception) {
            Result.retry()  // Spróbuj ponownie za 15 minut
        }
    }

    /**
     * Sprawdza wszystkie leki i wysyła powiadomienia dla tych, których czas nadszedł
     * 
     * Algorytm sprawdzania "na chłopski rozum":
     * 
     * 1. POBRANIE DANYCH (1 zapytanie):
     *    - Pobierz WSZYSTKIE leki ze wszystkich pacjentów naraz
     *    - db.collection("drugs").get().await() - suspend function, czeka na wynik
     * 
     * 2. FILTROWANIE (dla każdego leku):
     *    a) Czy lek jest aktywny dzisiaj? (data dziś między startDate a endDate)
     *    b) Czy to czas na lek? (godzina teraz ±5 minut od drug.time)
     *    c) Czy lek nie został już wzięty dzisiaj? (sprawdź w drug_history)
     * 
     * 3. WYSŁANIE POWIADOMIENIA:
     *    - Jeśli wszystkie warunki spełnione -> NotificationHelper.show()
     *    - Unikalny notificationId żeby nie duplikować
     * 
     * Dlaczego pobieramy wszystkie leki naraz?
     * - 1 zapytanie zamiast N zapytań (po jednym na pacjenta)
     * - Szybsze, mniej obciążające dla Firestore
     * - WorkManager ma ograniczony czas wykonania (10 minut)
     * 
     * Dlaczego sprawdzamy duplikaty?
     * - Worker uruchamia się co 15 minut
     * - Jeśli lek o 12:00, to może być sprawdzony o 12:00, 12:15, 12:30...
     * - Bez sprawdzenia użytkownik dostałby 3 powiadomienia o tym samym leku
     */
    private suspend fun checkAndSendDrugReminders() {
        val currentDate = dateFormat.format(Date())  // YYYY-MM-DD
        val currentTime = timeFormat.format(Date())  // HH:MM
        
        // Pobierz wszystkie leki ze wszystkich pacjentów jednym zapytaniem
        val drugsSnapshot = db.collection("drugs").get().await()
        
        // Sprawdź każdy lek czy wymaga powiadomienia
        for (document in drugsSnapshot.documents) {
            val drug = document.toObject(Drug::class.java) ?: continue
            drug.id = document.id  // Firestore ID potrzebne do notificationId
            
            // Filtr 1: Czy lek jest aktywny w tym dniu?
            if (isDrugActiveToday(drug, currentDate)) {
                // Filtr 2: Czy to czas na lek (±5 minut tolerancja)?
                if (isTimeForDrug(drug.time, currentTime)) {
                    // Filtr 3: Czy lek nie został już wzięty dzisiaj?
                    val alreadyTaken = checkIfDrugTakenToday(drug.patientEmail, drug.name, currentDate)
                    
                    if (!alreadyTaken) {
                        // ✅ Wszystkie warunki spełnione - wyślij powiadomienie
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

    /**
     * Sprawdza czy lek powinien być brany dzisiaj na podstawie dat kuracji
     * 
     * Logika dat kuracji "na chłopski rozum":
     * - Pacjent ustawia datę START i datę KONIEC kuracji
     * - Lek jest aktywny tylko w dniach: START <= DZISIAJ <= KONIEC
     * - Jeśli dzisiaj jest przed START -> za wcześnie na powiadomienia
     * - Jeśli dzisiaj jest po KONIEC -> kuracja już zakończona
     * 
     * Parsing dat w Javie/Kotlinie:
     * - SimpleDateFormat.parse() konwertuje String -> Date
     * - Date.before() i Date.after() porównują chronologicznie
     * - try-catch bo parsing może się nie powieść (uszkodzone dane)
     * 
     * Przypadki brzegowe:
     * - Null dates -> return false (bezpieczne zachowanie)
     * - Niepoprawny format -> return false
     * - START > KONIEC -> będzie false (poprawne)
     */
    private fun isDrugActiveToday(drug: Drug, currentDate: String): Boolean {
        return try {
            // Konwersja stringów na obiekty Date do porównania
            val startDate = dateFormat.parse(drug.startDate)
            val endDate = dateFormat.parse(drug.endDate)
            val today = dateFormat.parse(currentDate)
            
            // Sprawdź czy wszystkie daty są poprawne i czy dzisiaj mieści się w przedziale
            today != null && startDate != null && endDate != null &&
                    !today.before(startDate) && !today.after(endDate)
        } catch (e: Exception) {
            // Jeśli parsing się nie powiedzie, uznaj lek za nieaktywny (bezpieczne)
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
