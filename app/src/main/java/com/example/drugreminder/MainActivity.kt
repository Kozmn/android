package com.example.drugreminder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainActivity - główna aktywność aplikacji odpowiedzialna za wyświetlanie i zarządzanie lekami
 *
 * Architektura:
 * - Implementuje wzorzec Observer przez interfejs DrugAdapter.OnDrugActionListener
 * - Wykorzystuje lazy initialization dla Firebase komponentów
 * - Zarządza cyklem życia powiadomień w tle
 *
 * Funkcjonalności główne:
 * 1. Wyświetlanie listy leków w RecyclerView z dynamiczną aktualizacją
 * 2. Rozróżnienie widoków dla pacjenta i opiekuna (różne uprawnienia)
 * 3. Obsługa przycisków nawigacyjnych i akcji użytkownika
 * 4. Automatyczny system powiadomień o lekach w tle
 * 
 * Komponenty UI:
 * - RecyclerView z adapterem do wyświetlania leków
 * - Przyciski kontekstowe (widoczność zależna od typu użytkownika)
 * - System nawigacji między aktywnościami
 * 
 * Integracja zewnętrzna:
 * - Firebase Authentication - zarządzanie sesjami użytkowników
 * - Firebase Firestore - przechowywanie i synchronizacja danych leków
 * - WorkManager - planowanie powiadomień w tle
 */
class MainActivity : AppCompatActivity(), DrugAdapter.OnDrugActionListener {

    // Komponenty Firebase inicjalizowane leniwie dla optymalizacji
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    
    // Elementy interfejsu użytkownika
    private lateinit var recyclerView: RecyclerView
    private lateinit var drugAdapter: DrugAdapter
    private val drugList = mutableListOf<Drug>() // Lista leków wyświetlana w UI
    
    // Stan użytkownika do zarządzania uprawnieniami
    private var currentUserEmail: String? = null
    private var currentUserType: String? = null // "pacjent" lub "opiekun"

