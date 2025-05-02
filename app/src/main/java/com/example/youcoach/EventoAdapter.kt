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
import android.widget.Switch
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
    private val onFormazioneAggiornata: () -> Unit,
    private val dettagliMode: Boolean = false,

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val db = DatabaseManager()
    private var casa: Boolean = true
    private var inModificaMode = false
    private var inEliminazioneMode = false


    init {
        db.getCasa(idPartita, dataPartita) { risultatoCasa ->
            casa = risultatoCasa == true
            notifyDataSetChanged()
        }
    }

    inner class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val minutaggioTextView: TextView = itemView.findViewById(R.id.minutaggio)
        val nomeGiocatoreTextView: TextView = itemView.findViewById(R.id.nome_giocatore)
        val modificaButton: ImageButton = itemView.findViewById(R.id.modificaEvento_button)
        val eliminaButton: ImageButton = itemView.findViewById(R.id.eliminaEvento_button)
        val immagineEvento: ImageView = itemView.findViewById(R.id.immagine_evento)
        val dettagliEvento: TextView = itemView.findViewById(R.id.dettaglio_evento)
        val cardEvento: MaterialCardView = itemView.findViewById(R.id.card_evento)
    }

    inner class DettaglioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int = if (dettagliMode) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == 1) R.layout.item_evento_fine_partita else R.layout.item_evento
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return if (viewType == 1) DettaglioViewHolder(view) else EventoViewHolder(view)
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val evento = eventi[position]

        if (holder is EventoViewHolder) {
            val minuto = evento.minutaggio.split(":").getOrNull(0)?.toIntOrNull()?.plus(1) ?: 0
            holder.minutaggioTextView.text = "$minuto'"

            setEventoIcona(evento.nomeEvento, holder.immagineEvento)
            setBackground(evento.squadra, holder.cardEvento)
            holder.nomeGiocatoreTextView.text = evento.nomeCompletoGiocatore
            holder.nomeGiocatoreTextView.visibility =
                if (evento.nomeCompletoGiocatore == "N/A") View.GONE else View.VISIBLE

            if (evento.dettagli.isEmpty()) {
                holder.dettagliEvento.text = ""
                holder.dettagliEvento.visibility = View.GONE
            } else {
                formatDettagli(evento.dettagli) { dettagliFormattati ->
                    holder.dettagliEvento.text = dettagliFormattati
                    holder.dettagliEvento.visibility = View.VISIBLE
                }
            }

            holder.modificaButton.setOnClickListener {apriDialogModifica(evento, false)}

            holder.eliminaButton.setOnClickListener {confermaEliminazione(evento, position, false)}

    }
        else if (holder is DettaglioViewHolder) {

            val eventoASinistra = evento.squadra == casa
            val boxSx = holder.itemView.findViewById<View>(R.id.box_sinistra)
            val boxDx = holder.itemView.findViewById<View>(R.id.box_destra)
            val cerchio = holder.itemView.findViewById<TextView>(R.id.minutaggio_cerchio)
            val modificaSx = holder.itemView.findViewById<ImageButton>(R.id.modifica_evento_sx)
            val eliminaSx = holder.itemView.findViewById<ImageButton>(R.id.elimina_evento_sx)
            val modificaDx = holder.itemView.findViewById<ImageButton>(R.id.modifica_evento_dx)
            val eliminaDx = holder.itemView.findViewById<ImageButton>(R.id.elimina_evento_dx)

            boxSx.visibility = if (eventoASinistra) View.VISIBLE else View.GONE
            boxDx.visibility = if (!eventoASinistra) View.VISIBLE else View.GONE

            cerchio.text = "${evento.minutaggio.split(":").firstOrNull()?.toIntOrNull()?.plus(1) ?: 0}'"

            val (cardId, nomeId, tipoId, iconId) = if (eventoASinistra)
                arrayOf(R.id.card_evento_sinistra, R.id.nome_giocatore_sx, R.id.dettaglio_evento_sx, R.id.immagine_evento_sx)
            else
                arrayOf(R.id.card_evento_destra, R.id.nome_giocatore_dx, R.id.dettaglio_evento_dx, R.id.immagine_evento_dx)

            val card = holder.itemView.findViewById<MaterialCardView>(cardId)
            val nome = card.findViewById<TextView>(nomeId)
            val tipo = card.findViewById<TextView>(tipoId)
            val icona = card.findViewById<ImageView>(iconId)

            nome.text = evento.nomeCompletoGiocatore
            if(nome.text == "Sconosciuto") nome.text = ""

            if (evento.dettagli.isEmpty()) {
                tipo.text = ""
                tipo.visibility = View.GONE
            } else {
                formatDettagli(evento.dettagli) { dettagliFormattati ->
                    tipo.text = dettagliFormattati
                    tipo.visibility = View.VISIBLE
                }
            }

            icona.setImageResource(getIconaEvento(evento.nomeEvento))

            val btnModifica = holder.itemView.findViewById<ImageButton>(
                if (eventoASinistra) R.id.modifica_evento_sx else R.id.modifica_evento_dx
            )
            val btnElimina = holder.itemView.findViewById<ImageButton>(
                if (eventoASinistra) R.id.elimina_evento_sx else R.id.elimina_evento_dx
            )

            modificaSx.visibility = if (eventoASinistra && inModificaMode) View.VISIBLE else View.GONE
            eliminaSx.visibility = if (eventoASinistra && inEliminazioneMode) View.VISIBLE else View.GONE

            modificaDx.visibility = if (!eventoASinistra && inModificaMode) View.VISIBLE else View.GONE
            eliminaDx.visibility = if (!eventoASinistra && inEliminazioneMode) View.VISIBLE else View.GONE

            btnModifica.setOnClickListener {apriDialogModifica(evento, true)}

            btnElimina.setOnClickListener {confermaEliminazione(evento, position, true)}


        }
    }

    override fun getItemCount(): Int = eventi.size

    private fun getIconaEvento(nomeEvento: String): Int {
        return when (nomeEvento.lowercase()) {
            "tiro" -> R.drawable.tiro
            "giallo" -> R.drawable.yellow_card
            "rosso" -> R.drawable.red_card
            "gol" -> R.drawable.gol
            "parata" -> R.drawable.parata
            "fuorigioco" -> R.drawable.fuorigioco
            "cambio" -> R.drawable.cambio
            "infortunio" -> R.drawable.infortunio_live
            "fallo" -> R.drawable.fallo
            "angolo" -> R.drawable.corner
            else -> R.drawable.assist
        }
    }

    private fun setEventoIcona(nomeEvento: String, eventoIcona: ImageView) {
        eventoIcona.setImageResource(getIconaEvento(nomeEvento))
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
                db.getNomeCognomeGiocatore(value) { _, cognome ->
                    result.append("Ass: $cognome\n")
                    callback(result.toString())
                }
            } else if (key == "Entra: " && value is String) {
                db.getNomeCognomeGiocatore(value) { _, cognome ->
                    result.append("Entra: $cognome\n")
                    callback(result.toString())
                }
            } else {
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

    fun updateEventi(newEventi: List<Evento>) {
        eventi.clear()
        eventi.addAll(newEventi)
        notifyDataSetChanged()
    }

    fun toggleModalita(modifica: Boolean, elimina: Boolean) {
        if ((inModificaMode && modifica) || (inEliminazioneMode && elimina)) {
            inModificaMode = false
            inEliminazioneMode = false
        } else {
            inModificaMode = modifica
            inEliminazioneMode = elimina
        }
        notifyDataSetChanged()
    }

    private fun apriDialogModifica(evento: Evento, salvaStats: Boolean) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.finestra_aggiungi_evento, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        val spinnerGiocatore = dialogView.findViewById<Spinner>(R.id.spinnerGiocatore)
        val spinnerTipoEvento = dialogView.findViewById<Spinner>(R.id.spinnerEvento)
        val textDettagli = dialogView.findViewById<TextView>(R.id.textDettagli)
        val textTitolo = dialogView.findViewById<TextView>(R.id.textAggiungiEvento)
        val textGiocatore = dialogView.findViewById<TextView>(R.id.textGiocatore)
        val spinnerDettagli = dialogView.findViewById<Spinner>(R.id.spinnerDettagli)
        val editTextMinutaggio = dialogView.findViewById<EditText>(R.id.editTextMinutaggio)
        val buttonConferma = dialogView.findViewById<Button>(R.id.buttonConferma)
        val buttonAnnulla = dialogView.findViewById<ImageButton>(R.id.buttonAnnulla)
        val switchAvversario = dialogView.findViewById<Switch>(R.id.switch_squadra)

        switchAvversario.isChecked = !evento.squadra

        if (evento.idEvento == "") textTitolo.text = "Aggiungi evento"
        else textTitolo.text = "Modifica evento"

        editTextMinutaggio.setText(evento.minutaggio)

        fun aggiornaVisibilitaCampiAvversario(attivo: Boolean) {
            val tipiEventoAvversario = listOf("Gol", "Tiro", "Angolo", "Giallo", "Rosso", "Fuorigioco")
            val tipiEventoCompleti = listOf("Gol", "Parata", "Tiro", "Fuorigioco", "Cambio", "Infortunio", "Fallo", "Giallo", "Rosso", "Angolo")
            val eventiDaUsare = if (attivo) tipiEventoAvversario else tipiEventoCompleti

            val tipoAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, eventiDaUsare)
            tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipoEvento.adapter = tipoAdapter

            spinnerGiocatore.visibility = if (attivo) View.GONE else View.VISIBLE
            textGiocatore.visibility = if (attivo) View.GONE else View.VISIBLE
            spinnerDettagli.visibility = View.GONE
            textDettagli.visibility = View.GONE
        }

        aggiornaVisibilitaCampiAvversario(switchAvversario.isChecked)

        switchAvversario.setOnCheckedChangeListener { _, isChecked ->
            aggiornaVisibilitaCampiAvversario(isChecked)
        }

        db.getTitolari(idPartita, dataPartita) { titolari ->
            val listaGiocatori = mutableListOf<Pair<String, String>>()
            val idGiocatori = titolari.keys.toList()

            var counter = 0
            idGiocatori.forEach { idGiocatore ->
                db.getNomeCognomeGiocatore(idGiocatore) { _, cognome ->
                    listaGiocatori.add((idGiocatore to cognome) as Pair<String, String>)
                    counter++

                    if (counter == idGiocatori.size) {
                        val listaOrdinata = listaGiocatori.sortedBy { it.second }
                        val cognomi = listaOrdinata.map { it.second }
                        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, cognomi)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerGiocatore.adapter = adapter
                        val pos = listaOrdinata.indexOfFirst { it.first == evento.nomeGiocatore }
                        if (pos != -1) spinnerGiocatore.setSelection(pos)
                    }
                }
            }
        }

        spinnerTipoEvento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tipoEventoSelezionato = spinnerTipoEvento.selectedItem as String
                val isAvversario = switchAvversario.isChecked

                fun setupSpinner(options: List<String>, selectedValue: String?, label: String) {
                    val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, options)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerDettagli.adapter = adapter
                    textDettagli.text = label
                    spinnerDettagli.visibility = View.VISIBLE
                    textDettagli.visibility = View.VISIBLE
                    selectedValue?.let {
                        spinnerDettagli.setSelection(options.indexOf(it).takeIf { it != -1 } ?: 0)
                    }
                }

                if (isAvversario) {
                    when (tipoEventoSelezionato) {
                        "Tiro" -> setupSpinner(listOf("In porta", "Fuori porta"), evento.dettagli["motivo"]?.toString(), "Dettagli:")
                        else -> {
                            spinnerDettagli.visibility = View.GONE
                            textDettagli.visibility = View.GONE
                        }
                    }
                    return
                }

                when (tipoEventoSelezionato) {
                    "Gol" -> {
                        popolaSpinnerGiocatori(spinnerDettagli, evento.nomeGiocatore, idPartita, dataPartita)
                        textDettagli.text = "Assist:"
                        textDettagli.visibility = View.VISIBLE
                        spinnerDettagli.visibility = View.VISIBLE
                    }
                    "Tiro" -> setupSpinner(listOf("In porta", "Fuori porta"), evento.dettagli["motivo"]?.toString(), "Dettagli:")
                    "Infortunio" -> setupSpinner(listOf("Muscolare", "Traumatico"), evento.dettagli["tipoInfortunio"]?.toString(), "Dettagli:")
                    "Fallo" -> setupSpinner(listOf("Fatto", "Subito"), evento.dettagli["tipoFallo"]?.toString(), "Dettagli:")
                    "Giallo" -> setupSpinner(listOf("Proteste", "Fallo"), evento.dettagli["motivo"]?.toString(), "Dettagli:")
                    "Rosso" -> setupSpinner(listOf("Doppia Ammonizione", "Rosso Diretto"), evento.dettagli["motivo"]?.toString(), "Dettagli:")
                    "Cambio" -> {
                        popolaSpinnerPanchinari(spinnerDettagli, idPartita, dataPartita)
                        textDettagli.text = "Entra:"
                        textDettagli.visibility = View.VISIBLE
                        spinnerDettagli.visibility = View.VISIBLE
                    }
                    "Angolo" -> {
                        spinnerDettagli.visibility = View.GONE
                        textDettagli.visibility = View.GONE
                        spinnerGiocatore.visibility = View.GONE
                        textGiocatore.visibility = View.GONE
                    }
                    else -> {
                        spinnerDettagli.visibility = View.GONE
                        textDettagli.visibility = View.GONE
                        spinnerGiocatore.visibility = View.VISIBLE
                        textGiocatore.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        buttonAnnulla.setOnClickListener { dialog.dismiss() }

        buttonConferma.setOnClickListener {
            val tipoEvento = spinnerTipoEvento.selectedItem as String
            val minuto = editTextMinutaggio.text.toString()
            val isAvversario = switchAvversario.isChecked
            val dettagli = mutableMapOf<String, String>()

            val giocatoreCognome = spinnerGiocatore.selectedItem as? String

            if (!isAvversario && (giocatoreCognome == null || giocatoreCognome == "Giocatore")) {
                Toast.makeText(context, "Seleziona un giocatore", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (tipoEvento == "Tiro" && spinnerDettagli.visibility == View.VISIBLE) {
                val esito = spinnerDettagli.selectedItem as? String
                if (!esito.isNullOrBlank()) {
                    dettagli["motivo"] = esito
                }
            }

            when (tipoEvento) {
                "Tiro" -> {
                    val esito = spinnerDettagli.selectedItem as? String
                    if (!esito.isNullOrBlank()) {
                        dettagli["motivo"] = esito
                    }
                }

                "Fallo" -> {
                    val tipo = spinnerDettagli.selectedItem as? String
                    if (!tipo.isNullOrBlank()) {
                        dettagli["tipoFallo"] = tipo
                    }
                }

                "Giallo", "Rosso" -> {
                    val motivo = spinnerDettagli.selectedItem as? String
                    if (!motivo.isNullOrBlank()) {
                        dettagli["motivo"] = motivo
                    }
                }

                "Infortunio" -> {
                    val tipo = spinnerDettagli.selectedItem as? String
                    if (!tipo.isNullOrBlank()) {
                        dettagli["tipoInfortunio"] = tipo
                    }
                }

                "Gol" -> {
                    val assistCognome = spinnerDettagli.selectedItem as? String
                    if (!assistCognome.isNullOrBlank() && assistCognome != "Giocatore") {
                        db.getGiocatoreIdPerCognome(assistCognome) { idAssist ->
                            if (idAssist != null) {
                                dettagli["idAssist"] = idAssist
                            }
                        }
                    }
                }

                "Cambio" -> {
                    val entranteCognome = spinnerDettagli.selectedItem as? String
                    if (!entranteCognome.isNullOrBlank() && entranteCognome != "Giocatore") {
                        db.getGiocatoreIdPerCognome(entranteCognome) { idEntrante ->
                            if (idEntrante != null) {
                                dettagli["Entra: "] = idEntrante
                                val uscenteCognome = giocatoreCognome ?: return@getGiocatoreIdPerCognome
                                db.getGiocatoreIdPerCognome(uscenteCognome) { idUscente ->
                                    if (idUscente != null) {
                                        db.modificaFormazione(idPartita, dataPartita, idUscente, idEntrante) { success, _ ->
                                            if (success) onFormazioneAggiornata()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


            fun completamento(idGiocatore: String) {
                val squadra = !isAvversario
                if (evento.idEvento.isNotBlank()) {
                    db.modificaEvento(evento.idEvento, idPartita, dataPartita, minuto, idGiocatore, tipoEvento, dettagli) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        if (success && salvaStats) {
                            val statsManager = StatsManager(idPartita, dataPartita)
                            db.getCasa(idPartita, dataPartita) { isCasa ->
                                val eventiLocali = eventi.toList()
                                val golMiaSquadra = eventiLocali.count { it.nomeEvento == "Gol" && it.squadra }
                                val golAvversari = eventiLocali.count { it.nomeEvento == "Gol" && !it.squadra }
                                val golCasa = if (isCasa) golMiaSquadra else golAvversari
                                val golOspite = if (isCasa) golAvversari else golMiaSquadra
                                statsManager.salvaStats(golCasa, golOspite) {}
                            }
                        }
                        dialog.dismiss()
                    }
                } else {
                    db.aggiungiEvento(idPartita, dataPartita, minuto, idGiocatore, tipoEvento, squadra, dettagli)
                    if (salvaStats) {
                        val statsManager = StatsManager(idPartita, dataPartita)
                        db.getCasa(idPartita, dataPartita) { isCasa ->
                            val eventiLocali = eventi.toList()
                            val golMiaSquadra = eventiLocali.count { it.nomeEvento == "Gol" && it.squadra }
                            val golAvversari = eventiLocali.count { it.nomeEvento == "Gol" && !it.squadra }
                            val golCasa = if (isCasa) golMiaSquadra else golAvversari
                            val golOspite = if (isCasa) golAvversari else golMiaSquadra
                            statsManager.salvaStats(golCasa, golOspite) {}
                        }
                    }
                    dialog.dismiss()
                }
            }

            if (isAvversario) {
                db.getGiocatoreIdPerCognome("Sconosciuto") { idFake ->
                    completamento(idFake ?: "")
                }
            } else {
                db.getGiocatoreIdPerCognome(giocatoreCognome ?: "") {
                    if (it != null) completamento(it)
                    else Toast.makeText(context, "Giocatore non trovato", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }



    private fun confermaEliminazione(evento: Evento, position: Int, salvaStats: Boolean) {
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
                                    if (salvaStats) {
                                        db.getCasa(idPartita, dataPartita) { isCasa ->
                                            val statsManager = StatsManager(idPartita, dataPartita)
                                            val eventiLocali = eventi.toList()
                                            val golMiaSquadra = eventiLocali.count { it.nomeEvento == "Gol" && it.squadra }
                                            val golAvversari = eventiLocali.count { it.nomeEvento == "Gol" && !it.squadra }

                                            val golCasa = if (isCasa) golMiaSquadra else golAvversari
                                            val golOspite = if (isCasa) golAvversari else golMiaSquadra

                                            statsManager.salvaStats(golCasa, golOspite) { /* callback, se ti serve */ }
                                        }
                                    }

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
                            if (salvaStats) {
                                db.getCasa(idPartita, dataPartita) { isCasa ->
                                    val statsManager = StatsManager(idPartita, dataPartita)

                                    val eventiLocali = eventi.toList()
                                    val golMiaSquadra = eventiLocali.count { it.nomeEvento == "Gol" && it.squadra }
                                    val golAvversari = eventiLocali.count { it.nomeEvento == "Gol" && !it.squadra }

                                    val golCasa = if (isCasa) golMiaSquadra else golAvversari
                                    val golOspite = if (isCasa) golAvversari else golMiaSquadra

                                    statsManager.salvaStats(golCasa, golOspite) { /* callback, se ti serve */ }
                                }
                            }

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

    fun apriDialogAggiungi() {
        val eventoFinto = Evento(
            idEvento = "",
            nomeGiocatore = "",
            nomeEvento = "",
            squadra = casa,
            minutaggio = "0:00",
            dettagli = emptyMap()
        )
        apriDialogModifica(eventoFinto, true)
    }







}
