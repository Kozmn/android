package com.example.drugreminder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainActivity - główna aktywność aplikacji
 *
 * Funkcjonalności:
 * 1. Wyświetlanie listy leków w RecyclerView
 * 2. Różne widoki dla pacjenta i opiekuna
 * 3. Obsługa przycisków:
 *    - Dodawanie leków (tylko pacjent)
 *    - Dodawanie opiekuna (tylko pacjent)
 *    - Wyświetlanie statystyk
 *    - Wylogowanie
 * 4. System powiadomień o lekach
 * 
 * Integracja z Firebase:
 * - Firestore do przechowywania leków
 * - Authentication do zarządzania sesją
 */
class MainActivity : AppCompatActivity(), DrugAdapter.OnDrugActionListener {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var drugAdapter: DrugAdapter
    private val drugList = mutableListOf<Drug>()
    
    private var currentUserEmail: String? = null
    private var currentUserType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_main)

        // Sprawdzenie autoryzacji
        if (auth.currentUser == null) {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
            return
        }

        currentUserEmail = auth.currentUser?.email
        
        initViews()
        getUserTypeAndSetupUI()
        loadDrugs()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rv_drugs)
        val addDrugButton = findViewById<Button>(R.id.btn_add_drug)
        val addCaregiverButton = findViewById<Button>(R.id.btn_add_caregiver)
        val statsButton = findViewById<Button>(R.id.btn_stats)
        val logoutButton = findViewById<Button>(R.id.btn_logout)

        // Konfiguracja RecyclerView
        drugAdapter = DrugAdapter(drugList, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = drugAdapter

        // Obsługa przycisków
        addDrugButton.setOnClickListener {
            startActivity(Intent(this, AddDrugActivity::class.java))
        }
        
        addCaregiverButton.setOnClickListener {
            startActivity(Intent(this, AddCaregiverActivity::class.java))
        }
        
        statsButton.setOnClickListener {
            startActivity(Intent(this, StatActivity::class.java))
        }
        
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
        if (currentUserType != null) {
            loadDrugs()
        }
    }
}