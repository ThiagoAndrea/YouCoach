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
        // L'icona dei dettagli è definita nel layout con id "immagine_dettaglio"
        val immagineDettaglio: ImageView = itemView.findViewById(R.id.immagine_dettaglio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = eventi[position]

        // Imposta i campi principali
        holder.minutaggioTextView.text = evento.minutaggio
        holder.nomeEventoTextView.text = evento.nomeEvento
        holder.nomeGiocatoreTextView.text = evento.nomeGiocatore

        // Imposta il listener per l'icona dei dettagli per mostrare un dialogo
        holder.immagineDettaglio.setOnClickListener {
            val details = buildEventDetails(evento)
            AlertDialog.Builder(it.context)
                .setTitle("Dettagli evento")
                .setMessage(details)
                .setPositiveButton("Ok", null)
                .show()
        }

        // Puoi aggiungere listener per i pulsanti di modifica ed eliminazione se necessario
        holder.modificaButton.setOnClickListener {
            // Logica per modificare l'evento
        }
        holder.eliminaButton.setOnClickListener {
            // Logica per eliminare l'evento
        }
    }

    override fun getItemCount(): Int = eventi.size

    // Funzione per aggiornare la lista degli eventi
    fun updateEventi(newEventi: List<Evento>) {
        eventi.clear()
        eventi.addAll(newEventi)
        notifyDataSetChanged()
    }

    // Costruisce una stringa di dettagli basandosi sul tipo di evento e sui campi non null
    private fun buildEventDetails(evento: Evento): String {
        val sb = StringBuilder()
        sb.append("Minutaggio: ${evento.minutaggio}\n")
        sb.append("Evento: ${evento.nomeEvento}\n")
        sb.append("Giocatore: ${evento.nomeGiocatore}\n")

        when (evento.nomeEvento.lowercase()) {
            "gol" -> {
                evento.tipoGol?.let { sb.append("Tipo Gol: $it\n") }
                evento.corpoGol?.let { sb.append("Corpo Gol: $it\n") }
            }
            "tiro" -> {
                evento.tipoTiro?.let { sb.append("Tipo Tiro: $it\n") }
            }
            "cambio" -> {
                evento.giocatoreCambio?.let { sb.append("Giocatore Cambio: $it\n") }
            }
            "fallo" -> {
                evento.falloFatto?.let { sb.append("Fallo: ${if (it) "Fatto" else "Subito"}\n") }
            }
            "giallo" -> {
                evento.tipoGiallo?.let { sb.append("Tipo Giallo: $it\n") }
            }
            "rosso" -> {
                evento.tipoRosso?.let { sb.append("Tipo Rosso: $it\n") }
            }

        }

        return sb.toString()
    }
}

