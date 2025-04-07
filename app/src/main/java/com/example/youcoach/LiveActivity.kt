package com.example.youcoach

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import androidx.core.view.doOnPreDraw


class LiveActivity : BaseActivity() {

    private lateinit var minutaggio: TextView
    private lateinit var fabStartMatch: FloatingActionButton
    private lateinit var fabEndHalf: FloatingActionButton
    private lateinit var fabEndMatch: FloatingActionButton
    private lateinit var tempoDiGioco: TextView
    private lateinit var recyclerFormazione: RecyclerView
    private lateinit var recyclerPanchina: RecyclerView
    private lateinit var recyclerViewEvents: RecyclerView
    private lateinit var sideDrawer: NavigationView
    private lateinit var buttonOpenDrawer: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var eventoAdapter: EventoAdapter
    private lateinit var squadraCasa: TextView
    private lateinit var squadraOspite: TextView
    private lateinit var punteggioCasa: TextView
    private lateinit var punteggioOspite: TextView
    private lateinit var buttonEventiCasa: ImageButton
    private lateinit var buttonEventiOspite: ImageButton

    private var startTime = 0L
    private var timeInMilliseconds = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var matchStarted = false
    private var minutiPerTempo = 45
    private var tempi = 2
    private var tempoCorrente = 1
    private var pressed = true
    private lateinit var partitaId: String
    private lateinit var dataPartita: String

