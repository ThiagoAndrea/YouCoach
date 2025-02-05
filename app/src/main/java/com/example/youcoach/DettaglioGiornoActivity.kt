package com.example.youcoach

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*

class DettaglioGiornoActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private var allenamentoId: String? = null
    private lateinit var formattedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_giorno)
        setupBottomNavigation(R.id.nav_calendar)

        database = FirebaseDatabase.getInstance().reference

        val cardTraining: MaterialCardView = findViewById(R.id.card_training)
        val cardDate: TextView = findViewById(R.id.card_date)
        val cardTime: TextView? = findViewById(R.id.card_time)
        val backButton: ImageButton = findViewById(R.id.back_button)
        val presenzeButton: Button = findViewById(R.id.presenze_button)

        val selectedDay = intent.getStringExtra("selectedDay")
        val selectedMonth = intent.getIntExtra("selectedMonth", -1)
        val selectedYear = intent.getIntExtra("selectedYear", -1)

        formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay?.toIntOrNull() ?: 0)
        val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
        cardDate.text = date

        caricaAllenamento(formattedDate, cardTraining, cardTime)

        presenzeButton.setOnClickListener {
            caricaGiocatoriEApriDialog()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun caricaAllenamento(formattedDate: String, cardTraining: MaterialCardView, cardTime: TextView?) {
        database.child("Allenamenti").child(formattedDate)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val allenamentoSnapshot = snapshot.children.firstOrNull()
                        allenamentoSnapshot?.let {
                            allenamentoId = it.key
                            val orarioInizio = it.child("orarioInizio").value as? String ?: "N/A"
                            val orarioFine = it.child("orarioFine").value as? String ?: "N/A"

                            cardTraining.visibility = View.VISIBLE
                            cardTime?.text = "$orarioInizio - $orarioFine"
                        }
                    } else {
                        cardTraining.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DettaglioGiornoActivity, "Errore nel recupero dell'allenamento", Toast.LENGTH_SHORT).show()
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
}
