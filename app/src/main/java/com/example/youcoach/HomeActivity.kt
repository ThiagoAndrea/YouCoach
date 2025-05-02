package com.example.youcoach

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.card.MaterialCardView

class HomeActivity : BaseActivity() {

    private val db = DatabaseManager()
    private lateinit var risultatoUltimaPartita: TextView
    private lateinit var esitoUltimaPartita: TextView
    private lateinit var avversarioUltimaPartita: TextView
    private lateinit var numeroAllenamentiTextView: TextView
    private lateinit var numeroPartiteTextView: TextView
    private lateinit var avversario1TextView: TextView
    private lateinit var avversario2TextView: TextView




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setupBottomNavigation(R.id.nav_home)

        val buttonDetails = findViewById<Button>(R.id.btn_vai_a_partita)

        val modificaImpostazioniSquadra = findViewById<ImageButton>(R.id.modifica_impostazioni)
        val nomeSquadra = findViewById<TextView>(R.id.nome_squadra)


        setUI()
        modificaImpostazioniSquadra.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.finestra_modifica_squadra, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            val editNomeSquadra = dialogView.findViewById<EditText>(R.id.edit_nome_squadra)
            val editIndirizzoSquadra = dialogView.findViewById<EditText>(R.id.edit_indirizzo_squadra)
            val buttonConferma = dialogView.findViewById<Button>(R.id.button_conferma_modifica)
            val buttonAnnulla = dialogView.findViewById<Button>(R.id.button_annulla_modifica)

            db.getSquadraPrincipale { nome, indirizzo ->
                editNomeSquadra.setText(nome ?: "")
                editIndirizzoSquadra.setText(indirizzo ?: "")
            }

            buttonConferma.setOnClickListener {
                val nuovoNome = editNomeSquadra.text.toString()
                val nuovoIndirizzo = editIndirizzoSquadra.text.toString()

                db.modificaSquadraPrincipale(nuovoNome, nuovoIndirizzo) { success, message ->
                    if (success) {
                        nomeSquadra.text = nuovoNome
                        Toast.makeText(this, "Squadra aggiornata!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Errore: $message", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }

            buttonAnnulla.setOnClickListener { dialog.dismiss() }

            dialog.show()
        }

        buttonDetails.setOnClickListener {
            db.getUltimaPartita() { data, partitaId ->
                val intent = Intent(this, FinePartitaActivity::class.java).apply {
                    putExtra("DATA", data)
                    putExtra("PARTITA_ID", partitaId)
                }
                startActivity(intent)
            }
        }



    }

    private fun setUI(){
        risultatoUltimaPartita = findViewById(R.id.risultato_ultima_partita)
        esitoUltimaPartita = findViewById(R.id.esito_ultima_partita)
        avversarioUltimaPartita = findViewById(R.id.avversario_ultima_partita)
        numeroAllenamentiTextView = findViewById(R.id.num_allenamenti)
        numeroPartiteTextView = findViewById(R.id.num_partite)
        avversario1TextView = findViewById(R.id.avversario_1)
        avversario2TextView = findViewById(R.id.avversario_2)
        val card1 = findViewById<MaterialCardView>(R.id.card_prossima_partita1)
        val card2 = findViewById<MaterialCardView>(R.id.card_prossima_partita2)
        val logo1 = findViewById<ImageView>(R.id.logo1)
        val logo2 = findViewById<ImageView>(R.id.logo2)
        val cardAllenamenti = findViewById<MaterialCardView>(R.id.card_allenamenti_fatti)
        val cardPartite = findViewById<MaterialCardView>(R.id.card_partite_fatte)
        val btnVediStatistiche = findViewById<Button>(R.id.btn_vedi_statistiche)



        db.getSquadraPrincipale{nomeSquadra, _ ->
            db.getUltimaPartita{data, partitaId ->
                if(partitaId != null && data != null){
                    db.getPartita(data){_, avversario, _, _, _, casa, _, _, _ ->
                        avversarioUltimaPartita.text = avversario
                        db.getRisultatoPartita(partitaId, data) { esito, golCasa, golOspite ->
                            esitoUltimaPartita.text = esito
                                risultatoUltimaPartita.text = "$golCasa - $golOspite"
                        }
                    }

                        }
                    }
        }

        db.getNumeroAllenamentiFatti { count ->
            numeroAllenamentiTextView.text = count.toString()
        }

        db.getNumeroPartiteFatte { count ->
            numeroPartiteTextView.text = count.toString()
        }

        cardAllenamenti.setOnClickListener {
            val intent = Intent(this, StatisticheAllenamentiActivity::class.java)
            startActivity(intent)
        }

        cardPartite.setOnClickListener {
            val intent = Intent(this, StatistichePartiteActivity::class.java)
            startActivity(intent)
        }

        btnVediStatistiche.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }


        db.getProssimePartite { partite ->
            if (partite.isNotEmpty()) {
                val prima = partite[0]
                avversario1TextView.text = prima["avversario"].toString()
                logo1.setImageResource(if (prima["casa"] as Boolean) R.drawable.home else R.drawable.aereo)
                setupCardClick(card1, prima["data"].toString())

                if (partite.size > 1) {
                    val seconda = partite[1]
                    card2.visibility = View.VISIBLE
                    avversario2TextView.text = seconda["avversario"].toString()
                    logo2.setImageResource(if (seconda["casa"] as Boolean) R.drawable.home else R.drawable.aereo)
                    setupCardClick(card2, prima["data"].toString())
                } else {
                    card2.visibility = View.GONE
                }
            } else {
                card1.visibility = View.GONE
                card2.visibility = View.GONE
            }
        }


    }

    private fun setupCardClick(card: MaterialCardView, dataPartita: String) {
        val parts = dataPartita.split("-")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val day = parts[2].toInt()

            card.setOnClickListener {
                val intent = Intent(this, DettaglioPartitaActivity::class.java).apply {
                    putExtra("selectedDay", day.toString())
                    putExtra("selectedMonth", month)
                    putExtra("selectedYear", year)
                }
                Log.d("INTENT_HOME", "Intent da Home: selectedDay=$day, selectedMonth=$month, selectedYear=$year")
                startActivity(intent)
            }
        }
    }



}