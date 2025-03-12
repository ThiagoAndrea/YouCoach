package com.example.youcoach

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.w3c.dom.Text

class EventoAdapter(private var eventi: MutableList<Evento>) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    inner class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val minutaggioTextView: TextView = itemView.findViewById(R.id.minutaggio)
        val nomeGiocatoreTextView: TextView = itemView.findViewById(R.id.nome_giocatore)
        val modificaButton: ImageButton = itemView.findViewById(R.id.modificaEvento_button)
        val eliminaButton: ImageButton = itemView.findViewById(R.id.eliminaEvento_button)
        val immagineDettaglio: ImageView = itemView.findViewById(R.id.immagine_dettaglio)
        val immagineEvento: ImageView = itemView.findViewById(R.id.immagine_evento)
        val dettagliEvento: TextView = itemView.findViewById(R.id.dettaglio_evento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = eventi[position]
        val minutoString = evento.minutaggio?.split(":")?.get(0)?.toIntOrNull() ?: 0
        val minuto = minutoString + 1
        holder.minutaggioTextView.text = "${minuto}'"

        getGiocatoreNome(evento.nomeGiocatore) { nomeGiocatore ->
            holder.nomeGiocatoreTextView.text = nomeGiocatore
        }

        setEventoIcona(evento.nomeEvento, holder.immagineEvento)

        if (evento.dettagli.isEmpty()) {
            holder.dettagliEvento.text = ""
            holder.dettagliEvento.visibility = View.GONE
        } else {
            formatDettagli(evento.dettagli) { dettagliFormattati ->
                holder.dettagliEvento.text = dettagliFormattati
                holder.dettagliEvento.visibility = View.VISIBLE
            }
        }


        holder.modificaButton.setOnClickListener {
            // Logica per modificare l'evento
        }

        holder.eliminaButton.setOnClickListener {
            // Logica per eliminare l'evento
        }
    }

    override fun getItemCount(): Int = eventi.size

    fun updateEventi(newEventi: List<Evento>) {
        eventi.clear()
        eventi.addAll(newEventi)
        notifyDataSetChanged()
    }


    private fun getGiocatoreNome(idGiocatore: String, callback: (String) -> Unit) {
        val databaseReference =
            FirebaseDatabase.getInstance().getReference("Giocatori").child(idGiocatore)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nome = snapshot.child("cognome").getValue(String::class.java) ?: "Sconosciuto"
                callback(nome)
            }

            override fun onCancelled(error: DatabaseError) {
                callback("Errore")
            }
        })
    }

    private fun setEventoIcona(nomeEvento: String, eventoIcona: ImageView) {
        when (nomeEvento) {
            "Tiro" -> eventoIcona.setImageResource(R.drawable.tiro)
            "Cartellino Giallo" -> eventoIcona.setImageResource(R.drawable.yellow_card)
            "Cartellino Rosso" -> eventoIcona.setImageResource(R.drawable.red_card)
            "Gol" -> eventoIcona.setImageResource(R.drawable.gol)
            "Parata" -> eventoIcona.setImageResource(R.drawable.parata)
            "Fuorigioco" -> eventoIcona.setImageResource(R.drawable.fuorigioco)
            "Cambio" -> eventoIcona.setImageResource(R.drawable.round_arrows)
            "Infortunio" -> eventoIcona.setImageResource(R.drawable.infortunio_live)
            "Fallo" -> eventoIcona.setImageResource(R.drawable.fallo)
            else -> eventoIcona.setImageResource(R.drawable.assist)
        }
    }

    private fun formatDettagli(dettagli: Map<String, Any?>, callback: (String) -> Unit) {
        val result = StringBuilder()
        dettagli.entries.forEach { (key, value) ->
            if (key == "idAssist" && value is String) {
                getGiocatoreNome(value) { cognome ->
                    result.append("Ass: $cognome\n")
                    callback(result.toString())
                }
            } else {
                result.append("${value ?: "N/A"}\n")
            }
        }
    }


    private fun formatKey(key: String): String {
        return key.replace(Regex("([a-z])([A-Z])"), "$1 $2") // Aggiunge spazio tra parole
            .replace("_", " ") // Sostituisce underscore con spazio
            .capitalize() // Prima lettera maiuscola
    }
}
