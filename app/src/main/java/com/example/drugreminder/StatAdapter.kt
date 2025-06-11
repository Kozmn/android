package com.example.drugreminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * StatAdapter - wyświetla historię brania leków w formie listy
 * 
 * Pacjent widzi tylko swoją historię leków.
 * Opiekun widzi historię wszystkich swoich pacjentów z informacją czyj to lek.
 * 
 * Każdy wpis pokazuje nazwę leku, datę, godzinę i czy został wzięty.
 * Używa RecyclerView żeby móc przewijać długie listy bez lagów.
 */
class StatAdapter(
    private val historyList: List<DrugHistory>,
    private val showPatientInfo: Boolean = false
) : RecyclerView.Adapter<StatAdapter.StatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        // Tworzy nowy wiersz dla jednego wpisu z historii
        // RecyclerView używa tego szablonu do tworzenia wierszy
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stat, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        // Wypełnia wiersz danymi z konkretnego wpisu historii
        val history = historyList[position]
        
        holder.drugName.text = history.drugName
        holder.date.text = "Data: ${history.date}"
        holder.timeTaken.text = "Godzina wzięcia: ${history.timeTaken}"
        holder.status.text = if (history.taken) "Wzięty" else "Nie wzięty"
        
        // Dla opiekuna pokazujemy też email pacjenta
        // Opiekun musi wiedzieć który pacjent brał dany lek
        if (showPatientInfo) {
            holder.patient.visibility = View.VISIBLE
            holder.patient.text = "Pacjent: ${history.patientEmail}"
        } else {
            holder.patient.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = historyList.size

    /**
     * ViewHolder przechowuje wszystkie pola tekstowe z jednego wiersza
     * 
     * Szukanie pól przez findViewById jest wolne, więc robimy to tylko raz.
     * Potem tylko zmieniamy tekst w już znalezionych polach.
     */
    class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val drugName: TextView = itemView.findViewById(R.id.tv_stat_drug_name)
        val patient: TextView = itemView.findViewById(R.id.tv_stat_patient)
        val date: TextView = itemView.findViewById(R.id.tv_stat_date)
        val timeTaken: TextView = itemView.findViewById(R.id.tv_stat_time_taken)
        val status: TextView = itemView.findViewById(R.id.tv_stat_status)
    }
}