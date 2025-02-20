package com.example.youcoach

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GiocatoreAdapter(private val giocatoreList: List<Giocatore>): RecyclerView.Adapter<GiocatoreAdapter.GiocatoreViewHolder>(){

    class GiocatoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nomeTextView: TextView = view.findViewById(R.id.nome_giocatore)
        val cognomeTextView: TextView = view.findViewById(R.id.cognome_giocatore)
        val etaTextView: TextView = view.findViewById(R.id.eta_giocatore)
        val ruoloTextView: TextView = view.findViewById(R.id.ruolo_giocatore)
        val detailsButton: ImageButton = view.findViewById(R.id.details_button)
        val sfondoGiocatore: ImageView = view.findViewById(R.id.sfondo_giocatore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiocatoreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_giocatore, parent, false)
        return GiocatoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GiocatoreViewHolder, position: Int) {
        val giocatore = giocatoreList[position]

        // Imposta i dati del giocatore nei TextView
        holder.nomeTextView.text = giocatore.nome
        holder.cognomeTextView.text = giocatore.cognome
        holder.etaTextView.text = "${giocatore.eta}"
        holder.ruoloTextView.text = giocatore.ruolo

        val backgroundColor = when (giocatore.ruolo.lowercase()) {
            "portiere" -> R.drawable.circle_background_portiere
            "difensore" -> R.drawable.circle_background_difensore
            "centrocampista" -> R.drawable.circle_backkground_centrocampista
            "attaccante" -> R.drawable.circle_background_attaccante
            else -> R.drawable.circle_background_green// Colore di default
        }

        holder.sfondoGiocatore.setBackgroundResource(backgroundColor)

        // Gestisci il click sul pulsante "Dettagli"
        holder.detailsButton.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DettaglioGiocatoreActivity::class.java).apply {
                putExtra("GIOCATORE_ID", giocatore.id) // Passa l'ID del giocatore
                putExtra("GIOCATORE", giocatore) // Passa l'oggetto Giocatore
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return giocatoreList.size
    }
}