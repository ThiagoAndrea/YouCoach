package com.example.youcoach

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.database.database

class FormazioneAdapter(
    private val formazione: Map<String, String>,
    private val data: String,
    private val idPartita: String,
    private val eventoManager: EventoManager,
    private val ruoliOrdine: List<String> = listOf("Portiere", "Difensore", "Centrocampista", "Trequartista", "Attaccante")
) : RecyclerView.Adapter<FormazioneAdapter.RigaViewHolder>() {

    private var buttonsEnabled: Boolean = false

    private fun normalizzaRuolo(ruolo: String): String {
        return ruolo.replace(Regex("\\s\\d+$"), "")
    }

    private fun estraiNumeroRuolo(ruolo: String): Int {
        return Regex("\\d+$").find(ruolo)?.value?.toIntOrNull() ?: Int.MAX_VALUE
    }


    private val formazioneRaggruppata: List<List<String>> = ruoliOrdine.map { ruolo ->
        val giocatoriInRuolo = formazione
            .filter { normalizzaRuolo(it.value) == ruolo }
            .toList()
            .sortedBy { estraiNumeroRuolo(it.second) }
            .map { it.first }

        Log.d("FormazioneAdapter", "Ruolo: $ruolo → Giocatori: $giocatoriInRuolo")
        giocatoriInRuolo
    }.filter { it.isNotEmpty() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RigaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_riga, parent, false)
        Log.d("FormazioneAdapter", "Formazione raggruppata: $formazioneRaggruppata")
        return RigaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RigaViewHolder, position: Int) {
        val giocatori = formazioneRaggruppata[position]
        holder.bind(giocatori)
    }

    override fun getItemCount(): Int = formazioneRaggruppata.size


    inner class RigaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gridGiocatori: GridLayout = itemView.findViewById(R.id.gridGiocatori)

        fun bind(giocatori: List<String>) {
            Log.d("FormazioneAdapter", "Numero di giocatori per questa riga: ${giocatori.size}")

            gridGiocatori.removeAllViews()
            gridGiocatori.columnCount = 5

            giocatori.forEach { idGiocatore ->
                val giocatoreView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_giocatore_live, gridGiocatori, false) as ConstraintLayout

                val cognomeGiocatore: TextView = giocatoreView.findViewById(R.id.cognome_giocatore)
                val cerchioGiocatore: TextView = giocatoreView.findViewById(R.id.cerchio_giocatore)

                recuperaCognomeGiocatore(idGiocatore) { cognome ->
                    cognome?.let {
                        cognomeGiocatore.text = it
                    } ?: run {
                        Log.e(
                            "FormazioneAdapter",
                            "Cognome non trovato per il giocatore con id: $idGiocatore"
                        )
                    }
                }

                recuperaNomeCognomeGiocatore(idGiocatore) { nome, cognome ->
                    if (nome != null && cognome != null) {
                        val iniziali =
                            "${nome.firstOrNull() ?: ""}${cognome.firstOrNull() ?: ""}".uppercase()
                        cerchioGiocatore.text = iniziali
                    } else {
                        cerchioGiocatore.text = "?"
                    }
                }

                val params = GridLayout.LayoutParams().apply {
                    width = 0  // Può essere MATCH_PARENT se vuoi che si espanda
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    ) // Occupa una sola colonna in modo equo
                }
                giocatoreView.layoutParams = params

                gridGiocatori.addView(giocatoreView)

                // Abilita o disabilita il pulsante in base a buttonsEnabled
                cerchioGiocatore.isEnabled = buttonsEnabled
                cerchioGiocatore.alpha = if (buttonsEnabled) 1f else 0.5f // Opzionale: cambia l'opacità

                cerchioGiocatore.setOnClickListener {
                    if (buttonsEnabled) { // Solo se i pulsanti sono abilitati
                        (itemView.context as? LiveActivity)?.getCurrentMinutaggio()?.let { minutaggio ->
                            mostraDialogEventoGiocatore(itemView, idGiocatore, minutaggio)
                        }
                    }
                }
            }
        }


        // Funzione per recuperare il cognome dal database
        private fun recuperaCognomeGiocatore(idGiocatore: String, callback: (String?) -> Unit) {
            val database = Firebase.database.reference
            val giocatoriRef = database.child("Giocatori")

            Log.d("FormazioneAdapter", "Recuperando cognome per il giocatore con ID: $idGiocatore")

            giocatoriRef.child(idGiocatore).get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val cognome = dataSnapshot.child("cognome").getValue(String::class.java)
                    Log.d("FormazioneAdapter", "Cognome trovato: $cognome")
                    callback(cognome)
                } else {
                    Log.e(
                        "FormazioneAdapter",
                        "Giocatore con ID: $idGiocatore non trovato nel database"
                    )
                    callback(null)
                }
            }.addOnFailureListener {
                Log.e("FormazioneAdapter", "Errore nel recupero del cognome: ${it.message}")
                callback(null)
            }
        }

        private fun recuperaNomeCognomeGiocatore(
            idGiocatore: String,
            callback: (String?, String?) -> Unit
        ) {
            val database = Firebase.database.reference.child("Giocatori").child(idGiocatore)

            database.get().addOnSuccessListener { snapshot ->
                val nome = snapshot.child("nome").getValue(String::class.java)
                val cognome = snapshot.child("cognome").getValue(String::class.java)
                callback(nome, cognome)
            }.addOnFailureListener {
                Log.e("Adapter", "Errore nel recupero dati per $idGiocatore")
                callback(null, null)
            }
        }

        private fun mostraDialogEventoGiocatore(view: View, idGiocatore: String, minutaggio: String) {
            val context = view.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.finestra_eventi_giocatore, null)
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()

            val textEvento: TextView = dialogView.findViewById(R.id.textEvento)
            recuperaNomeCognomeGiocatore(idGiocatore){nome, cognome ->
                if(nome!=null && cognome!=null ){
                    textEvento.text="$nome $cognome"
                } else {
                    textEvento.text="Dettaglio giocatore"
                }
            }

            val btnTiro: ImageButton = dialogView.findViewById(R.id.tiro)
            val btnGol: ImageButton = dialogView.findViewById(R.id.gol)
            val btnFuorigioco: ImageButton = dialogView.findViewById(R.id.fuorigioco)
            val btnCambio: ImageButton = dialogView.findViewById(R.id.cambio)
            val btnInfortunio: ImageButton = dialogView.findViewById(R.id.infortunio)
            val btnFallo: ImageButton = dialogView.findViewById(R.id.fallo)
            val btnGiallo: ImageButton = dialogView.findViewById(R.id.giallo)
            val btnRosso: ImageButton = dialogView.findViewById(R.id.rosso)
            val btnParata: ImageButton = dialogView.findViewById(R.id.parata)
            val btnChiudi: Button = dialogView.findViewById(R.id.btn_chiudi)


            btnTiro.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, minutaggio, idGiocatore, "Tiro", true)
                avviaAnimazione(view, R.drawable.tiro)
                dialog.dismiss()
            }

            btnGol.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, minutaggio,idGiocatore, "Gol", true)
                avviaAnimazione(view, R.drawable.gol)
                dialog.dismiss()
            }

            btnFuorigioco.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, minutaggio,idGiocatore, "Fuorigioco", true)
                avviaAnimazione(view, R.drawable.fuorigioco)
                dialog.dismiss()
            }

            btnCambio.setOnClickListener {
                eventoManager.registraEvento(idPartita, data,minutaggio, idGiocatore, "Cambio", true)
                avviaAnimazione(view, R.drawable.round_arrows)
                dialog.dismiss()
            }

            btnInfortunio.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, minutaggio,idGiocatore, "Infortunio", true)
                avviaAnimazione(view, R.drawable.infortunio_live)
                dialog.dismiss()
            }

            btnFallo.setOnClickListener {
                eventoManager.registraEvento(idPartita, data,minutaggio, idGiocatore, "Fallo", true)
                avviaAnimazione(view, R.drawable.fallo)
                dialog.dismiss()
            }

            btnGiallo.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, minutaggio,idGiocatore, "Cartellino Giallo", true)
                avviaAnimazione(view, R.drawable.yellow_card)
                dialog.dismiss()
            }

            btnRosso.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, minutaggio,idGiocatore, "Cartellino Rosso", true)
                avviaAnimazione(view, R.drawable.red_card)
                dialog.dismiss()
            }

            btnParata.setOnClickListener {
                eventoManager.registraEvento(idPartita, data,minutaggio, idGiocatore, "Parata", true)
                avviaAnimazione(view, R.drawable.parata)
                dialog.dismiss()
            }

            btnChiudi.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun avviaAnimazione(view: View, iconaResId: Int) {
            val giocatoreView = view.findViewById<ConstraintLayout>(R.id.item_giocatore_live)
            val location = IntArray(2)
            giocatoreView.getLocationOnScreen(location)

            // Coordinate di partenza (centro del giocatore)
            val startX = location[0].toFloat() + giocatoreView.width / 2f
            val startY = location[1].toFloat() + giocatoreView.height / 2f



            (view.context as? LiveActivity)?.animaIconaEvento(startX, startY, iconaResId)
        }

    }

    fun setButtonsEnabled(enabled: Boolean) {
        buttonsEnabled = enabled
        notifyDataSetChanged() // Notifica l'adapter per aggiornare la vista
    }
}
