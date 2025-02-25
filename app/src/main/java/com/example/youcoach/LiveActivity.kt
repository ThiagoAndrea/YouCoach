package com.example.youcoach

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LiveActivity : BaseActivity() {

    private lateinit var minutaggio: TextView
    private lateinit var fabStartMatch: FloatingActionButton
    private var startTime = 0L
    private var timeInMilliseconds = 0L
    private var handler = android.os.Handler()
    private var matchStarted = false

    private lateinit var recyclerFormazione: RecyclerView
    private lateinit var recyclerPanchina: RecyclerView
    private lateinit var formazioneAdapter: FormazioneAdapter
    private lateinit var panchinaAdapter: PanchinaAdapter
    private val eventoManager = EventoManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)

        val partitaId = intent.getStringExtra("PARTITA_ID")
        val dataPartita = intent.getStringExtra("DATA")

        if (partitaId.isNullOrEmpty() || dataPartita.isNullOrEmpty()) {
            Log.e("LiveActivity", "PARTITA_ID o DATA mancanti. Uscita dall'activity.")
            finish()
            return
        }

        Log.d("LiveActivity", "PARTITA_ID: $partitaId, DATA: $dataPartita")

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
                    formazioneAdapter = FormazioneAdapter(formazioneMap, dataPartita, partitaId, eventoManager)
                    recyclerFormazione.adapter = formazioneAdapter
                } else {
                    Log.e("LiveActivity", "Nessun dato per 'titolari'")
                }

                val panchinaList = snapshot.child("panchina").children.mapNotNull { it.getValue(String::class.java) }

                if (panchinaList.isNotEmpty()) {
                    caricaRuoliGiocatori(panchinaList)
                } else {
                    Log.e("LiveActivity", "Nessun dato per 'panchina'")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveActivity", "Errore Firebase: ${error.message}")
            }
        })

        minutaggio = findViewById(R.id.minutaggio)
        fabStartMatch = findViewById(R.id.fab_start_match)

        fabStartMatch.setOnClickListener {
            if (!matchStarted) {
                startTime = SystemClock.elapsedRealtime() - timeInMilliseconds
                handler.post(updateTimerThread)
                matchStarted = true
                fabStartMatch.setImageResource(R.drawable.fallo) // Cambia icona a "pausa"
            } else {
                handler.removeCallbacks(updateTimerThread)
                matchStarted = false
                fabStartMatch.setImageResource(R.drawable.fischietto) // Cambia icona a "fischietto"
            }
        }
    }

    private fun caricaRuoliGiocatori(panchinaList: List<String>) {
        val databaseRef = Firebase.database.reference.child("Giocatori")
        val ordineRuoli = mapOf("Portiere" to 1, "Difensore" to 2, "Centrocampista" to 3, "Attaccante" to 4)

        val tasks = panchinaList.map { idGiocatore ->
            databaseRef.child(idGiocatore).child("ruolo").get()
                .continueWith { task ->
                    val ruolo = task.result?.getValue(String::class.java) ?: "Altro"
                    idGiocatore to ruolo
                }
        }

        Tasks.whenAllComplete(tasks).addOnSuccessListener {
            val giocatoriOrdinati = tasks.mapNotNull { it.result as? Pair<String, String> }
                .sortedBy { ordineRuoli[it.second] ?: Int.MAX_VALUE }
                .map { it.first }

            panchinaAdapter = PanchinaAdapter(giocatoriOrdinati)
            recyclerPanchina.adapter = panchinaAdapter

            Log.d("LiveActivity", "Panchina caricata con ${giocatoriOrdinati.size} giocatori")
        }.addOnFailureListener {
            Log.e("LiveActivity", "Errore nel caricamento dei ruoli")
        }
    }

    private val updateTimerThread = object : Runnable {
        override fun run() {
            timeInMilliseconds = SystemClock.elapsedRealtime() - startTime
            minutaggio.text = formatTime(timeInMilliseconds)
            handler.postDelayed(this, 1000)
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
