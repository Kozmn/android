# DrugReminder - Dokumentacja Aplikacji

## 📱 Opis Aplikacji

**DrugReminder** to aplikacja mobilna na system Android służąca do zarządzania lekami i przypominania o ich przyjmowaniu. Aplikacja została zaprojektowana z myślą o pacjentach oraz ich opiekunach, oferując różne poziomy dostępu i funkcjonalności.

---

## 🏗️ Architektura Aplikacji

### Wzorce Projektowe
- **MVP (Model-View-Presenter)** - separacja logiki biznesowej od interfejsu użytkownika
- **Observer Pattern** - komunikacja między komponentami przez interfejsy
- **Singleton Pattern** - globalne obiekty jak NotificationHelper
- **ViewHolder Pattern** - optymalizacja wydajności RecyclerView
- **Repository Pattern** - abstrakcja dostępu do danych (Firebase)

### Technologie
- **Język**: Kotlin
- **Framework**: Android SDK
- **Baza danych**: Firebase Firestore (NoSQL)
- **Autoryzacja**: Firebase Authentication
- **Powiadomienia**: Android Notification API + WorkManager
- **UI**: Android Views + RecyclerView
- **Asynchroniczność**: Kotlin Coroutines

---

## 👥 Typy Użytkowników

### 🏥 Pacjent
**Uprawnienia:**
- ✅ Dodawanie nowych leków do systemu
- ✅ Oznaczanie leków jako przyjęte/nieprzyjęte
- ✅ Usuwanie własnych leków
- ✅ Przeglądanie własnej historii przyjmowania
- ✅ Dodawanie opiekunów do swojego konta
- ✅ Otrzymywanie powiadomień o lekach

### 👨‍⚕️ Opiekun
**Uprawnienia:**
- ✅ Przeglądanie leków wszystkich przypisanych pacjentów
- ✅ Przeglądanie historii przyjmowania pacjentów
- ✅ Generowanie raportów dla pacjentów
- ❌ Dodawanie/usuwanie leków (tylko pacjent)
- ❌ Oznaczanie leków jako przyjęte (tylko pacjent)

---

## 📊 Model Danych

### 🏗️ Struktura Bazy Danych (Firebase Firestore)

#### Kolekcja: `users`
```kotlin
data class User(
    var email: String = "",              // Klucz główny
    var accountType: String = "",        // "pacjent" | "opiekun"
    var caregivers: MutableList<String> = mutableListOf()  // Lista emaili opiekunów
)
```

#### Kolekcja: `drugs`
```kotlin
data class Drug(
    var id: String = "",              // Auto-generowane ID dokumentu
    var name: String = "",            // Nazwa leku
    var startDate: String = "",       // YYYY-MM-DD
    var endDate: String = "",         // YYYY-MM-DD
    var time: String = "",            // HH:MM (24h)
    var dosage: String = "",          // Opis dawkowania
    var additionalInfo: String = "",  // Dodatkowe informacje
    var patientEmail: String = ""     // Klucz obcy do users
)
```

#### Kolekcja: `drug_history`
```kotlin
data class DrugHistory(
    var drugId: String = "",          // Referencja do drugs
    var drugName: String = "",        // Denormalizacja dla wydajności
    var date: String = "",            // YYYY-MM-DD
    var timeTaken: String = "",       // HH:MM
    var taken: Boolean = false,       // true/false
    var patientEmail: String = ""     // Klucz obcy do users
)
```

---

## 🎯 Główne Funkcjonalności

### 1. 🔐 System Autoryzacji
- **Rejestracja** z wyborem typu konta (pacjent/opiekun)
- **Logowanie** przez Firebase Authentication
- **Kontrola sesji** z automatycznym przekierowaniem
- **Zarządzanie uprawnieniami** na podstawie typu konta

### 2. 💊 Zarządzanie Lekami

#### Dodawanie Leków (Tylko Pacjent)
- **Formularz walidowany** z polami:
  - Nazwa leku (wymagane)
  - Data rozpoczęcia kuracji (DatePicker)
  - Data zakończenia kuracji (DatePicker)
  - Godzina przyjmowania (TimePicker)
  - Dawkowanie (wymagane)
  - Dodatkowe informacje (opcjonalne)

#### Walidacje
- ✅ Sprawdzanie wypełnienia pól wymaganych
- ✅ Walidacja logiczności dat (koniec ≥ początek)
- ✅ Format daty i czasu przez native pickery
- ✅ Zabezpieczenie przed nieprawidłowymi danymi

#### Wyświetlanie Leków
- **Lista dla pacjenta**: tylko własne leki
- **Lista dla opiekuna**: leki wszystkich przypisanych pacjentów + info o pacjencie
- **Akcje dostępne**: oznacz jako wzięty, usuń (tylko pacjent)

### 3. 📊 System Historii i Statystyk

#### Śledzenie Przyjmowania
- **Automatyczne zapisywanie** każdej akcji użytkownika
- **Timestamp** z rzeczywistą godziną przyjęcia/odrzucenia
- **Status binarny** - wzięty/nie wzięty

