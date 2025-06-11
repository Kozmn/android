package com.example.drugreminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * PatientAdapter - wyświetla listę pacjentów dla opiekuna
 *
 * Opiekun może mieć wielu pacjentów pod opieką. Ten adapter pokazuje ich emaile
 * w formie przewijanej listy. Po kliknięciu na pacjenta można zobaczyć jego leki.
 * 
 * Używa RecyclerView dla wydajności - nawet przy setkach pacjentów lista będzie szybka.
 * Każdy wiersz pokazuje email pacjenta i reaguje na dotknięcia.
 */
class PatientAdapter(
    private var patients: List<User>, // Lista pacjentów do pokazania
    private val onPatientClickListener: (User) -> Unit // Co zrobić gdy ktoś kliknie
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    /**
     * ViewHolder przechowuje pola tekstowe z jednego wiersza
     * 
     * Szukanie pól przez findViewById jest wolne, więc robimy to tylko raz.
     * Potem tylko zmieniamy tekst w już znalezionych polach.
     */
    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val patientEmailTextView: TextView = itemView.findViewById(R.id.tv_patient_email)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        // Tworzy nowy wiersz dla jednego pacjenta
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        // Wypełnia wiersz danymi konkretnego pacjenta
        val patient = patients[position]
        holder.patientEmailTextView.text = patient.email // Pokazuje email pacjenta
        
        // Ustawia co ma się stać gdy opiekun kliknie na tego pacjenta
        holder.itemView.setOnClickListener {
            onPatientClickListener(patient) // Wywołuje funkcję przekazaną z głównego ekranu
        }
    }

    override fun getItemCount(): Int = patients.size

    /**
     * Aktualizuje listę pacjentów gdy coś się zmieni
     * 
     * Gdy opiekun doda nowego pacjenta, lista musi się odświeżyć.
     * notifyDataSetChanged() mówi RecyclerView żeby sprawdził wszystko od nowa.
     */
    fun updatePatients(newPatients: List<User>) {
        this.patients = newPatients        // Zapisuje nową listę
        notifyDataSetChanged()             // Odświeża widok na ekranie
    }
}