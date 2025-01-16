package com.example.youcoach

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

class CalendarAdapter(
    private val context: Context,
    private val days: List<String>,
    private val currentDay: Int, // Giorno corrente (oggi)
    private val currentMonth: Int, // Mese corrente
    private val currentYear: Int, // Anno corrente
    private val displayedMonth: Int, // Mese visualizzato
    private val displayedYear: Int, // Anno visualizzato
    private val trainingDays: Map<String, Boolean>
) : BaseAdapter() {

    override fun getCount(): Int {
        return days.size
    }

    override fun getItem(position: Int): Any {
        return days[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.calendar_cell, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val day = days[position]
        viewHolder.txtDay.text = day

        val dateKey = "$displayedYear-${"%02d".format(displayedMonth + 1)}-${"%02d".format(day.toIntOrNull() ?: 0)}"
        val hasTraining = trainingDays[dateKey] == true

        if (hasTraining) {
            // Mostra l'icona dell'allenamento
            viewHolder.imgTraining.visibility = View.VISIBLE
        } else {
            // Nascondi l'icona
            viewHolder.imgTraining.visibility = View.GONE
        }



        // Evidenzia solo la data di oggi
        if (days[position].toIntOrNull() == currentDay &&
            displayedMonth == currentMonth &&
            displayedYear == currentYear) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.giorno_selezionato))
            viewHolder.txtDay.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            viewHolder.txtDay.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }

        return view
    }

    // ViewHolder per migliorare le prestazioni
    private class ViewHolder(view: View) {
        val txtDay: TextView = view.findViewById(R.id.txtDay)
        val imgTraining: ImageView = view.findViewById(R.id.img_training)
    }
}