#### Raporty dla Opiekunów
- **Agregacja danych** z wielu pacjentów
- **Grupowanie po pacjentach** dla czytelności
- **Export do tekstu** z możliwością udostępnienia
- **Analiza adherencji** (przestrzegania zaleceń)

### 4. 🔔 System Powiadomień w Tle

#### Architektura Powiadomień
```
WorkManager (co 15 min)
    ↓
DrugReminderWorker
    ↓
Firebase Firestore (sprawdzenie leków)
    ↓
NotificationHelper (wyświetlenie)
    ↓
DrugActionReceiver (obsługa akcji)
```

#### Logika Czasowa
- **Sprawdzanie co 15 minut** przez WorkManager
- **Tolerancja ±5 minut** od docelowej godziny
- **Weryfikacja dat kuracji** (start ≤ dzisiaj ≤ koniec)
- **Kontrola duplikatów** - sprawdzenie czy już wzięty dzisiaj

#### Powiadomienia Interaktywne
- **Akcja "Wzięty"**: zapisuje pozytywną historię + anuluje powiadomienie
- **Akcja "Nie wzięty"**: zapisuje negatywną historię + anuluje powiadomienie
- **Kliknięcie główne**: otwiera aplikację

### 5. 🤝 System Opiekunów

#### Zarządzanie Relacjami
- **Dodawanie opiekuna** przez pacjenta (podanie emaila)
- **Walidacja istnienia** opiekuna w systemie
- **Relacja many-to-many** - jeden opiekun może mieć wielu pacjentów

#### Widok Opiekuna
- **Lista wszystkich pacjentów** z ich lekami
- **Identyfikacja pacjenta** przy każdym leku
- **Dostęp do historii** wszystkich przypisanych pacjentów
- **Brak uprawnień modyfikacji** (tylko odczyt)

---

## 📱 Struktura Aktywności

### StartActivity
- **Ekran powitalny** z wyborem logowania/rejestracji
- **Punkt wejścia** do aplikacji
- **Przekierowanie** zalogowanych użytkowników

### LoginActivity
- **Formularz logowania** z walidacją
- **Integracja z Firebase Auth**
- **Obsługa błędów** autoryzacji

### RegisterActivity
- **Formularz rejestracji** z wyborem typu konta
- **Tworzenie dokumentu użytkownika** w Firestore
- **Walidacja unikalności** emaila

### MainActivity
- **Główny ekran** aplikacji
- **Lista leków** z RecyclerView
- **Przyciski kontekstowe** zależne od uprawnień
- **Zarządzanie powiadomieniami** w tle

### AddDrugActivity
- **Formularz dodawania leku** z walidacją
- **Native pickers** dla dat i czasu
- **Zapisywanie do Firestore**

### AddCaregiverActivity
- **Formularz dodawania opiekuna**
- **Walidacja istnienia** użytkownika
- **Aktualizacja listy opiekunów**

### StatActivity
- **Wyświetlanie historii** przyjmowania leków
- **Różne widoki** dla pacjenta i opiekuna
- **Export danych** do tekstu

---

## 🔧 Komponenty Techniczne

### Adaptery RecyclerView

#### DrugAdapter
- **Wyświetlanie listy leków** z dynamiczną widocznością elementów
- **Obsługa akcji** przez interfejs OnDrugActionListener
- **ViewHolder pattern** dla wydajności
- **Kontekstowe wyświetlanie** (pacjent vs opiekun)

#### StatAdapter
- **Lista historii** przyjmowania leków
- **Grupowanie po pacjentach** dla opiekunów
- **Formatowanie dat i czasów**
- **Kolorowanie statusów** (wzięty/nie wzięty)

### Powiadomienia

#### NotificationHelper (Singleton)
- **Tworzenie kanałów** powiadomień (Android 8.0+)
- **Generowanie powiadomień** z akcjami
- **Zarządzanie PendingIntent** dla interakcji
- **Kompatybilność wsteczna** z starszymi wersjami

#### DrugActionReceiver (BroadcastReceiver)
- **Obsługa akcji** z powiadomień
- **Zapisywanie historii** do Firestore
- **Anulowanie powiadomień** po akcji
- **Wyświetlanie potwierdzenia** użytkownikowi

#### DrugReminderWorker (CoroutineWorker)
- **Periodyczne sprawdzanie** leków w tle
- **Asynchroniczne operacje** na Firestore
- **Filtrowanie leków** według czasu i dat
- **Generowanie powiadomień** w odpowiednim momencie

---

## 🔒 Bezpieczeństwo i Walidacja

### Kontrola Dostępu
- **Autoryzacja Firebase** dla wszystkich operacji
- **Sprawdzanie uprawnień** na poziomie UI
- **Filtrowanie danych** według użytkownika
- **Walidacja po stronie klienta** przed zapisem

