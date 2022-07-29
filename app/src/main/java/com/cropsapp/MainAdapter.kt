package com.cropsapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import java.text.DateFormat
import java.util.*

class MainAdapter(private val picturesDirectory: File?) :
    RecyclerView.Adapter<MainAdapter.ViewHolder>() {
    companion object {
        const val STATUS_ERROR = -2.0
        const val STATUS_AWAITING = -1.0
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_view_list_main)
        val textDate: TextView = view.findViewById(R.id.text_date_list_main)
        val textStatus: TextView = view.findViewById(R.id.text_status_list_main)
        val textPrediction: TextView = view.findViewById(R.id.text_prediction_list_main)
        val imageStatus: ImageView = view.findViewById(R.id.image_status_list_main)
    }

    private var files =
        File(picturesDirectory, "").listFiles()
            ?: emptyArray()

    init {
        files.sortByDescending { it.lastModified() }
    }

    private fun updateFiles() {
        files =
            File(picturesDirectory, "").listFiles()
                ?: emptyArray()
        files.sortByDescending { it.lastModified() }
    }

    fun addFile() {
        updateFiles()
        notifyItemInserted(0)
    }

    fun deleteFile(position: Int) {
        updateFiles()
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_main, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.imageView.context).load(files[position]).into(holder.imageView)
        holder.textDate.text = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.GERMANY)
            .format(Date(files[position].lastModified()))
        holder.textPrediction.text = holder.textPrediction.context
            .getString(R.string.list_item_prediction_empty)
        with(holder.textStatus.context) {
            val textFile = files[position].getTextFile(this)
            try {
                val result = textFile.readText().toDouble()
                if (result in 0.0..100.0)
                    holder.textPrediction.text = getString(R.string.list_item_prediction, result)
                holder.textStatus.text =
                    when (result) {
                        STATUS_AWAITING -> getString(R.string.list_item_awaiting)
                        in 0.0..100.0 -> getString(R.string.list_item_complete)
                        else -> getString(R.string.list_item_error)
                    }
                holder.imageStatus.setImageDrawable(
                    when (result) {
                        STATUS_AWAITING -> AppCompatResources.getDrawable(
                            this,
                            R.drawable.ic_baseline_access_time_24
                        )
                        in 0.0..100.0 -> AppCompatResources.getDrawable(
                            this,
                            R.drawable.ic_baseline_done_24
                        )
                        else -> AppCompatResources.getDrawable(
                            this,
                            R.drawable.ic_baseline_error_outline_24
                        )
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                textFile.writeText(STATUS_ERROR.toString())
                holder.textStatus.text = getString(R.string.list_item_error)
            }
        }

        holder.itemView.setOnClickListener {
            Intent(it.context, DetailActivity::class.java).apply {
                putExtra("uri", files[position].toUri())
                putExtra("position", holder.adapterPosition.toString())
                it.context.startActivity(this)
            }
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }
}