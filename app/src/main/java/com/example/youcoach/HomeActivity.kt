package com.example.youcoach

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class HomeActivity : BaseActivity() {

    private val db = DatabaseManager()
    private lateinit var risultatoUltimaPartita: TextView
    private lateinit var esitoUltimaPartita: TextView
    private lateinit var nomeSquadra: TextView
    private lateinit var avversarioUltimaPartita: TextView


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

        db.getSquadraPrincipale(){nomeSquadra, _ ->
            db.getUltimaPartita(){data, partitaId ->
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
    }

}