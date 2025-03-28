package com.example.youcoach

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

class PresenzeDialogFragment(
    private val giocatori: List<Giocatore>,
    private val presenzeIniziali: Map<String, Int>,
    private val onConfermaClick: (Map<String, Int>) -> Unit
) : DialogFragment() {

    private val presenzeMap: MutableMap<String, Int> by lazy {
        if (presenzeIniziali.isEmpty()) {
            giocatori.associate { it.id to 1 }.toMutableMap()
        } else {
            giocatori.associate { it.id to (presenzeIniziali[it.id] ?: 1) }.toMutableMap()
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.finestra_presenze, null)

        val recyclerGiocatori = dialogView.findViewById<RecyclerView>(R.id.recycler_presenze)
        recyclerGiocatori.layoutManager = LinearLayoutManager(requireContext())
        recyclerGiocatori.adapter = PresenzeAdapter(giocatori, presenzeMap) { idGiocatore, stato ->
            presenzeMap[idGiocatore] = stato
        }


        return AlertDialog.Builder(requireContext())
            .setTitle("Gestisci Presenze")
            .setView(dialogView)
            .setPositiveButton("Conferma") { _, _ -> onConfermaClick(presenzeMap) }
            .setNegativeButton("Annulla", null)
            .create()
    }
}
