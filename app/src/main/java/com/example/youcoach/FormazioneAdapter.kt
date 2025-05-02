package com.example.youcoach

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class FormazioneAdapter(
    private var formazione: Map<String, String>,
    private val data: String,
    private val idPartita: String,
    private val onFormazioneAggiornata: () -> Unit,
    private val ruoliOrdine: List<String> = listOf("Portiere", "Difensore", "Centrocampista", "Trequartista", "Attaccante")

) : RecyclerView.Adapter<FormazioneAdapter.RigaViewHolder>() {

    private var buttonsEnabled: Boolean = false
    private val db = DatabaseManager()

    private fun normalizzaRuolo(ruolo: String): String {
        return ruolo.replace(Regex("\\s\\d+$"), "")
    }

    private fun estraiNumeroRuolo(ruolo: String): Int {
        return Regex("\\d+$").find(ruolo)?.value?.toIntOrNull() ?: Int.MAX_VALUE
    }


    private var formazioneRaggruppata: List<List<String>> = ruoliOrdine.map { ruolo ->
        val giocatoriInRuolo = formazione
            .filter { normalizzaRuolo(it.value) == ruolo }
            .toList()
            .sortedBy { estraiNumeroRuolo(it.second) }
            .map { it.first }

        giocatoriInRuolo
    }.filter { it.isNotEmpty() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RigaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_riga, parent, false)
        Log.d("FormazioneAdapter", "Formazione raggruppata: $formazioneRaggruppata")
        return RigaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RigaViewHolder, position: Int) {
        val giocatori = formazioneRaggruppata[position]
        holder.bind(giocatori)
    }

    override fun getItemCount(): Int = formazioneRaggruppata.size


    inner class RigaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gridGiocatori: GridLayout = itemView.findViewById(R.id.gridGiocatori)

        fun bind(giocatori: List<String>) {
            Log.d("FormazioneAdapter", "Numero di giocatori per questa riga: ${giocatori.size}")

            gridGiocatori.removeAllViews()
            gridGiocatori.columnCount = 5

            giocatori.forEach { idGiocatore ->
                val giocatoreView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_giocatore_live, gridGiocatori, false) as ConstraintLayout

                val cognomeGiocatore: TextView = giocatoreView.findViewById(R.id.cognome_giocatore)
                val cerchioGiocatore: TextView = giocatoreView.findViewById(R.id.cerchio_giocatore)
                val maxLunghezza = 10
                db.getNomeCognomeGiocatore(idGiocatore) { nome, cognome ->
                    if (cognome != null) {
                        val testoModificato = if (cognome.length > maxLunghezza) {
                            cognome.substring(0, maxLunghezza - 1) + "..."
                        } else {
                            cognome
                        }
                        cognomeGiocatore.text = testoModificato
                    } else {
                        Log.e(
                            "FormazioneAdapter",
                            "Cognome non trovato per il giocatore con id: $idGiocatore"
                        )
                    }

                    val iniziali =
                        "${nome?.firstOrNull() ?: ""}${cognome?.firstOrNull() ?: ""}".uppercase()
                    cerchioGiocatore.text = if (iniziali.isNotBlank()) iniziali else "?"
                }


                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )
                }
                giocatoreView.layoutParams = params

                gridGiocatori.addView(giocatoreView)

                cerchioGiocatore.isEnabled = buttonsEnabled
                cerchioGiocatore.alpha = if (buttonsEnabled) 1f else 0.5f

                cerchioGiocatore.setOnClickListener {
                    if (buttonsEnabled) {
                        (itemView.context as? LiveActivity)?.getCurrentMinutaggio()
                            ?.let { minutaggio ->
                                mostraDialogEventoGiocatore(itemView, idGiocatore, minutaggio)
                            }
                    }
                }
            }
        }


        private fun mostraDialogEventoGiocatore(
            view: View,
            idGiocatore: String,
            minutaggio: String
        ) {
            val context = view.context
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.finestra_eventi_giocatore, null)
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()

            val textEvento: TextView = dialogView.findViewById(R.id.textEvento)
            db.getNomeCognomeGiocatore(idGiocatore) { nome, cognome ->
                if (nome != null && cognome != null) {
                    textEvento.text = "$nome $cognome"
                } else {
                    textEvento.text = "Dettaglio giocatore"
                }
            }

            val btnTiro: ImageButton = dialogView.findViewById(R.id.tiro)
            val btnGol: ImageButton = dialogView.findViewById(R.id.gol)
            val btnFuorigioco: ImageButton = dialogView.findViewById(R.id.fuorigioco)
            val btnCambio: ImageButton = dialogView.findViewById(R.id.cambio)
            val btnInfortunio: ImageButton = dialogView.findViewById(R.id.infortunio)
            val btnFallo: ImageButton = dialogView.findViewById(R.id.fallo)
            val btnGiallo: ImageButton = dialogView.findViewById(R.id.giallo)
            val btnRosso: ImageButton = dialogView.findViewById(R.id.rosso)
            val btnParata: ImageButton = dialogView.findViewById(R.id.parata)
            val btnChiudi: Button = dialogView.findViewById(R.id.btn_chiudi)


            btnTiro.setOnClickListener {
                val options = arrayOf("In porta", "Fuori porta")
                val builder = AlertDialog.Builder(view.context)

                builder.setTitle("Seleziona se il tiro è in porta o fuori porta")
                    .setItems(options) { dialog, which ->
                        val tipoTiro = options[which]
                        val dettagliTiro = mapOf("tipoTiro" to tipoTiro)
                        db.aggiungiEvento(
                            idPartita,
                            data,
                            minutaggio,
                            idGiocatore,
                            "Tiro",
                            true,
                            dettagliTiro
                        )
                        avviaAnimazione(view, R.drawable.tiro)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Annulla") { dialog, _ ->
                        dialog.dismiss()
                    }
                builder.create().show()
                dialog.dismiss()
            }

            btnGol.setOnClickListener {
                mostraDialogAssist(view, idGiocatore, minutaggio)
                dialog.dismiss()
            }

            btnFuorigioco.setOnClickListener {
                db.aggiungiEvento(idPartita, data, minutaggio, idGiocatore, "Fuorigioco", true)
                avviaAnimazione(view, R.drawable.fuorigioco)
                dialog.dismiss()
            }

            btnCambio.setOnClickListener {
                mostraDialogSostituzione(view, idGiocatore, minutaggio)
                dialog.dismiss()
            }

            btnInfortunio.setOnClickListener {
                val options = arrayOf("Muscolare", "Traumatico")
                val builder = AlertDialog.Builder(view.context)
                builder.setTitle("Seleziona tipo di infortunio")
                    .setItems(options) { dialog, which ->
                        val tipoInfortunio = options[which]
                        val dettagliInfortunio = mapOf("tipoInfortunio" to tipoInfortunio)
                        db.aggiungiEvento(
                            idPartita,
                            data,
                            minutaggio,
                            idGiocatore,
                            "Infortunio",
                            true,
                            dettagliInfortunio
                        )
                        avviaAnimazione(view, R.drawable.infortunio_live)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Annulla") { dialog, _ ->
                        dialog.dismiss()
                    }
                builder.create().show()
                dialog.dismiss()
            }


            btnFallo.setOnClickListener {
                val options = arrayOf("Subito", "Fatto")
                val builder = AlertDialog.Builder(view.context)

                builder.setTitle("Seleziona tipo di fallo")
                    .setItems(options) { dialog, which ->
                        val tipoFallo = options[which]
                        val dettagliFallo = mapOf("tipoFallo" to tipoFallo)
                        db.aggiungiEvento(
                            idPartita,
                            data,
                            minutaggio,
                            idGiocatore,
                            "Fallo",
                            true,
                            dettagliFallo
                        )
                        avviaAnimazione(view, R.drawable.fallo)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Annulla") { dialog, _ ->
                        dialog.dismiss()
                    }

                builder.create().show()
                dialog.dismiss()
            }


            btnGiallo.setOnClickListener {
                val options = arrayOf("Proteste", "Fallo")
                val builder = AlertDialog.Builder(view.context)
                builder.setTitle("Seleziona il motivo per il cartellino giallo")
                    .setItems(options) { dialog, which ->
                        val motivoCartellinoGiallo = options[which]
                        val dettagliCartellinoGiallo = mapOf("motivo" to motivoCartellinoGiallo)
                        db.aggiungiEvento(
                            idPartita,
                            data,
                            minutaggio,
                            idGiocatore,
                            "Giallo",
                            true,
                            dettagliCartellinoGiallo
                        )
                        avviaAnimazione(view, R.drawable.yellow_card)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Annulla") { dialog, _ ->
                        dialog.dismiss()
                    }
                builder.create().show()
                dialog.dismiss()
            }

            btnRosso.setOnClickListener {
                val options = arrayOf("Doppia Ammonizione", "Rosso Diretto")
                val builder = AlertDialog.Builder(view.context)

                builder.setTitle("Seleziona il motivo per il cartellino rosso")
                    .setItems(options) { dialog, which ->
                        val motivoCartellinoRosso = options[which]
                        val dettagliCartellinoRosso = mapOf("motivo" to motivoCartellinoRosso)
                        db.aggiungiEvento(
                            idPartita,
                            data,
                            minutaggio,
                            idGiocatore,
                            "Rosso",
                            true,
                            dettagliCartellinoRosso
                        )
                        avviaAnimazione(view, R.drawable.red_card)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Annulla") { dialog, _ ->
                        dialog.dismiss()
                    }
                builder.create().show()
                dialog.dismiss()
            }

            btnParata.setOnClickListener {
                db.aggiungiEvento(idPartita, data, minutaggio, idGiocatore, "Parata", true)
                avviaAnimazione(view, R.drawable.parata)
                dialog.dismiss()
            }
            btnChiudi.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun avviaAnimazione(view: View, iconaResId: Int) {
            val giocatoreView = view.findViewById<ConstraintLayout>(R.id.item_giocatore_live)
            val location = IntArray(2)
            giocatoreView.getLocationOnScreen(location)

            val startX = location[0].toFloat() + giocatoreView.width / 2f
            val startY = location[1].toFloat() + giocatoreView.height / 2f
            (view.context as? LiveActivity)?.animaIconaEvento(startX, startY, iconaResId)
        }

        private fun mostraDialogAssist(view: View, idMarcatore: String, minutaggio: String) {
            val context = view.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.finestra_assist, null)
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()

            val spinnerAssist: Spinner = dialogView.findViewById(R.id.spinnerAssist)
            val btnConferma: Button = dialogView.findViewById(R.id.btnConfermaAssist)
            val btnNessunAssist: Button = dialogView.findViewById(R.id.btnNessunAssist)
            val btnChiudi: ImageButton = dialogView.findViewById(R.id.btnChiudiAssist)

            val giocatoriTitolari = formazione.keys.toList()

            val giocatoriNomi = mutableListOf("Seleziona giocatore")
            val giocatoriIdMap = mutableMapOf<String, String>()

            giocatoriTitolari.forEach { idGiocatore ->
                db.getNomeCognomeGiocatore(idGiocatore) { nome, cognome ->
                    if (nome != null && cognome != null) {
                        val nomeCompleto = "$nome $cognome"
                        giocatoriNomi.add(nomeCompleto)
                        giocatoriIdMap[nomeCompleto] = idGiocatore

                        if (giocatoriNomi.size == giocatoriTitolari.size + 1) {
                            val adapter = ArrayAdapter(
                                context,
                                android.R.layout.simple_spinner_dropdown_item,
                                giocatoriNomi
                            )
                            spinnerAssist.adapter = adapter
                        }
                    }
                }
            }

            btnConferma.setOnClickListener {
                val assistSelezionato = spinnerAssist.selectedItem.toString()
                val idAssistMan = giocatoriIdMap[assistSelezionato]
                val dettagliGol = mapOf("idAssist" to idAssistMan)
                db.aggiungiEvento(
                    idPartita,
                    data,
                    minutaggio,
                    idMarcatore,
                    "Gol",
                    true,
                    dettagliGol
                )
                avviaAnimazione(view, R.drawable.gol)
                dialog.dismiss()
            }
            btnNessunAssist.setOnClickListener {
                db.aggiungiEvento(idPartita, data, minutaggio, idMarcatore, "Gol", true)
                avviaAnimazione(view, R.drawable.gol)
                dialog.dismiss()
            }

            btnChiudi.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun mostraDialogSostituzione(
            view: View,
            idGiocatoreUscente: String,
            minutaggio: String
        ) {
            val context = view.context
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.finestra_sostituzione, null)
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()

            val textUscente: TextView = dialogView.findViewById(R.id.textGiocatoreUscente)
            val spinnerEntrante: Spinner = dialogView.findViewById(R.id.spinnerEntrante)
            val btnConferma: Button = dialogView.findViewById(R.id.btnConfermaSostituzione)
            val btnChiudi: ImageButton = dialogView.findViewById(R.id.btnChiudiSostituzione)

            db.getNomeCognomeGiocatore(idGiocatoreUscente) { nome, cognome ->
                textUscente.text = "$nome $cognome"
            }

            val panchinaNomi = mutableListOf<String>()
            val panchinaIdMap = mutableMapOf<String, String>()

            db.getPanchina(idPartita, data) { panchinari ->
                if (panchinari.isEmpty()) {
                    spinnerEntrante.isEnabled = false
                    btnConferma.isEnabled = false
                    return@getPanchina
                }

                fun processGiocatore(index: Int) {
                    if (index >= panchinari.size) {
                        val adapter = ArrayAdapter(
                            context,
                            android.R.layout.simple_spinner_dropdown_item,
                            panchinaNomi
                        )
                        spinnerEntrante.adapter = adapter
                        return
                    }
                    val idGiocatore = panchinari[index]
                    db.getNomeCognomeGiocatore(idGiocatore) { nome, cognome ->
                        if (nome != null && cognome != null) {
                            val nomeCompleto = "$nome $cognome"
                            panchinaNomi.add(nomeCompleto)
                            panchinaIdMap[nomeCompleto] = idGiocatore
                        }
                        processGiocatore(index + 1)
                    }
                }
                processGiocatore(0)
            }

            btnConferma.setOnClickListener {
                val entranteSelezionato = spinnerEntrante.selectedItem?.toString()
                if (entranteSelezionato != null && panchinaIdMap.containsKey(entranteSelezionato)) {
                    val idEntrante = panchinaIdMap[entranteSelezionato]!!
                    eseguiSostituzioneNelDatabase(idGiocatoreUscente, idEntrante, minutaggio)
                    avviaAnimazione(view, R.drawable.cambio)
                }
                dialog.dismiss()
            }

            btnChiudi.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun eseguiSostituzioneNelDatabase(
            idUscente: String,
            idEntrante: String,
            minutaggio: String
        ) {
            db.modificaFormazione(idPartita, data, idUscente, idEntrante) { success, message ->
                if (success) {
                    db.aggiungiEvento(idPartita, data, minutaggio, idUscente, "Cambio", true, mapOf("Entra: " to idEntrante))
                    onFormazioneAggiornata()
                }
            }
        }

    }

    fun updateFormazione(nuovaFormazione: Map<String, String>) {
        this.formazione = nuovaFormazione
        this.formazioneRaggruppata = ruoliOrdine.map { ruolo ->
            nuovaFormazione
                .filter { normalizzaRuolo(it.value) == ruolo }
                .toList()
                .sortedBy { estraiNumeroRuolo(it.second) }
                .map { it.first }
        }.filter { it.isNotEmpty() }

        notifyDataSetChanged()
    }

    fun setButtonsEnabled(enabled: Boolean) {
        buttonsEnabled = enabled
        notifyDataSetChanged()
    }


}
