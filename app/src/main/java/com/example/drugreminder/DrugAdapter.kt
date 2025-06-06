package com.example.drugreminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter do wyświetlania listy leków w RecyclerView
 *
 * Funkcjonalności:
 * - Wyświetlanie szczegółów leku (nazwa, dawka, godzina)
 * - Obsługa przycisków "Wzięty" i "Usuń"
 * - Dynamiczne ukrywanie/pokazywanie dodatkowych informacji
 *
 * Komponenty:
 * - DrugViewHolder: przechowuje referencje do widoków elementu listy
 * - OnDrugActionListener: interfejs do komunikacji z MainActivity
 *
 * Wykorzystanie:
 * - Główna lista leków w MainActivity
 * - Obsługa interakcji użytkownika z lekami
 */
class DrugAdapter(
    private val drugList: List<Drug>,
    private val listener: OnDrugActionListener,
    private val showPatientInfo: Boolean = false
) : RecyclerView.Adapter<DrugAdapter.DrugViewHolder>() {

    interface OnDrugActionListener {
        fun onDrugTaken(drug: Drug)
        fun onDrugDelete(drug: Drug)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrugViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drug, parent, false)
        return DrugViewHolder(view)
    }

    override fun onBindViewHolder(holder: DrugViewHolder, position: Int) {
        val drug = drugList[position]
        
        holder.drugName.text = drug.name
        holder.drugTime.text = "Godzina: ${drug.time}"
        holder.drugDosage.text = "Dawka: ${drug.dosage}"
        holder.drugDates.text = "Od ${drug.startDate} do ${drug.endDate}"
        
        // Wyświetlanie informacji o pacjencie (tylko dla opiekuna)
        if (showPatientInfo) {
            holder.drugPatient.text = "Pacjent: ${drug.patientEmail}"
            holder.drugPatient.visibility = View.VISIBLE
        } else {
            holder.drugPatient.visibility = View.GONE
        }
        
        // Wyświetlanie dodatkowych informacji
        if (drug.additionalInfo.isNotEmpty()) {
            holder.drugInfo.text = "Info: ${drug.additionalInfo}"
            holder.drugInfo.visibility = View.VISIBLE
        } else {
            holder.drugInfo.visibility = View.GONE
        }

        // Obsługa przycisków
        holder.takenButton.setOnClickListener {
            listener.onDrugTaken(drug)
        }

        holder.deleteButton.setOnClickListener {
            listener.onDrugDelete(drug)
        }
    }

    override fun getItemCount(): Int = drugList.size

    /**
     * ViewHolder przechowujący widoki elementu listy
     */
    class DrugViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val drugName: TextView = itemView.findViewById(R.id.tv_drug_name)
        val drugTime: TextView = itemView.findViewById(R.id.tv_drug_time)
        val drugDosage: TextView = itemView.findViewById(R.id.tv_drug_dosage)
        val drugPatient: TextView = itemView.findViewById(R.id.tv_drug_patient)
        val drugDates: TextView = itemView.findViewById(R.id.tv_drug_dates)
        val drugInfo: TextView = itemView.findViewById(R.id.tv_drug_info)
        val takenButton: Button = itemView.findViewById(R.id.btn_taken)
        val deleteButton: Button = itemView.findViewById(R.id.btn_delete)
    }
}