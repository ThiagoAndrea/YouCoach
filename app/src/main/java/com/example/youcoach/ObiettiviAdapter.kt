package com.example.youcoach

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class ObiettiviAdapter(
    private var obiettivi: List<String>,
    private val isEditMode: Boolean,
    private val obiettiviSelezionati: Map<String, Boolean> = emptyMap(),
    private val onObiettivoChecked: ((String, Boolean) -> Unit)? = null,
    private val onObiettivoEliminato: ((String) -> Unit)? = null // Aggiunto callback per eliminazione
) : RecyclerView.Adapter<ObiettiviAdapter.ObiettivoViewHolder>() {

    private val selezioni = obiettiviSelezionati.toMutableMap()
    var isDeleteMode: Boolean = false

    fun updateObiettivi(newObiettivi: List<String>) {
        obiettivi = newObiettivi
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObiettivoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_obiettivo, parent, false)
        return ObiettivoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ObiettivoViewHolder, position: Int) {
        val obiettivo = obiettivi[position]
        val isChecked = selezioni[obiettivo] ?: false
        holder.bind(obiettivo, isChecked, isEditMode, isDeleteMode, onObiettivoChecked, onObiettivoEliminato)
    }

    override fun getItemCount(): Int = obiettivi.size

    fun toggleDeleteMode() {
        isDeleteMode = !isDeleteMode
        notifyDataSetChanged()
    }

    class ObiettivoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBoxObiettivo: CheckBox = itemView.findViewById(R.id.checkBoxObiettivo)
        private val textViewObiettivo: TextView = itemView.findViewById(R.id.textViewObiettivo)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.button_delete_obiettivo)

        fun bind(
            obiettivo: String,
            isChecked: Boolean,
            isEditMode: Boolean,
            isDeleteMode: Boolean,
            onObiettivoChecked: ((String, Boolean) -> Unit)?,
            onObiettivoEliminato: ((String) -> Unit)? // Aggiunto parametro
        ) {
            checkBoxObiettivo.isEnabled = true
            textViewObiettivo.text = obiettivo
            checkBoxObiettivo.isChecked = isChecked

            buttonDelete.visibility = if (isDeleteMode) View.VISIBLE else View.GONE
            checkBoxObiettivo.visibility = if (isEditMode) View.VISIBLE else View.GONE

            if (isEditMode) {
                checkBoxObiettivo.setOnCheckedChangeListener { _, isChecked ->
                    onObiettivoChecked?.invoke(obiettivo, isChecked)
                }
            }

            buttonDelete.setOnClickListener {
                onObiettivoEliminato?.invoke(obiettivo)
            }
        }
    }
}

