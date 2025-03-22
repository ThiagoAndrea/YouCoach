package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RosaActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var giocatoreAdapter: GiocatoreAdapter
    private val playerList = mutableListOf<Giocatore>()
    private val db = DatabaseManager()
    private lateinit var dettaglioGiocatoreLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rosa)
        setupBottomNavigation(R.id.nav_rosa)

        recyclerView = findViewById(R.id.recyclerGiocatore)
        recyclerView.layoutManager = LinearLayoutManager(this)

        giocatoreAdapter = GiocatoreAdapter(playerList)
        recyclerView.adapter = giocatoreAdapter


        dettaglioGiocatoreLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                loadGiocatoriOrdinati()
            }
        }

        val buttonAggiungiGiocatore = findViewById<ImageButton>(R.id.add_player_button)
        buttonAggiungiGiocatore.setOnClickListener {
            val intent = Intent(this, AggiungiGiocatoreActivity::class.java)
            dettaglioGiocatoreLauncher.launch(intent)
        }
        loadGiocatoriOrdinati()
    }

    override fun onResume() {
        super.onResume()
        loadGiocatoriOrdinati()
    }

    private fun loadGiocatoriOrdinati() {
        db.getGiocatori { giocatoriList ->
            playerList.clear()
            if (giocatoriList.isNotEmpty()) {
                for (giocatore in giocatoriList) {
                    playerList.add(giocatore)
                }

                val ruoloPriority = mapOf(
                    "portiere" to 1,
                    "difensore" to 2,
                    "centrocampista" to 3,
                    "attaccante" to 4
                )

                playerList.sortWith(compareBy(
                    { ruoloPriority[it.ruolo.lowercase()] ?: 99 },
                    { it.cognome }
                ))
                giocatoreAdapter.notifyDataSetChanged()
            }
        }
    }
}