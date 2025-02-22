package com.example.youcoach

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FormazioneActivity : BaseActivity() {

    private lateinit var spinnerModuli: Spinner
    private lateinit var recyclerViewPosizioni: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var buttonConferma: Button

    private var formazioneAdapter: SelezioneGiocatoreAdapter? = null


    private val rosaConvocati = mutableListOf<Giocatore>()


    private val moduli: Map<String, Map<String, Int>> = mapOf(
        "3-5-2" to mapOf(
            "Portiere" to 1,
            "Difensore" to 3,
            "Centrocampista" to 5,
            "Attaccante" to 2
        ),
        "3-4-1-2" to mapOf(
            "Portiere" to 1,
            "Difensore" to 3,
            "Centrocampista" to 4,
            "Trequartista" to 2,
            "Attaccante" to 2
        ),
        "3-4-2-1" to mapOf(
            "Portiere" to 1,
            "Difensore" to 3,
            "Centrocampista" to 4,
            "Trequartista" to 1,
            "Attaccante" to 2
        ),
        "4-5-1" to mapOf(
            "Portiere" to 1,
            "Difensore" to 4,
            "Centrocampista" to 5,
            "Attaccante" to 1
        ),
        "4-4-2" to mapOf(
            "Portiere" to 1,
            "Difensore" to 4,
            "Centrocampista" to 4,
            "Attaccante" to 2
        ),
        "4-3-2-1" to mapOf(
            "Portiere" to 1,
            "Difensore" to 4,
            "Centrocampista" to 3,
            "Trequartista" to 2,
            "Attaccante" to 1
        ),
        "4-3-1-2" to mapOf(
            "Portiere" to 1,
            "Difensore" to 4,
            "Centrocampista" to 3,
            "Trequartista" to 1,
            "Attaccante" to 2
        ),
        "4-3-3" to mapOf(
            "Portiere" to 1,
            "Difensore" to 4,
            "Centrocampista" to 3,
            "Attaccante" to 3
        ),
        "4-2-4" to mapOf(
            "Portiere" to 1,
            "Difensore" to 4,
            "Centrocampista" to 2,
            "Attaccante" to 4
        ),
        "4-2-3-1" to mapOf(
            "Portiere" to 1,
            "Difensore" to 4,
            "Centrocampista" to 2,
            "Trequartista" to 3,
            "Attaccante" to 1
        ),
        "5-2-3" to mapOf(
            "Portiere" to 1,
            "Difensore" to 5,
            "Centrocampista" to 2,
            "Attaccante" to 3
        ),
        "5-3-2" to mapOf(
            "Portiere" to 1,
            "Difensore" to 5,
            "Centrocampista" to 3,
            "Attaccante" to 2
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formazione_titolari)
        setupBottomNavigation(R.id.nav_calendar)

        spinnerModuli = findViewById(R.id.spinnerModuli)
        recyclerViewPosizioni = findViewById(R.id.recyclerPosizioni)
        recyclerViewPosizioni.layoutManager = LinearLayoutManager(this)
        database = Firebase.database.reference
        buttonConferma = findViewById(R.id.buttonConferma)

        val moduliList = moduli.keys.toList()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, moduliList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModuli.adapter = spinnerAdapter

        val partitaId = intent.getStringExtra("PARTITA_ID")
        val data = intent.getStringExtra("DATA")
        if (partitaId != null && data != null) {
            caricaDatiPartita(partitaId, data)
            caricaGiocatori(partitaId, data) {
                setupModuloListener(moduliList)
                spinnerModuli.setSelection(0)
            }
        }

        buttonConferma.setOnClickListener {
            val currentAdapter = formazioneAdapter

            if (partitaId != null && data != null && currentAdapter != null) {
                salvaFormazione(currentAdapter, partitaId, data)

            } else {
                Log.e("FormazioneActivity", "Impossibile salvare la formazione: dati mancanti")
            }
        }
    }


    private fun caricaGiocatori(partitaId: String, dataPartita: String, onComplete: () -> Unit) {
        val convocatiRef =
            database.child("Partite").child(dataPartita).child(partitaId).child("convocati")

        convocatiRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(convSnapshot: DataSnapshot) {
                val convocatiMap = convSnapshot.children
                    .associate { it.key!! to (it.getValue(Boolean::class.java) ?: false) }

                val giocatoriRef = database.child("Giocatori")
                giocatoriRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        rosaConvocati.clear()
                        for (data in snapshot.children) {
                            val giocatore = data.getValue(Giocatore::class.java)
                            if (giocatore != null && convocatiMap[giocatore.id] == true) {
                                rosaConvocati.add(giocatore)
                            }
                        }
                        onComplete()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "FormazioneActivity",
                            "Errore caricamento giocatori: ${error.message}"
                        )
                        onComplete()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FormazioneActivity", "Errore caricamento convocati: ${error.message}")
                onComplete()
            }
        })
    }

    private fun caricaDatiPartita(partitaId: String, dataPartita: String) {
        val partitaRef = database.child("Partite").child(dataPartita).child(partitaId)
        partitaRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val isCasa = snapshot.child("casa").getValue(Boolean::class.java) ?: true
                    val avversario =
                        snapshot.child("avversario").getValue(String::class.java) ?: "Avversario"
                    val titoloPartita = if (isCasa) {
                        "BedizzoleU16 - $avversario"
                    } else {
                        "$avversario - BedizzoleU16"
                    }
                    findViewById<TextView>(R.id.testo_partita).text = titoloPartita
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestisci l'errore
            }
        })
    }


    @SuppressLint("SetTextI18n")
    private fun setupModuloListener(moduliList: List<String>) {
        spinnerModuli.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val moduloSelezionato = moduliList[position]
                val configurazione = moduli[moduloSelezionato] ?: emptyMap()
                val posizioni = mutableListOf<Posizione>()

                configurazione.forEach { (ruolo, quantita) ->
                    repeat(quantita) { posizioni.add(Posizione(ruolo, it + 1)) }
                }

                formazioneAdapter = SelezioneGiocatoreAdapter(this@FormazioneActivity, posizioni, rosaConvocati)
                recyclerViewPosizioni.adapter = formazioneAdapter
                Log.d("FormazioneActivity", "formationAdapter assegnato con ${posizioni.size} posizioni")
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Nessuna azione necessaria
            }
        })
    }


    private fun salvaFormazione(adapter: SelezioneGiocatoreAdapter, partitaId: String, dataPartita: String) {
        val titolari = mutableMapOf<String, String>()
        adapter.selezioni.forEach { (posizione, giocatoreId) ->
            val key = giocatoreId
            titolari[key] = "${posizione.ruolo} ${posizione.posizioneIndex}"
        }

        val titolariIds = titolari.keys.toSet()
        val panchina = rosaConvocati.filter { it.id !in titolariIds }.map { it.id }

        val formazioneMap = mapOf(
            "titolari" to titolari,
            "panchina" to panchina
        )

        database.child("Partite").child(dataPartita).child(partitaId).child("formazione")
            .setValue(formazioneMap)
            .addOnSuccessListener {
                Log.d("FormazioneActivity", "Formazione salvata con successo!")
            }
            .addOnFailureListener { error ->
                Log.e("FormazioneActivity", "Errore durante il salvataggio della formazione: ${error.message}")
            }
    }
}

