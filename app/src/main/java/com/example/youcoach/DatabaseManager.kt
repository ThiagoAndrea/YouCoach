package com.example.youcoach

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseManager {
    private val database = FirebaseDatabase.getInstance().reference

    fun getNomeCognomeGiocatore(idGiocatore: String, callback: (String?, String?) -> Unit) {
        database.child("Giocatori").child(idGiocatore).get()
            .addOnSuccessListener { snapshot ->
                val nome = snapshot.child("nome").getValue(String::class.java) ?: "Sconosciuto"
                val cognome = snapshot.child("cognome").getValue(String::class.java) ?: "Sconosciuto"
                callback(nome, cognome)
            }
            .addOnFailureListener {
                callback(null, null)
            }
    }

    fun getGiocatori(callback: (List<Giocatore>) -> Unit) {
        val giocatoriRef = database.child("Giocatori")

        val prioritaRuoli = mapOf(
            "Portiere" to 1,
            "Difensore" to 2,
            "Centrocampista" to 3,
            "Attaccante" to 4
        )

        giocatoriRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(giocatoriSnapshot: DataSnapshot) {
                if (giocatoriSnapshot.exists()) {
                    val giocatori = giocatoriSnapshot.children.mapNotNull { it.getValue(Giocatore::class.java) }
                        .sortedWith(compareBy<Giocatore> { prioritaRuoli[it.ruolo] ?: Int.MAX_VALUE }
                            .thenBy { it.cognome })

                    callback(giocatori)
                } else {
                    callback(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    fun getGiocatore(giocatoreId: String, callback: (Giocatore?) -> Unit) {
        val giocatoreRef = database.child("Giocatori").child(giocatoreId)
        giocatoreRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val giocatore = snapshot.getValue(Giocatore::class.java)
                callback(giocatore)
            }
            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun getGiocatoreIdPerCognome(cognome: String, callback: (String?) -> Unit){
        getGiocatori { giocatori ->
            val giocatore = giocatori.find { it.cognome == cognome }
            callback(giocatore?.id)
        }
    }

    fun getConvocatiMap(formattedDate: String, partitaId: String, callback: (Map<String, Boolean>) -> Unit) {
        val convocazioniRef = database.child("Partite")
            .child(formattedDate)
            .child(partitaId)
            .child("convocati")

        convocazioniRef.get().addOnSuccessListener { convocazioniSnapshot ->
            val convocazioniIniziali = mutableMapOf<String, Boolean>()
            convocazioniSnapshot.children.forEach {
                val playerId = it.key ?: ""
                val stato = it.getValue(Boolean::class.java) ?: true
                convocazioniIniziali[playerId] = stato
            }
            callback(convocazioniIniziali)
        } .addOnFailureListener{
            callback(emptyMap())
        }
    }

    fun getPresenzePerAllenamento(data: String, allenamentoId: String, callback: (Map<String, Int>) -> Unit) {
        val presenzeRef = database.child("Allenamenti").child(data).child(allenamentoId).child("presenze")

        presenzeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val presenzeIniziali = mutableMapOf<String, Int>()
                snapshot.children.forEach {
                    val playerId = it.key ?: ""
                    val stato = it.value.toString().toIntOrNull() ?: 0
                    presenzeIniziali[playerId] = stato
                }
                callback(presenzeIniziali)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyMap())
            }
        })
    }

    fun getObiettivi(callback: (List<String>, Map<String, Boolean>) -> Unit) {
        database.child("Obiettivi").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val obiettiviList = mutableListOf<String>()
                val obiettiviMappa = mutableMapOf<String, Boolean>()

                for (document in snapshot.children) {
                    val obiettivo = document.getValue(String::class.java)
                    if (obiettivo != null) {
                        obiettiviList.add(obiettivo)
                        obiettiviMappa[obiettivo] = false
                    }
                }
                callback(obiettiviList, obiettiviMappa)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList(), emptyMap())
            }
        })
    }

    fun getTitolari(idPartita: String, dataPartita: String, callback: (Map<String, String>) -> Unit) {
        val titolariRef = database.child("Partite").child(dataPartita).child(idPartita).child("formazione").child("titolari")

        titolariRef.get().addOnSuccessListener { snapshot ->
            val titolari = snapshot.children
                .mapNotNull { it.key?.let { idGiocatore -> it.getValue(String::class.java)?.let { nome -> idGiocatore to nome } } }
                .toMap()
            callback(titolari)
        }.addOnFailureListener {
            callback(emptyMap())
        }
    }

    fun getPanchina(idPartita: String, dataPartita: String, callback: (List<String>) -> Unit) {
        val panchinaRef = database.child("Partite").child(dataPartita).child(idPartita).child("formazione").child("panchina")

        panchinaRef.get().addOnSuccessListener { snapshot ->
            val panchina = snapshot.children.mapNotNull { it.getValue(String::class.java) }
            callback(panchina)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    fun getFormazioneTitolare(idPartita: String, dataPartita: String, callback: (Map<String, String>) -> Unit) {
        val titolariRef = database.child("Partite").child(dataPartita).child(idPartita).child("formazione").child("formazione_iniziale")

        titolariRef.get().addOnSuccessListener { snapshot ->
            val titolari = snapshot.children
                .mapNotNull { it.key?.let { idGiocatore -> it.getValue(String::class.java)?.let { nome -> idGiocatore to nome } } }
                .toMap()
            callback(titolari)
        }.addOnFailureListener {
            callback(emptyMap())
        }
    }

    fun getFormazioneMap(idPartita: String, dataPartita: String, callback: (Map<String, String>, List<String>) -> Unit) {
        getTitolari(idPartita, dataPartita) { titolari ->
            getPanchina(idPartita, dataPartita) { panchina ->
                callback(titolari, panchina)
            }
        }
    }

    fun getConvocati(idPartita: String, dataPartita: String, callback: (List<String>) -> Unit) {
        database.child("Partite").child(dataPartita).child(idPartita).child("convocati").get().addOnSuccessListener {
            snapshot -> val giocatori = snapshot.children
            .filter { it.getValue(Boolean::class.java) == true }
            .mapNotNull { it.key }
            callback(giocatori)
        }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun getGiorniOccupatiMensili(year: Int, month: Int, callback:  (Map<String, Boolean>, Map<String, Boolean>)  -> Unit){
        val monthKey = "$year-${"%02d".format(month + 1)}"
        val trainingRef = database.child("Allenamenti").orderByKey().startAt(monthKey).endAt("$monthKey-31")
        val matchRef = database.child("Partite").orderByKey().startAt(monthKey).endAt("$monthKey-31")
        val trainingDays = mutableMapOf<String, Boolean>()
        val matchDays = mutableMapOf<String, Boolean>()

        trainingRef.get().addOnSuccessListener { trainingSnapshot ->
            for (dateSnapshot in trainingSnapshot.children) {
                dateSnapshot.key?.let { trainingDays[it] = true }
            }

            matchRef.get().addOnSuccessListener { matchSnapshot ->
                for (dateSnapshot in matchSnapshot.children) {
                    dateSnapshot.key?.let { matchDays[it] = true }
                }

                callback(trainingDays, matchDays)
            }.addOnFailureListener {
                callback(trainingDays, emptyMap())
            }
        }.addOnFailureListener {
            callback(emptyMap(), emptyMap())
        }
    }

    fun getEventoPerData(date: String, callback: (String?) -> Unit) {
        database.child("Partite").child(date).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                callback("partita")
            } else {
                database.child("Allenamenti").child(date).get().addOnSuccessListener { snap ->
                    callback(if (snap.exists()) "allenamento" else null)
                }.addOnFailureListener { callback(null) }
            }
        }.addOnFailureListener { callback(null) }
    }

    fun getAllenamento(date: String, callback: (String?, String?, String?, List<String>?) -> Unit) {
        val allenamentoRef = database.child("Allenamenti").child(date)
        allenamentoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firstAllenamento = snapshot.children.firstOrNull()
                    if (firstAllenamento != null) {
                        val allenamentoID = firstAllenamento.key
                        val orarioInizio = firstAllenamento.child("orarioInizio").getValue(String::class.java) ?: "N/A"
                        val orarioFine = firstAllenamento.child("orarioFine").getValue(String::class.java) ?: "N/A"
                        val obiettivi = firstAllenamento.child("obiettivi").children.mapNotNull { obiettivo ->
                            obiettivo.getValue(String::class.java)
                        }
                        callback(allenamentoID, orarioInizio, orarioFine, obiettivi)
                    } else {
                        callback(null, null, null, null)
                    }
                } else {
                    callback(null, null, null, null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null, null, null, null)
            }
        })
    }

    fun getPartita(formattedDate: String, callback: (String?, String?, String?, String?, String?, Boolean?, Int?, Int?, Int?) -> Unit) {
        val partitaRef = database.child("Partite").child(formattedDate)
        partitaRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firstPartita = snapshot.children.firstOrNull()
                    if (firstPartita != null) {
                        val idPartita = firstPartita.key
                        val avversario = firstPartita.child("avversario").value as? String ?: "N/A"
                        val orario = firstPartita.child("orario").value as? String ?: "N/A"
                        val luogo = firstPartita.child("luogo").value as? String ?: "N/A"
                        val competizione = firstPartita.child("competizione").value as? String ?: "N/A"
                        val casa = firstPartita.child("casa").value as? Boolean ?: false
                        val minutiPerTempo = (firstPartita.child("minuti_per_tempo").value as? Number)?.toInt() ?: 0
                        val numeroTempi = (firstPartita.child("numero_tempi").value as? Number)?.toInt() ?: 0
                        val numeroCalciatori = (firstPartita.child("numero_calciatori").value as? Number)?.toInt() ?: 0
                        callback(idPartita, avversario, orario, luogo, competizione, casa, minutiPerTempo, numeroTempi, numeroCalciatori)
                    } else {
                        callback(null, null, null, null, null, null, null, null, null)
                    }
                } else {
                    callback(null, null, null, null, null, null, null, null, null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null, null, null, null, null, null, null, null, null)
            }
        })
    }

    fun getPartite(callback: (List<Partita>) -> Unit) {
        val partiteRef = database.child("Partite")

        partiteRef.get().addOnSuccessListener { snapshot ->
            val listaPartite = mutableListOf<Partita>()

            snapshot.children.forEach { dataNode ->
                val dataPartita = dataNode.key ?: return@forEach

                dataNode.children.forEach { partitaNode ->
                    val map = partitaNode.value as? Map<String, Any> ?: return@forEach

                    val partita = Partita(
                        id = partitaNode.key ?: "",
                        orario = map["orario"] as? String ?: "",
                        luogo = map["luogo"] as? String ?: "",
                        avversario = map["avversario"] as? String ?: "",
                        risultato = map["risultato"] as? String ?: "",
                        competizione = map["competizione"] as? String ?: "",
                        numero_tempi = (map["numero_tempi"] as? Long)?.toInt() ?: 0,
                        minuti_per_tempo = (map["minuti_per_tempo"] as? Long)?.toInt() ?: 0,
                        numero_calciatori = (map["numero_calciatori"] as? Long)?.toInt() ?: 0,
                        casa = map["casa"] as? Boolean ?: false,
                        modulo = map["modulo"] as? String ?: "",
                        convocati = (map["convocati"] as? Map<String, Boolean>) ?: emptyMap(),
                        titolari = (map["titolari"] as? Map<String, String>) ?: emptyMap(),
                        data = dataPartita
                    )

                    listaPartite.add(partita)
                }
            }

            callback(listaPartite)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    fun getCasa(partitaId: String, dataPartita: String, callback: (Boolean) -> Unit){
        database.child("Partite").child(dataPartita).child(partitaId).child("casa").get()
            .addOnSuccessListener { snapshot ->
                if(snapshot.exists()) {
                    val casa = snapshot.getValue(Boolean::class.java) ?: false
                    callback(casa)
                }
                else {
                    callback(false)
                }
            }

    }

    fun getGiocata(partitaId: String, dataPartita: String, callback: (Boolean) -> Unit) {
        val giocataRef = database
            .child("Partite")
            .child(dataPartita)
            .child(partitaId)
            .child("giocata")

        giocataRef.get()
            .addOnSuccessListener { snapshot ->
                val giocata = snapshot.getValue(Boolean::class.java) ?: false
                callback(giocata)
            }
            .addOnFailureListener {
                callback(false) // fallback se c'è un errore
            }
    }

    fun getMinutiPerTempo(partitaId: String, dataPartita: String, callback: (Int) -> Unit) {
        database.child("Partite").child(dataPartita).child(partitaId).child("minuti_per_tempo")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val minutiPerTempo = snapshot.getValue(Int::class.java) ?: 45
                    callback(minutiPerTempo)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(45)
                }
            })
    }

    fun getTempi(partitaId: String, dataPartita: String, callback: (Int) -> Unit) {
        database.child("Partite").child(dataPartita).child(partitaId).child("numero_tempi")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tempi = snapshot.getValue(Int::class.java) ?: 2
                    callback(tempi)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(2)
                }
            })
    }

    fun getEventi(partitaId: String, dataPartita: String, callback: (List<Evento>) -> Unit) {
        val eventiRef = database.child("Partite").child(dataPartita).child(partitaId).child("eventi")

        eventiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val eventiList = mutableListOf<Evento>()
                val eventiRaw = mutableListOf<Evento>()

                for (eventSnapshot in snapshot.children) {
                    try {
                        val evento = eventSnapshot.getValue(Evento::class.java)
                        if (evento != null) {
                            eventiRaw.add(evento)
                        }
                    } catch (e: Exception) {
                        callback(emptyList())
                        return
                    }
                }

                if (eventiRaw.isEmpty()) {
                    callback(emptyList())
                    return
                }

                var completati = 0

                eventiRaw.forEach { evento ->
                    getNomeCognomeGiocatore(evento.nomeGiocatore) { _, cognome ->
                        if (cognome != null) {
                            evento.nomeCompletoGiocatore = cognome
                        }
                        eventiList.add(evento)
                        completati++

                        if (completati == eventiRaw.size) {
                            callback(eventiList)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    fun getSquadraPrincipale(callback: (String?, String?) -> Unit) {
        val squadraRef = database.child("Squadra")
        squadraRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val nome = snapshot.child("nome").getValue(String::class.java)
                    val indirizzo = snapshot.child("indirizzo").getValue(String::class.java)
                    callback(nome, indirizzo)
                } catch (e: Exception) {
                    callback(null, null)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                callback(null, null)
            }
        })
    }

    fun getUltimaPartita(callback: (data: String?, partitaId: String?) -> Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dbRef = FirebaseDatabase.getInstance().getReference("Partite")

        dbRef.orderByKey().endAt(today).limitToLast(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val ultimaData = snapshot.children.last().key!!
                    val partitaNode = snapshot.children.last().children.first()
                    val partitaId = partitaNode.key!!
                    callback(ultimaData, partitaId)
                } else {
                    callback(null, null)
                }
            }
            .addOnFailureListener {
                callback(null, null)
            }
    }

    fun getProssimePartite(callback: (List<Map<String, Any>>) -> Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val partiteRef = FirebaseDatabase.getInstance().getReference("Partite")

        partiteRef.orderByKey().startAt(today).get().addOnSuccessListener { snapshot ->
            val result = mutableListOf<Map<String, Any>>()

            for (dataNode in snapshot.children) {
                val data = dataNode.key ?: continue

                for (partitaNode in dataNode.children) {
                    val id = partitaNode.key ?: continue
                    val map = partitaNode.value as? Map<String, Any> ?: continue
                    val giocata = map["giocata"] as? Boolean ?: false
                    if (giocata) continue

                    val avversario = map["avversario"] as? String ?: "Sconosciuto"
                    val casa = map["casa"] as? Boolean ?: false

                    result.add(
                        mapOf(
                            "id" to id,
                            "data" to data,
                            "avversario" to avversario,
                            "casa" to casa
                        )
                    )
                }
            }

            callback(result)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    fun getNumeroAllenamentiFatti(callback: (Int) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("Allenamenti")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        ref.orderByKey().endAt(today).get().addOnSuccessListener { snapshot ->
            var count = 0
            snapshot.children.forEach { giornoSnapshot ->
                count += giornoSnapshot.childrenCount.toInt()
            }
            callback(count)
        }.addOnFailureListener {
            callback(0)
        }
    }



    fun getNumeroPartiteFatte(callback: (Int) -> Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val partiteRef = FirebaseDatabase.getInstance().getReference("Partite")

        partiteRef.orderByKey().endAt(today).get()
            .addOnSuccessListener { snapshot ->
                var count = 0
                snapshot.children.forEach { giorno ->
                    count += giorno.childrenCount.toInt()
                }
                callback(count)
            }.addOnFailureListener {
                callback(0)
            }
    }


    fun getRisultatoPartita(partitaId: String, dataPartita: String, callback: (String?, Int, Int) -> Unit) {
        val risultatoRef = database.child("Partite").child(dataPartita).child(partitaId).child("risultato")
        risultatoRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val esito = snapshot.child("esito").getValue(String::class.java)
                    val golCasa = snapshot.child("golCasa").getValue(Int::class.java) ?: 0
                    val golOspite = snapshot.child("golOspite").getValue(Int::class.java) ?: 0

                    callback(esito, golCasa, golOspite)
                } else {
                    callback(null, 0, 0)
                }
            }
            .addOnFailureListener {
                callback(null, 0, 0)
            }

    }

    fun getStatsPartita(partitaId: String, dataPartita: String, callback: (List<StatisticaPartita>) -> Unit) {
        val statisticheRef = database.child("Partite").child(dataPartita).child(partitaId).child("stats").child("partita")

        val ordineStat = listOf(
            "Tiri",
            "Tiri in porta",
            "Angoli",
            "Falli fatti",
            "Fuorigiochi",
            "Cartellini gialli",
            "Cartellini rossi"
        )

        statisticheRef.get().addOnSuccessListener { snapshot ->
            val listStats = mutableListOf<StatisticaPartita>()
            snapshot.children.forEach { stat ->
                val nome = stat.key ?: return@forEach
                val casaValue = stat.child("casa").getValue(Int::class.java) ?: 0
                val ospiteValue = stat.child("ospite").getValue(Int::class.java) ?: 0
                val tot = casaValue + ospiteValue
                val perCasa = if (tot == 0) 50f else (casaValue.toFloat() / tot) * 100
                val perOspite = 100f - perCasa

                listStats.add(StatisticaPartita(nome, casaValue.toString(), ospiteValue.toString(), perCasa, perOspite))
            }
            val ordinata = listStats.sortedWith(
                compareBy { stat ->
                    ordineStat.indexOfFirst { it.equals(stat.nome, ignoreCase = true) }.takeIf { it >= 0 } ?: Int.MAX_VALUE
                }
            )

            callback(ordinata)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    fun getStatsGiocatorePartita(giocatoreId: String, partitaId: String, callback: (Map<String, Any>) -> Unit) {
        val ref = database
            .child("Giocatori")
            .child(giocatoreId)
            .child("stats")
            .child("partite")
            .child(partitaId)

        ref.get().addOnSuccessListener { snapshot ->
            val map = snapshot.value as? Map<String, Any> ?: emptyMap()
            callback(map)
        }.addOnFailureListener {
            callback(emptyMap())
        }
    }

    /**
     * Funzioni di aggiunta al database
     */

    fun aggiungiGiocatore(
        nome: String,
        cognome: String,
        eta: Int,
        ruolo: String,
        callback: (Boolean, String) -> Unit
    ) {
        val giocatoreId = database.child("Giocatori").push().key
        if (giocatoreId != null) {
            val giocatore = Giocatore(giocatoreId, nome, cognome, eta, ruolo)
            database.child("Giocatori").child(giocatoreId).setValue(giocatore)
                .addOnSuccessListener {
                    callback(true, "Giocatore aggiunto con successo")
                }
                .addOnFailureListener { e ->
                    callback(false, "Errore durante l'aggiunta: ${e.message}")
                }
        }
        else {
            callback(false, "Errore: impossibile generare l'ID del giocatore")
        }
    }

    fun aggiungiAllenamento(
        data: String,
        orarioInizio: String,
        orarioFine: String,
        obiettiviSelezionati: List<String>,
        callback: (Boolean, String) -> Unit
    ) {
        val allenamentoId = database.child("Allenamenti").child(data).push().key
        if (allenamentoId != null) {
            val allenamento = Allenamento(
                id = allenamentoId,
                orarioInizio = orarioInizio,
                orarioFine = orarioFine,
                obiettivi = obiettiviSelezionati
            )
            database.child("Allenamenti").child(data).child(allenamentoId).setValue(allenamento)
                .addOnSuccessListener {
                    callback(true, "Allenamento aggiunto con successo")
                }
                .addOnFailureListener { e ->
                    callback(false, "Errore durante l'aggiunta: ${e.message}")
                }
        } else {
            callback(false, "Errore: impossibile generare l'ID dell'allenamento")
        }
    }

    fun aggiungiObiettivo(
        obiettivo: String,
        callback: (Boolean, String) -> Unit
    ) {
        val obiettivoId = database.child("Obiettivi").push().key
        if(obiettivoId != null) {
            database.child("Obiettivi").child(obiettivoId).setValue(obiettivo).addOnSuccessListener {
                callback(true, "Obiettivo aggiunto con successo")
            }
                .addOnFailureListener { e ->
                    callback(false, "Errore durante l'aggiunta: ${e.message}")
            }
        } else
            callback(false, "Errore: impossibile generare l'ID dell'obiettivo")
    }


    fun aggiungiPartita(
        selectedDate: String,
        partitaId: String?,
        orario: String,
        luogo: String,
        avversario: String,
        competizione: String,
        numTempi: Int,
        minTempi: Int,
        numGiocatori: Int,
        casa: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        val partitaUpdates = mutableMapOf<String, Any>(
            "orario" to orario,
            "luogo" to luogo,
            "avversario" to avversario,
            "competizione" to competizione,
            "numero_tempi" to numTempi,
            "minuti_per_tempo" to minTempi,
            "numero_calciatori" to numGiocatori,
            "casa" to casa,
            "giocata" to false
        )
        val partitaIdFinal = partitaId ?: database.child("Partite").child(selectedDate).push().key!!
        database.child("Partite").child(selectedDate).child(partitaIdFinal)
            .updateChildren(partitaUpdates)
            .addOnSuccessListener {
                callback(true, "Partita salvata con successo")
            }
            .addOnFailureListener { e ->
                callback(false, "Errore durante il salvataggio: ${e.message}")
            }
    }

    fun aggiungiPresenzeAllenamento(
        data: String,
        allenamentoId: String,
        presenze: Map<String, Int>,
        callback: (Boolean, String) -> Unit
    ) {
        val presenzeRef = database.child("Allenamenti").child(data).child(allenamentoId).child("presenze")
        presenzeRef.setValue(presenze)
            .addOnSuccessListener {
                val updates = mutableMapOf<String, Any>()
                presenze.forEach { (giocatoreId, presenzaValore) ->
                    val path = "/Giocatori/$giocatoreId/stats/allenamenti/$allenamentoId"
                    updates[path] = presenzaValore
                }
                database.updateChildren(updates)
                    .addOnSuccessListener {
                        callback(true, "Presenze salvate correttamente in entrambi i nodi")
                    }
                    .addOnFailureListener {
                        callback(false, "Errore nel salvataggio nel nodo Giocatori: ${it.message}")
                    }
            }
            .addOnFailureListener {
                callback(false, "Errore nel salvataggio del nodo Allenamenti: ${it.message}")
            }
    }


    fun aggiungiConvocatiPartita(
        partitaId: String,
        data: String,
        convocazioniConfermate: Map<String, Boolean>,
        callback: (Boolean, String) -> Unit)
    {
        val convocatiRef = database.child("Partite").child(data).child(partitaId).child("convocati")
        convocatiRef.setValue(convocazioniConfermate)
            .addOnSuccessListener {
                callback(true, "Convocazioni aggiornate con successo!")
            }
            .addOnFailureListener { exception ->
                callback(false, "Errore durante l'aggiornamento delle convocazioni: ${exception.message}")
            }
    }

    fun aggiungiFormazionePartita(
        adapter: SelezioneGiocatoreAdapter,
        partitaId: String,
        dataPartita: String,
        callback: (Boolean, String) -> Unit
    ) {
        getConvocati(partitaId, dataPartita) { convocati ->
            val titolari = mutableMapOf<String, String>()
            adapter.selezioni.forEach { (posizione, giocatoreId) ->
                titolari[giocatoreId] = "${posizione.ruolo} ${posizione.posizioneIndex}"
            }
            val titolariIds = titolari.keys.toSet()
            val panchina = convocati.filter { it !in titolariIds }
            val formazione_iniziale = titolari.toMap()

            val formazioneMap = mapOf(
                "titolari" to titolari,
                "panchina" to panchina,
                "formazione_iniziale" to formazione_iniziale
            )

            database.child("Partite").child(dataPartita).child(partitaId).child("formazione")
                .setValue(formazioneMap)
                .addOnSuccessListener { callback(true, "Formazione aggiornata con successo!") }
                .addOnFailureListener { error ->
                    callback(false, "Errore durante l'aggiornamento delle formazioni: ${error.message}")
                }
        }
    }

    fun aggiungiEvento (idPartita: String, data: String, minutaggio: String, nomeGiocatore: String, nomeEvento: String, squadra: Boolean, dettagliEvento: Map<String, Any?> = emptyMap()) {
        val eventiRef = database.child("Partite").child(data).child(idPartita).child("eventi")
        val eventoId = eventiRef.push().key ?: return

        val eventoMap = mapOf(
            "idEvento" to eventoId,
            "minutaggio" to minutaggio,
            "nomeEvento" to nomeEvento,
            "nomeGiocatore" to nomeGiocatore,
            "squadra" to squadra,
            "dettagli" to dettagliEvento
        )

        eventiRef.child(eventoId).setValue(eventoMap)
    }



    fun aggiungiStatsPartita(partitaId: String, dataPartita: String, statsMap: Map<String, Pair<Int, Int>>) {
        val statsRef = database.child("Partite").child(dataPartita).child(partitaId).child("stats").child("partita")
        val stat = statsMap.mapValues { (_, value) ->
            mapOf("casa" to value.first, "ospite" to value.second)
        }
        statsRef.setValue(stat)
    }

    fun aggiungiStatsGiocatore(partitaId: String, dataPartita: String, giocatoreId: String, statsGiocatoreMap: Map<String, Any>) {
        val database = FirebaseDatabase.getInstance().reference
        database
            .child("Partite")
            .child(dataPartita)
            .child(partitaId)
            .child("stats")
            .child("giocatori")
            .child(giocatoreId)
            .updateChildren(statsGiocatoreMap)

        database
            .child("Giocatori")
            .child(giocatoreId)
            .child("stats")
            .child("partite")
            .child(partitaId)
            .updateChildren(statsGiocatoreMap)
    }




    fun aggiungiRisultato(
        partitaId: String,
        dataPartita: String,
        esito: String,
        golCasa: Int,
        golOspite: Int,
        callback: (Boolean) -> Unit
    ) {
        val risultatoRef = database
            .child("Partite")
            .child(dataPartita)
            .child(partitaId)
            .child("risultato")

        val risultatoMap = mapOf(
            "esito" to esito,
            "golCasa" to golCasa,
            "golOspite" to golOspite
        )

        risultatoRef.setValue(risultatoMap)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }


    /**
     *  Funzioni di modifica del database
     */

    fun modificaGiocatore(
        giocatoreId: String,
        nome: String,
        cognome: String,
        eta: Int,
        ruolo: String,
        callback: (Boolean, String) -> Unit
    ) {
        val giocatore = Giocatore(giocatoreId, nome, cognome, eta, ruolo)
        database.child("Giocatori").child(giocatoreId).setValue(giocatore)
            .addOnSuccessListener {
                callback(true, "Giocatore modificato con successo")
            }
            .addOnFailureListener { e ->
                callback(false, "Errore durante la modifica del giocatore: ${e.message}")
            }
    }

    fun modificaAllenamento(
        data: String,
        allenamentoId: String,
        orarioInizio: String,
        orarioFine: String,
        obiettiviSelezionati: List<String>,
        callback: (Boolean, String) -> Unit
    ) {
        val aggiornamenti = mapOf(
            "orarioInizio" to orarioInizio,
            "orarioFine" to orarioFine,
            "obiettivi" to obiettiviSelezionati
        )
        database.child("Allenamenti").child(data).child(allenamentoId)
            .updateChildren(aggiornamenti)
            .addOnSuccessListener {
                callback(true, "Allenamento modificato con successo")
            }
            .addOnFailureListener { e ->
                callback(false, "Errore durante la modifica: ${e.message}")
            }
    }

    fun modificaEvento(
        eventoId: String,
        partitaId: String,
        dataPartita: String,
        minutaggio: String,
        nomeGiocatore: String,
        nomeEvento: String,
        dettagli: Map<String, Any?>,
        callback: (Boolean, String) -> Unit
    ) {
        val eventoRef = database.child("Partite").child(dataPartita).child(partitaId).child("eventi").child(eventoId)
        val aggiornamenti = mapOf(
            "minutaggio" to minutaggio,
            "nomeGiocatore" to nomeGiocatore,
            "nomeEvento" to nomeEvento,
            "dettagli" to dettagli
        )
        eventoRef.updateChildren(aggiornamenti)
            .addOnSuccessListener {
                callback(true, "Evento modificato con successo")
            }
            .addOnFailureListener { e ->
                callback(false, "Errore durante la modifica: ${e.message}")
    }
    }

    fun modificaSquadraPrincipale(
        nome: String,
        indirizzo: String,
        callback: (Boolean, String) -> Unit
    ) {
        val squadraRef = database.child("Squadra")

        val aggiornamenti = mapOf(
            "nome" to nome,
            "indirizzo" to indirizzo
        )

        squadraRef.updateChildren(aggiornamenti)
            .addOnSuccessListener {
                callback(true, "Squadra aggiornata con successo")
            }
            .addOnFailureListener {
                callback(false, "Errore durante l'aggiornamento: ${it.message}")
            }
    }

    fun modificaFormazione(
        idPartita: String,
        dataPartita: String,
        idUscente: String,
        idEntrante: String,
        callback: (Boolean, String) -> Unit
    ) {
        val partitaRef = database.child("Partite").child(dataPartita).child(idPartita)

        partitaRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val titolari = snapshot.child("formazione/titolari").children.associate {
                    it.key to it.getValue(String::class.java)
                }
                val panchina = snapshot.child("formazione/panchina").children.map { it.getValue(String::class.java) }

                if (!titolari.containsKey(idUscente)) {
                    callback(false, "Il giocatore uscente non è tra i titolari")
                    return
                }
                if (!panchina.contains(idEntrante)) {
                    callback(false, "Il giocatore entrante non è in panchina")
                    return
                }
                val updates = hashMapOf<String, Any>()

                val ruoloUscente = titolari[idUscente] ?: "Panchina"

                val nuoviTitolari = titolari.toMutableMap().apply {
                    remove(idUscente)
                    put(idEntrante, ruoloUscente)
                }

                val nuovaPanchina = panchina.toMutableList().apply {
                    remove(idEntrante)
                    add(idUscente)
                }
                updates["formazione/titolari"] = nuoviTitolari
                updates["formazione/panchina"] = nuovaPanchina

                partitaRef.updateChildren(updates)
                    .addOnSuccessListener {
                        callback(true, "Sostituzione effettuata: $idEntrante entra per $idUscente")
                    }
                    .addOnFailureListener { e ->
                        callback(false, "Errore durante la sostituzione: ${e.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, "Errore database: ${error.message}")
            }
        })
    }

    fun modificaPartitaGiocata(
        partitaId: String,
        dataPartita: String,
        giocata: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        val giocataRef = database
            .child("Partite")
            .child(dataPartita)
            .child(partitaId)
            .child("giocata")

        giocataRef.setValue(giocata)
            .addOnSuccessListener {
                callback(true, "Attributo 'giocata' aggiornato a $giocata")
            }
            .addOnFailureListener { e ->
                callback(false, "Errore durante l'aggiornamento: ${e.message}")
            }
    }


    /**
     * Funzioni di eliminazione dal database
     */
    fun eliminaAllenamento(data: String, allenamentoId: String, callback: (Boolean, String) -> Unit){
        val allenamentoRef = database.child("Allenamenti").child(data).child(allenamentoId)
        allenamentoRef.removeValue()
            .addOnSuccessListener { callback (true, "Allenamento eliminato con successo") }
            .addOnFailureListener{ callback(true, "Errore durante l'eliminazione: ${it.message}")}
    }

    fun eliminaGiocatore(giocatoreId: String, callback: (Boolean, String) -> Unit) {
        val giocatoreRef = database.child("Giocatori").child(giocatoreId)
        giocatoreRef.removeValue()
            .addOnSuccessListener { callback(true, "Giocatore eliminato con successo") }
            .addOnFailureListener { callback(false, "Errore durante l'eliminazione del giocatore: ${it.message}")}
    }

    fun eliminaPartita(data: String, partitaId: String, callback: (Boolean, String) -> Unit) {
        val partitaRef = database.child("Partite").child(data).child(partitaId)
        partitaRef.removeValue()
            .addOnSuccessListener { callback(true, "Partita eliminata con successo")}
            .addOnFailureListener { callback(false, "Errore durante l'eliminazione della partita: ${it.message}")
            }
    }

    fun eliminaEvento(eventoId: String, partitaId: String, dataPartita: String, callback: (Boolean, String) -> Unit){
        val eventoRef=database.child("Partite").child(dataPartita).child(partitaId).child("eventi").child(eventoId)

        eventoRef.removeValue()
            .addOnSuccessListener { callback(true, "Evento eliminato con successo") }
            .addOnFailureListener { callback(false, "Errore durante l'eliminazione: ${it.message}")}
    }

    fun eliminaObiettivo(obiettivo: String, callback: (Boolean, String) -> Unit) {
        database.child("Obiettivi")
            .orderByValue()
            .equalTo(obiettivo)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        dataSnapshot.children.forEach { snapshot ->
                            snapshot.ref.removeValue()
                                .addOnSuccessListener {
                                    callback(true, "Obiettivo eliminato con successo")
                                }
                                .addOnFailureListener { e ->
                                    callback(false, "Errore durante l'eliminazione: ${e.message}")
                                }
                        }
                    } else {
                        callback(false, "Obiettivo non trovato")
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    callback(false, "Errore nel database: ${databaseError.message}")
                }
            })
    }



    /**
     * Listeners
     */

    fun setupEventiListener(data: String, partitaId: String, callback: (List<Evento>) -> Unit): ValueEventListener {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val eventi = mutableListOf<Evento>()
                snapshot.children.forEach { ds ->
                    try {
                        val evento = ds.getValue(Evento::class.java)?.apply {
                            idEvento = ds.key ?: ""
                        }
                        evento?.let { eventi.add(it) }
                    } catch (_: Exception) {
                    }
                }
                callback(eventi)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }

        database.child("Partite").child(data).child(partitaId).child("eventi")
            .addValueEventListener(eventListener)

        return eventListener
    }

    /**
     * Statistiche
     */

    fun getStatsAllenamentiPerMese(
        giocatoreId: String,
        meseFiltro: String,
        callback: (presenze: Int, assenze: Int, ritardi: Int, infortuni: Int) -> Unit
    ) {
        val statsRef = database.child("Giocatori").child(giocatoreId).child("stats").child("allenamenti")
        val allenamentiRef = database.child("Allenamenti")

        val mesiMap = mapOf(
            "Gennaio" to "01", "Febbraio" to "02", "Marzo" to "03", "Aprile" to "04",
            "Maggio" to "05", "Giugno" to "06", "Luglio" to "07", "Agosto" to "08",
            "Settembre" to "09", "Ottobre" to "10", "Novembre" to "11", "Dicembre" to "12"
        )
        val meseNumero = mesiMap[meseFiltro]

        statsRef.get().addOnSuccessListener { statsSnapshot ->
            val allenamentiGiocatore = statsSnapshot.children.associate { it.key!! to it.getValue(Int::class.java)!! }

            allenamentiRef.get().addOnSuccessListener { allenamentiSnapshot ->
                var presenze = 0
                var assenze = 0
                var ritardi = 0
                var infortuni = 0

                allenamentiSnapshot.children.forEach { giornoSnapshot ->
                    val dataAllenamento = giornoSnapshot.key ?: return@forEach
                    val meseAllenamento = dataAllenamento.split("-").getOrNull(1)
                    if (meseNumero != null && meseAllenamento != meseNumero) return@forEach
                    giornoSnapshot.children.forEach { allenamentoEntry ->
                        val idAllenamento = allenamentoEntry.key ?: return@forEach

                        val stato = allenamentiGiocatore[idAllenamento] ?: return@forEach

                        when (stato) {
                            0 -> assenze++
                            1 -> infortuni++
                            2 -> presenze++
                            3 -> ritardi++
                        }
                    }
                }

                callback(presenze, assenze, ritardi, infortuni)
            }
        }.addOnFailureListener {
            callback(0, 0, 0, 0)
        }
    }


    fun getPercentualeObiettivoPresenze(
        giocatoreId: String,
        obiettivoFiltro: String,
        callback: (percentuale: Float) -> Unit
    ) {
        val statsRef = database.child("Giocatori").child(giocatoreId).child("stats").child("allenamenti")
        val allenamentiRef = database.child("Allenamenti")

        statsRef.get().addOnSuccessListener { statsSnapshot ->
            val allenamentiGiocatore = statsSnapshot.children.associate { it.key!! to it.getValue(Int::class.java)!! }

            allenamentiRef.get().addOnSuccessListener { allenamentiSnapshot ->
                var totAllenamentiConObiettivo = 0
                var presenzeEffettive = 0

                allenamentiSnapshot.children.forEach { giornoSnapshot ->
                    giornoSnapshot.children.forEach { allenamentoSnapshot ->
                        val idAllenamento = allenamentoSnapshot.key ?: return@forEach
                        val stato = allenamentiGiocatore[idAllenamento] ?: return@forEach

                        val obiettivi = allenamentoSnapshot.child("obiettivi").children.mapNotNull {
                            it.getValue(String::class.java)
                        }

                        if (obiettivoFiltro.isBlank() || obiettivi.contains(obiettivoFiltro)) {
                            totAllenamentiConObiettivo++
                            if (stato == 2 || stato == 3) {
                                presenzeEffettive++
                            }
                        }
                    }
                }

                val percentuale = if (totAllenamentiConObiettivo > 0) {
                    (presenzeEffettive.toFloat() / totAllenamentiConObiettivo) * 100f
                } else {
                    0f
                }

                callback(percentuale)
            }
        }.addOnFailureListener {
            callback(0f)
        }
    }


    fun calcolaStatsPartitaGiocatoreBasic(giocatoreId: String, callback: (convocazioni: Int, titolari: Int, minuti: Int, gol: Int) -> Unit){
            val ref = FirebaseDatabase.getInstance().reference
                .child("Giocatori")
                .child(giocatoreId)
                .child("stats")
                .child("partite")

            ref.get().addOnSuccessListener { snapshot ->
                var convocazioni = 0
                var titolari = 0
                var minuti = 0
                var gol = 0

                snapshot.children.forEach { partita ->
                    val convocato = partita.child("convocato").getValue(Boolean::class.java) == true
                    val titolareVal = partita.child("titolare").getValue(Boolean::class.java) == true
                    val minutiGiocati = partita.child("minutiGiocati").getValue(Int::class.java) ?: 0
                    val golSegnati = partita.child("Gol").getValue(Int::class.java) ?: 0

                    if (convocato) convocazioni++
                    if (titolareVal) titolari++
                    minuti += minutiGiocati
                    gol += golSegnati
                }

                callback(convocazioni, titolari, minuti, gol)
            }.addOnFailureListener {
                callback(0, 0, 0, 0)
            }
        }

    fun calcolaStatisticheFiltratePerGiocatore(
        giocatoreId: String,
        meseSelezionato: String,
        competizioniSelezionate: List<String>,
        avversarioSelezionato: String,
        listaPartite: List<Partita>,
        callback: (
            convocazioni: Int,
            titolari: Int,
            subentrati: Int,
            usciti: Int,
            infortuni: Int,
            minuti: Int,
            gol: Int,
            assist: Int,
            tiri: Int,
            falliFatti: Int,
            falliSubiti: Int,
            gialli: Int,
            rossi: Int,
            fuorigioco: Int
        ) -> Unit
    ) {
        database.child("Giocatori").child(giocatoreId).child("stats").child("partite").get()
            .addOnSuccessListener { snapshot ->
                var convocazioni = 0
                var titolari = 0
                var subentrati = 0
                var usciti = 0
                var infortuni = 0
                var minuti = 0
                var gol = 0
                var assist = 0
                var tiri = 0
                var falliFatti = 0
                var falliSubiti = 0
                var gialli = 0
                var rossi = 0
                var fuorigioco = 0

                snapshot.children.forEach { partitaSnap ->
                    val idPartita = partitaSnap.key ?: return@forEach
                    val stats = partitaSnap.value as? Map<String, Any> ?: return@forEach

                    val partita = listaPartite.find { it.id == idPartita } ?: return@forEach

                    val meseMatch = meseSelezionato == "Tutti" || Utils.estraiMese(partita.data) == meseSelezionato
                    val competizioneMatch = competizioniSelezionate.isEmpty() || competizioniSelezionate.contains(partita.competizione)
                    val avversarioMatch = avversarioSelezionato == "Tutte" || partita.avversario == avversarioSelezionato

                    if (meseMatch && competizioneMatch && avversarioMatch) {
                        if ((stats["convocato"] as? Boolean) == true) convocazioni++
                        if ((stats["titolare"] as? Boolean) == true) titolari++
                        if ((stats["subentrato"] as? Boolean) == true) subentrati++
                        if ((stats["uscito"] as? Boolean) == true) usciti++
                        if ((stats["infortunato"] as? Boolean) == true) infortuni++

                        minuti += (stats["minutiGiocati"] as? Long)?.toInt() ?: 0
                        gol += (stats["gol"] as? Long)?.toInt() ?: 0
                        assist += (stats["assist"] as? Long)?.toInt() ?: 0
                        tiri += (stats["tiri"] as? Long)?.toInt() ?: 0
                        falliFatti += (stats["falliFatti"] as? Long)?.toInt() ?: 0
                        falliSubiti += (stats["falliSubiti"] as? Long)?.toInt() ?: 0
                        gialli += (stats["cartelliniGialli"] as? Long)?.toInt() ?: 0
                        rossi += (stats["cartelliniRossi"] as? Long)?.toInt() ?: 0
                        fuorigioco += (stats["fuorigiochi"] as? Long)?.toInt() ?: 0
                    }
                }

                callback(
                    convocazioni,
                    titolari,
                    subentrati,
                    usciti,
                    infortuni,
                    minuti,
                    gol,
                    assist,
                    tiri,
                    falliFatti,
                    falliSubiti,
                    gialli,
                    rossi,
                    fuorigioco
                )
            }
            .addOnFailureListener {
                callback(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            }
    }

    fun getStatistichePartiteAggregate(
        mese: String?,
        competizioni: List<String>,
        callback: (
            vittorie: Int,
            pareggi: Int,
            sconfitte: Int,
            golFatti: Int,
            golSubiti: Int,
            tiriFatti: Int,
            tiriSubiti: Int,
            falliFatti: Int,
            falliSubiti: Int,
            angoliFatti: Int,
            angoliSubiti: Int
        ) -> Unit
    ) {
        getPartite { partite ->
            val filtrate = partite.filter { partita ->
                (mese == null || Utils.estraiMese(partita.data) == mese) &&
                        competizioni.contains(partita.competizione)
            }

            if (filtrate.isEmpty()) {
                callback(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                return@getPartite
            }

            var vittorie = 0
            var pareggi = 0
            var sconfitte = 0
            var golFatti = 0
            var golSubiti = 0
            var tiriFatti = 0
            var tiriSubiti = 0
            var falliFatti = 0
            var falliSubiti = 0
            var angoliFatti = 0
            var angoliSubiti = 0

            var completate = 0
            val totale = filtrate.size

            filtrate.forEach { partita ->
                val casa = partita.casa
                val partitaRef = FirebaseDatabase.getInstance()
                    .getReference("Partite")
                    .child(partita.data)
                    .child(partita.id)
                    .child("risultato")

                partitaRef.get().addOnSuccessListener { snapshot ->
                    val esito = snapshot.child("esito").getValue(String::class.java)?.lowercase() ?: ""
                    val golCasa = snapshot.child("golCasa").getValue(Int::class.java) ?: 0
                    val golOspite = snapshot.child("golOspite").getValue(Int::class.java) ?: 0

                    val fatti = if (casa) golCasa else golOspite
                    val subiti = if (casa) golOspite else golCasa

                    golFatti += fatti
                    golSubiti += subiti

                    when (esito) {
                        "vittoria" -> vittorie++
                        "pareggio" -> pareggi++
                        "sconfitta" -> sconfitte++
                    }

                    getStatsPartita(partita.id, partita.data) { statsList ->
                        statsList.forEach { stat ->
                            val nome = stat.nome.lowercase()
                            val casaVal = stat.casaValue.toIntOrNull() ?: 0
                            val ospiteVal = stat.ospiteValue.toIntOrNull() ?: 0

                            val fattiStat = if (casa) casaVal else ospiteVal
                            val subitiStat = if (casa) ospiteVal else casaVal

                            when (nome) {
                                "tiri" -> {
                                    tiriFatti += fattiStat
                                    tiriSubiti += subitiStat
                                }
                                "falli fatti" -> {
                                    falliFatti += fattiStat
                                    falliSubiti += subitiStat
                                }
                                "angoli" -> {
                                    angoliFatti += fattiStat
                                    angoliSubiti += subitiStat
                                }
                            }
                        }

                        completate++
                        if (completate == totale) {
                            callback(
                                vittorie, pareggi, sconfitte,
                                golFatti, golSubiti,
                                tiriFatti, tiriSubiti,
                                falliFatti, falliSubiti,
                                angoliFatti, angoliSubiti
                            )
                        }
                    }

                }.addOnFailureListener {
                    completate++
                    if (completate == totale) {
                        callback(
                            vittorie, pareggi, sconfitte,
                            golFatti, golSubiti,
                            tiriFatti, tiriSubiti,
                            falliFatti, falliSubiti,
                            angoliFatti, angoliSubiti
                        )
                    }
                }
            }
        }
    }

    fun getStatisticheAllenamentiAggregate(
        mese: String?,
        obiettivoFiltro: String?,
        callback: (presenzeGiocatori: Map<String, Int>, obiettivi: Map<String, Int>) -> Unit
    ) {
        val allenamentiRef = FirebaseDatabase.getInstance().getReference("Allenamenti")

        allenamentiRef.get().addOnSuccessListener { snapshot ->
            val presenzeMap = mutableMapOf<String, Int>()
            val obiettiviMap = mutableMapOf<String, Int>()

            for (dataSnapshot in snapshot.children) {
                val dataAllenamento = dataSnapshot.key ?: continue
                if (mese != null && Utils.estraiMese(dataAllenamento) != mese) continue

                for (allenamentoSnapshot in dataSnapshot.children) {
                    val obiettivi = allenamentoSnapshot.child("obiettivi").children.mapNotNull {
                        it.getValue(String::class.java)
                    }

                    if (obiettivoFiltro != null && obiettivoFiltro !in obiettivi) continue
                    for (ob in obiettivi) {
                        obiettiviMap[ob] = obiettiviMap.getOrDefault(ob, 0) + 1
                    }

                    val presenze = allenamentoSnapshot.child("presenze")
                    for (giocatoreSnapshot in presenze.children) {
                        val idGiocatore = giocatoreSnapshot.key ?: continue
                        val stato = giocatoreSnapshot.getValue(Int::class.java) ?: continue

                        if (stato == 0 || stato == 1) {
                            presenzeMap[idGiocatore] = presenzeMap.getOrDefault(idGiocatore, 0) + 1
                        }
                    }
                }
            }

            callback(presenzeMap, obiettiviMap)
        }.addOnFailureListener {
            callback(emptyMap(), emptyMap())
        }
    }







}




