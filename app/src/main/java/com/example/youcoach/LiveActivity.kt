package com.example.youcoach

import android.app.AlertDialog
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
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

class LiveActivity : BaseActivity() {

    // UI elements
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
    private var handler = android.os.Handler()
    private var matchStarted = false
    private var minutiPerTempo = 45
    private var tempi = 2
    private var tempoCorrente = 1
    private var pressed = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)

        initializeUI()

        val partitaId = intent.getStringExtra("PARTITA_ID")
        val dataPartita = intent.getStringExtra("DATA")

        if (partitaId.isNullOrEmpty() || dataPartita.isNullOrEmpty()) {
            Log.e("LiveActivity", "PARTITA_ID o DATA mancanti. Uscita dall'activity.")
            finish()
            return
        }

        setupRecyclerViews()

        recyclerViewEvents.layoutManager = LinearLayoutManager(this)
        eventoAdapter = EventoAdapter(mutableListOf())
        recyclerViewEvents.adapter = eventoAdapter
        loadEventiFromFirebase(partitaId, dataPartita)

        loadPartitaData(partitaId, dataPartita)
        loadMinutiPerTempo(partitaId, dataPartita)
        loadTempi(partitaId, dataPartita)
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

        recyclerViewEvents = findViewById<RecyclerView>(R.id.recyclerViewEvents)

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
    }

    // Load partita data from Firebase
    private fun loadPartitaData(partitaId: String, dataPartita: String) {
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

    // Load minuti per tempo from Firebase
    private fun loadMinutiPerTempo(partitaId: String, dataPartita: String) {
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

    // Load tempi (number of periods) from Firebase
    private fun loadTempi(partitaId: String, dataPartita: String) {
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

    // Format time in MM:SS format
    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // Timer update thread
    private val updateTimerThread = object : Runnable {
        override fun run() {
            timeInMilliseconds = SystemClock.elapsedRealtime() - startTime
            minutaggio.text = formatTime(timeInMilliseconds)
            handler.postDelayed(this, 1000)
        }
    }

    // Toggle match state (start/pause)
    private fun toggleMenu() {
        if (!matchStarted) {
            startTime = SystemClock.elapsedRealtime() - timeInMilliseconds
            handler.post(updateTimerThread)
            matchStarted = true
            fabStartMatch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.delete)
            fabStartMatch.setImageResource(R.drawable.pause)
        } else {
            toggleEndButtonsVisibility()
        }
    }

    // Toggle visibility for end buttons
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

    // Confirm and finish time/partita
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

    // End time logic
    private fun endTime() {
        handler.removeCallbacks(updateTimerThread)
        matchStarted = false
        tempoCorrente++
        tempoDiGioco.text = "$tempoCorrente T"
        resetTimer()
        fabEndHalf.visibility = View.GONE
        fabEndMatch.visibility = View.GONE
        pressed = true
    }

    // End match logic
    private fun endMatch() {
        handler.removeCallbacks(updateTimerThread)
        matchStarted = false
        fabEndHalf.visibility = View.GONE
        fabEndMatch.visibility = View.GONE
        fabStartMatch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.confirm)
    }

    // Reset timer after a period ends
    private fun resetTimer() {
        startTime = (minutiPerTempo * 60 * 1000).toLong()
        timeInMilliseconds = startTime
        minutaggio.text = formatTime(startTime)
        fabStartMatch.setImageResource(R.drawable.fischietto)
    }

    private fun toggleSideDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun loadEventiFromFirebase(partitaId: String, dataPartita: String) {
        val database = Firebase.database.reference
        // Adatta il percorso al tuo database (es: "Partite/$dataPartita/$partitaId/eventi")
        val eventiRef = database.child("Partite").child(dataPartita).child(partitaId).child("Eventi")

        eventiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val eventiList = mutableListOf<Evento>()
                for (eventSnapshot in snapshot.children) {
                    val evento = eventSnapshot.getValue(Evento::class.java)
                    if (evento != null) {
                        eventiList.add(evento)
                    }
                }
                eventoAdapter.updateEventi(eventiList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveActivity", "Errore nel caricamento degli eventi: ${error.message}")
            }
        })
    }

}