### Walidacja Danych
- **Wymagane pola** - sprawdzanie pustych wartości
- **Formaty dat** - native pickery zapobiegają błędom
- **Logika biznesowa** - data końca ≥ data początku
- **Sanityzacja inputów** - trim() i walidacja długości

### Obsługa Błędów
- **Try-catch bloki** dla operacji Firebase
- **Komunikaty użytkownikowi** przez Toast
- **Graceful degradation** przy braku połączenia
- **Retry mechanizmy** w WorkManager

---

## 🚀 Wydajność i Optymalizacja

### Lazy Initialization
- **Firebase instancje** inicjalizowane przy pierwszym użyciu
- **Adaptery RecyclerView** tworzone na żądanie
- **Context** przekazywany tylko gdy potrzebny

### Asynchroniczne Operacje
- **Kotlin Coroutines** dla nieblokujących operacji I/O
- **Firebase callbacks** dla operacji bazodanowych
- **WorkManager** dla zadań w tle
- **Background threads** dla ciężkich obliczeń

### Optymalizacja UI
- **ViewHolder pattern** w RecyclerView
- **Warunkowe wyświetlanie** elementów UI
- **Minimalne layouty** dla lepszej wydajności
- **Recykling widoków** w listach

---

## 📋 Wymagania Systemowe

### Minimum
- **Android 7.0** (API 24)
- **2GB RAM**
- **100MB miejsca** na dysku
- **Połączenie internetowe** (WiFi/dane mobilne)

### Zalecane
- **Android 10.0+** (API 29+)
- **4GB+ RAM**
- **Aktywne konto Google** dla Firebase

### Uprawnienia
- `INTERNET` - komunikacja z Firebase
- `POST_NOTIFICATIONS` - powiadomienia (Android 13+)
- `SCHEDULE_EXACT_ALARM` - dokładne alarmy
- `USE_EXACT_ALARM` - używanie alarmów
- `RECEIVE_BOOT_COMPLETED` - restart po restarcie systemu

---

## 🛠️ Instalacja i Uruchomienie

### Wymagania Deweloperskie
```bash
# Android Studio Arctic Fox 2020.3.1+
# Kotlin 1.5+
# Gradle 7.0+
# Firebase projekt z skonfigurowanymi:
#   - Authentication (Email/Password)
#   - Firestore Database
#   - google-services.json w app/
```

### Budowanie Aplikacji
```bash
# Klonowanie repozytorium
git clone <repo-url>
cd DrugReminder

# Budowanie debug
./gradlew assembleDebug

# Budowanie release
./gradlew assembleRelease

# Uruchomienie testów
./gradlew test

# Instalacja na urządzeniu
./gradlew installDebug
```

---

## 🐛 Rozwiązywanie Problemów

### Typowe Problemy

#### Brak powiadomień
1. Sprawdź uprawnienia aplikacji w ustawieniach Androida
2. Wyłącz optymalizację baterii dla aplikacji
3. Zweryfikuj działanie WorkManager w logach

#### Błędy Firebase
1. Sprawdź połączenie internetowe
2. Zweryfikuj konfigurację google-services.json
3. Sprawdź reguły Firestore Security Rules

#### Problemy z synchronizacją
1. Wymuś odświeżenie przez wyjście i wejście do aplikacji
2. Sprawdź czy użytkownik jest zalogowany
3. Zweryfikuj uprawnienia dostępu do danych

---

## 📈 Przyszłe Usprawnienia

### Planowane Funkcjonalności
- 📊 **Zaawansowane analytics** - wykresy adherencji
- 🔄 **Synchronizacja offline** - cache lokalny
- 🌍 **Wielojęzyczność** - lokalizacja aplikacji
- 📱 **Widget na ekran główny** - szybki dostęp
- 🔐 **Biometric authentication** - odcisk palca/twarz
- 📧 **Powiadomienia email** - raporty dla opiekunów
- 🗣️ **Przypomnienia głosowe** - TTS notifications
- 📷 **Skanowanie leków** - rozpoznawanie OCR

### Optymalizacje Techniczne
- 🏗️ **Migration to Jetpack Compose** - nowoczesny UI
- 🔄 **Room Database** - lokalne cache
- 🧪 **Unit Testing** - pokrycie testami
- 📱 **Tablet UI** - responsywny design
- ⚡ **Performance monitoring** - Firebase Performance

---

## 📞 Wsparcie

Dla deweloperów pracujących nad aplikacją:

### Dokumentacja Kodu
- Wszystkie klasy zawierają szczegółowe komentarze w języku polskim
- Metody publiczne udokumentowane z parametrami i wartościami zwracanymi
- Przykłady użycia w komentarzach

### Konwencje Nazewnictwa
- **Klasy**: PascalCase (MainActivity, DrugAdapter)
- **Metody/Zmienne**: camelCase (addDrug, currentUser)
- **Stałe**: UPPER_SNAKE_CASE (ACTION_DRUG_TAKEN)
- **Pliki zasobów**: snake_case (activity_main.xml)

---

*Dokumentacja wygenerowana automatycznie na podstawie analizy kodu aplikacji DrugReminder*
