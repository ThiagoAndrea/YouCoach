package com.example.youcoach

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*

class StatistichePartiteActivity : BaseActivity() {

    private lateinit var pieChartRisultati: PieChart
    private lateinit var pieChartGol: PieChart
    private lateinit var pieChartFalli: PieChart
    private lateinit var pieChartAngoli: PieChart
    private lateinit var pieChartTiri: PieChart
    private lateinit var spinnerMese: Spinner
    private lateinit var chipGroup: ChipGroup
    private lateinit var backButton: ImageButton
    private val db = DatabaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats_partite)
        setupBottomNavigation(R.id.nav_stats)

        pieChartRisultati = findViewById(R.id.pieChartRisultati)
        pieChartGol = findViewById(R.id.pieChartGol)
        pieChartFalli = findViewById(R.id.pieChartFalli)
        pieChartAngoli = findViewById(R.id.pieChartAngoli)
        pieChartTiri = findViewById(R.id.pieChartTiri)
        spinnerMese = findViewById(R.id.spinnerMese)
        chipGroup = findViewById(R.id.chipGroup)
        backButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i)
            if (chip is Chip) {
                chip.isChecked = true
            }
        }


        setupMeseSpinner()
        setupChipGroupListener()
    }

    private fun setupMeseSpinner() {
        val mesi = listOf(
            "Tutti", "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
        )

        spinnerMese.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mesi)
        spinnerMese.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                updateChart()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupChipGroupListener() {
        chipGroup.setOnCheckedStateChangeListener { _, _ ->
            updateChart()
        }
    }

    private fun updateChart() {
        val mese = if (spinnerMese.selectedItemPosition == 0) null else spinnerMese.selectedItem.toString()
        val competizioni = chipGroup.checkedChipIds.mapNotNull { id ->
            findViewById<Chip>(id)?.text?.toString()
        }

        db.getStatistichePartiteAggregate(mese, competizioni) { vittorie, pareggi, sconfitte,
                                                                golFatti, golSubiti, tiriFatti, tiriSubiti, falliFatti, falliSubiti, angoliFatti, angoliSubiti ->

            setPieChart(pieChartRisultati, mapOf(
                "V" to vittorie,
                "P" to pareggi,
                "S" to sconfitte
            ))

            setPieChart(pieChartGol, mapOf(
                "Fatti" to golFatti,
                "Subiti" to golSubiti
            ))

            setPieChart(pieChartTiri, mapOf(
                "Fatti" to tiriFatti,
                "Subiti" to tiriSubiti
            ))

            setPieChart(pieChartFalli, mapOf(
                "Fatti" to falliFatti,
                "Subiti" to falliSubiti
            ))

            setPieChart(pieChartAngoli, mapOf(
                "Fatti" to angoliFatti,
                "Subiti" to angoliSubiti
            ))
        }
    }

    private fun setPieChart(pieChart: PieChart, dataMap: Map<String, Int>) {
        val ordinePreferito = listOf("V", "P", "S", "Fatti", "Subiti", "Nessun dato")

        val entries = ordinePreferito.mapNotNull { chiave ->
            if (dataMap.containsKey(chiave)) PieEntry(dataMap[chiave]!!.toFloat(), chiave) else null
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.sliceSpace = 3f
        dataSet.valueTextSize = 14f
        dataSet.setValueTextColor(R.color.black)

        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() +
                ColorTemplate.COLORFUL_COLORS.toList()

        val data = PieData(dataSet)

        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() == 0) "" else value.toInt().toString()
            }
        })


        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(false)
        pieChart.setDrawHoleEnabled(false)
        pieChart.setDrawEntryLabels(false)
        pieChart.legend.isEnabled = true
        pieChart.invalidate()
    }






}
