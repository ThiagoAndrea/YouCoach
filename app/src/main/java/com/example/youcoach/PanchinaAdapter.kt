package com.example.youcoach

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class PanchinaAdapter(
    // La lista degli ID dei giocatori in panchina
    private val panchinari: List<String>
) : RecyclerView.Adapter<PanchinaAdapter.RigaViewHolder>() {

    private val db = DatabaseManager()
    private val panchinaRaggruppata: List<List<String>> = panchinari.chunked(5)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RigaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_riga, parent, false)
        Log.d("PanchinaAdapter", "Creata nuova riga per i panchinari")
        return RigaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RigaViewHolder, position: Int) {
        val giocatoriRiga = panchinaRaggruppata[position]
        holder.bind(giocatoriRiga)
    }

    override fun getItemCount(): Int = panchinaRaggruppata.size

    inner class RigaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gridGiocatori: GridLayout = itemView.findViewById(R.id.gridGiocatori)

        fun bind(giocatori: List<String>) {
            Log.d("PanchinaAdapter", "Numero di giocatori in questa riga: ${giocatori.size}")

            gridGiocatori.removeAllViews()
            gridGiocatori.columnCount = 5

            giocatori.forEach { idGiocatore ->
                val giocatoreView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_giocatore_live, gridGiocatori, false) as ConstraintLayout

                val cognomeGiocatore: TextView = giocatoreView.findViewById(R.id.cognome_giocatore)
                val cerchioGiocatore: TextView = giocatoreView.findViewById(R.id.cerchio_giocatore)

                db.getNomeCognomeGiocatore(idGiocatore) { nome, cognome ->
                    if (nome != null && cognome != null) {
                        cognomeGiocatore.text = cognome
                        val iniziali =
                            "${nome.firstOrNull() ?: ""}${cognome.firstOrNull() ?: ""}".uppercase()
                        cerchioGiocatore.text = iniziali
                    } else {
                        cerchioGiocatore.text = "?"
                    }
                }


                val params = GridLayout.LayoutParams().apply {
                    width = 0 // Puoi usare MATCH_PARENT se preferisci
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }

                giocatoreView.layoutParams = params
                gridGiocatori.addView(giocatoreView)
                cerchioGiocatore.setOnClickListener {

                    Log.d("Adapter", "Cliccato giocatore con ID: $idGiocatore")
                    // Qui puoi aprire un dialog, mostrare dettagli, ecc.
                }


            }
        }
    }
}
