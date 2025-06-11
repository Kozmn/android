package com.example.drugreminder

import com.google.firebase.firestore.FirebaseFirestore

/**
 * FirestoreHelper - narzędzia do optymalizacji bazy danych
 *
 * Firestore to baza danych w chmurze Google. Ta klasa tworzy indeksy
 * żeby wyszukiwanie było szybkie nawet przy tysiącach leków i użytkowników.
 * 
 * Indeksy działają jak spis treści w książce - zamiast przeszukiwać wszystko,
 * baza od razu wie gdzie szukać konkretnych danych.
 * 
 * Kolekcje w bazie:
 * - users: dane użytkowników (email, typ konta)
 * - drugs: informacje o lekach (nazwa, dawka, godziny)
 * - drug_history: historia brania leków (kto, kiedy, co wziął)
 */
object FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()

    // Nazwy kolekcji w bazie danych - używamy stałych żeby uniknąć literówek
    const val USERS_COLLECTION = "users"           // Kolekcja z danymi użytkowników
    const val DRUGS_COLLECTION = "drugs"           // Kolekcja z lekami
    const val DRUG_HISTORY_COLLECTION = "drug_history"  // Kolekcja z historią brania

    // Tworzy indeksy dla szybkiego wyszukiwania
    fun createIndexes() {
        // Indeks dla historii leków
        // Tworzy dummy dokument żeby Firestore zrozumiał po jakich polach będziemy szukać
        db.collection(DRUG_HISTORY_COLLECTION)
            .document("dummy")
            .set(mapOf(
                "drugId" to "",        // Często szukamy historii konkretnego leku
                "date" to "",          // Często szukamy historii z konkretnej daty
                "patientEmail" to ""   // Często szukamy historii konkretnego pacjenta
            ))
            .addOnSuccessListener {
                // Usuwamy dummy dokument po utworzeniu indeksu
                db.collection(DRUG_HISTORY_COLLECTION)
                    .document("dummy")
                    .delete()
            }

        // Indeks dla leków
        // Tworzy indeks dla szybkiego wyszukiwania leków według właściciela i daty
        db.collection(DRUGS_COLLECTION)
            .document("dummy")
            .set(mapOf(
                "patientEmail" to "",  // Często szukamy leków konkretnego pacjenta
                "endDate" to "",       // Często szukamy leków kończących się w danym dniu
                "time" to ""           // Często szukamy leków o konkretnej godzinie
            ))
            .addOnSuccessListener {
                db.collection(DRUGS_COLLECTION)
                    .document("dummy")
                    .delete()
            }
    }
}
