package com.example.youcoach

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StatisticaAdapter(private val statistiche: List<StatisticaPartita>) :
    RecyclerView.Adapter<StatisticaAdapter.StatViewHolder>() {

    inner class StatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label = view.findViewById<TextView>(R.id.text_label)
        val leftValue = view.findViewById<TextView>(R.id.text_value_left)
        val rightValue = view.findViewById<TextView>(R.id.text_value_right)
        val barLeft = view.findViewById<View>(R.id.bar_left)
        val barRight = view.findViewById<View>(R.id.bar_right)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistica, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        val stat = statistiche[position]
        holder.label.text = stat.nome
        holder.leftValue.text = stat.casaValue
        holder.rightValue.text = stat.ospiteValue

        val leftWeight = stat.casaPercentage
        val rightWeight = stat.ospitePercentage

        holder.barLeft.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, leftWeight)
        holder.barRight.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, rightWeight)
    }

    override fun getItemCount() = statistiche.size
}
