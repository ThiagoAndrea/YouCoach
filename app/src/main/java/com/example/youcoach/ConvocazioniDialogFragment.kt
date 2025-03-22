package com.example.youcoach

import ConvocazioniAdapter
import PresenzeAdapter
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Finestra modale per la gestione delle presenze dei giocatori.
 * @param giocatori La lista dei giocatori disponibili.
 * @param onConfermaClick Callback da eseguire quando l'utente conferma le presenze.
 */

class ConvocazioniDialogFragment(
    private val giocatori: List<Giocatore>,
    private val convocazioniIniziali: Map<String, Boolean>,
    private val onConfermaClick: (Map<String, Boolean>) -> Unit
) : DialogFragment() {

    private val convocazioniMap: MutableMap<String, Boolean> by lazy {
        if (convocazioniIniziali.isEmpty()) {
            giocatori.associate { it.id to true }.toMutableMap()
        } else {
            giocatori.associate { it.id to (convocazioniIniziali[it.id] ?: true) }.toMutableMap()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.finestra_presenze, null)

        val recyclerConvocazioni = dialogView.findViewById<RecyclerView>(R.id.recycler_presenze)
        recyclerConvocazioni.layoutManager = LinearLayoutManager(requireContext())
        recyclerConvocazioni.adapter = ConvocazioniAdapter(giocatori, convocazioniMap) { idGiocatore, stato ->
            convocazioniMap[idGiocatore] = stato
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Gestisci Convocazioni")
            .setView(dialogView)
            .setPositiveButton("Conferma") { _, _ -> onConfermaClick(convocazioniMap) }
            .setNegativeButton("Annulla", null)
            .create()
    }
}


