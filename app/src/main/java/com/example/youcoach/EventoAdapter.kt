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
    private val context: Context,
    private val onFormazioneAggiornata: () -> Unit
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

        holder.nomeGiocatoreTextView.text = evento.nomeCompletoGiocatore
        holder.nomeGiocatoreTextView.visibility =
            if (evento.nomeCompletoGiocatore == "N/A") View.GONE else View.VISIBLE


        holder.modificaButton.setOnClickListener {

            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.finestra_aggiungi_evento, null)
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
                    val giocatoreAdapter = ArrayAdapter(
                        context,
                        android.R.layout.simple_spinner_item,
                        emptyList<String>()
                    )
                    giocatoreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerGiocatore.adapter = giocatoreAdapter
                    return@getTitolari
                }

                var counter = 0
                idGiocatori.forEach { idGiocatore ->
                    db.getNomeCognomeGiocatore(idGiocatore) { _, cognome ->
                        listaGiocatori.add((idGiocatore to cognome) as Pair<String, String>)
                        counter++

                        if (counter == idGiocatori.size) {
                            val listaOrdinata = listaGiocatori.sortedBy { it.second }

                            val cognomi = listaOrdinata.map { it.second }

                            val giocatoreAdapter = ArrayAdapter(
                                context,
                                android.R.layout.simple_spinner_item, cognomi
                            )
                            giocatoreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerGiocatore.adapter = giocatoreAdapter
                            val posizione =
                                listaOrdinata.indexOfFirst { it.first == evento.nomeGiocatore }
                            if (posizione != -1) {
                                spinnerGiocatore.setSelection(posizione)
                            }
                        }
                    }
                }
            }


            val tipiEvento = listOf(
                "Gol",
                "Parata",
                "Tiro",
                "Fuorigioco",
                "Cambio",
                "Infortunio",
                "Fallo",
                "Giallo",
                "Rosso",
                "Angolo"
            )
            val tipoEventoAdapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, tipiEvento)
            tipoEventoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipoEvento.adapter = tipoEventoAdapter

            val posizioneEvento = tipiEvento.indexOf(evento.nomeEvento)
            if (posizioneEvento != -1) {
                spinnerTipoEvento.setSelection(posizioneEvento)
            }

            editTextMinutaggio.setText(evento.minutaggio)

            spinnerTipoEvento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val tipoEventoSelezionato = tipiEvento[position]

                    fun setupSpinner(options: List<String>, selectedValue: String?, label: String) {
                        val adapter =
                            ArrayAdapter(context, android.R.layout.simple_spinner_item, options)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerDettagli.adapter = adapter
                        textDettagli.text = label
                        spinnerDettagli.visibility = View.VISIBLE
                        textDettagli.visibility = View.VISIBLE
                        selectedValue?.let {
                            spinnerDettagli.setSelection(
                                options.indexOf(it).takeIf { it != -1 } ?: 0)
                        }
                    }

                    when (tipoEventoSelezionato) {
                        "Gol" -> {
                            popolaSpinnerGiocatori(
                                spinnerDettagli,
                                evento.nomeGiocatore,
                                idPartita,
                                dataPartita
                            )
                            textDettagli.text = "Assist:"
                            spinnerDettagli.visibility = View.VISIBLE
                        }

                        "Tiro" -> setupSpinner(
                            listOf("In porta", "Fuori porta"),
                            evento.dettagli["motivo"].toString(), "Dettagli:"
                        )

                        "Infortunio" -> setupSpinner(
                            listOf("Muscolare", "Traumatico"),
                            evento.dettagli["tipoInfortunio"].toString(), "Dettagli:"
                        )

                        "Fallo" -> setupSpinner(
                            listOf("Fatto", "Subito"),
                            evento.dettagli["tipoFallo"].toString(), "Dettagli:"
                        )

                        "Giallo" -> setupSpinner(
                            listOf("Proteste", "Fallo"),
                            evento.dettagli["motivo"].toString(), "Dettagli:"
                        )

                        "Rosso" -> setupSpinner(
                            listOf("Doppia Ammonizione", "Rosso Diretto"),
                            evento.dettagli["motivo"].toString(), "Dettagli:"
                        )

                        "Parata", "Fuorigioco" -> {
                            spinnerDettagli.visibility = View.GONE
                            textDettagli.visibility = View.GONE
                        }

                        "Cambio" -> {
                            popolaSpinnerPanchinari(spinnerDettagli, idPartita, dataPartita)
                            textDettagli.text = "Entra:"
                            spinnerDettagli.visibility = View.VISIBLE
                        }
                    }

                    val showGiocatore = tipoEventoSelezionato !in listOf("Angolo")
                    spinnerGiocatore.visibility = if (showGiocatore) View.VISIBLE else View.GONE
                    textGiocatore.visibility = if (showGiocatore) View.VISIBLE else View.GONE
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            buttonConferma.setOnClickListener {
                val nuovoGiocatoreCognome = spinnerGiocatore.selectedItem as String
                val nuovoTipoEvento = spinnerTipoEvento.selectedItem as String
                val nuovoMinutaggio = editTextMinutaggio.text.toString()

                fun modificaConDettagli(
                    dettagli: Map<String, String>,
                    onComplete: () -> Unit = {}
                ) {
                    db.getGiocatoreIdPerCognome(nuovoGiocatoreCognome) { idGiocatore ->
                        if (idGiocatore != null) {
                            db.modificaEvento(
                                evento.idEvento,
                                idPartita,
                                dataPartita,
                                nuovoMinutaggio,
                                idGiocatore,
                                nuovoTipoEvento,
                                dettagli
                            ) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) onComplete()
                                dialog.dismiss()
                            }
                        } else {
                            Toast.makeText(context, "Giocatore non trovato", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                when (nuovoTipoEvento) {
                    "Gol" -> {
                        val assistCognome = spinnerDettagli.selectedItem as String
                        if (assistCognome != "Giocatore") {
                            db.getGiocatoreIdPerCognome(assistCognome) { idAssist ->
                                if (idAssist != null) {
                                    modificaConDettagli(mapOf("idAssist" to idAssist))
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Assistente non trovato",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            modificaConDettagli(emptyMap())
                        }
                    }

                    "Cambio" -> {
                        val entranteCognome = spinnerDettagli.selectedItem as String
                        db.getGiocatoreIdPerCognome(entranteCognome) { idEntrante ->
                            if (idEntrante != null) {
                                modificaConDettagli(mapOf("Entra: " to idEntrante)) {
                                    db.getGiocatoreIdPerCognome(nuovoGiocatoreCognome) { idUscente ->
                                        if (idUscente != null) {
                                            db.modificaFormazione(
                                                idPartita,
                                                dataPartita,
                                                idUscente,
                                                idEntrante
                                            ) { success, _ ->
                                                if (success) onFormazioneAggiornata()
                                            }
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Giocatore entrante non trovato",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    "Giallo" -> modificaConDettagli(mapOf("motivo" to (spinnerDettagli.selectedItem as String)))
                    "Rosso" -> modificaConDettagli(mapOf("motivo" to (spinnerDettagli.selectedItem as String)))
                    "Infortunio" -> modificaConDettagli(mapOf("tipoInfortunio" to (spinnerDettagli.selectedItem as String)))
                    "Tiro" -> modificaConDettagli(mapOf("motivo" to (spinnerDettagli.selectedItem as String)))
                    "Fallo" -> modificaConDettagli(mapOf("tipoFallo" to (spinnerDettagli.selectedItem as String)))

                    else -> {
                        modificaConDettagli(emptyMap())
                    }
                }
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
                        when (evento.nomeEvento) {
                            "Cambio" -> {
                                val uscente = evento.nomeGiocatore
                                val entrante = evento.dettagli["Entra: "]?.toString() ?: ""
                                if (uscente != null && entrante.isNotEmpty()) {
                                    db.modificaFormazione(idPartita, dataPartita,entrante,uscente) { success, message ->
                                        if (success) {
                                            onFormazioneAggiornata()
                                        }
                                        db.eliminaEvento(
                                            evento.idEvento,
                                            idPartita,
                                            dataPartita
                                        ) { success, message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                        eventi.removeAt(position)
                                        notifyItemRemoved(position)
                                    }
                                  }
                                }
                            else -> {
                                db.eliminaEvento(
                                    evento.idEvento,
                                    idPartita,
                                    dataPartita
                                ) { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                                eventi.removeAt(position)
                                notifyItemRemoved(position)
                            }
                        }

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
            val giocatoriNomi = mutableListOf("Giocatore")
            val giocatoriIdMap = mutableMapOf<String, String>()

            titolari.forEach { (idGiocatore, _) ->
                db.getNomeCognomeGiocatore(idGiocatore) { _, nomeGiocatore ->
                    if (idGiocatore != idGiocatoreCorrente) {
                        val nomeCompleto = "$nomeGiocatore"
                        giocatoriNomi.add(nomeCompleto)
                        giocatoriIdMap[nomeCompleto] = idGiocatore
                    }
                }
            }
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, giocatoriNomi)
            spinner.adapter = adapter
        }
    }

    private fun popolaSpinnerPanchinari(spinnerDettagli: Spinner, idPartita: String, dataPartita: String) {
        db.getPanchina(idPartita, dataPartita){ panchinari ->
            val giocatoriNomi = mutableListOf("Giocatore")
            val giocatoriIdMap = mutableMapOf<String, String>()

            panchinari.forEach{ idGiocatore ->
                db.getNomeCognomeGiocatore(idGiocatore) { _, cognomeGiocatore ->
                    if (idGiocatore != cognomeGiocatore) {
                        val cognome = "$cognomeGiocatore"
                        giocatoriNomi.add(cognome)
                        giocatoriIdMap[cognome] = idGiocatore
                    }
                }
            }
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, giocatoriNomi)
            spinnerDettagli.adapter = adapter

        }
    }


}
