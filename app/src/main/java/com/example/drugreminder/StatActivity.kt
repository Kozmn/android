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
 * Aktywność odpowiedzialna za wyświetlanie historii i statystyk przyjmowania leków
 *
 * Funkcjonalności:
 * - Wyświetlanie historii przyjmowania leków w RecyclerView
 * - Sortowanie historii według daty (najnowsze pierwsze)
 * 
 * Implementacja:
 * - Wykorzystanie Firebase Firestore do pobierania danych
 *
 * Bezpieczeństwo:
 * - Filtrowanie danych według zalogowanego użytkownika
 */
class StatActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val currentUserEmail by lazy { FirebaseAuth.getInstance().currentUser?.email ?: "" }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var statAdapter: StatAdapter
    private val historyList = mutableListOf<DrugHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_stat)

        initViews()
        loadHistory()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rv_stats)
        val backButton = findViewById<Button>(R.id.btn_back)
        val copyButton = findViewById<Button>(R.id.btn_copy)
        val shareButton = findViewById<Button>(R.id.btn_share)

        // Konfiguracja RecyclerView
        statAdapter = StatAdapter(historyList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = statAdapter

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

    private fun loadHistory() {
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
                    // Sortowanie po dacie (najnowsze pierwsze)
                    historyList.sortByDescending { "${it.date} ${it.timeTaken}" }
                    statAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun formatHistoryAsText(): String {
        val currentDate = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("pl", "PL")).format(Date())
        
        val sb = StringBuilder()
        sb.append("Historia leków - Pacjent: $currentUserEmail\n")
        sb.append("Data wygenerowania: $currentDate\n\n")
        sb.append("=== HISTORIA PRZYJMOWANIA LEKÓW ===\n\n")

        // Grupowanie po datach
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