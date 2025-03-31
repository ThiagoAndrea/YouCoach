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
        var lastSelection: String? = null // Aggiunto per gestire lo stato durante lo scrolling
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
        holder.giocatoreSpinner.onItemSelectedListener = null
        val selezioniAltre = selezioni.filter { it.key != posizioneItem }.values.toSet()
        val currentSelection = selezioni[posizioneItem]

        val giocatoriConsentiti = giocatori.filter {
            (!selezioniAltre.contains(it.id)) || (it.id == currentSelection)
        }.sortedWith(compareByDescending<Giocatore> { it.ruolo == posizioneItem.ruolo }
            .thenBy { it.nome })

        val giocatoriString = giocatoriConsentiti.map { "${it.cognome}" }

        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, giocatoriString)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.giocatoreSpinner.adapter = spinnerAdapter

        val index = giocatoriConsentiti.indexOfFirst { it.id == currentSelection }
        if (index >= 0) {
            holder.giocatoreSpinner.setSelection(index)
            holder.lastSelection = currentSelection
        } else if (giocatoriConsentiti.isNotEmpty()) {
            holder.giocatoreSpinner.setSelection(0)
            selezioni[posizioneItem] = giocatoriConsentiti[0].id
            holder.lastSelection = giocatoriConsentiti[0].id
        }

        // 5. Reimposta il listener con controllo dello stato
        holder.giocatoreSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val nuovoId = giocatoriConsentiti[pos].id
                if (holder.lastSelection != nuovoId) {
                    selezioni[posizioneItem] = nuovoId
                    holder.lastSelection = nuovoId
                    notifyDataSetChanged()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}