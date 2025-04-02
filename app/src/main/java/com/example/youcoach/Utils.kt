package com.example.youcoach

object Utils {

    fun troncaTesto(testo: String?, maxLunghezza: Int): String {
        if (testo == null) {
            return ""
        }
        return if (testo.length > maxLunghezza) {
            testo.substring(0, maxLunghezza - 2) + "..."
        } else {
            testo
        }
    }
}