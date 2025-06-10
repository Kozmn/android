package com.example.drugreminder

/**
 * DrugHistory - klasa modelu danych reprezentująca historię przyjmowania leków
 *
 * Architektura:
 * - Data class zapewniająca automatyczne generowanie metod equals, hashCode, toString
 * - Kompatybilność z Firebase Firestore przez domyślne wartości wszystkich pól
 * - Służy jako jednostka audytu dla działań użytkownika
 *
 * Struktura danych:
 * - drugId: referencja do dokumentu leku w kolekcji "drugs"
 * - drugName: nazwa leku (duplikacja dla wydajności zapytań)
 * - date: data zdarzenia w formacie ISO (YYYY-MM-DD)
 * - timeTaken: rzeczywista godzina przyjęcia/odrzucenia (HH:MM)
 * - taken: flaga logiczna określająca status (true = przyjęty, false = pominięty)
 * - patientEmail: identyfikator pacjenta (klucz obcy)
 *
 * Zastosowania w systemie:
 * - Audyt przestrzegania zaleceń lekarskich
 * - Generowanie raportów adherencji (StatActivity)
 * - Eksport danych do formatów zewnętrznych
 * - Analiza wzorców przyjmowania leków
 * - Podstawa dla alertów o pominiętych dawkach
 */
data class DrugHistory(
    var drugId: String = "",          // ID dokumentu leku w Firestore
    var drugName: String = "",        // Nazwa leku (denormalizacja)
    var date: String = "",            // Data zdarzenia (YYYY-MM-DD)
    var timeTaken: String = "",       // Godzina rzeczywista (HH:MM)
    var taken: Boolean = false,       // Status przyjęcia (true/false)
    var patientEmail: String = ""     // Email pacjenta (klucz obcy)
)