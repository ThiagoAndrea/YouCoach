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
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class PanchinaAdapter(
    // La lista degli ID dei giocatori in panchina
    private val panchinari: List<String>
) : RecyclerView.Adapter<PanchinaAdapter.RigaViewHolder>() {

    // Raggruppa i giocatori in righe da 5 elementi
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

                // Recupera il cognome in modo asincrono
                recuperaCognomeGiocatore(idGiocatore) { cognome ->
                    if (cognome != null) {
                        cognomeGiocatore.text = cognome
                    } else {
                        Log.e("PanchinaAdapter", "Cognome non trovato per il giocatore con id: $idGiocatore")
                    }
                }


                recuperaNomeCognomeGiocatore(idGiocatore) { nome, cognome ->
                    if (nome != null && cognome != null) {
                        val iniziali = "${nome.firstOrNull() ?: ""}${cognome.firstOrNull() ?: ""}".uppercase()
                        cerchioGiocatore.text = iniziali
                    } else {
                        cerchioGiocatore.text = "?"
                    }
                }

                // Imposta i LayoutParams per distribuire in 5 colonne in maniera equa
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

        // Funzione per recuperare il cognome dal database Firebase
        private fun recuperaCognomeGiocatore(idGiocatore: String, callback: (String?) -> Unit) {
            val database = Firebase.database.reference
            val giocatoriRef = database.child("Giocatori")

            Log.d("PanchinaAdapter", "Recupero cognome per il giocatore con ID: $idGiocatore")

            giocatoriRef.child(idGiocatore).get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val cognome = dataSnapshot.child("cognome").getValue(String::class.java)
                    Log.d("PanchinaAdapter", "Cognome trovato: $cognome")
                    callback(cognome)
                } else {
                    Log.e("PanchinaAdapter", "Giocatore con ID: $idGiocatore non trovato nel database")
                    callback(null)
                }
            }.addOnFailureListener {
                Log.e("PanchinaAdapter", "Errore nel recupero del cognome: ${it.message}")
                callback(null)
            }
        }

        private fun recuperaNomeCognomeGiocatore(idGiocatore: String, callback: (String?, String?) -> Unit) {
            val database = Firebase.database.reference.child("Giocatori").child(idGiocatore)

            database.get().addOnSuccessListener { snapshot ->
                val nome = snapshot.child("nome").getValue(String::class.java)
                val cognome = snapshot.child("cognome").getValue(String::class.java)
                callback(nome, cognome)
            }.addOnFailureListener {
                Log.e("Adapter", "Errore nel recupero dati per $idGiocatore")
                callback(null, null)
            }
        }
    }
}
