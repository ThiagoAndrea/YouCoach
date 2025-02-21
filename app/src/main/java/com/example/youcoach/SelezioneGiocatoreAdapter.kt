package com.example.youcoach

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SelezioneGiocatoreAdapter(
    private val context: Context,
    private val posizioni: List<Posizione>,
    private val giocatori: List<Giocatore>
) : RecyclerView.Adapter<SelezioneGiocatoreAdapter.PosizioneViewHolder>() {

    // Mappa che memorizza la selezione per ogni posizione (chiave: posizione, valore: id del giocatore selezionato)
    val selezioni = mutableMapOf<Posizione, String>()

    inner class PosizioneViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ruoloTextView: TextView = view.findViewById(R.id.ruoloTextView)
        val giocatoreSpinner: Spinner = view.findViewById(R.id.giocatoreSpinner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosizioneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selezione_giocatore, parent, false)
        return PosizioneViewHolder(view)
    }

    override fun onBindViewHolder(holder: PosizioneViewHolder, position: Int) {
        val posizioneItem = posizioni[position]
        holder.ruoloTextView.text = "${posizioneItem.ruolo} ${posizioneItem.posizioneIndex}"

        // Ordina i giocatori: prima quelli del ruolo richiesto, poi gli altri
        val giocatoriOrdinati = giocatori.sortedWith(
            compareByDescending<Giocatore> { it.ruolo == posizioneItem.ruolo } // Priorità ai giocatori con lo stesso ruolo
                .thenBy { it.nome } // Poi ordina alfabeticamente per nome
        )

        // Crea la lista di stringhe per lo Spinner
        val giocatoriString = giocatoriOrdinati.map { "${it.nome} ${it.cognome}" }

        // Adapter per lo Spinner
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, giocatoriString)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.giocatoreSpinner.adapter = spinnerAdapter

        // Mantieni la selezione precedente, se esiste
        val selectedGiocatoreId = selezioni[posizioneItem]
        val index = giocatoriOrdinati.indexOfFirst { it.id == selectedGiocatoreId }
        if (index >= 0) {
            holder.giocatoreSpinner.setSelection(index)
        } else {
            holder.giocatoreSpinner.setSelection(0) // Default alla prima opzione
            selezioni[posizioneItem] = giocatoriOrdinati.firstOrNull()?.id ?: ""
        }

        // Gestisci la selezione dello Spinner
        holder.giocatoreSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, spinnerPosition: Int, id: Long) {
                selezioni[posizioneItem] = giocatoriOrdinati[spinnerPosition].id
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Nessuna azione necessaria
            }
        })
    }



    override fun getItemCount(): Int = posizioni.size
}
