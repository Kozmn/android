package com.example.drugreminder

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Klasa pomocnicza do zarządzania strukturą bazy danych Firestore
 *
 * Kolekcje:
 * - users: informacje o użytkownikach
 * - drugs: dane o lekach
 * - drug_history: historia brania leków
 */
object FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()

    // Nazwy kolekcji
    const val USERS_COLLECTION = "users"
    const val DRUGS_COLLECTION = "drugs"
    const val DRUG_HISTORY_COLLECTION = "drug_history"

    // Indeksy dla optymalizacji zapytań
    fun createIndexes() {
        // drug_history collection
        db.collection(DRUG_HISTORY_COLLECTION)
            .document("dummy")
            .set(mapOf(
                "drugId" to "",
                "date" to "",
                "patientEmail" to ""
            ))
            .addOnSuccessListener {
                db.collection(DRUG_HISTORY_COLLECTION)
                    .document("dummy")
                    .delete()
            }

        // drugs collection
        db.collection(DRUGS_COLLECTION)
            .document("dummy")
            .set(mapOf(
                "patientEmail" to "",
                "endDate" to "",
                "time" to ""
            ))
            .addOnSuccessListener {
                db.collection(DRUGS_COLLECTION)
                    .document("dummy")
                    .delete()
            }
    }
}
