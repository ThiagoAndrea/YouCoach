package com.example.youcoach

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout



class FinePartitaActivity : BaseActivity() {


    private lateinit var partitaId: String
    private lateinit var dataPartita: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerEventi: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var db: DatabaseManager
    private lateinit var squadraCasa: TextView
    private lateinit var squadraOspite: TextView
    private lateinit var risultato: TextView
    private lateinit var esito: TextView
    private lateinit var btnAggiungiEvento: View
    private lateinit var btnModificaEvento: View
    private lateinit var btnEliminaEvento: View
    private lateinit var eventiContainer: ConstraintLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fine_partita)
        setupBottomNavigation(R.id.nav_stats)

        partitaId = intent.getStringExtra("PARTITA_ID") ?: ""
        dataPartita = intent.getStringExtra("DATA") ?: ""

        setUi()
        btnAggiungiEvento.setOnClickListener {
            (recyclerEventi.adapter as? EventoAdapter)?.apriDialogAggiungi()
        }
        btnModificaEvento.setOnClickListener{setModifica()}
        btnEliminaEvento.setOnClickListener{setEliminazione()}

        tabLayout.addTab(tabLayout.newTab().setText("Dettagli"))
        tabLayout.addTab(tabLayout.newTab().setText("Statistiche"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        recyclerEventi.visibility = View.VISIBLE
                        eventiContainer.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }

                    1 -> {
                        recyclerEventi.visibility = View.GONE
                        eventiContainer.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        db.getStatsPartita(partitaId, dataPartita) { statistiche ->
            recyclerView.adapter = StatisticaAdapter(statistiche)
        }
    }

    private fun setUi() {
        tabLayout = findViewById(R.id.tab_layout)
        recyclerView = findViewById(R.id.recycler_statistiche)
        recyclerEventi = findViewById(R.id.recycler_eventi)
        squadraCasa = findViewById(R.id.text_squadra_casa)
        squadraOspite = findViewById(R.id.text_squadra_ospite)
        risultato = findViewById(R.id.text_score)
        esito = findViewById(R.id.text_esito)
        btnAggiungiEvento = findViewById(R.id.btn_aggiungi_evento)
        btnModificaEvento = findViewById(R.id.btn_modifica_evento)
        btnEliminaEvento = findViewById(R.id.btn_elimina_evento)
        eventiContainer = findViewById(R.id.eventi_container)



        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerEventi.layoutManager = LinearLayoutManager(this)

        recyclerView.visibility = View.GONE
        recyclerEventi.visibility = View.VISIBLE

        db = DatabaseManager()

        db.getSquadraPrincipale { squadra, _ ->
            db.getPartita(dataPartita) { _, avversario, _, _, _, casa, _, _, _ ->

                if (casa == true) {
                    squadraCasa.text = Utils.troncaTesto(squadra, 13)
                    squadraOspite.text = Utils.troncaTesto(avversario, 13)
                } else {
                    squadraCasa.text = Utils.troncaTesto(avversario, 13)
                    squadraOspite.text = Utils.troncaTesto(squadra, 13)
                }

                db.getRisultatoPartita(partitaId, dataPartita) { esitoPartita, golCasa, golOspite ->
                    risultato.text = "$golCasa - $golOspite"
                    esito.text = esitoPartita
                }

                db.getEventi(partitaId, dataPartita) { eventi ->
                    val eventiOrdinati = eventi.sortedBy {
                        it.minutaggio.split(":").firstOrNull()?.toIntOrNull() ?: 0
                    }

                    recyclerEventi.adapter = EventoAdapter(eventiOrdinati.toMutableList(), dataPartita, partitaId, this, {aggiornaStatistiche()}, true)
                }
            }
        }
    }

    private fun setModifica(){
        (recyclerEventi.adapter as? EventoAdapter)?.toggleModalita(modifica = true, elimina = false)
    }

    private fun setEliminazione(){
        (recyclerEventi.adapter as? EventoAdapter)?.toggleModalita(modifica = false, elimina = true)
    }

    private fun aggiornaStatistiche() {
        val manager = StatsManager(partitaId, dataPartita)

        val punteggio = risultato.text.split("-")
        val golCasa = punteggio.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val golOspite = punteggio.getOrNull(1)?.trim()?.toIntOrNull() ?: 0

        manager.salvaStats(golCasa, golOspite) { success ->
            if (success) {
                db.getStatsPartita(partitaId, dataPartita) { stats ->
                    recyclerView.adapter = StatisticaAdapter(stats)
                }
            }
        }
    }


}
