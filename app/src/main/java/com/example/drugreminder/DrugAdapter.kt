package com.example.drugreminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * DrugAdapter - adapter RecyclerView do wyświetlania listy leków
 *
 * Architektura:
 * - Implementuje wzorzec ViewHolder dla wydajności przewijania
 * - Wykorzystuje interfejs OnDrugActionListener dla komunikacji z Activity
 * - Obsługuje różne tryby wyświetlania (pacjent vs opiekun)
 *
 * Funkcjonalności główne:
 * - Wyświetlanie szczegółów leku (nazwa, dawka, godzina, daty kuracji)
 * - Obsługa akcji użytkownika przez przyciski "Wzięty" i "Usuń"
 * - Dynamiczne ukrywanie/pokazywanie dodatkowych informacji
 * - Kontekstowe wyświetlanie informacji o pacjencie dla opiekunów
 *
 * Komponenty wewnętrzne:
 * - DrugViewHolder: kontener referencji do elementów UI pojedynczego elementu
 * - OnDrugActionListener: interfejs callback dla komunikacji z Activity
 *
 * Parametry konstrukcji:
 * - drugList: lista obiektów Drug do wyświetlenia
 * - listener: implementacja interfejsu OnDrugActionListener
 * - showPatientInfo: flaga określająca czy pokazywać informacje o pacjencie
 */
class DrugAdapter(
    private val drugList: List<Drug>,
    private val listener: OnDrugActionListener,
    private val showPatientInfo: Boolean = false // true dla opiekunów, false dla pacjentów
) : RecyclerView.Adapter<DrugAdapter.DrugViewHolder>() {

    /**
     * Interfejs komunikacji z Activity obsługującą adapter
     * Implementuje wzorzec Observer dla zdarzeń związanych z lekami
     */
    interface OnDrugActionListener {
        fun onDrugTaken(drug: Drug)    // Wywołane gdy użytkownik oznacza lek jako wzięty
        fun onDrugDelete(drug: Drug)   // Wywołane gdy użytkownik chce usunąć lek
    }

    /**
     * Tworzy nowy ViewHolder na podstawie layoutu item_drug.xml
     * Wywoływane przez RecyclerView gdy potrzebny jest nowy element
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrugViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drug, parent, false)
        return DrugViewHolder(view)
    }

    /**
     * Wiąże dane obiektu Drug z elementami UI w ViewHolder
     * Konfiguruje wyświetlanie i obsługę zdarzeń dla konkretnego elementu listy
     */
    override fun onBindViewHolder(holder: DrugViewHolder, position: Int) {
        val drug = drugList[position]
        
        // Ustawienie podstawowych informacji o leku
        holder.drugName.text = drug.name
        holder.drugTime.text = "Godzina: ${drug.time}"
        holder.drugDosage.text = "Dawka: ${drug.dosage}"
        holder.drugDates.text = "Od ${drug.startDate} do ${drug.endDate}"
        
        // Kontekstowe wyświetlanie informacji o pacjencie (tylko dla opiekuna)
        if (showPatientInfo) {
            holder.drugPatient.text = "Pacjent: ${drug.patientEmail}"
            holder.drugPatient.visibility = View.VISIBLE
        } else {
            holder.drugPatient.visibility = View.GONE
        }
        
        // Warunkowe wyświetlanie dodatkowych informacji o leku
        if (drug.additionalInfo.isNotEmpty()) {
            holder.drugInfo.text = "Info: ${drug.additionalInfo}"
            holder.drugInfo.visibility = View.VISIBLE
        } else {
            holder.drugInfo.visibility = View.GONE
        }

        // Konfiguracja listenerów przycisków z przekazaniem obiektu drug do Activity
        holder.takenButton.setOnClickListener {
            listener.onDrugTaken(drug)
        }

        holder.deleteButton.setOnClickListener {
            listener.onDrugDelete(drug)
        }
    }

    /**
     * Zwraca liczbę elementów w liście - wymagane przez RecyclerView
     */
    override fun getItemCount(): Int = drugList.size

    /**
     * DrugViewHolder - kontener referencji do elementów UI pojedynczego elementu listy
     * 
     * Implementuje wzorzec ViewHolder dla optymalizacji wydajności RecyclerView
     * Przechowuje referencje do wszystkich TextView i Button w layoutcie item_drug.xml
     */
    class DrugViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val drugName: TextView = itemView.findViewById(R.id.tv_drug_name)          // Nazwa leku
        val drugTime: TextView = itemView.findViewById(R.id.tv_drug_time)          // Godzina przyjmowania
        val drugDosage: TextView = itemView.findViewById(R.id.tv_drug_dosage)      // Dawkowanie
        val drugPatient: TextView = itemView.findViewById(R.id.tv_drug_patient)    // Email pacjenta (dla opiekuna)
        val drugDates: TextView = itemView.findViewById(R.id.tv_drug_dates)        // Daty kuracji
        val drugInfo: TextView = itemView.findViewById(R.id.tv_drug_info)          // Dodatkowe informacje
        val takenButton: Button = itemView.findViewById(R.id.btn_taken)            // Przycisk "Wzięty"
        val deleteButton: Button = itemView.findViewById(R.id.btn_delete)          // Przycisk "Usuń"
    }
}