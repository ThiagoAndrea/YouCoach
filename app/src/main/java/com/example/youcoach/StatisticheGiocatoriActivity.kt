package com.example.youcoach

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout

class StatisticheGiocatoriActivity : BaseActivity() {

    private lateinit var barChart: BarChart
    private lateinit var spinnerMese: Spinner
    private lateinit var spinnerGiocatore: Spinner
    private lateinit var db: DatabaseManager
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutAllenamenti : androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var layoutPartite : androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var spinnerMesePartita: Spinner
    private lateinit var spinnerPartita: Spinner
    private lateinit var chipGroup: ChipGroup
    private lateinit var backButton: ImageButton
    private var listaPartite: List<Partita> = listOf()
    private var listaGiocatori: List<Giocatore> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats_giocatori)
        setupBottomNavigation(R.id.nav_stats)

        setUI()
        setUpTabs()
        backButton.setOnClickListener {
            finish()
        }
        setupSpinner()
        setupListeners()
        caricaPartite()
    }

    private fun setUI() {
        spinnerMese = findViewById(R.id.spinnerMese)
        spinnerGiocatore = findViewById(R.id.spinnerGiocatore)
        barChart = findViewById(R.id.barChartPresenze)
        tabLayout = findViewById(R.id.tab_layout)
        layoutAllenamenti = findViewById(R.id.layoutAllenamenti)
        layoutPartite = findViewById(R.id.layoutPartite)
        spinnerMesePartita = findViewById(R.id.spinnerMesePartita)
        spinnerPartita = findViewById(R.id.spinnerPartita)
        chipGroup = findViewById(R.id.chipGroup)
        db = DatabaseManager()
        backButton = findViewById(R.id.back_button)


    }

    private fun setUpTabs(){
        tabLayout.addTab(tabLayout.newTab().setText("Allenamenti"))
        tabLayout.addTab(tabLayout.newTab().setText("Partite"))
        layoutAllenamenti.visibility = View.VISIBLE
        layoutPartite.visibility = View.GONE
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        layoutAllenamenti.visibility = View.VISIBLE
                        layoutPartite.visibility = View.GONE
                    }
                    1 -> {
                        layoutAllenamenti.visibility = View.GONE
                        layoutPartite.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

    }

    private fun setupSpinner() {
        db.getGiocatori { giocatori ->
            listaGiocatori = giocatori
            val nomiCompleti = mutableListOf("").apply {
                addAll(giocatori.map { "${it.nome} ${it.cognome}" })
            }
            spinnerGiocatore.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomiCompleti).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        val mesi = listOf("Tutti", "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno", "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre")
        spinnerMese.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mesi).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun setupListeners() {
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = refreshStatisticheGiocatore()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerGiocatore.onItemSelectedListener = listener
        spinnerMese.onItemSelectedListener = listener
        spinnerMesePartita.onItemSelectedListener = listener
        spinnerPartita.onItemSelectedListener = listener

        chipGroup.setOnCheckedStateChangeListener { _, _ -> refreshStatisticheGiocatore() }
    }

    private fun refreshStatisticheGiocatore() {
        val posizioneGiocatore = spinnerGiocatore.selectedItemPosition
        if (posizioneGiocatore <= 0 || listaGiocatori.isEmpty() || posizioneGiocatore - 1 >= listaGiocatori.size) return

        val giocatore = listaGiocatori[posizioneGiocatore - 1]
        val idGiocatore = giocatore.id
        val tabAttivo = tabLayout.selectedTabPosition

        if (tabAttivo == 0) {
            val mese = spinnerMese.selectedItem?.toString() ?: "Tutti"

            db.getStatsAllenamentiPerMese(idGiocatore, mese) { presenze, assenze, ritardi, infortuni ->
                val totali = presenze + assenze + ritardi + infortuni
                val presenti = presenze + ritardi
                aggiornaBarChartAllenamenti(totali, presenti, ritardi, assenze, infortuni)
            }

        } else {
            val mese = spinnerMesePartita.selectedItem?.toString() ?: "Tutti"
            val avversario = spinnerPartita.selectedItem?.toString() ?: "Tutte"
            val competizioni = getCompetizioniSelezionate()

            db.calcolaStatisticheFiltratePerGiocatore(idGiocatore, mese, competizioni, avversario, listaPartite) {
                    convocazioni, titolari, subentrati, usciti, infortuni, minuti, gol, assist, tiri, falliFatti, falliSubiti, gialli, rossi, fuorigioco ->
                aggiornaStatsUI(convocazioni, titolari, subentrati, usciti, infortuni, minuti, gol, assist, tiri, falliFatti, falliSubiti, gialli, rossi, fuorigioco)
            }
        }
    }

    private fun aggiornaBarChartAllenamenti(totali: Int, presenti: Int, ritardi: Int, assenti: Int, infortunati: Int) {
        val entries = listOf(
            BarEntry(0f, totali.toFloat()),
            BarEntry(1f, presenti.toFloat()),
            BarEntry(2f, ritardi.toFloat()),
            BarEntry(3f, assenti.toFloat()),
            BarEntry(4f, infortunati.toFloat())
        )

        val labels = listOf("Tot", "P", "R", "A", "I")
        val colors = listOf(Color.LTGRAY, Color.GREEN, Color.YELLOW, Color.RED, Color.MAGENTA)

        val dataSet = BarDataSet(entries, "").apply {
            setColors(colors)
            valueTextSize = 14f
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.4f
        barData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        })

        barChart.data = barData
        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            granularity = 1f
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
        }

        barChart.axisLeft.apply {
            setDrawGridLines(false)
            granularity = 1f
            isGranularityEnabled = true
            axisMinimum = 0f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.invalidate()
        barChart.legend.isEnabled = false
    }

    private fun getCompetizioniSelezionate(): List<String> {
        val selezionate = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) selezionate.add(chip.text.toString())
        }
        return selezionate
    }

    private fun aggiornaStatsUI(
        convocazioni: Int, titolari: Int, subentrati: Int, usciti: Int, infortuni: Int,
        minuti: Int, gol: Int, assist: Int, tiri: Int, falliFatti: Int,
        falliSubiti: Int, gialli: Int, rossi: Int, fuorigioco: Int
    ) {
        findViewById<TextView>(R.id.numConvocazioniTot).text = convocazioni.toString()
        findViewById<TextView>(R.id.textTitolare).text = titolari.toString()
        findViewById<TextView>(R.id.numEntrante).text = subentrati.toString()
        findViewById<TextView>(R.id.numUscente).text = usciti.toString()
        findViewById<TextView>(R.id.numInfortuni).text = infortuni.toString()
        findViewById<TextView>(R.id.numMinuti).text = minuti.toString()
        findViewById<TextView>(R.id.numGol).text = gol.toString()
        findViewById<TextView>(R.id.numAssist).text = assist.toString()
        findViewById<TextView>(R.id.numTiri).text = tiri.toString()
        findViewById<TextView>(R.id.numFalliFatti).text = falliFatti.toString()
        findViewById<TextView>(R.id.numFalliSubiti).text = falliSubiti.toString()
        findViewById<TextView>(R.id.numGialli).text = gialli.toString()
        findViewById<TextView>(R.id.numRossi).text = rossi.toString()
        findViewById<TextView>(R.id.numFuorigioco).text = fuorigioco.toString()
    }

    private fun aggiornaSpinnerPartite(meseSelezionato: String) {
        val competizioniSelezionate = getCompetizioniSelezionate()
        val partiteFiltrate = listaPartite.filter {
            (meseSelezionato == "Tutti" || Utils.estraiMese(it.data) == meseSelezionato) &&
                    (competizioniSelezionate.isEmpty() || competizioniSelezionate.contains(it.competizione))
        }
        val nomiAvversari = listOf("Tutte") + partiteFiltrate.map { it.avversario }
        spinnerPartita.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomiAvversari).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun caricaPartite() {
        db.getPartite { partite ->
            listaPartite = partite
            val mesiDisponibili = listaPartite.mapNotNull { Utils.estraiMese(it.data) }.distinct().sortedBy { Utils.indiceMese(it) }
            spinnerMesePartita.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Tutti") + mesiDisponibili).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            aggiornaSpinnerPartite("Tutti")
        }
    }


}
