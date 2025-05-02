package com.example.youcoach

class StatsManager(private val partitaId: String, private val dataPartita: String) {
    private val db = DatabaseManager()

    fun salvaStats(golCasa: Int, golOspite: Int, callback: (Boolean) -> Unit) {
        val totalTasks = 5
        var completed = 0
        var allSuccess = true

        fun checkComplete(success: Boolean) {
            if (!success) allSuccess = false
            completed++
            if (completed == totalTasks) {
                callback(allSuccess)
            }
        }

        completaStatsRosa { checkComplete(it) }
        salvaStatisticheSquadra { checkComplete(it) }
        salvaStatisticheGiocatore { checkComplete(it) }
        salvaConvocazioniEMinuti { checkComplete(it) }
        salvaRisultatoPartita(golCasa, golOspite) { checkComplete(it) }
    }

    private fun salvaStatisticheSquadra(callback: (Boolean) -> Unit) {
        db.getCasa(partitaId, dataPartita){ casa ->
            val squadraInCasa = casa

            db.getEventi(partitaId, dataPartita) { eventi ->
                val nomeStatistica = mapOf(
                    "Tiro" to "Tiri",
                    "Fallo" to "Falli fatti",
                    "Gol" to "Tiri in porta",
                    "Angolo" to "Angoli",
                    "Giallo" to "Cartellini gialli",
                    "Rosso" to "Cartellini rossi",
                    "Fuorigioco" to "Fuorigiochi"
                )
                val eventiValidi = listOf(
                    "Tiri", "Tiri in porta", "Falli fatti", "Angoli", "Fuorigiochi",
                    "Cartellini gialli", "Cartellini rossi"
                )
                val statsMap = eventiValidi.associateWith { Pair(0, 0) }.toMutableMap()
                eventi.forEach { evento ->
                    val nomeEvento = evento.nomeEvento
                    val squadra = evento.squadra
                    val dettagli = evento.dettagli

                    when (nomeEvento) {
                        "Tiro" -> {
                            val labelTiri = nomeStatistica["Tiro"] ?: return@forEach
                            val attuale = statsMap[labelTiri] ?: Pair(0, 0)
                            val nuovoValore = calcolaValoreAggiornato(squadraInCasa, squadra, attuale)
                            statsMap[labelTiri] = nuovoValore
                            val esito = (dettagli["tipoTiro"] as? String)?.lowercase()
                            if (esito == "in porta") {
                                val labelTP = "Tiri in porta"
                                val attualeTP = statsMap[labelTP] ?: Pair(0, 0)
                                val nuovoTP = calcolaValoreAggiornato(squadraInCasa, squadra, attualeTP)
                                statsMap[labelTP] = nuovoTP
                            }
                        }

                        "Gol" -> {
                            val labelTiri = nomeStatistica["Tiro"] ?: return@forEach
                            val labelTP = nomeStatistica["Gol"] ?: return@forEach
                            val attTiro = statsMap[labelTiri] ?: Pair(0, 0)
                            val attTP = statsMap[labelTP] ?: Pair(0, 0)
                            statsMap[labelTiri] = calcolaValoreAggiornato(squadraInCasa, squadra, attTiro)
                            statsMap[labelTP] = calcolaValoreAggiornato(squadraInCasa, squadra, attTP)
                        }

                        "Fallo" -> {
                            val tipoFallo = (dettagli["tipoFallo"] as? String)?.lowercase()
                            val label = nomeStatistica["Fallo"] ?: return@forEach
                            val attuale = statsMap[label] ?: Pair(0, 0)
                            val assegnaACasa = if (tipoFallo == "subito") {
                                if (squadraInCasa == true) !squadra else squadra
                            } else {
                                if (squadraInCasa == true) squadra else !squadra
                            }
                            val nuovoValore = if (assegnaACasa) {
                                Pair(attuale.first + 1, attuale.second)
                            } else {
                                Pair(attuale.first, attuale.second + 1)
                            }
                            statsMap[label] = nuovoValore
                        }

                        "Angolo", "Giallo", "Rosso", "Fuorigioco" -> {
                            val label = nomeStatistica[nomeEvento] ?: return@forEach
                            val attuale = statsMap[label] ?: Pair(0, 0)
                            val nuovoValore = calcolaValoreAggiornato(squadraInCasa, squadra, attuale)
                            statsMap[label] = nuovoValore
                        }

                        else -> {
                        }
                    }
                }

                db.aggiungiStatsPartita(partitaId, dataPartita, statsMap)
                callback(true)
            }
        }
    }

