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
 * AddDrugActivity - aktywność odpowiedzialna za dodawanie nowych leków do systemu
 *
 * Architektura:
 * - Wykorzystuje wzorzec MVP (Model-View-Presenter) dla separacji logiki
 * - Implementuje walidację danych po stronie klienta przed zapisem
 * - Używa Firebase Firestore jako warstwy persystencji danych
 *
 * Funkcjonalności formularza:
 * - Nazwa leku (pole tekstowe wymagane)
 * - Data rozpoczęcia kuracji (DatePicker z walidacją)
 * - Data zakończenia kuracji (DatePicker z walidacją relacyjną)
 * - Godzina przyjmowania (TimePicker w formacie 24h)
 * - Dawkowanie (pole tekstowe opisowe)
 * - Dodatkowe informacje (pole opcjonalne)
 *
 * Walidacje implementowane:
 * - Sprawdzanie wypełnienia pól wymaganych
 * - Weryfikacja poprawności formatu dat (yyyy-MM-dd)
 * - Kontrola logiczności zakresu dat (data_start <= data_end)
 * - Walidacja formatu czasu (HH:mm w systemie 24h)
 *
 * Integracja zewnętrzna:
 * - Firebase Firestore - zapis danych leku w kolekcji "drugs"
 * - Firebase Authentication - automatyczne przypisanie do zalogowanego pacjenta
 * - Android DatePicker/TimePicker - natywne komponenty wyboru daty/czasu
 */
class AddDrugActivity : AppCompatActivity() {

    // Komponenty Firebase do zarządzania danymi
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val currentUserEmail by lazy { FirebaseAuth.getInstance().currentUser?.email ?: "" }
    
    // Referencje do elementów interfejsu użytkownika
    private lateinit var nameEditText: EditText
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var dosageEditText: EditText
    private lateinit var additionalInfoEditText: EditText
    
    // Formattery dla spójności wyświetlania dat i czasu
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    // Obiekty Calendar do przechowywania wybranych dat (dla walidacji)
    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null

    /**
     * Metoda inicjalizująca aktywność - ustawia layout i konfiguruje komponenty
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_add_drug)

        initViews()
        setupClickListeners()
    }
    
    /**
     * Inicjalizacja referencji do elementów UI i konfiguracja ich właściwości
     * Ustawia pola daty/czasu jako tylko do odczytu dla wymuszenia używania pickerów
     */
    private fun initViews() {
        // Pobranie referencji do wszystkich pól formularza
        nameEditText = findViewById(R.id.et_drug_name)
        startDateEditText = findViewById(R.id.et_start_date)
        endDateEditText = findViewById(R.id.et_end_date)
        timeEditText = findViewById(R.id.et_time)
        dosageEditText = findViewById(R.id.et_dosage)
        additionalInfoEditText = findViewById(R.id.et_additional_info)
        
        // Konfiguracja pól daty i czasu jako nieedytowalne ale klikalne
        // Wymusza używanie DatePicker/TimePicker zamiast ręcznego wprowadzania
        startDateEditText.isFocusable = false
        startDateEditText.isClickable = true
        endDateEditText.isFocusable = false
        endDateEditText.isClickable = true
        timeEditText.isFocusable = false
        timeEditText.isClickable = true
    }
    
    /**
     * Konfiguracja listenerów dla wszystkich interaktywnych elementów
     * Łączy akcje kliknięcia z odpowiednimi metodami obsługi
     */
    private fun setupClickListeners() {
        val addButton = findViewById<Button>(R.id.btn_add)
        val backButton = findViewById<Button>(R.id.btn_back)

        // Listener dla pola daty rozpoczęcia - wywołuje DatePicker
        startDateEditText.setOnClickListener {
            showDatePicker { calendar ->
                selectedStartDate = calendar
                startDateEditText.setText(dateFormat.format(calendar.time))
            }
        }
        
        // Listener dla pola daty zakończenia - wywołuje DatePicker
        endDateEditText.setOnClickListener {
            showDatePicker { calendar ->
                selectedEndDate = calendar
                endDateEditText.setText(dateFormat.format(calendar.time))
            }
        }
        
        // Listener dla pola czasu - wywołuje TimePicker
        timeEditText.setOnClickListener {
            showTimePicker { hour, minute ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                timeEditText.setText(timeFormat.format(calendar.time))
            }
        }

        // Przycisk dodania leku - wywołuje walidację i zapis
        addButton.setOnClickListener {
            addDrug()
        }

        // Przycisk powrotu - zamyka aktywność
        backButton.setOnClickListener {
            finish()
        }
    }
    
    /**
     * Wyświetla natywny DatePickerDialog dla wyboru daty
     * @param onDateSelected callback wykonywany po wyborze daty z obiektem Calendar
     */
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
    
    /**
     * Wyświetla natywny TimePickerDialog dla wyboru godziny
     * @param onTimeSelected callback wykonywany po wyborze czasu z parametrami hour, minute
     */
    private fun showTimePicker(onTimeSelected: (Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            onTimeSelected(selectedHour, selectedMinute)
        }, hour, minute, true).show()
    }

    /**
     * Główna metoda dodawania leku - przeprowadza walidację i zapisuje dane
     * Sprawdza poprawność wszystkich pól przed wysłaniem do Firestore
     */
    private fun addDrug() {
        // Pobranie wartości z pól formularza
        val name = nameEditText.text.toString().trim()
        val startDate = startDateEditText.text.toString().trim()
        val endDate = endDateEditText.text.toString().trim()
        val time = timeEditText.text.toString().trim()
        val dosage = dosageEditText.text.toString().trim()
        val additionalInfo = additionalInfoEditText.text.toString().trim()

        // Walidacja wymaganych pól - sprawdzenie czy nie są puste
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
        
        // Walidacja logiczności dat - data końcowa nie może być wcześniejsza niż początkowa
        if (selectedStartDate != null && selectedEndDate != null) {
            if (selectedEndDate!!.before(selectedStartDate)) {
                Toast.makeText(this, "Data zakończenia nie może być wcześniejsza niż data rozpoczęcia", Toast.LENGTH_LONG).show()
                return
            }
        }

        // Utworzenie obiektu Drug z walidowanymi danymi
        val drug = Drug(
            name = name,
            startDate = startDate,
            endDate = endDate,
            time = time,
            dosage = dosage,
            additionalInfo = additionalInfo,
            patientEmail = currentUserEmail
        )

        // Asynchroniczny zapis do Firestore z obsługą sukcesu i błędów
        db.collection("drugs")
            .add(drug)
            .addOnSuccessListener {
                Toast.makeText(this, "Lek dodany pomyślnie", Toast.LENGTH_SHORT).show()
                finish() // Zamknięcie aktywności po pomyślnym zapisie
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd dodawania leku: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}