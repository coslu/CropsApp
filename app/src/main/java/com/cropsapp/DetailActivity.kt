package com.cropsapp

import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cropsapp.databinding.ActivityDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.text.DateFormat
import java.util.*

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var textFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val uri = intent.getParcelableExtra<Uri>("uri")
        Glide.with(this).load(uri).into(binding.imageViewDetail)

        val file = File(URI(uri.toString()))
        textFile = file.getTextFile(this)
        updateStatus()

        binding.textDateDetail.text = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.GERMANY)
            .format(Date(file.lastModified()))

        binding.buttonRetryDetail.setOnClickListener {
            val detailActivity = this
            CoroutineScope(Dispatchers.IO).launch {
                NetworkOperations.defaultOperator.send(file, textFile, detailActivity)
            }
        }
    }

    override fun onStop() {
        NetworkOperations.defaultOperator.saveAwaitingFiles()
        super.onStop()
    }

    fun updateStatus() =
        when (val result = textFile.readText().toDouble()) {
            -1.0 -> {
                binding.imageStatusDetail.setImageDrawable(
                    AppCompatResources
                        .getDrawable(this, R.drawable.ic_baseline_access_time_24)
                )

                binding.textPredictionDetail.text = getString(R.string.list_item_awaiting)
                binding.buttonRetryDetail.visibility = View.GONE
            }
            in 0.0..100.0 -> {
                binding.imageStatusDetail.setImageDrawable(
                    AppCompatResources
                        .getDrawable(this, R.drawable.ic_baseline_done_24)
                )
                binding.textPredictionDetail.text = getString(R.string.list_item_prediction, result)
                binding.buttonRetryDetail.visibility = View.GONE
            }
            else -> {
                binding.imageStatusDetail.setImageDrawable(
                    AppCompatResources
                        .getDrawable(this, R.drawable.ic_baseline_error_outline_24)
                )
                binding.textPredictionDetail.text = getString(R.string.list_item_error)
                binding.buttonRetryDetail.visibility = View.VISIBLE
            }
        }
}