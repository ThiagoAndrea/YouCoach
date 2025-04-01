package com.example.youcoach

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout


class FinePartitaActivity : BaseActivity() {


    private lateinit var partitaId: String
    private lateinit var dataPartita: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fine_partita)

        partitaId = intent.getStringExtra("PARTITA_ID") ?: ""
        dataPartita = intent.getStringExtra("DATA") ?: ""

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val textDettagli = findViewById<TextView>(R.id.text_dettagli)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_statistiche)

        val listaStatistiche = listOf(
            Statistica("Goal previsti (xG)", "1.10", "0.39", 74f, 26f),
            Statistica("Possesso Palla", "59%", "41%", 59f, 41f),
            Statistica("Tiri totali", "16", "7", 69f, 31f),
            Statistica("Tiri in Porta", "6", "2", 75f, 25f),
            Statistica("Grandi occasioni", "3", "1", 75f, 25f),
            Statistica("Calci d’angolo", "5", "3", 63f, 37f),
            Statistica("Cartellini Gialli", "2", "4", 33f, 67f)
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = StatisticaAdapter(listaStatistiche)
        recyclerView.visibility = View.GONE

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
    }
}
