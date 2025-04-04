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
        getGiocatori{ giocatori ->
            val giocatore = giocatori.find { it.cognome == cognome }
            callback(giocatore?.id)
            if (giocatore != null) {
                callback(giocatore.id)
            } else {
                callback(null)
            }
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
                for (eventSnapshot in snapshot.children) {
                    try {
                        val evento = eventSnapshot.getValue(Evento::class.java)
                        if (evento != null) {
                            eventiList.add(evento)
                        }
                    } catch (e: Exception) {
                        callback(emptyList())
                    }
                }
                callback(eventiList)
            }
            override fun onCancelled(error: DatabaseError){
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
            "casa" to casa
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

            val formazioneMap = mapOf(
                "titolari" to titolari,
                "panchina" to panchina
            )

            database.child("Partite").child(dataPartita).child(partitaId).child("formazione")
                .setValue(formazioneMap)
                .addOnSuccessListener { callback(true, "Formazione aggiornata con successo!") }
                .addOnFailureListener { error ->
                    callback(false, "Errore durante l'aggiornamento delle formazioni: ${error.message}")
                }
        }
    }

    fun aggiungiEvento (idPartita: String, data: String, minutaggio: String,idGiocatore: String, nomeEvento: String, squadra: Boolean, dettagliEvento: Map<String, Any?> = emptyMap()) {
        val eventiRef = database.child("Partite").child(data).child(idPartita).child("eventi")
        val eventoId = eventiRef.push().key ?: return

        val eventoMap = mapOf(
            "idEvento" to eventoId,
            "minutaggio" to minutaggio,
            "nomeEvento" to nomeEvento,
            "nomeGiocatore" to idGiocatore,
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




    fun aggiungiRisultato(partitaId: String, dataPartita: String, esito: String, golCasa: Int, golOspite: Int) {
        val risultatoRef = database.child("Partite").child(dataPartita).child(partitaId).child("risultato")
        val risultatoMap = mapOf(
            "esito" to esito,
            "golCasa" to golCasa,
            "golOspite" to golOspite
        )
        risultatoRef.setValue(risultatoMap)
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


}

