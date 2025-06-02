package com.example.drugreminder

/**
 * Model reprezentujący użytkownika aplikacji
 *
 * Pola:
 * - email: adres email użytkownika (służy jako ID)
 * - accountType: typ konta ("pacjent" lub "opiekun")
 * - caregivers: lista emaili opiekunów (tylko dla pacjentów)
 *
 * Funkcje:
 * - isPatient(): sprawdza czy użytkownik jest pacjentem
 * - isCaregiver(): sprawdza czy użytkownik jest opiekunem
 *
 * Wykorzystanie:
 * - Zarządzanie uprawnieniami w aplikacji
 * - Przechowywanie relacji pacjent-opiekun
 * - Kontrola dostępu do funkcjonalności
 */
data class User(
    var email: String = "",
    var accountType: String = "",
    var caregivers: MutableList<String> = mutableListOf()
) {
    fun isPatient(): Boolean = accountType == "pacjent"
    fun isCaregiver(): Boolean = accountType == "opiekun"
}