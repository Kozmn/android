# DrugReminder 💊

**Aplikacja Android do zarządzania lekami z rolami pacjenta i opiekuna**

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)

## 📱 Funkcjonalności

### Dla Pacjentów
- ✅ **Dodawanie leków** - nazwa, dawkowanie, godziny przyjmowania
- ✅ **Powiadomienia** - automatyczne przypomnienia o lekach
- ✅ **Śledzenie przyjmowania** - oznaczanie wzięcia leku
- ✅ **Historia leków** - pełne dane o przyjmowanych lekach
- ✅ **Statystyki** - analiza regularności przyjmowania
- ✅ **Eksport danych** - kopiowanie i udostępnianie raportów

### Dla Opiekunów
- ✅ **Zarządzanie pacjentami** - dodawanie podopiecznych
- ✅ **Monitorowanie** - śledzenie przyjmowania leków pacjentów
- ✅ **Raporty** - przegląd statystyk wszystkich pacjentów
- ✅ **Powiadomienia** - alerty o nieprzyjętych lekach

## 🛠 Technologie

- **Platform:** Android (Kotlin)
- **Database:** Firebase Firestore
- **Authentication:** Firebase Auth
- **Background Tasks:** WorkManager
- **UI:** Material Design Components
- **Notifications:** Android Notification API
- **Build System:** Gradle with Kotlin DSL

## 🚀 Instalacja

### Wymagania
- Android Studio Arctic Fox lub nowszy
- JDK 11 lub nowszy
- Android SDK API 21+
- Konto Firebase

### Kroki instalacji

1. **Sklonuj repozytorium**
   ```bash
   git clone https://github.com/twoj-username/DrugReminder.git
   cd DrugReminder
   ```

2. **Skonfiguruj Firebase**
   - Utwórz projekt w [Firebase Console](https://console.firebase.google.com/)
   - Włącz Authentication (Email/Password)
   - Włącz Firestore Database
   - Pobierz `google-services.json` i umieść w `app/`

3. **Zbuduj projekt**
   ```bash
   ./gradlew build
   ```

4. **Uruchom aplikację**
   - Podłącz urządzenie Android lub uruchom emulator
   - Kliknij "Run" w Android Studio

## 📊 Architektura

```
app/
├── Activities/           # Ekrany aplikacji
│   ├── StartActivity     # Ekran powitalny
│   ├── LoginActivity     # Logowanie
│   ├── RegisterActivity  # Rejestracja
│   ├── MainActivity      # Lista leków
│   ├── AddDrugActivity   # Dodawanie leku
│   ├── AddCaregiverActivity # Dodawanie opiekuna
│   └── StatActivity      # Statystyki
├── Adapters/            # RecyclerView adaptery
├── Models/              # Modele danych (Drug, User, DrugHistory)
├── Helpers/             # Klasy pomocnicze (Firebase, Notifications)
├── Workers/             # Background tasks (WorkManager)
└── Receivers/           # Broadcast receivers (Notifications)
```

## 🔐 Bezpieczeństwo

- **Authentication** - Firebase Auth z weryfikacją email/hasło
- **Data Validation** - sprawdzanie wszystkich danych wejściowych
- **Access Control** - użytkownicy widzą tylko swoje dane
- **Secure Storage** - dane przechowywane w Firebase Firestore

## 📝 Licencja

Ten projekt jest licencjonowany na podstawie MIT License - zobacz plik [LICENSE](LICENSE) dla szczegółów.

## 👥 Autorzy

- **Twoje Imię** - *Główny programista* - [GitHub](https://github.com/twoj-username)

## 🤝 Wkład w projekt

1. Fork projektu
2. Utwórz branch funkcjonalności (`git checkout -b feature/NowaFunkcjonalnosc`)
3. Commit zmian (`git commit -m 'Dodaj nową funkcjonalność'`)
4. Push do branch (`git push origin feature/NowaFunkcjonalnosc`)
5. Otwórz Pull Request

## 📞 Kontakt

Projekt Link: [https://github.com/twoj-username/DrugReminder](https://github.com/twoj-username/DrugReminder)

---

⭐ Jeśli projekt Ci się podoba, zostaw gwiazdkę!
