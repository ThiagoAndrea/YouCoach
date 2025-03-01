package com.example.youcoach

import java.io.Serializable

data class Evento(
    val idEvento: String? = null,
    val minutaggio: Long ?= null,
    val nomeEvento: String = "",
    val nomeGiocatore: String = "",
    val squadra: Boolean = true,
    val dettagli: Map<String, Any?> = emptyMap()
): Serializable