    private fun salvaStatisticheGiocatore(callback: (Boolean) -> Unit) {
        db.getEventi(partitaId, dataPartita) { eventi ->
            val statsGiocatori = mutableMapOf<String, MutableMap<String, Any>>()

            fun incrementaStat(giocatore: String, chiave: String) {
                val stats = statsGiocatori.getOrPut(giocatore) { mutableMapOf() }
                val attuale = (stats[chiave] as? Int) ?: 0
                stats[chiave] = attuale + 1
            }

            eventi.forEach { evento ->
                if (!evento.squadra) return@forEach
                val nomeEvento = evento.nomeEvento
                val giocatoreId = evento.nomeGiocatore
                val dettagli = evento.dettagli

                when (nomeEvento) {
                    "Tiro" -> {
                        incrementaStat(giocatoreId, "Tiri")
                        val esito = (dettagli["tipoTiro"] as? String)?.lowercase()
                        if (esito == "in porta") {
                            incrementaStat(giocatoreId, "Tiri in porta")
                        }
                    }

                    "Gol" -> {
                        val assistId = (dettagli["idAssist"] as? String)
                        if (assistId != null) incrementaStat(assistId, "assist")
                        incrementaStat(giocatoreId, "gol")
                        incrementaStat(giocatoreId, "tiri")
                        incrementaStat(giocatoreId, "tiriInPorta")
                    }

                    "Fallo" -> {
                        val tipoFallo = (dettagli["tipoFallo"] as? String)?.lowercase()
                        if (tipoFallo == "subito") {
                            incrementaStat(giocatoreId, "falliSubiti")
                        } else {
                            incrementaStat(giocatoreId, "falliFatti")
                        }
                    }

                    "Giallo" -> incrementaStat(giocatoreId, "cartelliniGialli")
                    "Rosso" -> incrementaStat(giocatoreId, "cartelliniRossi")
                    "Fuorigioco" -> incrementaStat(giocatoreId, "fuorigiochi")
                    "Parata" -> incrementaStat(giocatoreId, "parate")
                    else -> {}
                }
            }

            statsGiocatori.forEach { (giocatoreId, statsMap) ->
                db.aggiungiStatsGiocatore(partitaId, dataPartita, giocatoreId, statsMap)
            }

            callback(true)
        }
    }

    private fun salvaConvocazioniEMinuti(callback: (Boolean) -> Unit) {
        db.getConvocati(partitaId, dataPartita) { convocati ->
            db.getFormazioneTitolare(partitaId, dataPartita) { titolari ->
                db.getEventi(partitaId, dataPartita) { eventi ->
                    val statsGiocatori = mutableMapOf<String, MutableMap<String, Any>>()
                    convocati.forEach { giocatoreId ->
                        statsGiocatori[giocatoreId] = mutableMapOf(
                            "convocato" to true,
                            "titolare" to (giocatoreId in titolari.keys),
                            "minutoInizio" to if (giocatoreId in titolari.keys) 0 else -1,
                            "minutiGiocati" to 0,
                            "uscito" to false,
                            "subentrato" to false,
                            "infortunato" to false
                        )
                    }
                    eventi.forEach { evento ->
                        when (evento.nomeEvento) {
                            "Cambio" -> {
                                val dettagli = evento.dettagli
                                val uscente = evento.nomeGiocatore
                                val entrante = dettagli["Entra: "] as? String ?: return@forEach
                                val minutoCambio = Utils.parseMinuto(evento.minutaggio)
                                statsGiocatori[uscente]?.let { statUscente ->
                                    val inizio = (statUscente["minutoInizio"] as? Int) ?: 0
                                    statUscente["minutiGiocati"] = minutoCambio - inizio
                                    statUscente["uscito"] = true
                                }
                                val statEntrante = statsGiocatori.getOrPut(entrante) {
                                    mutableMapOf(
                                        "convocato" to true,
                                        "titolare" to false,
                                        "uscito" to false,
                                        "subentrato" to false,
                                        "infortunato" to false,
                                        "minutiGiocati" to 0
                                    )
                                }
                                statEntrante["subentrato"] = true
                                statEntrante["minutoInizio"] = minutoCambio
                            }
                            "Infortunio" -> {
                                val giocatoreInfortunato = evento.nomeGiocatore
                                statsGiocatori[giocatoreInfortunato]?.set("infortunato", true)
                            }
                        }
                    }
                    db.getPartita(dataPartita) { _, _, _, _, _, _, minutiPerTempo, numeroTempi, _ ->
                        if (minutiPerTempo != null && numeroTempi != null) {
                            val durata = minutiPerTempo * numeroTempi

                            statsGiocatori.forEach { (_, stat) ->
                                val inizio = (stat["minutoInizio"] as? Int)
                                val uscito = (stat["uscito"] as? Boolean) ?: false

                                if (!uscito && inizio != null && inizio >= 0) {
                                    val giocati = durata - inizio
                                    stat["minutiGiocati"] = (stat["minutiGiocati"] as? Int ?: 0) + giocati
                                }
                                stat.remove("minutoInizio")
                            }

                            statsGiocatori.forEach { (id, statMap) ->
                                db.aggiungiStatsGiocatore(partitaId, dataPartita, id, statMap)
                            }

                            callback(true)
                        } else callback(false)
                    }
                }
            }
        }
    }

