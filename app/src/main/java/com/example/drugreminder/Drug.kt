package com.example.drugreminder

/**
 * Drug - klasa modelu danych reprezentująca lek w systemie
 *
 * Architektura:
 * - Implementuje wzorzec Data Class dla automatycznego generowania metod
 * - Kompatybilna z Firebase Firestore poprzez domyślne wartości
 * - Wykorzystuje konwencję Bean Pattern dla serializacji/deserializacji
 *
 * Struktura danych:
 * - id: unikalny identyfikator dokumentu w Firestore (generowany automatycznie)
 * - name: nazwa handlowa lub substancja czynna leku
 * - startDate: data rozpoczęcia kuracji w formacie ISO (YYYY-MM-DD)
 * - endDate: data planowanego zakończenia kuracji w formacie ISO (YYYY-MM-DD)
 * - time: godzina dziennego przyjmowania w formacie 24h (HH:MM)
 * - dosage: opis dawkowania (np. "1 tabletka", "5ml syropu")
 * - additionalInfo: dodatkowe wskazówki (np. "po posiłku", "na czczo")
 * - patientEmail: klucz obcy łączący lek z kontem pacjenta
 *
 * Zastosowania:
 * - Przechowywanie w bazie danych Firestore
 * - Wyświetlanie w interfejsie użytkownika (RecyclerView)
 * - Generowanie powiadomień o przyjęciu leku
 * - Tworzenie raportów i statystyk
 */
data class Drug(
    var id: String = "",              // Identyfikator dokumentu Firestore
    var name: String = "",            // Nazwa leku
    var startDate: String = "",       // Data rozpoczęcia (YYYY-MM-DD)
    var endDate: String = "",         // Data zakończenia (YYYY-MM-DD)
    var time: String = "",            // Godzina przyjmowania (HH:MM)
    var dosage: String = "",          // Dawkowanie
    var additionalInfo: String = "",  // Dodatkowe informacje
    var patientEmail: String = ""     // Email pacjenta (klucz obcy)
)