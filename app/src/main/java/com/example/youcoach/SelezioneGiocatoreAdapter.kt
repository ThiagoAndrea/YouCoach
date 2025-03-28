package com.example.youcoach

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SelezioneGiocatoreAdapter(
    private val context: Context,
    private val posizioni: List<Posizione>,
    private val giocatori: List<Giocatore>
) : RecyclerView.Adapter<SelezioneGiocatoreAdapter.PosizioneViewHolder>() {

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

    override fun getItemCount(): Int = posizioni.size

    override fun onBindViewHolder(holder: PosizioneViewHolder, position: Int) {
        val posizioneItem = posizioni[position]
        holder.ruoloTextView.text = "${posizioneItem.ruolo} ${posizioneItem.posizioneIndex}"

        // Costruiamo l'insieme degli ID già selezionati in altre posizioni
        val selezioniAltre = selezioni.filter { it.key != posizioneItem }.values.toSet()

        // Recuperiamo il giocatore già selezionato per questa posizione (se esiste)
        val currentSelection = selezioni[posizioneItem]

        // Costruiamo la lista dei giocatori consentiti:
        // - Includiamo tutti quelli non ancora selezionati
        // - Se il giocatore corrente è già selezionato, lo includiamo comunque
        val giocatoriConsentiti = giocatori.filter {
            (!selezioniAltre.contains(it.id)) || (it.id == currentSelection)
        }.sortedWith(compareByDescending<Giocatore> { it.ruolo == posizioneItem.ruolo }
            .thenBy { it.nome })

        // Creiamo la lista di stringhe per lo Spinner (es. "Nome Cognome")
        val giocatoriString = giocatoriConsentiti.map { "${it.nome} ${it.cognome}" }

        // Creiamo un ArrayAdapter per lo Spinner
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, giocatoriString)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.giocatoreSpinner.adapter = spinnerAdapter

        val index = giocatoriConsentiti.indexOfFirst { it.id == currentSelection }
        if (index >= 0) {
            holder.giocatoreSpinner.setSelection(index)
        } else {
            // Se non è stata effettuata una selezione, impostiamo il primo elemento di default
            if (giocatoriConsentiti.isNotEmpty()) {
                holder.giocatoreSpinner.setSelection(0)
                // Salviamo la selezione di default
                selezioni[posizioneItem] = giocatoriConsentiti[0].id
            }
        }

        // Gestiamo la selezione dello Spinner
        holder.giocatoreSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, spinnerPosition: Int, id: Long
            ) {
                val nuovoId = giocatoriConsentiti[spinnerPosition].id
                if (selezioni[posizioneItem] != nuovoId) {
                    selezioni[posizioneItem] = nuovoId
                    notifyDataSetChanged()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nessuna azione necessaria
            }
        }
    }
}
