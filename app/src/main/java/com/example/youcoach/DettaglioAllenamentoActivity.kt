package com.example.youcoach

import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*

class DettaglioAllenamentoActivity : BaseActivity() {

    private var allenamentoId: String? = null
    private lateinit var formattedDate: String
    private val db = DatabaseManager()

    private lateinit var cardTraining: MaterialCardView
    private lateinit var cardDate: TextView
    private lateinit var cardTime: TextView
    private lateinit var backButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var editButton: ImageButton
    private lateinit var presenzeButton: Button
    private lateinit var numPresenze: TextView
    private lateinit var numRitardi: TextView
    private lateinit var numAssenze: TextView
    private lateinit var numInfortunati: TextView

    private lateinit var recyclerViewObiettivi: RecyclerView
    private lateinit var obiettiviAdapter: ObiettiviAdapter
    private lateinit var obiettiviList: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_allenamento)
        setupBottomNavigation(R.id.nav_calendar)
        initUI()
        loadSelectedDate()
        caricaAllenamento(formattedDate)
        setupButtonListeners()
    }

    private fun initUI() {
        cardTraining = findViewById(R.id.card_training)
        cardDate = findViewById(R.id.card_date)
        cardTime = findViewById(R.id.card_time)
        backButton = findViewById(R.id.back_button)
        deleteButton = findViewById(R.id.eliminaAllenamento_button)
        editButton = findViewById(R.id.modificaAllenamento_button)
        presenzeButton = findViewById(R.id.presenze_button)
        numPresenze = findViewById(R.id.num_presenze)
        numRitardi = findViewById(R.id.num_ritardi)
        numAssenze = findViewById(R.id.num_assenze)
        numInfortunati = findViewById(R.id.num_infortunati)
        recyclerViewObiettivi = findViewById(R.id.recyclerViewObiettivi)
        recyclerViewObiettivi.layoutManager = LinearLayoutManager(this)
        obiettiviList = mutableListOf()
        obiettiviAdapter = ObiettiviAdapter(obiettiviList, false)
        recyclerViewObiettivi.adapter = obiettiviAdapter
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
        backButton.setOnClickListener { finish() }

        presenzeButton.setOnClickListener {
            caricaGiocatoriEApriDialog()
        }
        editButton.setOnClickListener{modificaAllenamento()}

        deleteButton.setOnClickListener{confermaEliminazione()}
    }

    private fun caricaAllenamento(formattedDate: String) {
        db.getAllenamento(formattedDate){
            allenamentoID, orarioInizio, orarioFine, obiettivi ->
            if(orarioInizio != null && orarioFine != null){
                allenamentoId = allenamentoID
                cardTraining.visibility = View.VISIBLE
                recyclerViewObiettivi.visibility = View.VISIBLE
                cardTime.text="$orarioInizio - $orarioFine"

                obiettiviList.clear()
                if (obiettivi != null) {
                    obiettiviList.addAll(obiettivi)
                }
                obiettiviAdapter.notifyDataSetChanged()
            } else {
                cardTraining.visibility = View.GONE
                recyclerViewObiettivi.visibility = View.GONE
                Toast.makeText(this, "Nessun allenamento trovato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun caricaGiocatoriEApriDialog() {
        db.getGiocatori { giocatori ->
            db.getPresenzePerAllenamento(formattedDate, allenamentoId ?: "") { presenzeIniziali ->
                aggiornaNumeriGiocatori(presenzeIniziali)
                apriDialogPresenze(giocatori, presenzeIniziali)
            }
        }
    }

    private fun apriDialogPresenze(giocatori: List<Giocatore>, presenzeIniziali: Map<String, Int>) {
        val dialog = PresenzeDialogFragment(giocatori, presenzeIniziali) { presenzeConfermate ->
            salvaPresenzeNelDatabase(presenzeConfermate)
        }
        dialog.show(supportFragmentManager, "PresenzeDialogFragment")
    }

    private fun salvaPresenzeNelDatabase(presenzeConfermate: Map<String, Int>) {
        if(allenamentoId != null) {
            db.aggiungiPresenzeAllenamento(formattedDate, allenamentoId!!, presenzeConfermate) { success, message ->
                if (success) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun modificaAllenamento() {
        if (allenamentoId != null) {
            val intent = Intent(this, AggiungiAllenamentoActivity::class.java).apply {
                putExtra("ALLENAMENTO_ID", allenamentoId)
                putExtra("DATA", formattedDate)
                putExtra("ORARIO_INIZIO", cardTime.text.toString().split(" - ")[0])
                putExtra("ORARIO_FINE", cardTime.text.toString().split(" - ")[1])
                putStringArrayListExtra("OBIETTIVI", ArrayList(obiettiviList))
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Errore: Nessun allenamento selezionato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confermaEliminazione() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Conferma eliminazione")
            .setMessage("Sei sicuro di voler eliminare questo allenamento?")
            .setPositiveButton("Elimina") { _, _ -> eliminaAllenamento() }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun eliminaAllenamento() {
        if (allenamentoId != null) {
            db.eliminaAllenamento(formattedDate, allenamentoId!!) { success, message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (success) {
                    finish()
                }
            }
        } else {
            Toast.makeText(this, "Errore: Nessun allenamento selezionato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun aggiornaNumeriGiocatori(presenze: Map<String, Int>) {
        var presenti = 0
        var ritardi = 0
        var assenti = 0
        var infortunati = 0

        presenze.values.forEach { stato ->
            when (stato) {
                1 -> presenti++
                2 -> ritardi++
                3 -> assenti++
                4 -> infortunati++
            }
        }
        numPresenze.text = presenti.toString()
        numRitardi.text = ritardi.toString()
        numAssenze.text = assenti.toString()
        numInfortunati.text = infortunati.toString()
    }
}