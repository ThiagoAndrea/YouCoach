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

    private lateinit var database: DatabaseReference
    private var allenamentoId: String? = null
    private lateinit var formattedDate: String

    // UI Components
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

    // RecyclerView for Obiettivi
    private lateinit var recyclerViewObiettivi: RecyclerView
    private lateinit var obiettiviAdapter: ObiettiviAdapter
    private lateinit var obiettiviList: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_allenamento)
        setupBottomNavigation(R.id.nav_calendar)

        database = FirebaseDatabase.getInstance().reference

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
        database.child("Allenamenti").child(formattedDate)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val allenamentoSnapshot = snapshot.children.firstOrNull()
                        allenamentoSnapshot?.let {
                            allenamentoId = it.key
                            val orarioInizio = it.child("orarioInizio").value as? String ?: "N/A"
                            val orarioFine = it.child("orarioFine").value as? String ?: "N/A"
                            val obiettivi = it.child("obiettivi").children.mapNotNull { obiettivo ->
                                obiettivo.getValue(String::class.java)
                            }

                            cardTime.text = "$orarioInizio - $orarioFine"

                            obiettiviList.clear()
                            obiettiviList.addAll(obiettivi)
                            obiettiviAdapter.notifyDataSetChanged()
                        }
                    } else {
                        cardTraining.visibility = View.GONE
                        recyclerViewObiettivi.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DettaglioAllenamentoActivity, "Errore nel recupero dell'allenamento", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun caricaGiocatoriEApriDialog() {
        val giocatoriRef = database.child("Giocatori")

        giocatoriRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(giocatoriSnapshot: DataSnapshot) {
                if (giocatoriSnapshot.exists()) {
                    val giocatori = giocatoriSnapshot.children.mapNotNull { it.getValue(Giocatore::class.java) }

                    val presenzeRef = database.child("Allenamenti").child(formattedDate).child(allenamentoId ?: "").child("presenze")
                    presenzeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(presenzeSnapshot: DataSnapshot) {
                            val presenzeIniziali = mutableMapOf<String, Int>()
                            presenzeSnapshot.children.forEach {
                                val playerId = it.key ?: ""
                                val stato = it.value.toString().toIntOrNull() ?: 0
                                presenzeIniziali[playerId] = stato
                            }

                            aggiornaNumeriGiocatori(presenzeIniziali)
                            apriDialogPresenze(giocatori, presenzeIniziali)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun apriDialogPresenze(giocatori: List<Giocatore>, presenzeIniziali: Map<String, Int>) {
        val dialog = PresenzeDialogFragment(giocatori, presenzeIniziali) { presenzeConfermate ->
            salvaPresenzeNelDatabase(presenzeConfermate)
        }
        dialog.show(supportFragmentManager, "PresenzeDialogFragment")
    }

    private fun salvaPresenzeNelDatabase(presenzeConfermate: Map<String, Int>) {
        val presenzeRef = database.child("Allenamenti").child(formattedDate).child(allenamentoId ?: "").child("presenze")
        presenzeRef.setValue(presenzeConfermate)
            .addOnSuccessListener { Toast.makeText(this, "Presenze aggiornate!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Errore!", Toast.LENGTH_SHORT).show() }
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
            database.child("Allenamenti").child(formattedDate).child(allenamentoId!!)
                .removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Allenamento eliminato con successo", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore durante l'eliminazione", Toast.LENGTH_SHORT).show()
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