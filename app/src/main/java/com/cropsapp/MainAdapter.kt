package com.cropsapp

import android.content.Context
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
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import java.text.DateFormat
import java.util.*

class MainAdapter(private val context: Context) : RecyclerView.Adapter<MainAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView = view.findViewById<ImageView>(R.id.image_view_list_main)
        val textDate = view.findViewById<TextView>(R.id.text_date_list_main)
        val textStatus = view.findViewById<TextView>(R.id.text_status_list_main)
        val textPrediction = view.findViewById<TextView>(R.id.text_prediction_list_main)
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
        with(holder.textStatus.context) {
            val name = "${files[position].nameWithoutExtension}.txt"
            try {
                val textFile = File(filesDir, name)
                holder.textStatus.text =
                    when (textFile.readText().toDouble()) {
                        -1.0 -> getString(
                            R.string.list_item_status,
                            getString(R.string.list_item_awaiting)
                        )
                        in 0.0..100.0 -> let {
                            holder.textPrediction.text = getString(
                                R.string.list_item_prediction,
                                textFile.readText().toDouble()
                            )
                            getString(
                                R.string.list_item_status,
                                getString(R.string.list_item_complete)
                            )
                        }
                        else -> getString(
                            R.string.list_item_status,
                            getString(R.string.list_item_error)
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                holder.textStatus.text =
                    getString(R.string.list_item_status, getString(R.string.list_item_error))
            }
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }
}