package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text

class RosaActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var giocatoreAdapter: GiocatoreAdapter
    private lateinit var nomeSquadra: TextView
    private val playerList = mutableListOf<Giocatore>()
    private val db = DatabaseManager()
    private lateinit var dettaglioGiocatoreLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rosa)
        setupBottomNavigation(R.id.nav_rosa)

        recyclerView = findViewById(R.id.recyclerGiocatore)
        nomeSquadra = findViewById(R.id.nome_squadra)
        db.getSquadraPrincipale{nome, _ ->
            nomeSquadra.text = Utils.troncaTesto(nome, 11)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)

        giocatoreAdapter = GiocatoreAdapter(playerList)
        recyclerView.adapter = giocatoreAdapter


        dettaglioGiocatoreLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                caricaGiocatori()
            }
        }

        val buttonAggiungiGiocatore = findViewById<ImageButton>(R.id.add_player_button)
        buttonAggiungiGiocatore.setOnClickListener {
            val intent = Intent(this, AggiungiGiocatoreActivity::class.java)
            dettaglioGiocatoreLauncher.launch(intent)
        }
        caricaGiocatori()
    }

    override fun onResume() {
        super.onResume()
        caricaGiocatori()
    }

    private fun caricaGiocatori() {
        db.getGiocatori { giocatoriList ->
            playerList.clear()
            playerList.addAll(giocatoriList)
            giocatoreAdapter.notifyDataSetChanged()
        }
    }

}