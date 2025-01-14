package com.example.youcoach

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class AggiungiAllenamentoActivity : BaseActivity() {

    private lateinit var database: DatabaseReference
    private var allenamentoId: String? = null // ID dell'allenamento (null se stiamo aggiungendo un nuovo allenamento)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aggiungi_allenamento)
        setupBottomNavigation(R.id.nav_rosa)

        database = Firebase.database.reference

        // Riferimenti ai campi di input
        val editTextData = findViewById<EditText>(R.id.editTextData)
        val editTextOrarioInizio = findViewById<EditText>(R.id.editTextOrarioInizio)
        val editTextOrarioFine = findViewById<EditText>(R.id.editTextOrarioFine)
        val buttonAggiungi = findViewById<Button>(R.id.buttonAggiungiAllenamento)
        val buttonBack = findViewById<ImageButton>(R.id.back_button)

        // Listener per il pulsante "Indietro"
        buttonBack.setOnClickListener {
            finish() // Chiude l'activity e torna alla schermata precedente
        }

        editTextData.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val data = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    editTextData.setText(data)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }



        editTextOrarioInizio.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                    editTextOrarioInizio.setText(time)
                },
                hour,
                minute,
                true // Formato 24 ore
            )
            timePickerDialog.show()
        }

        editTextOrarioFine.setOnClickListener {
            // Ottieni l'orario corrente
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            // Crea e mostra il TimePickerDialog
            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    // Formatta l'orario selezionato e impostalo nel campo di testo
                    val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                    editTextOrarioFine.setText(time)
                },
                hour,
                minute,
                true // Usa il formato 24 ore
            )
            timePickerDialog.show()
        }
        // Verifica se stiamo modificando un allenamento esistente
        allenamentoId = intent.getStringExtra("ALLENAMENTO_ID")
        if (allenamentoId != null) {
            // Modalità modifica: popola i campi con i dati esistenti
            val allenamento = intent.getSerializableExtra("ALLENAMENTO") as Allenamento
            editTextOrarioInizio.setText(allenamento.orarioInizio)
            editTextOrarioFine.setText(allenamento.orarioFine)

            // Cambia il testo del pulsante
            buttonAggiungi.text = "Modifica Allenamento"
        }

        // Gestisci il click sul pulsante "Aggiungi/Modifica Allenamento"
        buttonAggiungi.setOnClickListener {
            val data = editTextData.text.toString().trim()
            val orarioInizio = editTextOrarioInizio.text.toString().trim()
            val orarioFine = editTextOrarioFine.text.toString().trim()

            if (data.isEmpty() || orarioInizio.isEmpty() || orarioFine.isEmpty()) {
                // Mostra un messaggio di errore se i campi sono vuoti
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            } else {
                if (allenamentoId != null) {
                    // Modalità modifica: aggiorna l'allenamento esistente
                    modificaAllenamento(allenamentoId!!, data, orarioInizio, orarioFine)
                } else {
                    // Modalità aggiunta: aggiungi un nuovo allenamento
                    aggiungiAllenamento(data, orarioInizio, orarioFine)
                }
            }
        }
    }

    // Funzione per aggiungere un nuovo allenamento
    private fun aggiungiAllenamento(data: String, orarioInizio: String, orarioFine: String) {
        val allenamentoId = database.child("Allenamenti").push().key

        if (allenamentoId != null) {
            val allenamento = Allenamento(allenamentoId, data, orarioInizio, orarioFine)
            database.child("Allenamenti").child(allenamentoId).setValue(allenamento)
                .addOnSuccessListener {
                    Toast.makeText(this, "Allenamento aggiunto con successo", Toast.LENGTH_SHORT).show()
                    finish() // Chiudi l'Activity dopo l'aggiunta
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore durante l'aggiunta dell'allenamento", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Funzione per modificare un allenamento esistente
    private fun modificaAllenamento(allenamentoId: String, data: String, orarioInizio: String, orarioFine: String) {
        val allenamento = Allenamento(allenamentoId, data, orarioInizio, orarioFine)
        database.child("Allenamenti").child(allenamentoId).setValue(allenamento)
            .addOnSuccessListener {
                Toast.makeText(this, "Allenamento modificato con successo", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, CalendarActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent) // Chiudi l'Activity dopo la modifica
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore durante la modifica dell'allenamento", Toast.LENGTH_SHORT).show()
            }
    }
}