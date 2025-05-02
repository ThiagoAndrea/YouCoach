package com.example.youcoach

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.card.MaterialCardView

class DettaglioPartitaActivity : BaseActivity() {
    private var idPartita: String? = null
    private lateinit var data_partita: String

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
    private lateinit var offlineButton: Button
    private lateinit var numConvocati: TextView

    private val db = DatabaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_partita)
        setupBottomNavigation(R.id.nav_calendar)

        initUI()
        loadSelectedDate()
        caricaPartita(data_partita)
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
        offlineButton = findViewById(R.id.btn_offline)
        editButton = findViewById(R.id.modificaPartita_button)
        deleteButton = findViewById(R.id.eliminaPartita_button)
        numConvocati = findViewById(R.id.num_convocati)
    }

    private fun loadSelectedDate() {
        val selectedDay = intent.getStringExtra("selectedDay")
        val selectedMonth = intent.getIntExtra("selectedMonth", -1)
        val selectedYear = intent.getIntExtra("selectedYear", -1)
        data_partita = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay?.toIntOrNull() ?: 0)
        val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
        cardDate.text = date
    }

    private fun setupButtonListeners() {

        backButton.setOnClickListener {finish()}
        editButton.setOnClickListener{modificaPartita()}
        deleteButton.setOnClickListener{confermaEliminazione()}
        convocatiButton.setOnClickListener{caricaGiocatoriEApriDialog()}
        idPartita?.let {
            db.getGiocata(it, data_partita){ giocata ->
                if(!giocata) {
                    goLiveButton.text = "VAI AL LIVE"
                    goLiveButton.setOnClickListener { goLive() }
                    offlineButton.visibility = View.VISIBLE
                    offlineButton.setOnClickListener { offlineMode() }
                }
                else{
                    goLiveButton.text="Dettaglio"
                    goLiveButton.setOnClickListener{vaiAlDettaglio()}
                    offlineButton.visibility = View.GONE

                }
            }
        }


    }

    private fun goLive() {
        val intent = Intent(this, FormazioneActivity::class.java)
        intent.putExtra("PARTITA_ID", idPartita)
        intent.putExtra("DATA", data_partita)
        intent.putExtra("LIVE", true)
        startActivity(intent)
    }

    private fun vaiAlDettaglio(){
        val intent = Intent(this, FinePartitaActivity::class.java)
        intent.putExtra("PARTITA_ID", idPartita)
        intent.putExtra("DATA", data_partita)
        startActivity(intent)
    }

    private fun offlineMode() {
        val intent = Intent(this, FormazioneActivity::class.java)
        intent.putExtra("PARTITA_ID", idPartita)
        intent.putExtra("DATA", data_partita)
        intent.putExtra("LIVE", false)
        startActivity(intent)

    }

    private fun caricaPartita(formattedDate: String) {
        db.getPartita(formattedDate) { idPartita, avversario, orario, luogo, competizione, casa, minutiPerTempo, numeroTempi, numeroCalciatori ->
            this.idPartita = idPartita
            setupButtonListeners()
            if (avversario != null && orario != null && luogo != null) {
                if (casa == true) {
                    cardTitle.text = "Bedizzole U16 - $avversario"
                } else {
                    cardTitle.text = "$avversario - Bedizzole U16"
                }
                cardTime.text = orario
                matchLocation.text = luogo
                durataTempo.text = "Durata per tempo: $minutiPerTempo"
                numGiocatori.text = "Giocatori: $numeroCalciatori"
                numTempi.text = "Tempi: $numeroTempi"

                when (competizione) {
                    "Campionato" -> competitionIcon.setImageResource(R.drawable.campionato)
                    "Coppa" -> competitionIcon.setImageResource(R.drawable.coppa)
                    else -> competitionIcon.setImageResource(R.drawable.fair_play)
                }

                db.getConvocatiMap(formattedDate, this.idPartita
                    ?: "") { convocati ->
                    aggiornaNumeriConvocati(convocati)
                }
            } else {
                cardMatch.visibility = View.GONE
            }
        }
    }

    private fun caricaGiocatoriEApriDialog() {
        if (idPartita != null) {
            db.getGiocatori { giocatori ->
                db.getConvocatiMap(data_partita, idPartita!!) { convocati ->
                    apriDialogConvocati(giocatori, convocati)
                }
            }
        } else {
            Toast.makeText(this, "Errore: Partita o data mancanti", Toast.LENGTH_SHORT).show()
        }
    }

    private fun apriDialogConvocati(giocatori: List<Giocatore>, convocazioniIniziali: Map<String, Boolean>) {
        val dialog = ConvocazioniDialogFragment(giocatori, convocazioniIniziali) { convocazioniIniziali ->
            salvaConvocati(convocazioniIniziali)
        }
        dialog.show(supportFragmentManager, "ConvocazioniDialogFragment")
    }

    private fun salvaConvocati(convocazioniConfermate: Map<String, Boolean>) {
        if (idPartita != null) {
            db.aggiungiConvocatiPartita(idPartita!!, data_partita, convocazioniConfermate) { success, message ->
                if (success) {
                    aggiornaNumeriConvocati(convocazioniConfermate)
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Errore: Dati della partita mancanti", Toast.LENGTH_SHORT).show()
        }
    }

    private fun modificaPartita() {
        if (idPartita != null) {
            val intent = Intent(this, AggiungiPartitaActivity::class.java)
            intent.putExtra("PARTITA_ID", idPartita)
            intent.putExtra("DATA", data_partita)
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
        if (idPartita != null) {
            db.eliminaPartita(data_partita ?: "", idPartita!!) { success, message ->
                if (success) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Errore: Nessuna partita selezionata", Toast.LENGTH_SHORT).show()
        }
    }

    private fun aggiornaNumeriConvocati(convocazioni: Map<String, Boolean>) {
        var convocati = 0

        convocazioni.values.forEach { stato ->
            if (stato)
                convocati++
        }
        numConvocati.text = convocati.toString()
    }

}