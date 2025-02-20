package com.example.youcoach

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RosaActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var giocatoreAdapter: GiocatoreAdapter
    private val playerList = mutableListOf<Giocatore>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rosa)
        setupBottomNavigation(R.id.nav_rosa)

        database = Firebase.database.reference

        recyclerView = findViewById(R.id.recyclerGiocatore)
        recyclerView.layoutManager = LinearLayoutManager(this)

        giocatoreAdapter = GiocatoreAdapter(playerList)
        recyclerView.adapter = giocatoreAdapter

        loadPlayersFromFirebase()

        val buttonAggiungiGiocatore = findViewById<ImageButton>(R.id.add_player_button)
        buttonAggiungiGiocatore.setOnClickListener {
            val intent = Intent(this, AggiungiGiocatoreActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadPlayersFromFirebase() {
        val giocatoriRef = database.child("Giocatori") // Riferimento al nodo "Giocatori"
        giocatoriRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                playerList.clear() // Pulisce la lista esistente

                for (data in snapshot.children) {
                    val giocatore = data.getValue(Giocatore::class.java)
                    if (giocatore != null) {
                        Log.d("RosaActivity", "Giocatore caricato: ${giocatore.nome} ${giocatore.cognome}")
                        playerList.add(giocatore)
                    }
                }

                // Ordinamento dopo il caricamento dei dati
                val ruoloPriority = mapOf(
                    "portiere" to 1,
                    "difensore" to 2,
                    "centrocampista" to 3,
                    "attaccante" to 4
                )

                playerList.sortWith(compareBy(
                    { ruoloPriority[it.ruolo.lowercase()] ?: 99 }, // Default a 99 per ruoli sconosciuti
                    { it.nome }
                ))

                giocatoreAdapter.notifyDataSetChanged() // Notifica l'aggiornamento
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RosaActivity", "Errore nel caricamento dei dati: ${error.message}")
            }
        })
    }
}
