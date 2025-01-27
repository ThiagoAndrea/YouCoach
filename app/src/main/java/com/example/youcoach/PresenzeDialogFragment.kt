package com.example.youcoach

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
    private val onConfermaClick: (Map<String, Int>) -> Unit
) : DialogFragment() {

    // Mappa per tracciare le presenze (id del giocatore -> stato presenza)
    private val presenzeMap = mutableMapOf<String, Int>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Infla il layout della finestra modale
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.finestra_presenze, null)

        // Configura la RecyclerView per mostrare i giocatori
        val recyclerGiocatori = dialogView.findViewById<RecyclerView>(R.id.recycler_presenze)
        recyclerGiocatori.layoutManager = LinearLayoutManager(requireContext())

        // Configura l'adapter della RecyclerView
        val adapter = PresenzeAdapter(giocatori) { giocatore, nuovoStato ->
            // Aggiorna lo stato della presenza nella mappa
            presenzeMap[giocatore.id] = nuovoStato
            Log.d("PresenzeDialogFragment", "Giocatore: ${giocatore.nome}, Stato aggiornato: $nuovoStato")
        }
        recyclerGiocatori.adapter = adapter

        // Crea e configura il dialog
        return AlertDialog.Builder(requireContext())
            .setTitle("Gestisci Presenze") // Titolo del dialog
            .setView(dialogView) // Vista personalizzata
            .setPositiveButton("Conferma") { _, _ ->
                // Passa la mappa delle presenze alla funzione di callback
                onConfermaClick(presenzeMap)
            }
            .setNegativeButton("Annulla", null) // Pulsante per chiudere senza salvare
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("PresenzeDialogFragment", "Dialog distrutta e risorse rilasciate")
    }
}
