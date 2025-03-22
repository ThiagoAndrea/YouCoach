package com.example.youcoach

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import java.util.*

class CalendarActivity : BaseActivity() {

    private val db = DatabaseManager()
    private lateinit var gridView: GridView
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var currentMonthYearTextView: TextView
    private lateinit var addEventButton: ImageButton

    private val calendar: Calendar = Calendar.getInstance()
    private val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    private val currentMonth = calendar.get(Calendar.MONTH)
    private val currentYear = calendar.get(Calendar.YEAR)

    private var displayedMonth = currentMonth
    private var displayedYear = currentYear

    private val trainingDays = mutableMapOf<String, Boolean>()
    private val matchDays = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        setupBottomNavigation(R.id.nav_calendar)

        setupUI()
        setupListeners()
        giorniOccupati()
        updateCalendar()
    }

    private fun setupUI() {
        gridView = findViewById(R.id.gridView)
        prevButton = findViewById(R.id.prev_button)
        nextButton = findViewById(R.id.next_button)
        currentMonthYearTextView = findViewById(R.id.current_month_year)
        addEventButton = findViewById(R.id.add_event_button)
    }

    private fun setupListeners() {
        addEventButton.setOnClickListener { showEventDialog() }
        prevButton.setOnClickListener { meseSuccessivo() }
        nextButton.setOnClickListener { mesePrecedente() }
        gridView.setOnItemClickListener { _, _, position, _ -> clickGiorno(position) }
    }

    private fun giorniOccupati() {
        db.getGiorniOccupatiMensili(displayedYear, displayedMonth) { trainings, matches ->
            trainingDays.clear()
            trainingDays.putAll(trainings)
            matchDays.clear()
            matchDays.putAll(matches)
            updateCalendar()
        }
    }

    private fun updateCalendar() {
        val days = mutableListOf<String>()

        calendar.set(Calendar.MONTH, displayedMonth)
        calendar.set(Calendar.YEAR, displayedYear)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInMonth) {
            days.add(i.toString())
        }

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val offset = when (firstDayOfWeek) {
            Calendar.SUNDAY -> 6
            else -> firstDayOfWeek - Calendar.MONDAY
        }
        repeat(offset) { days.add(0, "") }

        gridView.adapter = CalendarAdapter(
            this, days, currentDay, currentMonth, currentYear,
            displayedMonth, displayedYear, trainingDays, matchDays
        )

        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        currentMonthYearTextView.text = "$monthName $displayedYear"
    }

    private fun meseSuccessivo() {
        if (displayedMonth == Calendar.JANUARY) {
            displayedMonth = Calendar.DECEMBER
            displayedYear--
        } else {
            displayedMonth--
        }
        giorniOccupati()
    }

    private fun mesePrecedente() {
        if (displayedMonth == Calendar.DECEMBER) {
            displayedMonth = Calendar.JANUARY
            displayedYear++
        } else {
            displayedMonth++
        }
        giorniOccupati()
    }

    private fun clickGiorno(position: Int) {
        val selectedDay = (gridView.adapter as CalendarAdapter).getItem(position) as String
        if (selectedDay.isNullOrEmpty()) return

        val formattedDate = String.format("%04d-%02d-%02d", displayedYear, displayedMonth + 1, selectedDay.toInt())

        db.getEventoPerData(formattedDate) { eventType ->
            when (eventType) {
                "partita" -> {
                    val intent = Intent(this, DettaglioPartitaActivity::class.java)
                    intent.putExtra("selectedDay", selectedDay)
                    intent.putExtra("selectedMonth", displayedMonth)
                    intent.putExtra("selectedYear", displayedYear)
                    startActivity(intent)
                }
                "allenamento" -> {
                    val intent = Intent(this, DettaglioAllenamentoActivity::class.java)
                    intent.putExtra("selectedDay", selectedDay)
                    intent.putExtra("selectedMonth", displayedMonth)
                    intent.putExtra("selectedYear", displayedYear)
                    startActivity(intent)
                }
                else -> Toast.makeText(this, "Nessun evento in questa data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.finestra_modale, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.show()

        dialogView.findViewById<Button>(R.id.allenamentoButton).setOnClickListener {
            startActivity(Intent(this, AggiungiAllenamentoActivity::class.java))
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.partitaButton).setOnClickListener {
            startActivity(Intent(this, AggiungiPartitaActivity::class.java))
            dialog.dismiss()
        }
    }
}
