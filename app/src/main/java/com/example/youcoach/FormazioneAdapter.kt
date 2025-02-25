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

    // Normalizza il ruolo rimuovendo i numeri alla fine (es. "Difensore 2" → "Difensore")
    private fun normalizzaRuolo(ruolo: String): String {
        return ruolo.replace(Regex("\\s\\d+$"), "") // Rimuove lo spazio e il numero finale
    }

    // Estrae il numero finale da un ruolo (es. "Difensore 2" → 2) per ordinare i giocatori
    private fun estraiNumeroRuolo(ruolo: String): Int {
        return Regex("\\d+$").find(ruolo)?.value?.toIntOrNull() ?: Int.MAX_VALUE
    }

    // Raggruppiamo e ordiniamo i giocatori
    private val formazioneRaggruppata: List<List<String>> = ruoliOrdine.map { ruolo ->
        val giocatoriInRuolo = formazione
            .filter { normalizzaRuolo(it.value) == ruolo }  // Filtra per ruolo corretto
            .toList()  // Converte in lista di coppie (ID, Ruolo)
            .sortedBy { estraiNumeroRuolo(it.second) }  // Ordina per numero nel ruolo
            .map { it.first } // Ottieni solo gli ID

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

                cerchioGiocatore.setOnClickListener {
                    mostraDialogEventoGiocatore(itemView, idGiocatore)
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

        private fun mostraDialogEventoGiocatore(view: View, idGiocatore: String) {
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
            val btnAssist: ImageButton = dialogView.findViewById(R.id.assist)
            val btnFuorigioco: ImageButton = dialogView.findViewById(R.id.fuorigioco)
            val btnCambio: ImageButton = dialogView.findViewById(R.id.cambio)
            val btnInfortunio: ImageButton = dialogView.findViewById(R.id.infortunio)
            val btnFallo: ImageButton = dialogView.findViewById(R.id.fallo)
            val btnGiallo: ImageButton = dialogView.findViewById(R.id.giallo)
            val btnRosso: ImageButton = dialogView.findViewById(R.id.rosso)
            val btnParata: ImageButton = dialogView.findViewById(R.id.parata)
            val btnChiudi: Button = dialogView.findViewById(R.id.btn_chiudi)


            btnTiro.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, idGiocatore, "Tiro")
                dialog.dismiss()
            }

            btnGol.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, idGiocatore, "Gol")
                dialog.dismiss()
            }

            btnAssist.setOnClickListener {
                eventoManager.registraEvento(idPartita, data,  idGiocatore, "Assist")
                dialog.dismiss()
            }

            btnFuorigioco.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, idGiocatore, "Fuorigioco")
                dialog.dismiss()
            }

            btnCambio.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, idGiocatore, "Cambio")
                dialog.dismiss()
            }

            btnInfortunio.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, idGiocatore, "Infortunio")
                dialog.dismiss()
            }

            btnFallo.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, idGiocatore, "Fallo")
                dialog.dismiss()
            }

            btnGiallo.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, idGiocatore, "Cartellino Giallo")
                dialog.dismiss()
            }

            btnRosso.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, idGiocatore, "Cartellino Rosso")
                dialog.dismiss()
            }

            btnParata.setOnClickListener {
                eventoManager.registraEvento(idPartita, data, idGiocatore, "Parata")
                dialog.dismiss()
            }

            btnChiudi.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }




    }
}
