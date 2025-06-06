package com.example.drugreminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * Aktywność umożliwiająca dodanie nowego leku do systemu
 *
 * Funkcjonalności:
 * - Formularz dodawania leku z polami:
 *   * Nazwa leku
 *   * Data rozpoczęcia (YYYY-MM-DD)
 *   * Data zakończenia (YYYY-MM-DD)
 *   * Godzina przyjmowania (HH:MM)
 *   * Dawkowanie
 *   * Dodatkowe informacje
 *
 * Walidacje:
 * - Sprawdzanie wymaganych pól
 * - Poprawność formatu dat
 * - Logiczność zakresu dat (start < end)
 * - Format godziny
 *
 * Integracja:
 * - Zapis do Firestore
 * - Automatyczne przypisanie do aktualnego pacjenta
 */
class AddDrugActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val currentUserEmail by lazy { FirebaseAuth.getInstance().currentUser?.email ?: "" }
    
    private lateinit var nameEditText: EditText
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var dosageEditText: EditText
    private lateinit var additionalInfoEditText: EditText
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_add_drug)

        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        nameEditText = findViewById(R.id.et_drug_name)
        startDateEditText = findViewById(R.id.et_start_date)
        endDateEditText = findViewById(R.id.et_end_date)
        timeEditText = findViewById(R.id.et_time)
        dosageEditText = findViewById(R.id.et_dosage)
        additionalInfoEditText = findViewById(R.id.et_additional_info)
        
        // Ustaw pola daty i czasu jako tylko do odczytu
        startDateEditText.isFocusable = false
        startDateEditText.isClickable = true
        endDateEditText.isFocusable = false
        endDateEditText.isClickable = true
        timeEditText.isFocusable = false
        timeEditText.isClickable = true
    }
    
    private fun setupClickListeners() {
        val addButton = findViewById<Button>(R.id.btn_add)
        val backButton = findViewById<Button>(R.id.btn_back)

        startDateEditText.setOnClickListener {
            showDatePicker { calendar ->
                selectedStartDate = calendar
                startDateEditText.setText(dateFormat.format(calendar.time))
            }
        }
        
        endDateEditText.setOnClickListener {
            showDatePicker { calendar ->
                selectedEndDate = calendar
                endDateEditText.setText(dateFormat.format(calendar.time))
            }
        }
        
        timeEditText.setOnClickListener {
            showTimePicker { hour, minute ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                timeEditText.setText(timeFormat.format(calendar.time))
            }
        }

        addButton.setOnClickListener {
            addDrug()
        }

        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun showDatePicker(onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(selectedCalendar)
        }, year, month, day).show()
    }
    
    private fun showTimePicker(onTimeSelected: (Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            onTimeSelected(selectedHour, selectedMinute)
        }, hour, minute, true).show()
    }

    private fun addDrug() {
        val name = nameEditText.text.toString().trim()
        val startDate = startDateEditText.text.toString().trim()
        val endDate = endDateEditText.text.toString().trim()
        val time = timeEditText.text.toString().trim()
        val dosage = dosageEditText.text.toString().trim()
        val additionalInfo = additionalInfoEditText.text.toString().trim()

        // Walidacja pól
        when {
            name.isEmpty() -> {
                Toast.makeText(this, "Podaj nazwę leku", Toast.LENGTH_SHORT).show()
                return
            }
            startDate.isEmpty() -> {
                Toast.makeText(this, "Wybierz datę rozpoczęcia", Toast.LENGTH_SHORT).show()
                return
            }
            endDate.isEmpty() -> {
                Toast.makeText(this, "Wybierz datę zakończenia", Toast.LENGTH_SHORT).show()
                return
            }
            time.isEmpty() -> {
                Toast.makeText(this, "Wybierz godzinę", Toast.LENGTH_SHORT).show()
                return
            }
            dosage.isEmpty() -> {
                Toast.makeText(this, "Podaj dawkę", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // Walidacja dat - data końcowa nie może być wcześniejsza niż początkowa
        if (selectedStartDate != null && selectedEndDate != null) {
            if (selectedEndDate!!.before(selectedStartDate)) {
                Toast.makeText(this, "Data zakończenia nie może być wcześniejsza niż data rozpoczęcia", Toast.LENGTH_LONG).show()
                return
            }
        }

        val drug = Drug(
            name = name,
            startDate = startDate,
            endDate = endDate,
            time = time,
            dosage = dosage,
            additionalInfo = additionalInfo,
            patientEmail = currentUserEmail
        )

        db.collection("drugs")
            .add(drug)
            .addOnSuccessListener {
                Toast.makeText(this, "Lek dodany pomyślnie", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd dodawania leku: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}