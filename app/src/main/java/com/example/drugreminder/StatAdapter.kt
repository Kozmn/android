package com.example.drugreminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter dla RecyclerView z historią brania leków
 */
class StatAdapter(
    private val historyList: List<DrugHistory>
) : RecyclerView.Adapter<StatAdapter.StatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stat, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        val history = historyList[position]
        
        holder.drugName.text = history.drugName
        holder.date.text = "Data: ${history.date}"
        holder.timeTaken.text = "Godzina wzięcia: ${history.timeTaken}"
        holder.status.text = if (history.taken) "Wzięty" else "Nie wzięty"
    }

    override fun getItemCount(): Int = historyList.size

    /**
     * ViewHolder przechowujący widoki elementu historii
     */
    class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val drugName: TextView = itemView.findViewById(R.id.tv_stat_drug_name)
        val date: TextView = itemView.findViewById(R.id.tv_stat_date)
        val timeTaken: TextView = itemView.findViewById(R.id.tv_stat_time_taken)
        val status: TextView = itemView.findViewById(R.id.tv_stat_status)
    }
}