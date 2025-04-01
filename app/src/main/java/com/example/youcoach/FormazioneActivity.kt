package com.example.youcoach

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FormazioneActivity : BaseActivity() {

    private lateinit var spinnerModuli: Spinner
    private lateinit var recyclerViewPosizioni: RecyclerView
    private val db = DatabaseManager()
    private lateinit var buttonConferma: Button
    private lateinit var backButton: ImageButton

    private var selezioneGiocatoreAdapter: SelezioneGiocatoreAdapter? = null


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
            "Trequartista" to 1,
            "Attaccante" to 2
        ),
        "3-4-2-1" to mapOf(
            "Portiere" to 1,
            "Difensore" to 3,
            "Centrocampista" to 4,
            "Trequartista" to 2,
            "Attaccante" to 1
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
        buttonConferma = findViewById(R.id.buttonConferma)
        backButton = findViewById(R.id.back_button)

        val moduliList = moduli.keys.toList()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, moduliList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModuli.adapter = spinnerAdapter

        val partitaId = intent.getStringExtra("PARTITA_ID")
        val data = intent.getStringExtra("DATA")
        if (partitaId != null && data != null) {
            caricaDatiPartita(data)
            caricaGiocatori(partitaId, data) {
                setupModuloListener(moduliList)
                spinnerModuli.setSelection(0)
            }
        }

        buttonConferma.setOnClickListener {
            val currentAdapter = selezioneGiocatoreAdapter

            if (partitaId != null && data != null && currentAdapter != null) {
                salvaFormazione(currentAdapter, partitaId, data)

            } else {
                Log.e("FormazioneActivity", "Impossibile salvare la formazione: dati mancanti")
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun caricaGiocatori(partitaId: String, dataPartita: String, onComplete: () -> Unit) {
        db.getConvocati(partitaId, dataPartita) { convocatiIds ->
            db.getGiocatori { giocatori ->
                rosaConvocati.clear()
                rosaConvocati.addAll(giocatori.filter { it.id in convocatiIds })
                onComplete()
            }
        }
    }

    private fun caricaDatiPartita(formattedDate: String) {
        db.getPartita(formattedDate) { _, avversario, _, _, _, casa, _, _, _ ->
            if (avversario != null && casa != null) {
                db.getSquadraPrincipale { nome, _ ->
                    val titoloPartita = if (casa) {
                        "$nome - $avversario"
                    } else {
                        "$avversario - $nome"
                    }
                    findViewById<TextView>(R.id.testo_partita).text = titoloPartita
                }
            }
        }
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

                selezioneGiocatoreAdapter = SelezioneGiocatoreAdapter(this@FormazioneActivity, posizioni, rosaConvocati)
                recyclerViewPosizioni.adapter = selezioneGiocatoreAdapter
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            }
        })
    }

    private fun salvaFormazione(adapter: SelezioneGiocatoreAdapter, partitaId: String, dataPartita: String) {
        db.aggiungiFormazionePartita(adapter, partitaId, dataPartita) { success, message ->
            if (success) {
                val intent = Intent(this, LiveActivity::class.java)
                intent.putExtra("PARTITA_ID", partitaId)
                intent.putExtra("DATA", dataPartita)
                startActivity(intent)
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

}

