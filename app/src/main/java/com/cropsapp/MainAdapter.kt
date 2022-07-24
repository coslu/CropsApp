package com.cropsapp

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import java.text.DateFormat
import java.util.*

class MainAdapter(private val context: Context) : RecyclerView.Adapter<MainAdapter.ViewHolder>() {
    companion object {
        const val STATUS_ERROR = -2.0
        const val STATUS_AWAITING = -1.0
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val constaintLayout = view.findViewById<ConstraintLayout>(R.id.constrint_layout_list_item)
        val imageView = view.findViewById<ImageView>(R.id.image_view_list_main)
        val textDate = view.findViewById<TextView>(R.id.text_date_list_main)
        val textStatus = view.findViewById<TextView>(R.id.text_status_list_main)
        val textPrediction = view.findViewById<TextView>(R.id.text_prediction_list_main)
        val imageStatus = view.findViewById<ImageView>(R.id.image_status_list_main)
    }

    private var files =
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "").listFiles()
            ?: emptyArray()

    init {
        files.sortByDescending { it.lastModified() }
    }

    fun updateFiles() {
        files =
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "").listFiles()
                ?: emptyArray()
        files.sortByDescending { it.lastModified() }
        notifyItemInserted(0)
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
        holder.textPrediction.text = context.getString(R.string.list_item_prediction_empty)
        var result = -2.0
        with(holder.textStatus.context) {
            val name = "${files[position].nameWithoutExtension}.txt"
            try {
                val textFile = File(filesDir, name)
                result = textFile.readText().toDouble()
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
                holder.textStatus.text =
                    getString(R.string.list_item_error)
            }
        }

        holder.itemView.setOnClickListener {
            Intent(it.context, DetailActivity::class.java).apply {
                putExtra("uri", files[position].toUri())
                putExtra("result", result)
                it.context.startActivity(this)
            }
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }
}