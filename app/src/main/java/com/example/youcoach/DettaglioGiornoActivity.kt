package com.example.youcoach

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class DettaglioGiornoActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private var allenamento: Allenamento? = null
    private lateinit var allenamentoId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_giorno)
        setupBottomNavigation(R.id.nav_calendar)

        // Inizializza il database Firebase
        database = Firebase.database.reference

        // Riferimenti alle view
        val cardTraining: MaterialCardView = findViewById(R.id.card_training)
        val cardDate: TextView = findViewById(R.id.card_date)
        val cardTime: TextView? = findViewById(R.id.card_time)
        val backButton: ImageButton = findViewById(R.id.back_button)
        val presenzeButton: Button = findViewById(R.id.presenze_button)

        // Ottieni il giorno selezionato dall'Intent
        val selectedDay = intent.getStringExtra("selectedDay")
        val selectedMonth = intent.getIntExtra("selectedMonth", -1)
        val selectedYear = intent.getIntExtra("selectedYear", -1)

        // Formatta la data nel formato "YYYY-MM-DD" per il database
        val formattedDate = String.format(
            "%04d-%02d-%02d",
            selectedYear,
            selectedMonth + 1,
            selectedDay?.toIntOrNull() ?: 0
        )
        val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
        cardDate.text = date

        Log.d("DettaglioGiornoActivity", "Data selezionata: $date")
        Log.d("DettaglioGiornoActivity", "Data formattata per il database: $formattedDate")

        // Recupera i dati dell'allenamento
        caricaAllenamento(formattedDate, cardTraining, cardTime)

        // Gestione del pulsante per le presenze
        presenzeButton.setOnClickListener {
            caricaGiocatoriEApriDialog(formattedDate)
        }

        // Gestisci il clic sul pulsante "Indietro"
        backButton.setOnClickListener {
            Log.d("DettaglioGiornoActivity", "Pulsante Indietro cliccato")
            finish()
        }
    }

    private fun caricaAllenamento(
        formattedDate: String,
        cardTraining: MaterialCardView,
        cardTime: TextView?
    ) {
        database.child("Allenamenti").child(formattedDate)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) {
                        return // Esci se l'activity è in fase di distruzione
                    }

                    Log.d("DettaglioGiornoActivity", "Dati recuperati dal database: ${snapshot.value}")
                    if (snapshot.exists()) {
                        val allenamentoSnapshot = snapshot.children.firstOrNull()
                        allenamentoSnapshot?.let {
                            allenamento = it.getValue(Allenamento::class.java)
                            if (allenamento != null) {
                                cardTraining.visibility = View.VISIBLE
                                val cardTimeString =
                                    "Orario: ${allenamento!!.orarioInizio} - ${allenamento!!.orarioFine}"
                                cardTime?.text = cardTimeString
                                allenamentoId = allenamento!!.id
                                Log.d("DettaglioGiornoActivity", "Allenamento trovato: $allenamento")
                            } else {
                                Log.e("DettaglioGiornoActivity", "Errore: dati allenamento non validi.")
                            }
                        }
                    } else {
                        cardTraining.visibility = View.GONE
                        Log.d("DettaglioGiornoActivity", "Nessun allenamento trovato per la data.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DettaglioGiornoActivity", "Errore nel recupero dell'allenamento: ${error.message}")
                    Toast.makeText(
                        this@DettaglioGiornoActivity,
                        "Errore nel recupero dell'allenamento",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun caricaGiocatoriEApriDialog(formattedDate: String) {
        database.child("Giocatori").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val giocatori = snapshot.children.mapNotNull { giocatoreSnapshot ->
                    val id = giocatoreSnapshot.key
                    val nome = giocatoreSnapshot.child("nome").getValue(String::class.java)
                    val cognome = giocatoreSnapshot.child("cognome").getValue(String::class.java)
                    if (id != null && nome != null && cognome != null) {
                        Giocatore(id, nome, cognome)
                    } else {
                        null
                    }
                }

                if (giocatori.isNotEmpty()) {
                    val dialog = PresenzeDialogFragment(giocatori) { presenzeMap ->
                        salvaPresenze(formattedDate, presenzeMap)
                    }
                    dialog.show(supportFragmentManager, "PresenzeDialogFragment")
                } else {
                    Toast.makeText(this@DettaglioGiornoActivity, "Nessun giocatore trovato.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DettaglioGiornoActivity, "Errore nel recupero dei giocatori", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun salvaPresenze(formattedDate: String, presenzeMap: Map<String, Int>) {
        database.child("Allenamenti").child(formattedDate).child(allenamentoId).child("presenze")
            .setValue(presenzeMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Presenze aggiornate", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore durante il salvataggio delle presenze", Toast.LENGTH_SHORT).show()
                Log.e("DettaglioGiornoActivity", "Errore: ${e.message}")
            }
    }
}
