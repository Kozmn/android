package com.example.drugreminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter dla RecyclerView z listą pacjentów (dla widoku opiekuna).
 */
class PatientAdapter(
    private var patients: List<User>, // Przechowuje User, aby mieć dostęp do emaila
    private val onPatientClickListener: (User) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    /**
     * ViewHolder przechowujący widoki elementu listy pacjentów.
     */
    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val patientEmailTextView: TextView = itemView.findViewById(R.id.tv_patient_email)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = patients[position]
        holder.patientEmailTextView.text = patient.email // Wyświetla email pacjenta
        holder.itemView.setOnClickListener {
            onPatientClickListener(patient)
        }
    }

    override fun getItemCount(): Int = patients.size

    /**
     * Aktualizuje listę pacjentów w adapterze.
     */
    fun updatePatients(newPatients: List<User>) {
        this.patients = newPatients
        notifyDataSetChanged()
    }
}