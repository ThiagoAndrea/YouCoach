package com.example.youcoach

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ObiettiviAdapter(
    private val obiettivi: List<String>, // Lista degli obiettivi
    private val isEditMode: Boolean, // Modalità modifica (true) o visualizzazione (false)
    private val obiettiviSelezionati: Map<String, Boolean> = emptyMap(), // Mappa delle selezioni iniziali
    private val onObiettivoChecked: ((String, Boolean) -> Unit)? = null // Callback per la selezione
) : RecyclerView.Adapter<ObiettiviAdapter.ObiettivoViewHolder>() {

    // Mappa interna per tenere traccia delle selezioni
    private val selezioni = obiettiviSelezionati.toMutableMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObiettivoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_obiettivo, parent, false)
        return ObiettivoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ObiettivoViewHolder, position: Int) {
        val obiettivo = obiettivi[position]
        val isChecked = selezioni[obiettivo] ?: false
        holder.bind(obiettivo, isChecked, isEditMode, onObiettivoChecked)
    }

    override fun getItemCount(): Int = obiettivi.size

    // Aggiorna la mappa delle selezioni
    fun setObiettiviSelezionati(selezioni: Map<String, Boolean>) {
        this.selezioni.clear()
        this.selezioni.putAll(selezioni)
        notifyDataSetChanged()
    }

    // ViewHolder per gli obiettivi
    class ObiettivoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBoxObiettivo: CheckBox = itemView.findViewById(R.id.checkBoxObiettivo)
        private val textViewObiettivo: TextView = itemView.findViewById(R.id.textViewObiettivo)

        fun bind(
            obiettivo: String,
            isChecked: Boolean,
            isEditMode: Boolean,
            onObiettivoChecked: ((String, Boolean) -> Unit)?
        ) {
            textViewObiettivo.text = obiettivo
            checkBoxObiettivo.isChecked = isChecked

            // Imposta la visibilità in base alla modalità di modifica
            checkBoxObiettivo.visibility = if (isEditMode) View.VISIBLE else View.GONE

            if (isEditMode) {
                checkBoxObiettivo.setOnCheckedChangeListener { _, isChecked ->
                    onObiettivoChecked?.invoke(obiettivo, isChecked)
                }
            } else {
                checkBoxObiettivo.setOnCheckedChangeListener(null) // Rimuovi il listener
            }
        }

    }
}