    /**
     * Główna metoda cyklu życia aktywności - inicjalizuje wszystkie komponenty
     * Przeprowadza weryfikację autoryzacji i konfiguruje interfejs użytkownika
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_main)

        // Weryfikacja stanu autoryzacji - przekierowanie na ekran logowania jeśli niezalogowany
        if (auth.currentUser == null) {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
            return
        }

        // Pobranie danych aktualnie zalogowanego użytkownika
        currentUserEmail = auth.currentUser?.email
        
        // Sekwencja inicjalizacji komponentów aplikacji
        initViews()
        getUserTypeAndSetupUI()
        loadDrugs()
        setupNotifications()
    }

    /**
     * Inicjalizacja elementów interfejsu użytkownika i konfiguracja przycisków
     * Ustawia listenery dla wszystkich interaktywnych elementów
     */
    private fun initViews() {
        // Znalezienie elementów UI w layoutcie
        recyclerView = findViewById(R.id.rv_drugs)
        val addDrugButton = findViewById<Button>(R.id.btn_add_drug)
        val addCaregiverButton = findViewById<Button>(R.id.btn_add_caregiver)
        val statsButton = findViewById<Button>(R.id.btn_stats)
        val logoutButton = findViewById<Button>(R.id.btn_logout)

        // Konfiguracja RecyclerView z liniowym układem elementów
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Konfiguracja przycisków nawigacyjnych z intencjami do odpowiednich aktywności
        addDrugButton.setOnClickListener {
            startActivity(Intent(this, AddDrugActivity::class.java))
        }
        
        addCaregiverButton.setOnClickListener {
            startActivity(Intent(this, AddCaregiverActivity::class.java))
        }
        
        statsButton.setOnClickListener {
            startActivity(Intent(this, StatActivity::class.java))
        }
        
        // Obsługa wylogowania z przeczyszczeniem sesji
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }
    }

    private fun getUserTypeAndSetupUI() {
        currentUserEmail?.let { email ->
            db.collection("users").document(email)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val user = documentSnapshot.toObject(User::class.java)
                        user?.let {
                            currentUserType = it.accountType
                            setupUIBasedOnUserType()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Błąd pobierania danych użytkownika", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupUIBasedOnUserType() {
        val addDrugButton = findViewById<Button>(R.id.btn_add_drug)
        val addCaregiverButton = findViewById<Button>(R.id.btn_add_caregiver)
        
        if (currentUserType == "pacjent") {
            addDrugButton.visibility = View.VISIBLE
            addCaregiverButton.visibility = View.VISIBLE
        } else {
            addDrugButton.visibility = View.GONE
            addCaregiverButton.visibility = View.GONE
        }
        
        // Konfiguracja adaptera z informacją o typie użytkownika
        drugAdapter = DrugAdapter(drugList, this, currentUserType == "opiekun")
        recyclerView.adapter = drugAdapter
    }

    private fun loadDrugs() {
        if (currentUserType == "pacjent") {
            loadPatientDrugs()
        } else {
            loadCaregiverPatientsDrugs()
        }
    }

    private fun loadPatientDrugs() {
        currentUserEmail?.let { email ->
            db.collection("drugs")
                .whereEqualTo("patientEmail", email)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        drugList.clear()
                        for (document in task.result) {
                            val drug = document.toObject(Drug::class.java)
                            drug.id = document.id
                            drugList.add(drug)
                        }
                        drugAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this, "Błąd ładowania leków", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun loadCaregiverPatientsDrugs() {
        currentUserEmail?.let { email ->
            db.collection("users")
                .whereArrayContains("caregivers", email)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val patientEmails = mutableListOf<String>()
                        for (document in task.result) {
                            val user = document.toObject(User::class.java)
                            if (user.isPatient()) {
                                patientEmails.add(user.email)
                            }
                        }
                        
                        if (patientEmails.isNotEmpty()) {
                            loadDrugsForPatients(patientEmails)
                        }
                    }
                }
        }
    }

    private fun loadDrugsForPatients(patientEmails: List<String>) {
        drugList.clear()
        for (patientEmail in patientEmails) {
            db.collection("drugs")
                .whereEqualTo("patientEmail", patientEmail)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (document in task.result) {
                            val drug = document.toObject(Drug::class.java)
                            drug.id = document.id
                            drugList.add(drug)
                        }
                        drugAdapter.notifyDataSetChanged()
                    }
                }
        }
    }

    override fun onDrugTaken(drug: Drug) {
        // Tylko pacjent może oznaczać leki jako wzięte
        if (currentUserType != "pacjent") {
            Toast.makeText(this, "Tylko pacjent może oznaczać leki jako wzięte", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val history = DrugHistory(
            drugId = drug.id,
            drugName = drug.name,
            date = currentDate,
            timeTaken = currentTime,
            taken = true,
            patientEmail = currentUserEmail ?: ""
        )

        db.collection("drug_history")
            .add(history)
            .addOnSuccessListener {
                Toast.makeText(this, "Oznaczono lek jako wzięty", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd zapisu", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDrugDelete(drug: Drug) {
        // Tylko pacjent może usuwać leki
        if (currentUserType != "pacjent") {
            Toast.makeText(this, "Tylko pacjent może usuwać leki", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("drugs").document(drug.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Lek usunięty", Toast.LENGTH_SHORT).show()
                loadDrugs()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd usuwania leku", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Odświeżenie listy leków po powrocie do aktywności
        loadDrugs()
    }

    private fun setupNotifications() {
        // Inicjalizacja kanału powiadomień
        NotificationHelper.createNotificationChannel(this)
        
        // Uruchomienie okresowego sprawdzania przypomnień
        val workRequest = PeriodicWorkRequestBuilder<DrugReminderWorker>(
            15, TimeUnit.MINUTES // Sprawdzanie co 15 minut
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "drug_reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}