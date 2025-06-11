package com.example.drugreminder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
 * StatActivity - ekran pokazujący historię brania leków
 *
 * Pacjent widzi tylko swoją historię posortowaną od najnowszych.
 * Opiekun widzi historię wszystkich swoich pacjentów z oznaczeniem czyj to lek.
 * 
 * Dodatkowo można eksportować historię - skopiować do schowka lub wysłać
 * przez inne aplikacje (email, WhatsApp, etc.) np. do lekarza.
 * 
 * Historia zawiera: nazwę leku, datę, godzinę wzięcia i czy został przyjęty.
 * Wszystko jest posortowane chronologicznie dla łatwego przeglądania.
 * 
 * Funkcje eksportu:
 * - Kopiowanie historii do schowka (można wkleić gdzie indziej)
 * - Udostępnianie przez email, WhatsApp itp.
 * - Ładne formatowanie z emoji i polskimi znakami
 *
 * Dlaczego jest skomplikowane:
 * - Pacjent: 1 zapytanie do bazy danych (tylko swoje leki)
 * - Opiekun: musi najpierw znaleźć swoich pacjentów, potem leki każdego z nich
 * - Wszystko musi być zsynchronizowane żeby nie pokazać niepełnych danych
 */
class StatActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val currentUserEmail by lazy { FirebaseAuth.getInstance().currentUser?.email ?: "" }
    private var currentUserType: String = ""
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var statAdapter: StatAdapter
    private val historyList = mutableListOf<DrugHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_stat)

        initViews()
        getUserTypeAndLoadHistory()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rv_stats)
        val backButton = findViewById<Button>(R.id.btn_back)
        val copyButton = findViewById<Button>(R.id.btn_copy)
        val shareButton = findViewById<Button>(R.id.btn_share)

        // Konfiguracja RecyclerView - adapter będzie utworzony po ustaleniu typu użytkownika
        recyclerView.layoutManager = LinearLayoutManager(this)

        backButton.setOnClickListener {
            finish()
        }

        copyButton.setOnClickListener {
            copyHistoryToClipboard()
        }

        shareButton.setOnClickListener {
            shareHistory()
        }
    }

    /**
     * Pobiera typ użytkownika i inicjuje odpowiednią logikę ładowania danych
     * 
     * Procedura inicjalizacji "na chłopski rozum":
     * 1. Sprawdź w bazie danych jaki typ konta ma zalogowany użytkownik
     * 2. Na podstawie typu (pacjent/opiekun) wybierz strategię pobierania danych
     * 3. Skonfiguruj adapter RecyclerView z odpowiednią flagą wyświetlania
     * 4. Rozpocznij ładowanie historii leków
     * 
     * Fallback w przypadku błędu:
     * - Jeśli nie można określić typu użytkownika, zakładamy "pacjent"
     * - Bezpieczniejsze niż crash - pacjent ma ograniczone uprawnienia
     * - W najgorszym przypadku opiekun zobaczy tylko pustą listę
     */
    private fun getUserTypeAndLoadHistory() {
        currentUserEmail.let { email ->
            db.collection("users").document(email)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val user = documentSnapshot.toObject(User::class.java)
                        user?.let {
                            currentUserType = it.accountType
                            setupAdapterAndLoadHistory()
                        }
                    }
                }
                .addOnFailureListener {
                    // Fallback do trybu pacjenta gdy nie można określić typu użytkownika
                    currentUserType = "pacjent"
                    setupAdapterAndLoadHistory()
                }
        }
    }

    private fun setupAdapterAndLoadHistory() {
        // Konfiguracja adaptera z informacją o typie użytkownika
        statAdapter = StatAdapter(historyList, currentUserType == "opiekun")
        recyclerView.adapter = statAdapter
        loadHistory()
    }

    /**
     * Rozgałęzienie logiki ładowania danych na podstawie typu użytkownika
     * 
     * Dwie różne strategie "na chłopski rozum":
     * 
     * PACJENT -> loadPatientHistory():
     * - 1 zapytanie do "drug_history" WHERE patientEmail = mój_email
     * - Proste, szybkie, bezpieczne
     * 
     * OPIEKUN -> loadCaregiverPatientsHistory():  
     * - Krok 1: Znajdź pacjentów, którzy mnie dodali (zapytanie do "users")
     * - Krok 2: Dla każdego pacjenta pobierz historię (N zapytań do "drug_history")
     * - Skomplikowane, wolniejsze, ale potrzebne dla funkcjonalności opiekuna
     */
    private fun loadHistory() {
        if (currentUserType == "pacjent") {
            loadPatientHistory()     // Prosty przypadek
        } else {
            loadCaregiverPatientsHistory()  // Skomplikowany przypadek
        }
    }

    /**
     * Pobiera historię leków dla pacjenta - prosty przypadek
     * 
     * Zapytanie Firestore "na chłopski rozum":
     * 1. Idź do kolekcji "drug_history"
     * 2. Znajdź wszystkie dokumenty gdzie patientEmail == mój email
     * 3. Skonwertuj każdy dokument na obiekt DrugHistory
     * 4. Posortuj według daty i czasu (najnowsze pierwsze)
     * 5. Odśwież adapter UI
     * 
     * Sortowanie według "${date} ${timeTaken}":
     * - String concatenation dat i czasów
     * - Format YYYY-MM-DD HH:MM sortuje się poprawnie leksykograficznie
     * - sortByDescending() daje najnowsze wpisy na górze
     */
    private fun loadPatientHistory() {
        db.collection("drug_history")
            .whereEqualTo("patientEmail", currentUserEmail)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    historyList.clear()
                    for (document in task.result) {
                        val history = document.toObject(DrugHistory::class.java)
                        historyList.add(history)
                    }
                    // Sortowanie chronologiczne - najnowsze wpisy pierwsze
                    historyList.sortByDescending { "${it.date} ${it.timeTaken}" }
                    statAdapter.notifyDataSetChanged()
                }
            }
    }

    /**
     * Pobiera historię dla opiekuna - skomplikowany dwuetapowy proces
     * 
     * Problem opiekuna "na chłopski rozum":
     * Opiekun nie ma bezpośredniego dostępu do historii. Musi:
     * 1. Znaleźć swoich podopiecznych (pacjentów, którzy go dodali)
     * 2. Pobrać historię każdego z nich
     * 
     * ETAP 1: Znajdź pacjentów
     * - Szukaj w "users" gdzie tablica "caregivers" zawiera mój email
     * - whereArrayContains() to specjalne zapytanie NoSQL dla tablic
     * - Filtruj tylko pacjentów (może być mix pacjent/opiekun w wyniku)
     * 
     * ETAP 2: Pobierz historie
     * - Jeśli znaleziono pacjentów -> loadHistoryForPatients()
     * - Jeśli brak -> wyczyść listę (opiekun bez podopiecznych)
     */
    private fun loadCaregiverPatientsHistory() {
        currentUserEmail.let { email ->
            // ETAP 1: Znajdź pacjentów, którzy dodali mnie jako opiekuna
            db.collection("users")
                .whereArrayContains("caregivers", email)  // Przeszukaj tablice
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val patientEmails = mutableListOf<String>()
                        for (document in task.result) {
                            val user = document.toObject(User::class.java)
                            // Filtruj tylko rzeczywistych pacjentów
                            if (user.isPatient()) {
                                patientEmails.add(user.email)
                            }
                        }
                        
                        // ETAP 2: Pobierz historie znalezionych pacjentów
                        if (patientEmails.isNotEmpty()) {
                            loadHistoryForPatients(patientEmails)
                        } else {
                            // Opiekun bez podopiecznych - pokaż pustą listę
                            historyList.clear()
                            statAdapter.notifyDataSetChanged()
                        }
                    }
                }
        }
    }

    /**
     * Pobiera historię dla listy pacjentów - wyzwanie synchronizacji asynchronicznych operacji
     * 
     * Problem asynchroniczności "na chłopski rozum":
     * 1. Mamy N pacjentów, każdy wymaga osobnego zapytania do Firestore
     * 2. Wszystkie zapytania lecą równolegle (async)
     * 3. Nie wiemy w jakiej kolejności odpowiedzi wrócą
     * 4. Nie wiemy kiedy WSZYSTKIE się skończą
     * 5. Dopiero po wszystkich możemy posortować i wyświetlić dane
     * 
     * Rozwiązanie - licznik completedQueries:
     * - Zmienna zliczająca ile zapytań już się zakończyło
     * - Po każdym zapytaniu: completedQueries++
     * - Gdy completedQueries == patientEmails.size -> wszystkie gotowe
     * - Dopiero wtedy sortuj i odśwież UI
     * 
     * Alternatywne rozwiązania (nie użyte):
     * - CountDownLatch - bardziej skomplikowane
     * - Kotlin Coroutines + async/await - wymagałoby refaktoringu
     * - RxJava/Reactive Streams - overkill dla tego przypadku
     * 
     * Sortowanie finalne:
     * - Wszystkie historie z wszystkich pacjentów w jednej liście
     * - Posortowane chronologicznie (najnowsze pierwsze)
     * - StatAdapter z flagą showPatientName=true pokaże czyja to historia
     */
    private fun loadHistoryForPatients(patientEmails: List<String>) {
        historyList.clear()
        var completedQueries = 0  // Licznik zakończonych zapytań
        
        // Uruchom równoległe zapytania dla każdego pacjenta
        for (patientEmail in patientEmails) {
            db.collection("drug_history")
                .whereEqualTo("patientEmail", patientEmail)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Dodaj historie tego pacjenta do globalnej listy
                        for (document in task.result) {
                            val history = document.toObject(DrugHistory::class.java)
                            historyList.add(history)
                        }
                    }
                    
                    // Zwiększ licznik zakończonych zapytań
                    completedQueries++
                    
                    // Sprawdź czy to było ostatnie zapytanie
                    if (completedQueries == patientEmails.size) {
                        // Wszystkie zapytania zakończone - teraz można sortować i wyświetlać
                        historyList.sortByDescending { "${it.date} ${it.timeTaken}" }
                        statAdapter.notifyDataSetChanged()
                    }
                }
        }
    }

    private fun formatHistoryAsText(): String {
        val currentDate = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("pl", "PL")).format(Date())
        
        val sb = StringBuilder()
        if (currentUserType == "pacjent") {
            sb.append("Historia leków - Pacjent: $currentUserEmail\n")
        } else {
            sb.append("Historia leków - Opiekun: $currentUserEmail\n")
            sb.append("Raport dla wszystkich pacjentów pod opieką\n")
        }
        sb.append("Data wygenerowania: $currentDate\n\n")
        sb.append("=== HISTORIA PRZYJMOWANIA LEKÓW ===\n\n")

        if (currentUserType == "opiekun") {
            // Grupowanie po pacjentach, potem po datach
            val groupedByPatient = historyList.groupBy { it.patientEmail }
            
            for ((patientEmail, patientHistory) in groupedByPatient) {
                sb.append("👤 PACJENT: $patientEmail\n")
                sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                
                val groupedByDate = patientHistory.groupBy { it.date }
                for ((date, entries) in groupedByDate.toSortedMap(reverseOrder())) {
                    sb.append("📅 $date\n")
                    
                    entries.sortedBy { it.timeTaken }.forEach { history ->
                        val statusIcon = if (history.taken) "✅" else "❌"
                        val statusText = if (history.taken) "Wzięty" else "Nie wzięty"
                        sb.append("  • ${history.drugName} - ${history.timeTaken} $statusIcon $statusText\n")
                    }
                    sb.append("\n")
                }
                sb.append("\n")
            }
        } else {
            // Grupowanie po datach (zachowanie dla pacjenta)
            val groupedByDate = historyList.groupBy { it.date }
            
            for ((date, entries) in groupedByDate.toSortedMap(reverseOrder())) {
                sb.append("📅 $date\n")
                
                entries.sortedBy { it.timeTaken }.forEach { history ->
                    val statusIcon = if (history.taken) "✅" else "❌"
                    val statusText = if (history.taken) "Wzięty" else "Nie wzięty"
                    sb.append("  • ${history.drugName} - ${history.timeTaken} $statusIcon $statusText\n")
                }
                sb.append("\n")
            }
        }

        if (historyList.isEmpty()) {
            sb.append("Brak zapisanych danych o przyjmowaniu leków.\n")
        }

        return sb.toString()
    }

    private fun copyHistoryToClipboard() {
        val historyText = formatHistoryAsText()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Historia leków", historyText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Historia skopiowana do schowka", Toast.LENGTH_SHORT).show()
    }

    private fun shareHistory() {
        val historyText = formatHistoryAsText()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, historyText)
            putExtra(Intent.EXTRA_SUBJECT, "Historia przyjmowania leków")
        }
        startActivity(Intent.createChooser(shareIntent, "Udostępnij historię przez:"))
    }
}