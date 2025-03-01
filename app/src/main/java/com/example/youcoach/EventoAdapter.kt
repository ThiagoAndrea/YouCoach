package com.example.youcoach

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventoAdapter(private var eventi: MutableList<Evento>) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    inner class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val minutaggioTextView: TextView = itemView.findViewById(R.id.minutaggio)
        val nomeEventoTextView: TextView = itemView.findViewById(R.id.nome_evento)
        val nomeGiocatoreTextView: TextView = itemView.findViewById(R.id.nome_giocatore)
        val modificaButton: ImageButton = itemView.findViewById(R.id.modificaEvento_button)
        val eliminaButton: ImageButton = itemView.findViewById(R.id.eliminaEvento_button)
        val immagineDettaglio: ImageView = itemView.findViewById(R.id.immagine_dettaglio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = eventi[position]

        // Imposta i dati principali dell'evento
        holder.minutaggioTextView.text = evento.minutaggio.toString()
        holder.nomeEventoTextView.text = evento.nomeEvento
        holder.nomeGiocatoreTextView.text = evento.nomeGiocatore

        // Listener per l'icona dei dettagli
        holder.immagineDettaglio.setOnClickListener {
            val details = buildEventDetails(evento)
            AlertDialog.Builder(it.context)
                .setTitle("Dettagli evento")
                .setMessage(details)
                .setPositiveButton("Ok", null)
                .show()
        }

        // Listener per la modifica
        holder.modificaButton.setOnClickListener {
            // Logica per modificare l'evento
        }

        // Listener per l'eliminazione
        holder.eliminaButton.setOnClickListener {
            // Logica per eliminare l'evento
        }
    }

    override fun getItemCount(): Int = eventi.size

    // Aggiorna la lista degli eventi
    fun updateEventi(newEventi: List<Evento>) {
        eventi.clear()
        eventi.addAll(newEventi)
        notifyDataSetChanged()
    }

    // Costruisce i dettagli dinamicamente dalla mappa
    private fun buildEventDetails(evento: Evento): String {
        val sb = StringBuilder()
        sb.append("Minutaggio: ${evento.minutaggio}\n")
        sb.append("Evento: ${evento.nomeEvento}\n")
        sb.append("Giocatore: ${evento.nomeGiocatore}\n")

        // Verifica se ci sono dettagli extra
        evento.dettagli?.forEach { (key, value) ->
            if (value != null) {
                sb.append("${formatKey(key)}: $value\n")
            }
        }

        return sb.toString()
    }

    // Formatta la chiave per renderla più leggibile
    private fun formatKey(key: String): String {
        return key.replace(Regex("([a-z])([A-Z])"), "$1 $2") // Aggiunge spazio tra parole
            .replace("_", " ") // Sostituisce underscore con spazio
            .capitalize() // Prima lettera maiuscola
    }
}