    private fun completaStatsRosa(callback: (Boolean) -> Unit) {
        db.getGiocatori { giocatori ->
            db.getConvocati(partitaId, dataPartita) { convocati ->
                val idConvocati = convocati.toSet()
                val statsBase = mapOf(
                    "uscito" to false,
                    "subentrato" to false,
                    "infortunato" to false,
                    "convocato" to false,
                    "titolare" to false,
                    "gol" to 0,
                    "minutiGiocati" to 0,
                    "falliFatti" to 0,
                    "falliSubiti" to 0,
                    "cartelliniGialli" to 0,
                    "cartelliniRossi" to 0,
                    "fuorigiochi" to 0,
                    "tiri" to 0,
                    "tiriInPorta" to 0,
                    "parate" to 0,
                    "assist" to 0
                )

                var completati = 0
                val tot = giocatori.size

                giocatori.forEach { giocatore ->
                    val id = giocatore.id
                    db.getStatsGiocatorePartita(id, partitaId) { esistenti ->
                        val statMap = mutableMapOf<String, Any>()
                        if (id in idConvocati) {
                            statsBase.forEach { (chiave, valoreBase) ->
                                if (!esistenti.containsKey(chiave)) statMap[chiave] = valoreBase
                            }
                        } else {
                            statMap.putAll(statsBase)
                        }
                        if (statMap.isNotEmpty()) {
                            db.aggiungiStatsGiocatore(partitaId, dataPartita, id, statMap)
                        }
                        completati++
                        if (completati == tot) callback(true)
                    }
                }
                if (tot == 0) callback(true)
            }
        }
    }

    private fun salvaRisultatoPartita(golCasa: Int, golOspite: Int, callback: (Boolean) -> Unit) {
        db.getCasa(partitaId, dataPartita) { casa ->
            val esito = when {
                golCasa > golOspite -> if (casa == true) "Vittoria" else "Sconfitta"
                golCasa < golOspite -> if (casa == true) "Sconfitta" else "Vittoria"
                else -> "Pareggio"
            }

            db.aggiungiRisultato(partitaId, dataPartita, esito, golCasa, golOspite) { success ->
                callback(success)
            }
        }
    }

    private fun calcolaValoreAggiornato(
        squadraInCasa: Boolean?,
        squadraEvento: Boolean,
        attuale: Pair<Int, Int>
    ): Pair<Int, Int> {
        return if (squadraInCasa == true) {
            if (squadraEvento) Pair(attuale.first + 1, attuale.second)
            else Pair(attuale.first, attuale.second + 1)
        } else {
            if (!squadraEvento) Pair(attuale.first + 1, attuale.second)
            else Pair(attuale.first, attuale.second + 1)
        }
    }


}
