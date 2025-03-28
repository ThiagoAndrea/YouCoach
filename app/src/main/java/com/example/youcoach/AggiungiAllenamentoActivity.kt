package com.example.youcoach

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class AggiungiAllenamentoActivity : BaseActivity() {

    private var allenamentoId: String? = null
    private var selectedDate: String = ""

    // UI Components
    private lateinit var editTextData: EditText
    private lateinit var editTextOrarioInizio: EditText
    private lateinit var editTextOrarioFine: EditText
    private lateinit var buttonAggiungi: Button
    private lateinit var buttonAggiungiObiettivo: ImageButton
    private lateinit var buttonEliminaObiettivo: ImageButton
    private lateinit var recyclerViewObiettivi: RecyclerView

    // Adapter and Data
    private lateinit var obiettiviAdapter: ObiettiviAdapter
    private lateinit var obiettiviList: MutableList<String>
    private lateinit var obiettiviMappa: MutableMap<String, Boolean>

    private val db = DatabaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aggiungi_allenamento)
        setupBottomNavigation(R.id.nav_calendar)

        initUI()
        caricaObiettivi()
        handleIntent()
        setupButtonListeners()
    }

    private fun initUI() {
        editTextData = findViewById(R.id.editTextData)
        editTextOrarioInizio = findViewById(R.id.editTextOrarioInizio)
        editTextOrarioFine = findViewById(R.id.editTextOrarioFine)
        buttonAggiungi = findViewById(R.id.btnAggiungiAllenamento)
        buttonAggiungiObiettivo = findViewById(R.id.aggiungiObiettivo_button)
        buttonEliminaObiettivo = findViewById(R.id.eliminaObiettivo_button)

        val buttonBack = findViewById<ImageButton>(R.id.back_button)

        recyclerViewObiettivi = findViewById(R.id.recyclerViewObiettivi)
        recyclerViewObiettivi.layoutManager = LinearLayoutManager(this)

        obiettiviList = mutableListOf()
        obiettiviMappa = mutableMapOf()

        buttonBack.setOnClickListener { finish() }
    }

    private fun handleIntent() {
        allenamentoId = intent.getStringExtra("ALLENAMENTO_ID")
        selectedDate = intent.getStringExtra("DATA") ?: ""

        if (allenamentoId != null) {
            val orarioInizio = intent.getStringExtra("ORARIO_INIZIO") ?: ""
            val orarioFine = intent.getStringExtra("ORARIO_FINE") ?: ""

            editTextOrarioInizio.setText(orarioInizio)
            editTextOrarioFine.setText(orarioFine)
            editTextData.setText(selectedDate.replace("-", "/"))


            buttonAggiungi.text = "Modifica Allenamento"

            aggiornaRecyclerView()
        }

    }

    private fun setupButtonListeners() {
        editTextData.setOnClickListener { selezionaData() }

        editTextOrarioInizio.setOnClickListener { selezionaOrario(editTextOrarioInizio) }
        editTextOrarioFine.setOnClickListener { selezionaOrario(editTextOrarioFine) }

        buttonAggiungiObiettivo.setOnClickListener {
            val dialog = AggiungiObiettivoDialogFragment { caricaObiettivi() }
            dialog.show(supportFragmentManager, "AggiungiObiettivoDialog")
        }

        buttonAggiungi.setOnClickListener {
            val orarioInizio = editTextOrarioInizio.text.toString().trim()
            val orarioFine = editTextOrarioFine.text.toString().trim()

            if (selectedDate.isEmpty() || orarioInizio.isEmpty() || orarioFine.isEmpty()) {
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            } else {
                val obiettiviSelezionati = obiettiviMappa.filter { it.value }.keys.toList()
                if (allenamentoId != null) {
                    db.modificaAllenamento(selectedDate, allenamentoId!!, orarioInizio, orarioFine, obiettiviSelezionati) { success, message ->
                        if (success) {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    db.aggiungiAllenamento(selectedDate, orarioInizio, orarioFine, obiettiviSelezionati) { success, message ->
                        if (success) {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        buttonEliminaObiettivo.setOnClickListener {
            obiettiviAdapter.toggleDeleteMode()
        }
    }

    private fun selezionaData() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                editTextData.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun selezionaOrario(editText: EditText) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                editText.setText(String.format("%02d:%02d", hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun caricaObiettivi() {
        db.getObiettivi { obiettivi, mappa ->
            obiettiviList.clear()
            obiettiviList.addAll(obiettivi)
            obiettiviMappa.clear()
            obiettiviMappa.putAll(mappa)

            if (allenamentoId != null) {
                val obiettiviSelezionati = intent.getStringArrayListExtra("OBIETTIVI") ?: arrayListOf()
                obiettiviSelezionati.forEach { obiettivo ->
                    obiettiviMappa[obiettivo] = true
                }
            }

            aggiornaRecyclerView()
        }
    }

    private fun aggiornaRecyclerView() {
        obiettiviAdapter = ObiettiviAdapter(
            obiettivi = obiettiviList,
            isEditMode = true,
            obiettiviSelezionati = obiettiviMappa,
            onObiettivoChecked = { obiettivo, isChecked ->
                obiettiviMappa[obiettivo] = isChecked
            },
            onObiettivoEliminato = { obiettivo ->
                // Mostra un dialog di conferma prima di eliminare
                AlertDialog.Builder(this)
                    .setTitle("Conferma eliminazione")
                    .setMessage("Sei sicuro di voler eliminare l'obiettivo \"$obiettivo\"?")
                    .setPositiveButton("Elimina") { _, _ ->
                        eliminaObiettivo(obiettivo)
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            }
        )

        recyclerViewObiettivi.adapter = obiettiviAdapter
    }

    private fun eliminaObiettivo(obiettivo: String) {
        val db = DatabaseManager()
        db.eliminaObiettivo(obiettivo) { success, message ->
            if (success) {
                obiettiviList.remove(obiettivo)
                obiettiviMappa.remove(obiettivo)

                obiettiviAdapter.updateObiettivi(obiettiviList)

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

}