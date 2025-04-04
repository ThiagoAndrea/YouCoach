package com.example.youcoach

data class StatisticaGiocatore(
    val convocato: Boolean = false,
    val titolare: Boolean = false,
    val gol: Int = 0,
    val minutiGiocati: Int = 0,
    val falliFatti: Int = 0,
    val falliSubiti: Int = 0,
    val cartelliniGialli: Int = 0,
    val cartelliniRossi: Int = 0,
    val fuorigiochi: Int = 0
)

