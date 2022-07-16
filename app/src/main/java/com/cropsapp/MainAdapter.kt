package com.cropsapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.DateFormat
import java.util.*

class MainAdapter(private val files: Array<File>): RecyclerView.Adapter<MainAdapter.ViewHolder>() {
    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imageView = view.findViewById<ImageView>(R.id.image_view_list_main)
        val textDate = view.findViewById<TextView>(R.id.text_date_list_main)
        val textStatus = view.findViewById<TextView>(R.id.text_status_list_main)
        val textPrediction = view.findViewById<TextView>(R.id.text_prediction_list_main)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_main, parent, false)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.imageView.setImageURI(files[position].toUri())
        holder.textDate.text = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.GERMANY)
            .format(Date(files[position].lastModified()))
    }

    override fun getItemCount(): Int {
        return files.size
    }
}