    private val db = DatabaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)

        partitaId = intent.getStringExtra("PARTITA_ID") ?: ""
        dataPartita = intent.getStringExtra("DATA") ?: ""

        if (partitaId.isEmpty() || dataPartita.isEmpty()) {
            Log.e("LiveActivity", "PARTITA_ID o DATA mancanti. Uscita dall'activity.")
            finish()
            return
        }

        initializeUI()
        setUpNomi()
        setupRecyclerViews()
        setUpIconeSuperiori()
        setUpListeners()
        setupEventiListener()
        db.getPartita(dataPartita) { _, avversario, _, _, _, _, _, _, _ ->
            if (avversario == null) {
                Toast.makeText(this, "Dati della partita non trovati per ID: $partitaId", Toast.LENGTH_LONG).show()
            }
        }

        db.getMinutiPerTempo(partitaId, dataPartita){minuti ->
            minutiPerTempo = minuti
        }

        db.getTempi(partitaId, dataPartita){numeroTempi ->
            tempi = numeroTempi
        }
    }

    private fun initializeUI() {
        minutaggio = findViewById(R.id.minutaggio)
        fabStartMatch = findViewById(R.id.fab_start_match)
        fabEndHalf = findViewById(R.id.fab_end_half)
        fabEndMatch = findViewById(R.id.fab_end_match)
        tempoDiGioco = findViewById(R.id.tempo_di_gioco)
        squadraCasa = findViewById(R.id.squadra_casa_text)
        squadraOspite = findViewById(R.id.squadra_ospite_text)
        punteggioCasa = findViewById(R.id.risultato_casa)
        punteggioOspite = findViewById(R.id.risultato_trasferta)
        buttonEventiCasa = findViewById(R.id.btn_eventi_casa)
        buttonEventiOspite = findViewById(R.id.btn_eventi_ospite)
        sideDrawer = findViewById(R.id.sideDrawer)
        buttonOpenDrawer = findViewById(R.id.buttonOpenDrawer)
        drawerLayout = findViewById(R.id.drawer_layout)

        recyclerViewEvents = sideDrawer.findViewById(R.id.recyclerViewEvents)
        recyclerViewEvents.layoutManager = LinearLayoutManager(this)
        eventoAdapter = EventoAdapter(mutableListOf(), dataPartita, partitaId, this){
            Utils.aggiornaUIDopoSostituzione(partitaId, dataPartita, recyclerFormazione, recyclerPanchina,db)
        }
        recyclerViewEvents.adapter = eventoAdapter

        fabStartMatch.setOnClickListener { toggleMenu() }
        fabEndHalf.setOnClickListener { confirmEndTime(false) }
        fabEndMatch.setOnClickListener { confirmEndTime(true) }

        fabStartMatch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.confirm)
        fabStartMatch.setImageResource(R.drawable.fischietto)

        buttonOpenDrawer.setOnClickListener { toggleSideDrawer() }
    }

    private fun setupRecyclerViews() {
        recyclerFormazione = findViewById(R.id.recyclerFormazione)
        recyclerPanchina = findViewById(R.id.recyclerPanchina)

        recyclerFormazione.layoutManager = LinearLayoutManager(this)
        recyclerPanchina.layoutManager = LinearLayoutManager(this)

        db.getFormazioneMap(partitaId, dataPartita) { titolari, panchina ->
            if (titolari.isNotEmpty()) {
                recyclerFormazione.adapter = FormazioneAdapter(titolari, dataPartita, partitaId, onFormazioneAggiornata = {
                    Utils.aggiornaUIDopoSostituzione(partitaId, dataPartita, recyclerFormazione, recyclerPanchina,db)
                })
            } else {
                Toast.makeText(this, "Nessun dato per 'titolari'", Toast.LENGTH_SHORT).show()
            }

            if (panchina.isNotEmpty()) {
                loadGiocatoriRoles(panchina)
            } else {
                Toast.makeText(this, "Nessun dato per 'panchina'", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGiocatoriRoles(panchinaList: List<String>) {
        val ordineRuoli = mapOf("Portiere" to 1, "Difensore" to 2, "Centrocampista" to 3, "Attaccante" to 4)
        db.getGiocatori { giocatori ->
            val giocatoriFiltrati = giocatori.filter { it.id in panchinaList }
            val giocatoriOrdinati = giocatoriFiltrati.sortedBy { ordineRuoli[it.ruolo] ?: Int.MAX_VALUE }
            val idGiocatoriOrdinati = giocatoriOrdinati.map { it.id }
            val panchinaAdapter = PanchinaAdapter(idGiocatoriOrdinati)
            recyclerPanchina.adapter = panchinaAdapter
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private val updateTimerThread = object : Runnable {
        override fun run() {
            timeInMilliseconds = SystemClock.elapsedRealtime() - startTime
            val formattedTime = formatTime(timeInMilliseconds)
            minutaggio.text = formattedTime
            handler.postDelayed(this, 1000)
        }
    }

    private fun toggleMenu() {
        if (!matchStarted) {
            startTime = SystemClock.elapsedRealtime() - timeInMilliseconds
            handler.post(updateTimerThread)
            matchStarted = true
            fabStartMatch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.delete)
            fabStartMatch.setImageResource(R.drawable.pause)
            (recyclerFormazione.adapter as? FormazioneAdapter)?.setButtonsEnabled(true)
        } else {
            toggleEndButtonsVisibility()
        }
    }

    private fun toggleEndButtonsVisibility() {
        if (pressed) {
            fabEndMatch.visibility = View.VISIBLE
            fabEndHalf.visibility = if (tempoCorrente < tempi) View.VISIBLE else View.GONE
            pressed = false
        } else {
            fabEndMatch.visibility = View.GONE
            fabEndHalf.visibility = View.GONE
            pressed = true
        }
    }

    private fun confirmEndTime(isEndMatch: Boolean) {
        if (isEndMatch) {
            showConfirmationDialog("Conferma Fine Partita", "Sei sicuro di voler terminare la partita?") {
                endMatch()
            }
        } else {
            showConfirmationDialog("Conferma Fine tempo", "Sei sicuro di voler terminare il tempo?") {
                endTime()
            }
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Sì") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun endTime() {
        handler.removeCallbacks(updateTimerThread)
        matchStarted = false
        tempoCorrente++
        tempoDiGioco.text = "$tempoCorrente T"
        resetTimer()
        fabEndHalf.visibility = View.GONE
        fabEndMatch.visibility = View.GONE
        pressed = true
        (recyclerFormazione.adapter as? FormazioneAdapter)?.setButtonsEnabled(false)
    }

    private fun endMatch() {
        handler.removeCallbacks(updateTimerThread)
        matchStarted = false
        fabEndHalf.visibility = View.GONE
        fabEndMatch.visibility = View.GONE
        fabStartMatch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.confirm)
        fabStartMatch.setImageResource(R.drawable.fischietto)
        (recyclerFormazione.adapter as? FormazioneAdapter)?.setButtonsEnabled(false)

        val golCasa = punteggioCasa.text.toString().toInt()
        val golOspite = punteggioOspite.text.toString().toInt()

        salvaStats(golCasa, golOspite) { success ->
            if (success) {
                val intent = Intent(this, FinePartitaActivity::class.java)
                intent.putExtra("PARTITA_ID", partitaId)
                intent.putExtra("DATA", dataPartita)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Errore nel salvataggio delle statistiche", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetTimer() {
        startTime = SystemClock.elapsedRealtime()
        timeInMilliseconds = 0
        minutaggio.text = formatTime(0)
        fabStartMatch.setImageResource(R.drawable.fischietto)
        fabStartMatch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.confirm)
    }

    private fun toggleSideDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
            db.getEventi(partitaId, dataPartita) { eventiList ->
                db.getGiocatori { giocatoriList ->
                    val mappaGiocatori = giocatoriList.associateBy { it.id }

                    val eventiConNomi = eventiList.map { evento ->
                        evento.nomeCompletoGiocatore = mappaGiocatori[evento.nomeGiocatore]?.cognome ?: "N/A"
                        evento
                    }

                    eventoAdapter.updateEventi(eventiConNomi)
                }
            }

        }
    }

    private fun setUpNomi(){
        db.getPartita(dataPartita) { idPartita, avversario, _, _, _, casa,_, _, _ ->
            if (idPartita != null && avversario != null) {
                val maxLunghezza = 12
                val testoModificato = if (avversario.length > maxLunghezza) {
                    avversario.substring(0, maxLunghezza - 2) + "..."
                } else {
                    avversario
                }
                db.getSquadraPrincipale { nome, _ ->
                    if (casa == true) {
                        squadraCasa.text = nome ?: "N/A"
                        squadraOspite.text = testoModificato
                    } else {
                        squadraCasa.text = testoModificato
                        squadraOspite.text = nome ?: "N/A"
                    }
                }
            }
        }
    }

    private fun setUpIconeSuperiori(){
        db.getCasa(partitaId, dataPartita){ casa ->
            if(casa == true){
                buttonEventiCasa.setImageResource(R.drawable.corner)
                buttonEventiOspite.setImageResource(R.drawable.punto_esclamativo)
            }else{
                buttonEventiCasa.setImageResource(R.drawable.punto_esclamativo)
                buttonEventiOspite.setImageResource(R.drawable.corner)
            }
        }
    }

    private fun setUpListeners() {
        setupButtonTags()
        buttonEventiCasa.setOnClickListener {
            Log.d("setUpListeners", "Button Eventi Casa clicked. Tag: ${it.tag}")
            when (it.tag) {
                "corner" -> {
                    Log.d("setUpListeners", "Casa: Angolo rilevato")
                    aggiungiAngolo()
                }
                "exclamation" -> {
                    Log.d("setUpListeners", "Casa: Mostra dialog avversari")
                    mostraDialogAvversari()
                }
            }
        }
        buttonEventiOspite.setOnClickListener {
            when (it.tag) {
                "corner" -> aggiungiAngolo()
                "exclamation" -> mostraDialogAvversari()
            }
        }
    }

    private fun setupButtonTags() {
        db.getCasa(partitaId, dataPartita){ casa ->
            if (casa == true) {
                buttonEventiCasa.tag = "corner"
                buttonEventiOspite.tag = "exclamation"
                buttonEventiCasa.setImageResource(R.drawable.corner)
                buttonEventiOspite.setImageResource(R.drawable.punto_esclamativo)
            } else {
                buttonEventiCasa.tag = "exclamation"
                buttonEventiOspite.tag = "corner"
                buttonEventiCasa.setImageResource(R.drawable.punto_esclamativo)
                buttonEventiOspite.setImageResource(R.drawable.corner)
            }
        }
    }

    private fun aggiungiAngolo() {
        getCurrentMinutaggio()?.let { minutaggio ->
            db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "Angolo", true)
        }
        animaIconaEvento(500f, 500f, R.drawable.corner)
    }

    private fun setupEventiListener() {
        db.setupEventiListener(dataPartita, partitaId) { eventi ->
            eventoAdapter.updateEventi(eventi)

            eventi.lastOrNull()?.let { ultimoEvento ->
                if (ultimoEvento.nomeEvento == "Gol") {
                    aggiornaGol(ultimoEvento)
                }
            }
        }
    }

    private fun aggiornaGol(evento: Evento) {
        db.getCasa(partitaId, dataPartita){ casa ->
            when {
                evento.squadra == true && casa == true -> {
                    punteggioCasa.text = (punteggioCasa.text.toString().toInt() + 1).toString()
                }
                evento.squadra == false && casa == false -> {
                    punteggioCasa.text = (punteggioCasa.text.toString().toInt() + 1).toString()
                }
                evento.squadra == true && casa == false -> {
                    punteggioOspite.text = (punteggioOspite.text.toString().toInt() + 1).toString()
                }
                evento.squadra == false && casa == true -> {
                    punteggioOspite.text = (punteggioOspite.text.toString().toInt() + 1).toString()
                }
            }
        }
    }

    private fun mostraDialogAvversari() {
        Log.d("mostraDialogAvversari", "Apertura finestra eventi avversari")
        val dialogView = layoutInflater.inflate(R.layout.finestra_eventi_avversari, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val btnGolAvversari: ImageButton = dialogView.findViewById(R.id.gol_avversari)
        val btnTiroAvversari: ImageButton = dialogView.findViewById(R.id.tiro_avversari)
        val btnFuorigiocoAvversari: ImageButton = dialogView.findViewById(R.id.fuorigioco_avversari)
        val btnAngoliAvversari: ImageButton = dialogView.findViewById(R.id.angoli_avversari)
        val btnGialloAvversari: ImageButton = dialogView.findViewById(R.id.giallo_avversari)
        val btnRossoAvversari: ImageButton = dialogView.findViewById(R.id.rosso_avversari)
        val btnChiudiAvversari: Button = dialogView.findViewById(R.id.btn_chiudi)

        getCurrentMinutaggio()?.let { minutaggio ->
            Log.d("mostraDialogAvversari", "Minutaggio attuale: $minutaggio")

            btnGolAvversari.setOnClickListener {
                db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "Gol", false)
                animaIconaEvento(500f, 500f, R.drawable.gol)
                dialog.dismiss()
            }

            btnTiroAvversari.setOnClickListener {
                val options = arrayOf("In porta", "Fuori porta")
                val builder = AlertDialog.Builder(it.context)

                builder.setTitle("Seleziona se il tiro è in porta o fuori porta")
                    .setItems(options) { dialog, which ->
                        val tipoTiro = options[which]
                        val dettagliTiro = mapOf("tipoTiro" to tipoTiro)
                        db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "Tiro", false, dettagliTiro)
                        animaIconaEvento(500f, 500f, R.drawable.tiro)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Annulla") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                dialog.dismiss()
            }

            btnFuorigiocoAvversari.setOnClickListener {
                db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "Fuorigioco", false)
                animaIconaEvento(500f, 500f, R.drawable.fuorigioco)
                dialog.dismiss()
            }

            btnAngoliAvversari.setOnClickListener {
                db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "Angolo", false)
                animaIconaEvento(500f, 500f, R.drawable.corner)
                dialog.dismiss()
            }
            btnGialloAvversari.setOnClickListener {
                db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "Giallo", false)
                animaIconaEvento(500f, 500f, R.drawable.yellow_card)
                dialog.dismiss()
            }
            btnRossoAvversari.setOnClickListener {
                db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "Rosso", false)
                animaIconaEvento(500f, 500f, R.drawable.red_card)
                dialog.dismiss()
            }
        }

        btnChiudiAvversari.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun animaIconaEvento(startX: Float, startY: Float, iconaResId: Int) {
        val iconaAnimata = findViewById<ImageView>(R.id.iconaAnimata)
        iconaAnimata.setImageResource(iconaResId)
        iconaAnimata.visibility = View.VISIBLE

        iconaAnimata.post {
            iconaAnimata.x = startX - iconaAnimata.width / 2f
            iconaAnimata.y = startY - iconaAnimata.height / 2f

            val buttonOpenDrawer = findViewById<ImageButton>(R.id.buttonOpenDrawer)

            buttonOpenDrawer.doOnPreDraw {
                val buttonCoordinates = IntArray(2)
                buttonOpenDrawer.getLocationInWindow(buttonCoordinates)
                val buttonX = buttonCoordinates[0].toFloat()
                val buttonY = buttonCoordinates[1].toFloat()

                val iconaCoordinates = IntArray(2)
                iconaAnimata.getLocationInWindow(iconaCoordinates)
                val iconaX = iconaCoordinates[0].toFloat()
                val iconaY = iconaCoordinates[1].toFloat()

                val endX = buttonX + buttonOpenDrawer.width / 2f - iconaAnimata.width / 4f
                val endY = buttonY

                val deltaX = endX - iconaX
                val deltaY = endY - iconaY

                val animazioneTraslazione = TranslateAnimation(0f, deltaX, 0f, deltaY).apply {
                    duration = 1000
                    setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}

                        override fun onAnimationEnd(animation: Animation?) {
                            iconaAnimata.x = endX
                            iconaAnimata.y = endY
                            iconaAnimata.visibility = View.GONE
                        }

                        override fun onAnimationRepeat(animation: Animation?) {}
                    })
                }

                iconaAnimata.startAnimation(animazioneTraslazione)

                Log.d("Animazione", "Posizione iniziale icona: X = $iconaX, Y = $iconaY")
                Log.d("Animazione", "Posizione finale icona: X = $endX, Y = $endY")
                Log.d("Animazione", "Delta X: $deltaX, Delta Y: $deltaY")
            }
        }
    }

    fun getCurrentMinutaggio(): String {

        val minutaggioConTempo = minutaggio.text
        val minuti = minutaggioConTempo.substring(0, 2).toInt()
        val secondi = minutaggioConTempo.substring(3).toInt()
        val minutiTotali = minuti + (minutiPerTempo * (tempoCorrente - 1))

        return String.format("%02d:%02d", minutiTotali, secondi)
    }

    fun salvaStats(golCasa: Int, golOspite: Int, callback: (Boolean) -> Unit) {
        val totalTasks = 5
        var completed = 0
        var allSuccess = true

        fun checkComplete(success: Boolean) {
            if (!success) allSuccess = false
            completed++
            if (completed == totalTasks) {
                callback(allSuccess)
            }
        }

        salvaStatisticheSquadra { checkComplete(it) }
        salvaStatisticheGiocatore { checkComplete(it) }
        salvaConvocazioniEMinuti { checkComplete(it) }
        completaStatsRosa { checkComplete(it) }
        salvaRisultatoPartita(golCasa, golOspite) { checkComplete(it) }
    }

    private fun salvaStatisticheSquadra(callback: (Boolean) -> Unit) {
        db.getCasa(partitaId, dataPartita){ casa ->
            val squadraInCasa = casa

            db.getEventi(partitaId, dataPartita) { eventi ->
                val nomeStatistica = mapOf(
                    "Tiro" to "Tiri",
                    "Fallo" to "Falli fatti",
                    "Gol" to "Tiri in porta",
                    "Angolo" to "Angoli",
                    "Giallo" to "Cartellini gialli",
                    "Rosso" to "Cartellini rossi",
                    "Fuorigioco" to "Fuorigiochi"
                )
                val eventiValidi = listOf(
                    "Tiri", "Tiri in porta", "Falli fatti", "Angoli", "Fuorigiochi",
                    "Cartellini gialli", "Cartellini rossi"
                )
                val statsMap = eventiValidi.associateWith { Pair(0, 0) }.toMutableMap()
                eventi.forEach { evento ->
                    val nomeEvento = evento.nomeEvento
                    val squadra = evento.squadra
                    val dettagli = evento.dettagli

                    when (nomeEvento) {
                        "Tiro" -> {
                            val labelTiri = nomeStatistica["Tiro"] ?: return@forEach
                            val attuale = statsMap[labelTiri] ?: Pair(0, 0)
                            val nuovoValore = calcolaValoreAggiornato(squadraInCasa, squadra, attuale)
                            statsMap[labelTiri] = nuovoValore
                            val esito = (dettagli["tipoTiro"] as? String)?.lowercase()
                            if (esito == "in porta") {
                                val labelTP = "Tiri in porta"
                                val attualeTP = statsMap[labelTP] ?: Pair(0, 0)
                                val nuovoTP = calcolaValoreAggiornato(squadraInCasa, squadra, attualeTP)
                                statsMap[labelTP] = nuovoTP
                            }
                        }

                        "Gol" -> {
                            val labelTiri = nomeStatistica["Tiro"] ?: return@forEach
                            val labelTP = nomeStatistica["Gol"] ?: return@forEach
                            val attTiro = statsMap[labelTiri] ?: Pair(0, 0)
                            val attTP = statsMap[labelTP] ?: Pair(0, 0)
                            statsMap[labelTiri] = calcolaValoreAggiornato(squadraInCasa, squadra, attTiro)
                            statsMap[labelTP] = calcolaValoreAggiornato(squadraInCasa, squadra, attTP)
                        }

                        "Fallo" -> {
                            val tipoFallo = (dettagli["tipoFallo"] as? String)?.lowercase()
                            val label = nomeStatistica["Fallo"] ?: return@forEach
                            val attuale = statsMap[label] ?: Pair(0, 0)
                            val assegnaACasa = if (tipoFallo == "subito") {
                                if (squadraInCasa == true) !squadra else squadra
                            } else {
                                if (squadraInCasa == true) squadra else !squadra
                            }
                            val nuovoValore = if (assegnaACasa) {
                                Pair(attuale.first + 1, attuale.second)
                            } else {
                                Pair(attuale.first, attuale.second + 1)
                            }
                            statsMap[label] = nuovoValore
                        }

                        "Angolo", "Giallo", "Rosso", "Fuorigioco" -> {
                            val label = nomeStatistica[nomeEvento] ?: return@forEach
                            val attuale = statsMap[label] ?: Pair(0, 0)
                            val nuovoValore = calcolaValoreAggiornato(squadraInCasa, squadra, attuale)
                            statsMap[label] = nuovoValore
                        }

                        else -> {
                        }
                    }
                }

                db.aggiungiStatsPartita(partitaId, dataPartita, statsMap)
                callback(true)
            }
        }
    }

    private fun salvaStatisticheGiocatore(callback: (Boolean) -> Unit) {
        db.getEventi(partitaId, dataPartita) { eventi ->
            val statsGiocatori = mutableMapOf<String, MutableMap<String, Any>>()

            fun incrementaStat(giocatore: String, chiave: String) {
                val stats = statsGiocatori.getOrPut(giocatore) { mutableMapOf() }
                val attuale = (stats[chiave] as? Int) ?: 0
                stats[chiave] = attuale + 1
            }

            eventi.forEach { evento ->
                if (!evento.squadra) return@forEach
                val nomeEvento = evento.nomeEvento
                val giocatoreId = evento.nomeGiocatore
                val dettagli = evento.dettagli

                when (nomeEvento) {
                    "Tiro" -> {
                        incrementaStat(giocatoreId, "Tiri")
                        val esito = (dettagli["tipoTiro"] as? String)?.lowercase()
                        if (esito == "in porta") {
                            incrementaStat(giocatoreId, "Tiri in porta")
                        }
                    }

                    "Gol" -> {
                        val assistId = (dettagli["assist"] as? String)
                        if (assistId != null) incrementaStat(assistId, "Assist")
                        incrementaStat(giocatoreId, "Gol")
                        incrementaStat(giocatoreId, "Tiri")
                        incrementaStat(giocatoreId, "Tiri in porta")
                    }

                    "Fallo" -> {
                        val tipoFallo = (dettagli["tipoFallo"] as? String)?.lowercase()
                        if (tipoFallo == "subito") {
                            incrementaStat(giocatoreId, "Falli subiti")
                        } else {
                            incrementaStat(giocatoreId, "Falli fatti")
                        }
                    }

                    "Giallo" -> incrementaStat(giocatoreId, "Cartellini gialli")
                    "Rosso" -> incrementaStat(giocatoreId, "Cartellini rossi")
                    "Fuorigioco" -> incrementaStat(giocatoreId, "Fuorigiochi")
                    "Parata" -> incrementaStat(giocatoreId, "Parate")
                    else -> {}
                }
            }

            statsGiocatori.forEach { (giocatoreId, statsMap) ->
                db.aggiungiStatsGiocatore(partitaId, dataPartita, giocatoreId, statsMap)
            }

            callback(true)
        }
    }

    private fun salvaConvocazioniEMinuti(callback: (Boolean) -> Unit) {
        db.getConvocati(partitaId, dataPartita) { convocati ->
            db.getTitolari(partitaId, dataPartita) { titolari ->
                db.getEventi(partitaId, dataPartita) { eventi ->
                    val statsGiocatori = mutableMapOf<String, MutableMap<String, Any>>()
                    convocati.forEach { giocatoreId ->
                        statsGiocatori[giocatoreId] = mutableMapOf(
                            "convocato" to true,
                            "titolare" to (giocatoreId in titolari.keys),
                            "minutoInizio" to if (giocatoreId in titolari.keys) 0 else -1,
                            "minutiGiocati" to 0
                        )
                    }

                    eventi.filter { it.nomeEvento == "Cambio" }
                        .forEach { evento ->
                            val dettagli = evento.dettagli
                            val uscente = evento.nomeGiocatore
                            val entrante = dettagli["Entra: "] as? String ?: return@forEach
                            val minutoCambio = Utils.parseMinuto(evento.minutaggio)

                            statsGiocatori[uscente]?.let { statUscente ->
                                val inizio = (statUscente["minutoInizio"] as? Int) ?: 0
                                statUscente["minutiGiocati"] = minutoCambio - inizio
                            }

                            val statEntrante = statsGiocatori.getOrPut(entrante) {
                                mutableMapOf("convocato" to true, "titolare" to false, "minutiGiocati" to 0)
                            }
                            statEntrante["minutoInizio"] = minutoCambio
                        }

                    db.getPartita(dataPartita) { _, _, _, _, _, _, minutiPerTempo, numeroTempi, _ ->
                        if (minutiPerTempo != null && numeroTempi != null) {
                            val durata = minutiPerTempo * numeroTempi
                            statsGiocatori.forEach { (_, stat) ->
                                val inizio = (stat["minutoInizio"] as? Int)
                                if (inizio != null && inizio >= 0) {
                                    val giocati = durata - inizio
                                    stat["minutiGiocati"] = (stat["minutiGiocati"] as? Int ?: 0) + giocati
                                }
                                stat.remove("minutoInizio")
                            }

                            statsGiocatori.forEach { (id, statMap) ->
                                db.aggiungiStatsGiocatore(partitaId, dataPartita, id, statMap)
                            }

                            callback(true)
                        } else callback(false)
                    }
                }
            }
        }
    }

    private fun completaStatsRosa(callback: (Boolean) -> Unit) {
        db.getGiocatori { giocatori ->
            db.getConvocati(partitaId, dataPartita) { convocati ->
                val idConvocati = convocati.toSet()
                val statsBase = mapOf(
                    "convocato" to false,
                    "titolare" to false,
                    "gol" to 0,
                    "minutiGiocati" to 0,
                    "falliFatti" to 0,
                    "falliSubiti" to 0,
                    "cartelliniGialli" to 0,
                    "cartelliniRossi" to 0,
                    "fuorigiochi" to 0,
                    "tiri" to 0,
                    "tiriInPorta" to 0,
                    "parate" to 0
                )

                var completati = 0
                val tot = giocatori.size

                giocatori.forEach { giocatore ->
                    val id = giocatore.id
                    db.getStatsGiocatorePartita(id, partitaId) { esistenti ->
                        val statMap = mutableMapOf<String, Any>()
                        if (id in idConvocati) {
                            statsBase.forEach { (chiave, valoreBase) ->
                                if (!esistenti.containsKey(chiave)) statMap[chiave] = valoreBase
                            }
                        } else {
                            statMap.putAll(statsBase)
                        }
                        if (statMap.isNotEmpty()) {
                            db.aggiungiStatsGiocatore(partitaId, dataPartita, id, statMap)
                        }
                        completati++
                        if (completati == tot) callback(true)
                    }
                }

                if (tot == 0) callback(true)
            }
        }
    }

    private fun salvaRisultatoPartita(golCasa: Int, golOspite: Int, callback: (Boolean) -> Unit) {
        db.getCasa(partitaId, dataPartita) { casa ->
            val esito = when {
                golCasa > golOspite -> if (casa == true) "Vittoria" else "Sconfitta"
                golCasa < golOspite -> if (casa == true) "Sconfitta" else "Vittoria"
                else -> "Pareggio"
            }

            db.aggiungiRisultato(partitaId, dataPartita, esito, golCasa, golOspite) { success ->
                callback(success)
            }
        }
    }


    private fun calcolaValoreAggiornato(
        squadraInCasa: Boolean?,
        squadraEvento: Boolean,
        attuale: Pair<Int, Int>
    ): Pair<Int, Int> {
        return if (squadraInCasa == true) {
            if (squadraEvento) Pair(attuale.first + 1, attuale.second)
            else Pair(attuale.first, attuale.second + 1)
        } else {
            if (!squadraEvento) Pair(attuale.first + 1, attuale.second)
            else Pair(attuale.first, attuale.second + 1)
        }
    }



}


