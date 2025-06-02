package com.example.drugreminder

/**
 * Model reprezentujący historię przyjmowania leków
 *
 * Pola:
 * - drugId: identyfikator leku
 * - drugName: nazwa leku
 * - date: data przyjęcia/pominięcia leku (format: YYYY-MM-DD)
 * - timeTaken: godzina przyjęcia leku (format: HH:MM)
 * - taken: flaga określająca czy lek został przyjęty
 * - patientEmail: email pacjenta
 *
 * Wykorzystanie:
 * - Przechowywanie historii w Firestore
 * - Wyświetlanie w StatActivity
 * - Eksport danych do tekstu i udostępnianie
 * - Analiza przestrzegania zaleceń
 */
data class DrugHistory(
    var drugId: String = "",
    var drugName: String = "",
    var date: String = "",
    var timeTaken: String = "",
    var taken: Boolean = false,
    var patientEmail: String = ""
)