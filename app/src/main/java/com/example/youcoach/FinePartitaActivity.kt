package com.example.youcoach

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout



class FinePartitaActivity : BaseActivity() {


    private lateinit var partitaId: String
    private lateinit var dataPartita: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var textDettagli: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var db: DatabaseManager
    private lateinit var squadraCasa: TextView
    private lateinit var squadraOspite: TextView
    private lateinit var risultato: TextView
    private lateinit var esito: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fine_partita)
        setupBottomNavigation(R.id.nav_stats)

        partitaId = intent.getStringExtra("PARTITA_ID") ?: ""
        dataPartita = intent.getStringExtra("DATA") ?: ""

        setUi()

        tabLayout.addTab(tabLayout.newTab().setText("Dettagli"))
        tabLayout.addTab(tabLayout.newTab().setText("Statistiche"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        textDettagli.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }

                    1 -> {
                        textDettagli.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        db.getStatsPartita(partitaId, dataPartita) { statistiche ->
            Log.d("DEBUG", "Statistiche trovate: ${statistiche.size}")
            recyclerView.adapter = StatisticaAdapter(statistiche)
        }
    }

    private fun setUi(){
        tabLayout = findViewById(R.id.tab_layout)
        textDettagli = findViewById(R.id.text_dettagli)
        recyclerView = findViewById(R.id.recycler_statistiche)
        squadraCasa = findViewById(R.id.text_squadra_casa)
        squadraOspite = findViewById(R.id.text_squadra_ospite)
        risultato = findViewById(R.id.text_score)
        esito = findViewById(R.id.text_esito)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.visibility = View.GONE
        db = DatabaseManager()

        db.getSquadraPrincipale{squadra, _ ->
            db.getPartita(dataPartita){_, avversario, _, _, _, casa, _, _, _ ->
                if(casa==true) {
                    squadraCasa.text = Utils.troncaTesto(squadra, 13)
                    squadraOspite.text = Utils.troncaTesto(avversario, 13)
                }
                else {
                    squadraCasa.text = Utils.troncaTesto(avversario, 13)
                    squadraOspite.text = Utils.troncaTesto(squadra, 13)
                }
                db.getRisultatoPartita(partitaId, dataPartita){esitoPartita, golCasa, golOspite ->
                    risultato.text = "$golCasa - $golOspite"
                    esito.text = esitoPartita
                }

            }
        }
    }
}
