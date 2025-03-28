package com.example.youcoach

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class EventoAdapter(
    private var eventi: MutableList<Evento>,
    private val dataPartita: String,
    private val idPartita: String,
    private val context: Context
) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    private val db = DatabaseManager()

    inner class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val minutaggioTextView: TextView = itemView.findViewById(R.id.minutaggio)
        val nomeGiocatoreTextView: TextView = itemView.findViewById(R.id.nome_giocatore)
        val modificaButton: ImageButton = itemView.findViewById(R.id.modificaEvento_button)
        val eliminaButton: ImageButton = itemView.findViewById(R.id.eliminaEvento_button)
        val immagineEvento: ImageView = itemView.findViewById(R.id.immagine_evento)
        val dettagliEvento: TextView = itemView.findViewById(R.id.dettaglio_evento)
        val cardEvento: MaterialCardView = itemView.findViewById(R.id.card_evento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = eventi[position]
        val minutoString = evento.minutaggio.split(":").get(0).toIntOrNull() ?: 0
        val minuto = minutoString + 1
        holder.minutaggioTextView.text = "${minuto}'"

        db.getNomeCognomeGiocatore(evento.nomeGiocatore) { nomeGiocatore, cognomeGiocatore ->
            if (nomeGiocatore != null && cognomeGiocatore != null) {
                val maxLunghezza = 10
                val testoModificato = if (cognomeGiocatore.length > maxLunghezza) {
                    cognomeGiocatore.substring(0, maxLunghezza - 2) + "..."
                } else {
                    cognomeGiocatore
                }
                holder.nomeGiocatoreTextView.text = testoModificato
            }
        }

        setEventoIcona(evento.nomeEvento, holder.immagineEvento)
        setBackground(evento.squadra, holder.cardEvento)
        if (evento.dettagli.isEmpty()) {
            holder.dettagliEvento.text = ""
            holder.dettagliEvento.visibility = View.GONE
        } else {
            formatDettagli(evento.dettagli) { dettagliFormattati ->
                holder.dettagliEvento.text = dettagliFormattati
                holder.dettagliEvento.visibility = View.VISIBLE
            }
        }
        if(evento.nomeGiocatore == "N/A")
            holder.nomeGiocatoreTextView.visibility = View.GONE

        holder.modificaButton.setOnClickListener {

            val dialogView = LayoutInflater.from(context).inflate(R.layout.finestra_aggiungi_evento, null)
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            val spinnerGiocatore = dialogView.findViewById<Spinner>(R.id.spinnerGiocatore)
            val spinnerTipoEvento = dialogView.findViewById<Spinner>(R.id.spinnerEvento)
            val textDettagli = dialogView.findViewById<TextView>(R.id.textDettagli)
            val textTitolo = dialogView.findViewById<TextView>(R.id.textAggiungiEvento)
            val textGiocatore = dialogView.findViewById<TextView>(R.id.textGiocatore)
            val spinnerDettagli = dialogView.findViewById<Spinner>(R.id.spinnerDettagli)
            val editTextMinutaggio = dialogView.findViewById<EditText>(R.id.editTextMinutaggio)
            val buttonConferma = dialogView.findViewById<Button>(R.id.buttonConferma)
            val buttonAnnulla = dialogView.findViewById<ImageButton>(R.id.buttonAnnulla)

            textTitolo.text = "Modifica evento"

            db.getTitolari(idPartita, dataPartita) { titolari ->
                val listaGiocatori = mutableListOf<Pair<String, String>>()
                val idGiocatori = titolari.keys.toList()

                if (idGiocatori.isEmpty()) {
                    val giocatoreAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, emptyList<String>())
                    giocatoreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerGiocatore.adapter = giocatoreAdapter
                    return@getTitolari
                }

                var counter = 0
                idGiocatori.forEach { idGiocatore ->
                    val ruolo = titolari[idGiocatore] ?: ""

                    db.getNomeCognomeGiocatore(idGiocatore) { _, cognome ->
                        listaGiocatori.add((ruolo to cognome) as Pair<String, String>)
                        counter++

                        if (counter == idGiocatori.size) {
                            val listaOrdinata = listaGiocatori
                                .sortedWith(compareBy({ it.first }, { it.second }))
                                .map { it.second }

                            val giocatoreAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, listaOrdinata)
                            giocatoreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerGiocatore.adapter = giocatoreAdapter
                        }
                    }
                }
            }


            val tipiEvento = listOf("Gol", "Parata", "Tiro", "Fuorigioco", "Cambio", "Infortunio", "Fallo", "Giallo", "Rosso", "Angolo")
            val tipoEventoAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, tipiEvento)
            tipoEventoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipoEvento.adapter = tipoEventoAdapter

            val posizioneEvento = tipiEvento.indexOf(evento.nomeEvento)
            if (posizioneEvento != -1) {
                spinnerTipoEvento.setSelection(posizioneEvento)
            }

            editTextMinutaggio.setText(evento.minutaggio)

            spinnerTipoEvento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val tipoEventoSelezionato = tipiEvento[position]
                    when (tipoEventoSelezionato) {
                        "Gol" -> {
                            popolaSpinnerGiocatori(spinnerDettagli, evento.nomeGiocatore, idPartita, dataPartita)
                            textDettagli.text = "Assist:"
                            spinnerGiocatore.visibility = View.VISIBLE
                            textGiocatore.visibility = View.VISIBLE
                            spinnerDettagli.visibility = View.VISIBLE
                        }
                        "Parata" -> {
                            spinnerGiocatore.visibility = View.VISIBLE
                            textGiocatore.visibility = View.VISIBLE
                            spinnerDettagli.visibility = View.GONE
                            textDettagli.visibility = View.GONE
                        }
                        "Tiro" -> {
                            val inPortaTiro = listOf("In porta", "Fuori porta")
                            val motivoAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, inPortaTiro)
                            motivoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerDettagli.adapter = motivoAdapter
                            spinnerGiocatore.visibility = View.VISIBLE
                            textGiocatore.visibility = View.VISIBLE
                            textDettagli.visibility = View.VISIBLE
                            textDettagli.text = "Dettagli:"
                            spinnerDettagli.visibility = View.VISIBLE

                            evento.dettagli["motivo"]?.let {
                                val pos = inPortaTiro.indexOf(it)
                                if (pos != -1) spinnerDettagli.setSelection(pos)
                            }
                        }
                        "Fuorigioco" -> {
                            spinnerGiocatore.visibility = View.VISIBLE
                            textGiocatore.visibility = View.VISIBLE
                            spinnerDettagli.visibility = View.GONE
                            textDettagli.visibility = View.GONE
                        }

                        "Cambio" -> {
                            spinnerDettagli.visibility = View.GONE
                            textDettagli.visibility = View.GONE
                        }

                        "Infortunio" -> {
                            val tipiInfortunio = listOf("Muscolare", "Traumatico")
                            val infortunioAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, tipiInfortunio)
                            infortunioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerDettagli.adapter = infortunioAdapter
                            spinnerGiocatore.visibility = View.VISIBLE
                            textGiocatore.visibility = View.VISIBLE
                            textDettagli.visibility = View.VISIBLE
                            spinnerDettagli.visibility = View.VISIBLE
                            textDettagli.text = "Dettagli:"

                            evento.dettagli["tipoInfortunio"]?.let {
                                val pos = tipiInfortunio.indexOf(it)
                                if (pos != -1) spinnerDettagli.setSelection(pos)
                            }
                        }

                        "Fallo" -> {
                            val tipoFallo = listOf("Fatto", "Subito")
                            val infortunioAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, tipoFallo)
                            infortunioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerDettagli.adapter = infortunioAdapter
                            spinnerGiocatore.visibility = View.VISIBLE
                            textGiocatore.visibility = View.VISIBLE
                            textDettagli.visibility = View.VISIBLE
                            spinnerDettagli.visibility = View.VISIBLE
                            textDettagli.text = "Dettagli:"
                            evento.dettagli["tipoInfortunio"]?.let {
                                val pos = tipoFallo.indexOf(it)
                                if (pos != -1) spinnerDettagli.setSelection(pos)
                            }
                        }

                        "Giallo" -> {
                            val motiviGiallo = listOf("Proteste", "Fallo")
                            val motivoAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, motiviGiallo)
                            motivoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerDettagli.adapter = motivoAdapter
                            spinnerGiocatore.visibility = View.VISIBLE
                            textGiocatore.visibility = View.VISIBLE
                            textDettagli.visibility = View.VISIBLE
                            spinnerDettagli.visibility = View.VISIBLE
                            textDettagli.text = "Dettagli:"

                            evento.dettagli["motivo"]?.let {
                                val pos = motiviGiallo.indexOf(it)
                                if (pos != -1) spinnerDettagli.setSelection(pos)
                            }
                        }
                        "Rosso" -> {
                            val motiviRosso = listOf("Doppia Ammonizione", "Rosso Diretto")
                            val motivoAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, motiviRosso)
                            motivoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerDettagli.adapter = motivoAdapter
                            spinnerGiocatore.visibility = View.VISIBLE
                            textGiocatore.visibility = View.VISIBLE
                            textDettagli.visibility = View.VISIBLE
                            spinnerDettagli.visibility = View.VISIBLE
                            textDettagli.text = "Dettagli:"

                            evento.dettagli["motivo"]?.let {
                                val pos = motiviRosso.indexOf(it)
                                if (pos != -1) spinnerDettagli.setSelection(pos)
                            }
                        }

                        else -> {
                            spinnerDettagli.visibility = View.GONE
                            spinnerGiocatore.visibility = View.GONE
                            textGiocatore.visibility = View.GONE
                            textDettagli.visibility = View.GONE
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            buttonConferma.setOnClickListener {

                val nuovoGiocatoreCognome = spinnerGiocatore.selectedItem as String
                db.getGiocatoreIdPerCognome(nuovoGiocatoreCognome) { idGiocatore ->
                    if (idGiocatore != null) {

                        val nuovoTipoEvento = spinnerTipoEvento.selectedItem as String
                        val nuovoMinutaggio = editTextMinutaggio.text.toString()

                        val dettagliSpecifici = when (nuovoTipoEvento) {
                            "Gol" -> mapOf("idAssist" to (spinnerDettagli.selectedItem as String))
                            "Giallo" -> mapOf("motivo" to (spinnerDettagli.selectedItem as String))
                            "Rosso" -> mapOf("motivo" to (spinnerDettagli.selectedItem as String))
                            "Infortunio" -> mapOf("tipoInfortunio" to (spinnerDettagli.selectedItem as String))
                            "Tiro" -> mapOf("motivo" to (spinnerDettagli.selectedItem as String))
                            "Fallo" -> mapOf("tipoInfortunio" to (spinnerDettagli.selectedItem as String))
                            else -> emptyMap()
                        }

                        if (evento.idEvento != null) {
                            db.modificaEvento(
                                evento.idEvento,
                                idPartita,
                                dataPartita,
                                nuovoMinutaggio,
                                idGiocatore,
                                nuovoTipoEvento,
                                dettagliSpecifici
                            ) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Giocatore non trovato", Toast.LENGTH_SHORT).show()
                    }
                }

                dialog.dismiss()
            }

            buttonAnnulla.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
        holder.eliminaButton.setOnClickListener {
            if (evento.idEvento != null) {
                val alertDialog = AlertDialog.Builder(context).apply {
                    setTitle("Conferma eliminazione")
                    setMessage("Sei sicuro di voler eliminare questo evento?")
                    setPositiveButton("Elimina") { dialog, which ->
                        db.eliminaEvento(evento.idEvento, idPartita, dataPartita) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                        eventi.removeAt(position)
                        notifyItemRemoved(position)
                    }
                    setNegativeButton("Annulla") { dialog, which ->
                        dialog.dismiss()
                    }
                }.create()

                alertDialog.show()
            }
        }
    }

    override fun getItemCount(): Int = eventi.size

    fun updateEventi(newEventi: List<Evento>) {
        eventi.clear()
        eventi.addAll(newEventi)
        notifyDataSetChanged()
    }

    private fun setEventoIcona(nomeEvento: String, eventoIcona: ImageView) {
        when (nomeEvento) {
            "Tiro" -> eventoIcona.setImageResource(R.drawable.tiro)
            "Giallo" -> eventoIcona.setImageResource(R.drawable.yellow_card)
            "Rosso" -> eventoIcona.setImageResource(R.drawable.red_card)
            "Gol" -> eventoIcona.setImageResource(R.drawable.gol)
            "Parata" -> eventoIcona.setImageResource(R.drawable.parata)
            "Fuorigioco" -> eventoIcona.setImageResource(R.drawable.fuorigioco)
            "Cambio" -> eventoIcona.setImageResource(R.drawable.round_arrows)
            "Infortunio" -> eventoIcona.setImageResource(R.drawable.infortunio_live)
            "Fallo" -> eventoIcona.setImageResource(R.drawable.fallo)
            "Angolo" -> eventoIcona.setImageResource(R.drawable.corner)
            else -> eventoIcona.setImageResource(R.drawable.assist)
        }
    }

    private fun setBackground(squadra: Boolean, cardView: MaterialCardView) {
        val background = ContextCompat.getColor(
            cardView.context,
            if (squadra) R.color.green_base else R.color.opposite_base
        )
        cardView.setCardBackgroundColor(background)
    }


    private fun formatDettagli(dettagli: Map<String, Any?>, callback: (String) -> Unit) {
        val result = StringBuilder()
        dettagli.entries.forEach { (key, value) ->
            if (key == "idAssist" && value is String) {
                db.getNomeCognomeGiocatore(value) { nome, cognome ->
                    result.append("Ass: $cognome\n")
                    callback(result.toString())
                }
            }
            else if (key == "Entra: " && value is String){
                db.getNomeCognomeGiocatore(value) { nome, cognome ->
                    result.append("Entra: $cognome\n")
                    callback(result.toString())
                }
            }
            else {
                result.append("${value ?: "N/A"}\n")
                callback(result.toString())
            }
        }
    }

    private fun popolaSpinnerGiocatori(spinner: Spinner, idGiocatoreCorrente: String, idPartita: String, dataPartita: String) {
        db.getTitolari(idPartita, dataPartita) { titolari ->
            val giocatoriNomi = mutableListOf("Seleziona giocatore")
            val giocatoriIdMap = mutableMapOf<String, String>()

            titolari.forEach { (idGiocatore, nomeGiocatore) ->
                if (idGiocatore != idGiocatoreCorrente) {
                    val nomeCompleto = "$nomeGiocatore"
                    giocatoriNomi.add(nomeCompleto)
                    giocatoriIdMap[nomeCompleto] = idGiocatore
                }
            }
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, giocatoriNomi)
            spinner.adapter = adapter
        }
    }

}
