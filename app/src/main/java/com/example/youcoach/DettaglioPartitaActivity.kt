package com.example.youcoach

import android.content.DialogInterface
import android.content.Intent
import android.content.SyncResult
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DettaglioPartitaActivity : BaseActivity() {
    private lateinit var database: DatabaseReference
    private var partitaId: String? = null
    private lateinit var formattedDate: String

    // UI Components
    private lateinit var cardMatch: MaterialCardView
    private lateinit var cardTitle: TextView
    private lateinit var cardDate: TextView
    private lateinit var cardTime: TextView
    private lateinit var matchLocation: TextView
    private lateinit var competitionIcon: ImageView
    private lateinit var numTempi: TextView
    private lateinit var durataTempo: TextView
    private lateinit var numGiocatori: TextView
    private lateinit var backButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var editButton: ImageButton
    private lateinit var convocatiButton: Button
    private lateinit var goLiveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_partita)
        setupBottomNavigation(R.id.nav_calendar)

        database = FirebaseDatabase.getInstance().reference

        initUI()
        loadSelectedDate()
        caricaPartita(formattedDate!!)
        setupButtonListeners()
    }

    private fun initUI() {
        cardMatch = findViewById(R.id.card_match)
        cardTitle = findViewById(R.id.card_title_partita)
        cardDate = findViewById(R.id.card_date_partita)
        cardTime = findViewById(R.id.card_time)
        matchLocation = findViewById(R.id.match_location)
        numTempi = findViewById(R.id.numero_tempi)
        durataTempo = findViewById(R.id.durata_tempo)
        numGiocatori = findViewById(R.id.numero_giocatori)
        backButton = findViewById(R.id.back_button)
        competitionIcon = findViewById(R.id.competition_icon)
        convocatiButton = findViewById(R.id.convocati_button)
        goLiveButton = findViewById(R.id.go_live_button)
        editButton = findViewById(R.id.modificaPartita_button)
        deleteButton = findViewById(R.id.eliminaPartita_button)
    }

    private fun loadSelectedDate() {
        val selectedDay = intent.getStringExtra("selectedDay")
        val selectedMonth = intent.getIntExtra("selectedMonth", -1)
        val selectedYear = intent.getIntExtra("selectedYear", -1)

        formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay?.toIntOrNull() ?: 0)
        val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
        cardDate.text = date
    }

    private fun setupButtonListeners() {

        backButton.setOnClickListener {finish()}
        editButton.setOnClickListener{modificaPartita()}
        deleteButton.setOnClickListener{confermaEliminazione()}
        convocatiButton.setOnClickListener{caricaGiocatoriEApriDialog()}
        goLiveButton.setOnClickListener{goLive()}

        //convocatiButton!!.setOnClickListener { v: View? -> caricaConvocatiEApriDialog() }
        //goLiveButton!!.setOnClickListener { v: View? -> avviaLive() }
    }

    private fun goLive() {
        val intent = Intent(this, FormazioneActivity::class.java)
        intent.putExtra("PARTITA_ID", partitaId)
        intent.putExtra("DATA", formattedDate)
        startActivity(intent)
    }

    private fun caricaPartita(formattedDate: String) {
        database.child("Partite").child(formattedDate)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val partitaSnapshot = snapshot.children.firstOrNull()
                        partitaSnapshot?.let {
                            partitaId = it.key
                            val avversario = it.child("avversario").value as? String ?: "N/A"
                            val orario = it.child("orario").value as? String ?: "N/A"
                            val location = it.child("luogo").value as? String ?: "N/A"
                            val competizione = it.child("competizione").value as? String ?: "N/A"
                            val casa = it.child("casa").value as? Boolean ?: false
                            val minPerTempo = (it.child("minuti_per_tempo").value as? Number)?.toInt() ?: 0
                            val numCalciatori = (it.child("numero_calciatori").value as? Number)?.toInt() ?: 0
                            val numeroTempi = (it.child("numero_tempi").value as? Number)?.toInt() ?: 0


                            if (casa) {
                                cardTitle.text = "Bedizzole U16 - $avversario"
                            } else {
                                cardTitle.text = "$avversario - Bedizzole U16"
                            }
                            cardTime.text="$orario"
                            matchLocation.text="$location"
                            durataTempo.text="Durata per tempo: $minPerTempo"
                            numGiocatori.text="Giocatori: $numCalciatori"
                            numTempi.text="Tempi: $numeroTempi"

                            when(competizione){
                                "Campionato" -> competitionIcon.setImageResource(R.drawable.campionato)
                                "Coppa" -> competitionIcon.setImageResource(R.drawable.coppa)
                                else -> competitionIcon.setImageResource(R.drawable.fair_play)
                            }
                        }


                    } else {
                        cardMatch.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@DettaglioPartitaActivity,
                        "Errore nel recupero della partita",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun caricaGiocatoriEApriDialog() {
        val giocatoriRef = database.child("Giocatori")

        giocatoriRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(giocatoriSnapshot: DataSnapshot) {
                if (giocatoriSnapshot.exists()) {
                    val giocatori = giocatoriSnapshot.children.mapNotNull { it.getValue(Giocatore::class.java) }

                    val convocazioniRef = database.child("Partite")
                        .child(formattedDate)
                        .child(partitaId ?: "")
                        .child("convocati")
                    convocazioniRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(presenzeSnapshot: DataSnapshot) {
                            val convocazioniIniziali = mutableMapOf<String, Boolean>()
                            presenzeSnapshot.children.forEach {
                                val playerId = it.key ?: ""
                                val stato = it.getValue(Boolean::class.java) ?: true
                                convocazioniIniziali[playerId] = stato
                            }

                            // Se convocazioniIniziali è vuota, vuol dire che non c'è ancora una lista salvata, quindi passa una mappa vuota
                            apriDialogConvocati(giocatori, convocazioniIniziali)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun apriDialogConvocati(giocatori: List<Giocatore>, convocazioniIniziali: Map<String, Boolean>) {
        val dialog = ConvocazioniDialogFragment(giocatori, convocazioniIniziali) { convocazioniIniziali ->
            salvaConvocatiNelDatabase(convocazioniIniziali)
        }
        dialog.show(supportFragmentManager, "ConvocazioniDialogFragment")
    }

    private fun salvaConvocatiNelDatabase(convocazioniConfermate: Map<String, Boolean>) {
        val presenzeRef = database.child("Partite")
            .child(formattedDate)
            .child(partitaId ?: "")
            .child("convocati")

        // Salva la mappa intera, che contiene sia true che false.
        presenzeRef.setValue(convocazioniConfermate)
            .addOnSuccessListener {
                Toast.makeText(this, "Convocazioni aggiornate!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore!", Toast.LENGTH_SHORT).show()
            }
    }



    private fun modificaPartita() {
        if (partitaId != null) {
            val intent = Intent(this, AggiungiPartitaActivity::class.java)
            intent.putExtra("PARTITA_ID", partitaId)
            intent.putExtra("DATA", formattedDate)
            intent.putExtra("ORARIO", cardTime.text.toString())
            intent.putExtra("LUOGO", matchLocation.text.toString())
            intent.putExtra("AVVERSARIO", cardTitle.text.toString().split(" - ").last()) // Estrai avversario dal titolo
            intent.putExtra("COMPETIZIONE", when (competitionIcon.drawable.constantState) {
                resources.getDrawable(R.drawable.campionato, null).constantState -> "Campionato"
                resources.getDrawable(R.drawable.coppa, null).constantState -> "Coppa"
                else -> "Amichevole"
            })
            intent.putExtra("CASA", cardTitle.text.startsWith("Bedizzole U16"))
            intent.putExtra("MINUTI_PER_TEMPO", durataTempo.text.toString().replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0)
            intent.putExtra("NUMERO_CALCIATORI", numGiocatori.text.toString().replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0)
            intent.putExtra("NUMERO_TEMPI", numTempi.text.toString().replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0)

            startActivity(intent)
        } else {
            Toast.makeText(this, "Errore: Nessuna partita selezionata", Toast.LENGTH_SHORT).show()
        }
    }


    private fun confermaEliminazione() {
        AlertDialog.Builder(this)
            .setTitle("Conferma eliminazione")
            .setMessage("Sei sicuro di voler eliminare questa partita?")
            .setPositiveButton(
                "Elimina"
            ) { dialog: DialogInterface?, which: Int -> eliminaPartita() }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun eliminaPartita() {
        if (partitaId != null) {
            database!!.child("Partite").child(formattedDate!!).child(partitaId!!)
                .removeValue()
                .addOnSuccessListener { aVoid: Void? ->
                    Toast.makeText(
                        this,
                        "Partita eliminata con successo",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                .addOnFailureListener { e: Exception? ->
                    Toast.makeText(
                        this,
                        "Errore durante l'eliminazione",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this, "Errore: Nessuna partita selezionata", Toast.LENGTH_SHORT).show()
        }
    }

    private fun avviaLive() {
        Toast.makeText(this, "Funzione live non ancora implementata", Toast.LENGTH_SHORT).show()
    }
}