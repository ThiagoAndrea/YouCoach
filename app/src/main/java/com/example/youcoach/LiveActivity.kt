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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
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

    // Data handling
    private val eventoManager = EventoManager()
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
        setupRecyclerViews()
        loadPartitaData()
        loadMinutiPerTempo()
        loadTempi()
    }

    // Initialize UI elements
    private fun initializeUI() {
        minutaggio = findViewById(R.id.minutaggio)
        fabStartMatch = findViewById(R.id.fab_start_match)
        fabEndHalf = findViewById(R.id.fab_end_half)
        fabEndMatch = findViewById(R.id.fab_end_match)
        tempoDiGioco = findViewById(R.id.tempo_di_gioco)

        sideDrawer = findViewById(R.id.sideDrawer)
        buttonOpenDrawer = findViewById(R.id.buttonOpenDrawer)
        drawerLayout = findViewById(R.id.drawer_layout)


        recyclerViewEvents = sideDrawer.findViewById(R.id.recyclerViewEvents)
        recyclerViewEvents.layoutManager = LinearLayoutManager(this)
        eventoAdapter = EventoAdapter(mutableListOf())
        recyclerViewEvents.adapter = eventoAdapter


        fabStartMatch.setOnClickListener { toggleMenu() }
        fabEndHalf.setOnClickListener { confirmEndTime(false) }
        fabEndMatch.setOnClickListener { confirmEndTime(true) }

        fabStartMatch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.confirm)
        fabStartMatch.setImageResource(R.drawable.fischietto)

        buttonOpenDrawer.setOnClickListener { toggleSideDrawer() }
    }

    // Set up RecyclerViews
    private fun setupRecyclerViews() {
        recyclerFormazione = findViewById(R.id.recyclerFormazione)
        recyclerPanchina = findViewById(R.id.recyclerPanchina)

        recyclerFormazione.layoutManager = LinearLayoutManager(this)
        recyclerPanchina.layoutManager = LinearLayoutManager(this)
        val database = Firebase.database.reference
        val formazioneRef = database.child("Partite").child(dataPartita).child(partitaId).child("formazione")

        formazioneRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("LiveActivity", "Snapshot ricevuto: ${snapshot.value}")

                val formazioneMap = snapshot.child("titolari").children
                    .mapNotNull { it.key?.let { idGiocatore -> idGiocatore to it.getValue(String::class.java) } }
                    .toMap() as Map<String, String>

                if (formazioneMap.isNotEmpty()) {
                    val formazioneAdapter = FormazioneAdapter(formazioneMap, dataPartita, partitaId, eventoManager)
                    recyclerFormazione.adapter = formazioneAdapter
                } else {
                    Log.e("LiveActivity", "Nessun dato per 'titolari'")
                }

                val panchinaList = snapshot.child("panchina").children.mapNotNull { it.getValue(String::class.java) }

                if (panchinaList.isNotEmpty()) {
                    loadGiocatoriRoles(panchinaList)
                } else {
                    Log.e("LiveActivity", "Nessun dato per 'panchina'")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveActivity", "Errore Firebase: ${error.message}")
            }
        })
    }

    // Load partita data from Firebase
    private fun loadPartitaData() {
        val database = Firebase.database.reference
        val partitaRef = database.child("Partite").child(dataPartita).child(partitaId)

        partitaRef.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                // Puoi recuperare altri dati della partita qui, se necessario
            } else {
                Log.e("LiveActivity", "Dati della partita non trovati per ID: $partitaId")
            }
        }.addOnFailureListener {
            Log.e("LiveActivity", "Errore nel recupero dei dati della partita: ${it.message}")
        }
    }

    // Load roles for players on the bench
    private fun loadGiocatoriRoles(panchinaList: List<String>) {
        val databaseRef = Firebase.database.reference.child("Giocatori")
        val ordineRuoli = mapOf("Portiere" to 1, "Difensore" to 2, "Centrocampista" to 3, "Attaccante" to 4)

        val tasks = panchinaList.map { idGiocatore ->
            databaseRef.child(idGiocatore).child("ruolo").get()
                .continueWith { task -> idGiocatore to (task.result?.getValue(String::class.java) ?: "Altro") }
        }

        Tasks.whenAllComplete(tasks).addOnSuccessListener {
            val giocatoriOrdinati = tasks.mapNotNull { it.result as? Pair<String, String> }
                .sortedBy { ordineRuoli[it.second] ?: Int.MAX_VALUE }
                .map { it.first }

            val panchinaAdapter = PanchinaAdapter(giocatoriOrdinati)
            recyclerPanchina.adapter = panchinaAdapter

            Log.d("LiveActivity", "Panchina caricata con ${giocatoriOrdinati.size} giocatori")
        }.addOnFailureListener {
            Log.e("LiveActivity", "Errore nel caricamento dei ruoli")
        }
    }


    private fun loadMinutiPerTempo() {
        val database = Firebase.database.reference
        database.child("Partite").child(dataPartita).child(partitaId).child("minuti_per_tempo")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    minutiPerTempo = snapshot.getValue(Int::class.java) ?: 45
                    Log.d("LiveActivity", "Minuti per tempo caricati: $minutiPerTempo")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LiveActivity", "Errore nel caricamento dei minuti per tempo")
                }
            })
    }

    private fun loadTempi() {
        val database = Firebase.database.reference
        database.child("Partite").child(dataPartita).child(partitaId).child("numero_tempi")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tempi = snapshot.getValue(Int::class.java) ?: 2
                    Log.d("LiveActivity", "Numero tempi caricati: $tempi")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LiveActivity", "Errore nel caricamento del numero di tempi")
                }
            })
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

    // Show a confirmation dialog
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

    // End match logic
    private fun endMatch() {
        handler.removeCallbacks(updateTimerThread)
        matchStarted = false
        fabEndHalf.visibility = View.GONE
        fabEndMatch.visibility = View.GONE
        fabStartMatch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.confirm)
        fabStartMatch.setImageResource(R.drawable.fischietto)
        (recyclerFormazione.adapter as? FormazioneAdapter)?.setButtonsEnabled(false)
    }

    // Reset timer after a period ends
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
            loadEventiFromFirebase()
        }
    }

    private fun loadEventiFromFirebase() {
        val database = Firebase.database.reference
        val eventiRef = database.child("Partite").child(dataPartita).child(partitaId).child("eventi")
        eventiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("LiveActivity", "Snapshot ricevuto: ${snapshot.value}")
                val eventiList = mutableListOf<Evento>()
                for (eventSnapshot in snapshot.children) {
                    try {
                        val evento = eventSnapshot.getValue(Evento::class.java)
                        if (evento != null) {
                            eventiList.add(evento)
                        }
                    } catch (e: Exception) {
                    }
                }
                Log.d("LiveActivity", "Numero di eventi caricati: ${eventiList.size}")
                eventoAdapter.updateEventi(eventiList)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveActivity", "Errore nel caricamento degli eventi: ${error.message}")
            }
        })
    }

    fun animaIconaEvento(startX: Float, startY: Float, iconaResId: Int) {
        val iconaAnimata = findViewById<ImageView>(R.id.iconaAnimata)
        iconaAnimata.setImageResource(iconaResId)
        iconaAnimata.visibility = View.VISIBLE

        iconaAnimata.post {
            // Imposta la posizione iniziale
            iconaAnimata.x = startX - iconaAnimata.width / 2f
            iconaAnimata.y = startY - iconaAnimata.height / 2f

            val buttonOpenDrawer = findViewById<ImageButton>(R.id.buttonOpenDrawer)

            // Aspettiamo che il layout sia pronto
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
                            // Imposta manualmente la posizione finale
                            iconaAnimata.x = endX
                            iconaAnimata.y = endY
                            // Ora possiamo nascondere l'icona
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


