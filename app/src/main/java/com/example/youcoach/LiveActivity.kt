package com.example.youcoach

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import androidx.core.view.doOnPreDraw
import org.w3c.dom.Text


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
        db.getPartita(dataPartita) { _, avversario, _, _, _, _, _, _ ->
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
        eventoAdapter = EventoAdapter(mutableListOf(), dataPartita, partitaId, this)
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
                recyclerFormazione.adapter = FormazioneAdapter(titolari, dataPartita, partitaId)
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
                eventoAdapter.updateEventi(eventiList)
            }
        }
    }

    private fun setUpNomi(){
        db.getPartita(dataPartita) { idPartita, avversario, _, _, _, casa, _, _ ->
            if (idPartita != null) {
                db.getSquadraPrincipale { nome, _ ->
                    if (casa == true) {
                        squadraCasa.text = nome ?: "N/A"
                        squadraOspite.text = avversario
                    } else {
                        squadraCasa.text = avversario
                        squadraOspite.text = nome ?: "N/A"
                    }
                }
            }
        }
    }

    private fun setUpIconeSuperiori(){
        db.getPartita(dataPartita){ idPartita, _, _, _, _, casa, _, _ ->
            if(idPartita != null){
                if(casa == true){
                    buttonEventiCasa.setImageResource(R.drawable.corner)
                    buttonEventiOspite.setImageResource(R.drawable.punto_esclamativo)
                }else{
                    buttonEventiCasa.setImageResource(R.drawable.punto_esclamativo)
                    buttonEventiOspite.setImageResource(R.drawable.corner)
                }
            }
        }
    }

    private fun setUpListeners() {
        val resources = buttonEventiCasa.resources
        val cornerDrawable = ResourcesCompat.getDrawable(resources, R.drawable.corner, null)
        val puntoEsclamativoDrawable = ResourcesCompat.getDrawable(resources, R.drawable.punto_esclamativo, null)

        buttonEventiCasa.setOnClickListener {
            val casaDrawableState = buttonEventiCasa.drawable?.constantState
            Log.d("setUpListeners", "Button Eventi Casa clicked. Drawable state: $casaDrawableState")

            when (casaDrawableState) {
                cornerDrawable?.constantState -> {
                    Log.d("setUpListeners", "Casa: Angolo rilevato")
                    aggiungiAngolo()
                }
                puntoEsclamativoDrawable?.constantState -> {
                    Log.d("setUpListeners", "Casa: Mostra dialog avversari")
                    mostraDialogAvversari()
                }
            }
        }

        buttonEventiOspite.setOnClickListener {
            val ospiteDrawableState = buttonEventiOspite.drawable?.constantState
            Log.d("setUpListeners", "Button Eventi Ospite clicked. Drawable state: $ospiteDrawableState")

            when (ospiteDrawableState) {
                cornerDrawable?.constantState -> {
                    Log.d("setUpListeners", "Ospite: Angolo rilevato")
                    aggiungiAngolo()
                }
                puntoEsclamativoDrawable?.constantState -> {
                    Log.d("setUpListeners", "Ospite: Mostra dialog avversari")
                    mostraDialogAvversari()
                }
            }
        }
    }

    private fun aggiungiAngolo() {
        getCurrentMinutaggio()?.let { minutaggio ->
            Log.d("aggiungiAngolo", "Aggiunto evento angolo: minutaggio = $minutaggio")
            db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "Angolo", true)
        } ?: Log.e("aggiungiAngolo", "Minutaggio nullo, impossibile aggiungere evento")
    }

    private fun mostraDialogAvversari() {
        Log.d("mostraDialogAvversari", "Apertura finestra eventi avversari")
        val dialogView = layoutInflater.inflate(R.layout.finestra_eventi_avversari, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val btnGolAvversari: ImageButton = dialogView.findViewById(R.id.gol_avversari)
        val btnTiroAvversari: ImageButton = dialogView.findViewById(R.id.tiro_avversari)
        val btnFuorigiocoAvversari: ImageButton = dialogView.findViewById(R.id.fuorigioco_avversari)
        val btnAngoliAvversari: ImageButton = dialogView.findViewById(R.id.angoli_avversari)
        val btnChiudiAvversari: Button = dialogView.findViewById(R.id.btn_chiudi)

        getCurrentMinutaggio()?.let { minutaggio ->
            Log.d("mostraDialogAvversari", "Minutaggio attuale: $minutaggio")

            btnGolAvversari.setOnClickListener {
                Log.d("mostraDialogAvversari", "Evento: Gol Avversario")
                db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "gol", false)
                animaIconaEvento(500f, 500f, R.drawable.gol)
                dialog.dismiss()
            }

            btnTiroAvversari.setOnClickListener {
                Log.d("mostraDialogAvversari", "Evento: Tiro Avversario")
                db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "tiro", false)
                animaIconaEvento(500f, 500f, R.drawable.tiro)
                dialog.dismiss()
            }

            btnFuorigiocoAvversari.setOnClickListener {
                Log.d("mostraDialogAvversari", "Evento: Fuorigioco Avversario")
                db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "fuorigioco", false)
                animaIconaEvento(500f, 500f, R.drawable.fuorigioco)
                dialog.dismiss()
            }

            btnAngoliAvversari.setOnClickListener {
                Log.d("mostraDialogAvversari", "Evento: Angolo Avversario")
                db.aggiungiEvento(partitaId, dataPartita, minutaggio, "N/A", "angolo", false)
                animaIconaEvento(500f, 500f, R.drawable.corner)
                dialog.dismiss()
            }
        } ?: Log.e("mostraDialogAvversari", "Minutaggio nullo, impossibile registrare evento")

        btnChiudiAvversari.setOnClickListener {
            Log.d("mostraDialogAvversari", "Chiusura finestra eventi avversari")
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
        return minutaggio.text.toString()
    }

}


