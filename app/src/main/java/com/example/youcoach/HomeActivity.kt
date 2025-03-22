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
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : BaseActivity() {

    private val db = DatabaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setupBottomNavigation(R.id.nav_home)

        val modificaImpostazioniSquadra = findViewById<ImageButton>(R.id.modifica_impostazioni)
        val nomeSquadra = findViewById<TextView>(R.id.nome_squadra)

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

    }
}