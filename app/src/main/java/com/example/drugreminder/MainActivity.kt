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

    /**
     * Pobiera typ konta użytkownika z Firestore i konfiguruje interfejs
     * 
     * Logika biznesowa "na chłopski rozum":
     * 1. Mamy email zalogowanego użytkownika z Firebase Auth
     * 2. Idziemy do kolekcji "users" w Firestore i szukamy dokumentu o id = email
     * 3. Jeśli znajdziemy, patrzymy na pole "accountType" (pacjent/opiekun)
     * 4. W zależności od typu konta pokazujemy/ukrywamy odpowiednie przyciski
     * 
     * Dlaczego asynchroniczne?
     * - Firebase Firestore działa przez internet, więc nie wiemy ile zajmie pobranie danych
     * - Gdybyśmy czekali synchronicznie, aplikacja by się zawiesiła
     * - Używamy callbacków: onSuccessListener gdy się uda, onFailureListener gdy błąd
     * 
     * Operator '?.' (safe call):
     * - currentUserEmail?.let oznacza "jeśli currentUserEmail nie jest null, to wykonaj"
     * - To Kotlinowe bezpieczeństwo przed crash'em gdy ktoś nie jest zalogowany
     */
    private fun getUserTypeAndSetupUI() {
        currentUserEmail?.let { email ->
            db.collection("users").document(email)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // Konwersja surowych danych z Firestore na obiekt User
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

    /**
     * Konfiguruje interfejs użytkownika na podstawie typu konta
     * 
     * System uprawnień w aplikacji "na chłopski rozum":
     * 
     * PACJENT może:
     * - Dodawać swoje leki (btn_add_drug VISIBLE)
     * - Dodawać opiekunów (btn_add_caregiver VISIBLE) 
     * - Oznaczać leki jako wzięte/nie wzięte
     * - Usuwać swoje leki
     * - Widzieć tylko swoje leki
     * 
     * OPIEKUN może:
     * - Tylko przeglądać leki swoich podopiecznych (przyciski GONE)
     * - Widzieć leki wszystkich pacjentów, którzy go dodali jako opiekuna
     * - NIE może dodawać leków (to pacjent musi sam)
     * - NIE może usuwać leków pacjenta
     * 
     * Adapter z flagą showPatientName:
     * - Dla pacjenta: showPatientName = false (widzi tylko swoje leki)
     * - Dla opiekuna: showPatientName = true (widzi czyj to lek)
     */
    private fun setupUIBasedOnUserType() {
        val addDrugButton = findViewById<Button>(R.id.btn_add_drug)
        val addCaregiverButton = findViewById<Button>(R.id.btn_add_caregiver)
        
        // Logika widoczności przycisków na podstawie uprawnień
        if (currentUserType == "pacjent") {
            addDrugButton.visibility = View.VISIBLE
            addCaregiverButton.visibility = View.VISIBLE
        } else {
            // Opiekun ma ograniczone uprawnienia - może tylko przeglądać
            addDrugButton.visibility = View.GONE
            addCaregiverButton.visibility = View.GONE
        }
        
        // Konfiguracja adaptera z informacją o typie użytkownika
        // Trzeci parametr to flaga showPatientName dla różnego wyświetlania
        drugAdapter = DrugAdapter(drugList, this, currentUserType == "opiekun")
        recyclerView.adapter = drugAdapter
    }

    /**
     * Kierownik ruchu dla ładowania leków - decyduje kto co widzi
     * 
     * Rozgałęzienie logiki "na chłopski rozum":
     * - Jeśli jestem pacjentem -> pobieram tylko SWOJE leki
     * - Jeśli jestem opiekunem -> pobieram leki WSZYSTKICH pacjentów, którzy mnie dodali
     * 
     * To jest kluczowa różnica w aplikacji - każdy typ użytkownika ma inną perspektywę
     */
    private fun loadDrugs() {
        if (currentUserType == "pacjent") {
            loadPatientDrugs()  // Proste - tylko moje leki
        } else {
            loadCaregiverPatientsDrugs()  // Skomplikowane - leki z wielu kont
        }
    }

    /**
     * Pobiera leki pacjenta - proste zapytanie do bazy
     * 
     * Zapytanie Firestore "na chłopski rozum":
     * 1. Idź do kolekcji "drugs" (jak szuflada z karteczkami)
     * 2. Znajdź wszystkie dokumenty gdzie patientEmail = mój email
     * 3. Dla każdego znalezionego dokumentu:
     *    - Skonwertuj surowe dane na obiekt Drug
     *    - Zachowaj id dokumentu (potrzebne do edycji/usuwania)
     *    - Dodaj do listy wyświetlanej w UI
     * 4. Powiedz adapterowi "odśwież się, masz nowe dane"
     * 
     * Dlaczego document.id osobno?
     * - Firestore automatycznie generuje unikalne ID dla dokumentów
     * - Te ID nie są częścią naszego obiektu Drug, ale potrzebujemy ich do operacji
     * - Bez tego nie moglibyśmy usuwać ani edytować leków
     */
    private fun loadPatientDrugs() {
        currentUserEmail?.let { email ->
            db.collection("drugs")
                .whereEqualTo("patientEmail", email)  // SQL-owe WHERE w NoSQL
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        drugList.clear()  // Wyrzuć stare dane, żeby nie duplikować
                        for (document in task.result) {
                            val drug = document.toObject(Drug::class.java)
                            drug.id = document.id  // Ważne! Zachowaj ID dokumentu
                            drugList.add(drug)
                        }
                        drugAdapter.notifyDataSetChanged()  // "Hej adapter, odśwież widok!"
                    } else {
                        Toast.makeText(this, "Błąd ładowania leków", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    /**
     * Pobiera leki dla opiekuna - skomplikowane dwuetapowe zapytanie
     * 
     * Problem opiekuna "na chłopski rozum":
     * Opiekun nie ma bezpośredniego dostępu do leków. Musi najpierw znaleźć:
     * 1. Którzy pacjenci dodali go jako opiekuna
     * 2. Potem pobrać leki tych pacjentów
     * 
     * ETAP 1: Znajdź swoich podopiecznych
     * - Szukamy w kolekcji "users" 
     * - Gdzie pole "caregivers" (to lista) zawiera mój email
     * - whereArrayContains to specjalne zapytanie "czy lista zawiera element X"
     * 
     * ETAP 2: Pobierz leki znalezionych pacjentów
     * - Dla każdego emaila pacjenta wywołujemy loadDrugsForPatients()
     * 
     * Dlaczego tak skomplikowanie?
     * - Firestore to NoSQL - nie ma JOIN-ów jak w SQL
     * - Musimy sami zrobić relację "wiele-do-wielu" między pacjentami a opiekunami
     */
    private fun loadCaregiverPatientsDrugs() {
        currentUserEmail?.let { email ->
            // ETAP 1: Znajdź pacjentów, którzy mają mnie w liście opiekunów
            db.collection("users")
                .whereArrayContains("caregivers", email)  // Przeszukuj tablice
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val patientEmails = mutableListOf<String>()
                        for (document in task.result) {
                            val user = document.toObject(User::class.java)
                            // Sprawdź czy to rzeczywiście pacjent (nie inny opiekun)
                            if (user.isPatient()) {
                                patientEmails.add(user.email)
                            }
                        }
                        
                        // ETAP 2: Jeśli mam podopiecznych, pobierz ich leki
                        if (patientEmails.isNotEmpty()) {
                            loadDrugsForPatients(patientEmails)
                        }
                    }
                }
        }
    }

    /**
     * Pobiera leki dla listy pacjentów - pętla wielu zapytań
     * 
     * Wyzwanie asynchroniczności "na chłopski rozum":
     * 1. Mamy listę emaili pacjentów (może być 5, może 20)
     * 2. Dla każdego musimy zrobić osobne zapytanie do Firestore
     * 3. Problem: nie wiemy w jakiej kolejności odpowiedzi przyjdą
     * 4. Problem: nie wiemy kiedy wszystkie się zakończą
     * 
     * Dlaczego clear() na początku?
     * - Żeby nie mieszać starych danych z nowymi
     * - drugList to nasza lokalna lista w pamięci telefonu
     * 
     * Dlaczego notifyDataSetChanged() w każdej iteracji?
     * - Żeby UI aktualizowało się na bieżąco
     * - Użytkownik widzi leki pojawiające się stopniowo (lepsze UX)
     * - Alternatywa: czekać na wszystkie i pokazać naraz (gorsze UX)
     * 
     * Potencjalny problem:
     * - Jeśli opiekun ma 100 pacjentów, zrobimy 100 zapytań naraz
     * - To może przeciążyć sieć lub bazę danych
     * - W produkcji lepiej byłoby grupować zapytania
     */
    private fun loadDrugsForPatients(patientEmails: List<String>) {
        drugList.clear()  // Wyczyść przed załadowaniem nowych danych
        
        // Iteruj przez każdy email pacjenta i pobierz jego leki
        for (patientEmail in patientEmails) {
            db.collection("drugs")
                .whereEqualTo("patientEmail", patientEmail)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Dodaj leki tego pacjenta do globalnej listy
                        for (document in task.result) {
                            val drug = document.toObject(Drug::class.java)
                            drug.id = document.id
                            drugList.add(drug)
                        }
                        // Aktualizuj UI po każdym zapytaniu (leki pojawiają się stopniowo)
                        drugAdapter.notifyDataSetChanged()
                    }
                    // Uwaga: brak obsługi błędów - w produkcji dodałbym logowanie
                }
        }
    }

    /**
     * Obsługuje oznaczenie leku jako wzięty - implementacja interfejsu OnDrugActionListener
     * 
     * Wzorzec Observer "na chłopski rozum":
     * 1. DrugAdapter (lista leków) nie wie jak zapisać historię do bazy
     * 2. MainActivity (ten plik) wie jak to zrobić
     * 3. Adapter wywołuje tę metodę gdy ktoś kliknie "Wzięty"
     * 4. Tutaj robimy całą logikę biznesową
     * 
     * Kontrola uprawnień:
     * - Sprawdzamy czy to pacjent (tylko pacjent może oznaczać swoje leki)
     * - Opiekun może tylko PRZEGLĄDAĆ, nie może działać za pacjenta
     * 
     * Proces zapisu historii:
     * 1. Pobierz aktualną datę i czas (moment działania, nie czas z harmonogramu)
     * 2. Stwórz obiekt DrugHistory z wszystkimi danymi
     * 3. Zapisz do kolekcji "drug_history" w Firestore
     * 4. Pokaż komunikat o sukcesie/błędzie
     * 
     * Denormalizacja danych:
     * - Zapisujemy drugName mimo że mamy drugId
     * - To duplikacja, ale ułatwia późniejsze raporty
     * - W NoSQL często robimy trade-off: duplikacja vs wydajność
     */
    override fun onDrugTaken(drug: Drug) {
        // Kontrola uprawnień - tylko pacjent może oznaczać SWOJE leki
        if (currentUserType != "pacjent") {
            Toast.makeText(this, "Tylko pacjent może oznaczać leki jako wzięte", Toast.LENGTH_SHORT).show()
            return
        }

        // Zapisz RZECZYWISTY moment przyjęcia leku (nie planowany czas z harmonogramu)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // Stwórz rekord historii z pełnymi danymi
        val history = DrugHistory(
            drugId = drug.id,                    // Referencja do leku
            drugName = drug.name,                // Denormalizacja dla wydajności raportów
            date = currentDate,                  // Kiedy RZECZYWIŚCIE wzięto
            timeTaken = currentTime,             // O której RZECZYWIŚCIE wzięto
            taken = true,                        // Status: wzięty
            patientEmail = currentUserEmail ?: "" // Kto wziął (bezpieczeństwo)
        )

        // Asynchroniczny zapis do Firestore
        db.collection("drug_history")
            .add(history)
            .addOnSuccessListener {
                Toast.makeText(this, "Oznaczono lek jako wzięty", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd zapisu", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Obsługuje usuwanie leku - implementacja interfejsu OnDrugActionListener
     * 
     * Bezpieczeństwo usuwania "na chłopski rozum":
     * 1. Tylko właściciel leku (pacjent) może go usunąć
     * 2. Opiekun nie może usuwać leków za pacjenta (może tylko przeglądać)
     * 3. Usuwamy z kolekcji "drugs" używając document ID
     * 4. Po sukcesie odświeżamy listę w UI
     * 
     * Firestore Document Delete:
     * - drug.id to unikalny identyfikator dokumentu w Firestore
     * - document(drug.id).delete() usuwa cały dokument bezpowrotnie
     * - To operacja atomowa - albo się uda w 100%, albo wcale
     * 
     * Brak kaskadowego usuwania:
     * - Usuwając lek NIE usuwamy historii jego przyjmowania
     * - Historia pozostaje dla celów statystycznych
     * - W niektórych aplikacjach medycznych to wymóg prawny
     * 
     * loadDrugs() po sukcesie:
     * - Moglibyśmy usunąć lek tylko z lokalnej listy (drugList.remove())
     * - Ale loadDrugs() zapewnia pełną synchronizację z bazą
     * - Wolniejsze, ale bezpieczniejsze
     */
    override fun onDrugDelete(drug: Drug) {
        // Kontrola uprawnień - tylko właściciel może usuwać
        if (currentUserType != "pacjent") {
            Toast.makeText(this, "Tylko pacjent może usuwać leki", Toast.LENGTH_SHORT).show()
            return
        }

        // Bezpowrotne usunięcie dokumentu z Firestore
        db.collection("drugs").document(drug.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Lek usunięty", Toast.LENGTH_SHORT).show()
                loadDrugs()  // Pełne odświeżenie danych z bazy
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd usuwania leku", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Cykl życia aktywności - wywoływane gdy użytkownik wraca do ekranu
     * 
     * Dlaczego odświeżamy dane w onResume()?
     * 
     * Scenariusz "na chłopski rozum":
     * 1. Użytkownik jest w MainActivity (lista leków)
     * 2. Klika "Dodaj lek" -> przechodzi do AddDrugActivity
     * 3. Dodaje nowy lek i wraca (finish() w AddDrugActivity)
     * 4. MainActivity się "budzi" (onResume) ale ma stare dane w liście
     * 5. Bez loadDrugs() użytkownik nie zobaczy nowo dodanego leku
     * 
     * Inne przypadki wymagające odświeżenia:
     * - Powrót ze StatActivity
     * - Powrót z aplikacji po otrzymaniu powiadomienia
     * - Aplikacja była zminimalizowana, a inny użytkownik dodał/usunął lek
     * 
     * Koszt operacji:
     * - loadDrugs() robi zapytanie do Firestore przy każdym powrocie
     * - To niepotrzebne jeśli nic się nie zmieniło
     * - W dużej aplikacji lepiej by było używać lokalnej cache i obserwatorów
     */
    override fun onResume() {
        super.onResume()
        // Odświeżenie listy leków po powrocie do aktywności
        loadDrugs()
    }

    /**
     * Konfiguruje system powiadomień w tle - serce aplikacji
     * 
     * Architektura powiadomień "na chłopski rozum":
     * 
     * 1. KANAŁ POWIADOMIEŃ (Android 8.0+):
     *    - Android wymaga grupowania powiadomień w "kanały"
     *    - Użytkownik może wyłączyć całe kanały w ustawieniach
     *    - Nasz kanał to "Przypomnienia o lekach"
     * 
     * 2. WORKMANAGER - PRACA W TLE:
     *    - Android coraz bardziej ogranicza aplikacje działające w tle
     *    - WorkManager to oficjalny sposób na zadania cykliczne
     *    - Działa nawet gdy aplikacja jest zamknięta lub telefon śpi
     * 
     * 3. OKRESOWOŚĆ 15 MINUT:
     *    - Android nie pozwala na częstsze sprawdzanie (oszczędność baterii)
     *    - DrugReminderWorker uruchamia się co 15 min
     *    - Sprawdza czy któryś lek powinien być teraz przyjęty
     * 
     * 4. KEEP POLICY:
     *    - Jeśli ta sama praca już istnieje, zostaw ją
     *    - Zapobiega duplikowaniu zadań przy częstym otwieraniu aplikacji
     * 
     * Dlaczego tutaj, a nie w onCreate()?
     * - onCreate() może być wywoływane wielokrotnie
     * - Chcemy mieć pewność, że praca w tle jest skonfigurowana
     * - Ale nie chcemy duplikować przy każdym uruchomieniu
     */
    private fun setupNotifications() {
        // KROK 1: Przygotuj kanał powiadomień (wymagane od Android 8.0)
        NotificationHelper.createNotificationChannel(this)
        
        // KROK 2: Utwórz pracę cykliczną do sprawdzania leków
        val workRequest = PeriodicWorkRequestBuilder<DrugReminderWorker>(
            15, TimeUnit.MINUTES // Minimalna dozwolona częstotliwość w Androidzie
        ).build()
        
        // KROK 3: Zaplanuj pracę z polityką zachowania istniejącej
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "drug_reminder_work",                    // Unikalna nazwa (nie duplikuj)
            ExistingPeriodicWorkPolicy.KEEP,         // Jeśli istnieje, zostaw
            workRequest                              // Szczegóły pracy
        )
        
        // Od tego momentu co 15 minut DrugReminderWorker sprawdza leki
        // i wysyła powiadomienia użytkownikom
    }
}