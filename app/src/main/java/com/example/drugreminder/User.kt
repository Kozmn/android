package com.example.drugreminder

/**
 * User - klasa modelu danych reprezentująca użytkownika systemu
 *
 * Architektura:
 * - Data class z metodami pomocniczymi dla zarządzania rolami
 * - Implementuje system uprawnień oparty na typach kont
 * - Obsługuje relacje many-to-many między pacjentami a opiekunami
 *
 * Struktura danych:
 * - email: unikalny identyfikator użytkownika (klucz główny)
 * - accountType: typ konta definiujący uprawnienia ("pacjent" | "opiekun")
 * - caregivers: lista emaili opiekunów przypisanych do pacjenta
 *
 * System uprawnień:
 * - Pacjent: może dodawać leki, przypisywać opiekunów, oznaczać leki jako przyjęte
 * - Opiekun: może tylko przeglądać leki i historię przypisanych pacjentów
 *
 * Relacje w systemie:
 * - Pacjent może mieć wielu opiekunów (1:N)
 * - Opiekun może monitorować wielu pacjentów (M:N)
 * - Lista "caregivers" jest przechowywana tylko w dokumencie pacjenta
 *
 * Zastosowania:
 * - Kontrola dostępu do funkcji aplikacji
 * - Personalizacja interfejsu użytkownika
 * - Zarządzanie relacjami opieka medyczna
 */
data class User(
    var email: String = "",                           // Unikalny identyfikator użytkownika
    var accountType: String = "",                     // Typ konta: "pacjent" lub "opiekun"
    var caregivers: MutableList<String> = mutableListOf()  // Lista emaili opiekunów
) {
    /**
     * Sprawdza czy użytkownik ma uprawnienia pacjenta
     * @return true jeśli accountType == "pacjent"
     */
    fun isPatient(): Boolean = accountType == "pacjent"
    
    /**
     * Sprawdza czy użytkownik ma uprawnienia opiekuna
     * @return true jeśli accountType == "opiekun"
     */
    fun isCaregiver(): Boolean = accountType == "opiekun"
}