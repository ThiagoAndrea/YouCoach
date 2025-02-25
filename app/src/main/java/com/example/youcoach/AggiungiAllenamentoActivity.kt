package com.example.youcoach

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.util.*

class AggiungiAllenamentoActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private var allenamentoId: String? = null
    private var selectedDate: String = ""

    // UI Components
    private lateinit var editTextData: EditText
    private lateinit var editTextOrarioInizio: EditText
    private lateinit var editTextOrarioFine: EditText
    private lateinit var buttonAggiungi: Button
    private lateinit var buttonAggiungiObiettivo: Button
    private lateinit var recyclerViewObiettivi: RecyclerView

    // Adapter and Data
    private lateinit var obiettiviAdapter: ObiettiviAdapter
    private lateinit var obiettiviList: MutableList<String>
    private lateinit var obiettiviMappa: MutableMap<String, Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aggiungi_allenamento)
        setupBottomNavigation(R.id.nav_calendar)

        database = FirebaseDatabase.getInstance().reference

        initUI()
        caricaObiettiviDaFirebase()
        handleIntent()
        setupButtonListeners()
    }

    private fun initUI() {
        editTextData = findViewById(R.id.editTextData)
        editTextOrarioInizio = findViewById(R.id.editTextOrarioInizio)
        editTextOrarioFine = findViewById(R.id.editTextOrarioFine)
        buttonAggiungi = findViewById(R.id.buttonAggiungiAllenamento)
        buttonAggiungiObiettivo = findViewById(R.id.aggiungiObiettivo_button)
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
            val obiettivi = intent.getStringArrayListExtra("OBIETTIVI") ?: arrayListOf()

            editTextOrarioInizio.setText(orarioInizio)
            editTextOrarioFine.setText(orarioFine)
            editTextData.setText(selectedDate.replace("-", "/"))  // Mostra la data in formato leggibile

            // Impostiamo la mappa con gli obiettivi selezionati
            obiettivi.forEach { obiettiviMappa[it] = true }

            buttonAggiungi.text = "Modifica Allenamento"

            // Ricarica gli obiettivi selezionati nel RecyclerView
            aggiornaRecyclerView()
        }
    }



    private fun setupButtonListeners() {

        editTextData.setOnClickListener { selezionaData() }

        editTextOrarioInizio.setOnClickListener { selezionaOrario(editTextOrarioInizio) }
        editTextOrarioFine.setOnClickListener { selezionaOrario(editTextOrarioFine) }

        // Add obiettivo dialog
        buttonAggiungiObiettivo.setOnClickListener {
            val dialog = AggiungiObiettivoDialogFragment { caricaObiettiviDaFirebase() }
            dialog.show(supportFragmentManager, "AggiungiObiettivoDialog")
        }

        // Save or update allenamento
        buttonAggiungi.setOnClickListener {
            val orarioInizio = editTextOrarioInizio.text.toString().trim()
            val orarioFine = editTextOrarioFine.text.toString().trim()

            if (selectedDate.isEmpty() || orarioInizio.isEmpty() || orarioFine.isEmpty()) {
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            } else {
                if (allenamentoId != null) {
                    modificaAllenamento(selectedDate, allenamentoId!!, orarioInizio, orarioFine)
                } else {
                    aggiungiAllenamento(selectedDate, orarioInizio, orarioFine)
                }
            }
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

    private fun caricaObiettiviDaFirebase() {
        database.child("Obiettivi").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                obiettiviList.clear()
                obiettiviMappa.clear()
                for (document in snapshot.children) {
                    val obiettivo = document.getValue(String::class.java)
                    if (obiettivo != null) {
                        obiettiviList.add(obiettivo)
                        obiettiviMappa[obiettivo] = false
                    }
                }
                aggiornaRecyclerView()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AggiungiAllenamentoActivity, "Errore nel recupero degli obiettivi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun aggiornaRecyclerView() {
        obiettiviAdapter = ObiettiviAdapter(
            obiettiviList,
            true,
            obiettiviMappa
        ) { obiettivo, isChecked ->
            obiettiviMappa[obiettivo] = isChecked  // Aggiorna la mappa quando una checkbox cambia
        }
        recyclerViewObiettivi.adapter = obiettiviAdapter
    }



    private fun aggiungiAllenamento(data: String, orarioInizio: String, orarioFine: String) {
        val allenamentoId = database.child("Allenamenti").child(data).push().key
        if (allenamentoId != null) {
            val allenamento = Allenamento(
                allenamentoId,
                orarioInizio,
                orarioFine,
                obiettiviMappa.filterValues { it }.keys.toList()
            )
            database.child("Allenamenti").child(data).child(allenamentoId).setValue(allenamento)
                .addOnSuccessListener {
                    Toast.makeText(this, "Allenamento aggiunto con successo", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore durante l'aggiunta", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun modificaAllenamento(data: String, allenamentoId: String, orarioInizio: String, orarioFine: String) {
        val aggiornamenti = mapOf(
            "orarioInizio" to orarioInizio,
            "orarioFine" to orarioFine,
            "obiettivi" to obiettiviMappa.filterValues { it }.keys.toList()  // Filtra gli obiettivi selezionati
        )

        database.child("Allenamenti").child(data).child(allenamentoId)
            .updateChildren(aggiornamenti)
            .addOnSuccessListener {
                Toast.makeText(this, "Allenamento modificato con successo", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore durante la modifica", Toast.LENGTH_SHORT).show()
            }
    }


}