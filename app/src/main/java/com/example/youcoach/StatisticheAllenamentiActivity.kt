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
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView

class StatisticheAllenamentiActivity : BaseActivity() {

    private lateinit var spinnerMese: Spinner
    private lateinit var spinnerObiettivo: Spinner
    private lateinit var barChartAllenamenti: BarChart
    private lateinit var pieChartObiettivo: PieChart
    private lateinit var backButton: ImageButton
    private val db = DatabaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats_allenamenti)
        setupBottomNavigation(R.id.nav_stats)

        spinnerMese = findViewById(R.id.spinnerMese)
        spinnerObiettivo = findViewById(R.id.spinnerObiettivo)
        barChartAllenamenti = findViewById(R.id.barChartAllenamenti)
        pieChartObiettivo = findViewById(R.id.pieChartObiettivo)
        backButton = findViewById(R.id.back_button)

        backButton.setOnClickListener {
            finish()
        }

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        val mesi = listOf("Tutti", "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre")

        spinnerMese.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mesi)

        db.getObiettivi { obiettiviList, _ ->
            val obiettiviAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Tutti") + obiettiviList)
            spinnerObiettivo.adapter = obiettiviAdapter
        }
    }

    private fun setupListeners() {
        spinnerMese.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateCharts()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerObiettivo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateCharts()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

    }

    private fun updateCharts() {
        val mese = if (spinnerMese.selectedItemPosition == 0) null else spinnerMese.selectedItem.toString()
        val obiettivo = if (spinnerObiettivo.selectedItemPosition == 0) null else spinnerObiettivo.selectedItem.toString()

        db.getStatisticheAllenamentiAggregate(mese, obiettivo) { presenzeMap, obiettiviMap ->
            val top5 = presenzeMap.entries
                .sortedByDescending { it.value }
                .take(5)

            val idGiocatoriTop = top5.map { it.key }
            val valoriPresenzeTop = top5.associate { it.key to it.value }

            val nomiGiocatori = mutableMapOf<String, String>()
            var completati = 0

            for (id in idGiocatoriTop) {
                db.getNomeCognomeGiocatore(id) { _ , cognome ->
                    val nomeCompleto = if (cognome != null) "$cognome" else "Sconosciuto"
                    nomiGiocatori[id] = nomeCompleto
                    completati++

                    if (completati == idGiocatoriTop.size) {
                        val labels = idGiocatoriTop.map { nomiGiocatori[it] ?: it }
                        val values = idGiocatoriTop.map { valoriPresenzeTop[it]?.toFloat() ?: 0f }

                        setBarChart(barChartAllenamenti, labels, values)
                        setPieChart(pieChartObiettivo, obiettiviMap)
                    }
                }
            }
        }


    }

    private fun setPieChart(pieChart: PieChart, dataMap: Map<String, Int>) {
        val entries = dataMap.map { PieEntry(it.value.toFloat(), it.key) }

        val dataSet = PieDataSet(entries, "")
        dataSet.sliceSpace = 3f
        dataSet.valueTextSize = 14f
        dataSet.setValueTextColor(android.graphics.Color.BLACK)
        dataSet.colors = generateDynamicGreenShades(entries.size)




        val data = PieData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() == 0) "" else value.toInt().toString()
            }
        })

        pieChart.data = data
        pieChart.setUsePercentValues(false)
        pieChart.setDrawHoleEnabled(false)
        pieChart.setDrawEntryLabels(false)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.CENTER
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.textSize = 14f

        pieChart.invalidate()
    }



    private fun setBarChart(barChart: BarChart, labels: List<String>, values: List<Float>) {
        val entries = labels.mapIndexed { index, label ->
            BarEntry(index.toFloat(), values[index])
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.valueTextSize = 12f
        dataSet.setValueTextColor(android.graphics.Color.BLACK)
        dataSet.colors = generateDynamicGreenShades(entries.size)

        val data = BarData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        })

        barChart.data = data

        val xAxis = barChart.xAxis
        xAxis.setDrawLabels(true)
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        xAxis.setGranularity(1f)
        xAxis.labelRotationAngle = -15f
        xAxis.textSize = 12f
        xAxis.yOffset = 10f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM)

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.invalidate()

    }

    fun generateDynamicGreenShades(count: Int): List<Int> {
        val colors = mutableListOf<Int>()
        val startHue = 100f   // verde-giallo
        val hueStep = 20f     // passo più ampio per differenziare meglio i colori
        val saturation = 0.85f
        val lightness = 0.65f

        for (i in 0 until count) {
            val hue = (startHue + i * hueStep) % 360f
            val color = Color.HSVToColor(floatArrayOf(hue, saturation, lightness))
            colors.add(color)
        }

        return colors
    }


}
