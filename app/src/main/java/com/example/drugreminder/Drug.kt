package com.example.drugreminder

/**
 * Model reprezentujący lek w aplikacji
 *
 * Pola:
 * - id: unikalny identyfikator leku
 * - name: nazwa leku
 * - startDate: data rozpoczęcia przyjmowania (format: YYYY-MM-DD)
 * - endDate: data zakończenia przyjmowania (format: YYYY-MM-DD)
 * - time: godzina przyjmowania leku (format: HH:MM)
 * - dosage: dawkowanie leku (np. "1 tabletka")
 * - additionalInfo: dodatkowe informacje o leku
 * - patientEmail: email pacjenta, do którego przypisany jest lek
 *
 * Wykorzystanie:
 * - Przechowywanie danych o lekach w Firestore
 * - Wyświetlanie informacji w RecyclerView
 * - Generowanie powiadomień
 */
data class Drug(
    var id: String = "",
    var name: String = "",
    var startDate: String = "",
    var endDate: String = "",
    var time: String = "",
    var dosage: String = "",
    var additionalInfo: String = "",
    var patientEmail: String = ""
)