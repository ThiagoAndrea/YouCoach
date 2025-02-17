package com.example.youcoach

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Locale

class CalendarActivity : BaseActivity() {

    private val database = FirebaseDatabase.getInstance().reference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        setupBottomNavigation(R.id.nav_calendar)

        val gridView: GridView = findViewById(R.id.gridView)
        val prevButton: Button = findViewById(R.id.prev_button)
        val nextButton: Button = findViewById(R.id.next_button)
        val currentMonthYearTextView: TextView = findViewById(R.id.current_month_year)
        val addEventButton: ImageButton = findViewById(R.id.add_event_button)

        // Ottieni la data corrente
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Variabili per il mese e l'anno visualizzati
        var displayedMonth = currentMonth
        var displayedYear = currentYear

        val trainingDays = mutableMapOf<String, Boolean>()
        val matchDays = mutableMapOf<String, Boolean>()

        addEventButton.setOnClickListener {
            // Infla il layout personalizzato
            val dialogView = layoutInflater.inflate(R.layout.finestra_modale, null)

            // Crea l'AlertDialog
            val builder = AlertDialog.Builder(this)
            builder.setView(dialogView) // Imposta il layout personalizzato

            // Crea e mostra il dialogo
            val dialog = builder.create()
            dialog.show()

            // Gestisci i clic sui pulsanti
            dialogView.findViewById<Button>(R.id.allenamentoButton).setOnClickListener {
                val intent = Intent(this, AggiungiAllenamentoActivity::class.java)
                startActivity(intent)
                dialog.dismiss() // Chiudi il dialogo
            }

            dialogView.findViewById<Button>(R.id.partitaButton).setOnClickListener {
                val intent = Intent(this, AggiungiPartitaActivity::class.java)
                startActivity(intent)
                dialog.dismiss() // Chiudi il dialogo
            }
        }

        // Funzione per aggiornare la GridView con i giorni del mese corrente
        fun updateCalendar() {
            // Crea una lista con i giorni del mese corrente
            val days = mutableListOf<String>()

            // Imposta il mese e l'anno nel calendario (usa displayedMonth e displayedYear)
            calendar.set(Calendar.MONTH, displayedMonth)
            calendar.set(Calendar.YEAR, displayedYear)

            // Aggiungi i giorni del mese
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in 1..daysInMonth) {
                days.add(i.toString())
            }

            // Aggiungi spazi vuoti per allineare i giorni della settimana (partendo da Lunedì)
            calendar.set(Calendar.DAY_OF_MONTH, 1) // Imposta il primo giorno del mese
            var firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Giorno della settimana (1 = Domenica, 2 = Lunedì, ..., 7 = Sabato)

            // Calcola l'offset per far partire la settimana da Lunedì
            val offset = when (firstDayOfWeek) {
                Calendar.SUNDAY -> 6 // Se il primo giorno è Domenica, aggiungi 6 spazi vuoti
                else -> firstDayOfWeek - Calendar.MONDAY // Altrimenti, sottrai 2 (perché Lunedì = 2)
            }
            for (i in 0 until offset) {
                days.add(0, "") // Aggiungi spazi vuoti all'inizio
            }

            // Imposta l'adapter per la GridView
            val adapter = CalendarAdapter(
                this,
                days,
                currentDay,
                currentMonth,
                currentYear,
                displayedMonth,
                displayedYear,
                trainingDays,
                matchDays
            )
            gridView.adapter = adapter

            // Aggiorna la TextView con il mese e l'anno visualizzati
            val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            currentMonthYearTextView.text = "$monthName $displayedYear"
        }

        fun getTrainingAndMatchDaysForMonth(year: Int, month: Int) {
            val monthKey = "$year-${"%02d".format(month + 1)}"
            val trainingRef = database.child("Allenamenti").orderByKey().startAt(monthKey).endAt("$monthKey-31")
            val matchRef = database.child("Partite").orderByKey().startAt(monthKey).endAt("$monthKey-31")

            trainingRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(trainingSnapshot: DataSnapshot) {
                    trainingDays.clear()
                    for (dateSnapshot in trainingSnapshot.children) {
                        dateSnapshot.key?.let { trainingDays[it] = true }
                    }

                    matchRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(matchSnapshot: DataSnapshot) {
                            matchDays.clear()
                            for (dateSnapshot in matchSnapshot.children) {
                                dateSnapshot.key?.let { matchDays[it] = true }
                            }
                            updateCalendar()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                this@CalendarActivity,
                                "Errore nel caricamento delle partite: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@CalendarActivity,
                        "Errore nel caricamento degli allenamenti: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

// Inizializza il calendario con il mese corrente
        getTrainingAndMatchDaysForMonth(displayedYear, displayedMonth)
        updateCalendar()

        // Gestisci il clic sul bottone "Prec"
        prevButton.setOnClickListener {
            // Torna al mese precedente
            if (displayedMonth == Calendar.JANUARY) {
                displayedMonth = Calendar.DECEMBER
                displayedYear--
            } else {
                displayedMonth--
            }
            getTrainingAndMatchDaysForMonth(displayedYear, displayedMonth)
            updateCalendar()
        }

        // Gestisci il clic sul bottone "Succ"
        nextButton.setOnClickListener {
            // Vai al mese successivo
            if (displayedMonth == Calendar.DECEMBER) {
                displayedMonth = Calendar.JANUARY
                displayedYear++
            } else {
                displayedMonth++
            }
            getTrainingAndMatchDaysForMonth(displayedYear, displayedMonth)
            updateCalendar()
        }

        gridView.setOnItemClickListener { _, _, position, _ ->
            val selectedDay = (gridView.adapter as CalendarAdapter).getItem(position) as String
            if (selectedDay.isNotEmpty()) {
                val formattedDate = String.format("%04d-%02d-%02d", displayedYear, displayedMonth + 1, selectedDay.toInt())

                database.child("Partite").child(formattedDate).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Se esiste una partita in questa data, apri DettaglioPartitaActivity
                            val intent = Intent(this@CalendarActivity, DettaglioPartitaActivity::class.java)
                            intent.putExtra("selectedDay", selectedDay)
                            intent.putExtra("selectedMonth", displayedMonth)
                            intent.putExtra("selectedYear", displayedYear)
                            startActivity(intent)
                        } else {
                            database.child("Allenamenti").child(formattedDate).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val intent = Intent(this@CalendarActivity, DettaglioAllenamentoActivity::class.java)
                                        intent.putExtra("selectedDay", selectedDay)
                                        intent.putExtra("selectedMonth", displayedMonth)
                                        intent.putExtra("selectedYear", displayedYear)
                                        startActivity(intent)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(this@CalendarActivity, "Errore nel caricamento", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@CalendarActivity, "Errore nel caricamento", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }

    }
}