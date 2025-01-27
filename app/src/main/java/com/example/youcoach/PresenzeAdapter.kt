package com.example.youcoach

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PresenzeAdapter(
    private val giocatori: List<Giocatore>,
    private val onPresenzaChanged: (Giocatore, Int) -> Unit
) : RecyclerView.Adapter<PresenzeAdapter.GiocatoreViewHolder>() {

    // Stati della presenza
    private val statiPresenza = listOf("Presente", "Assente", "Ritardo")

    inner class GiocatoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtGiocatore: TextView = itemView.findViewById(R.id.txt_giocatore)
        private val spinnerPresenza: Spinner = itemView.findViewById(R.id.spinner_presenza)

        fun bind(giocatore: Giocatore) {
            // Mostra nome e cognome del giocatore
            txtGiocatore.text = "${giocatore.nome} ${giocatore.cognome}"

            // Configura lo Spinner
            val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_item, statiPresenza)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPresenza.adapter = adapter

            // Imposta lo stato iniziale (es. "Presente")
            spinnerPresenza.setSelection(0)

            // Gestisci il cambio di stato
            spinnerPresenza.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val nuovoStato = position // 0: Presente, 1: Assente, 2: Ritardo
                    onPresenzaChanged(giocatore, nuovoStato)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiocatoreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_giocatore, parent, false)
        return GiocatoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GiocatoreViewHolder, position: Int) {
        holder.bind(giocatori[position])
    }

    override fun getItemCount(): Int = giocatori.size
}