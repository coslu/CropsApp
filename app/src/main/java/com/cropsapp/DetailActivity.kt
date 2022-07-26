package com.cropsapp

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.cropsapp.databinding.ActivityDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.text.DateFormat
import java.util.*

class DetailActivity : AppCompatActivity(), Notifiable {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var textFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NetworkOperations.addNotifiable(this)

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val uri = intent.getParcelableExtra<Uri>("uri")
        Glide.with(this).load(uri).into(binding.imageViewDetail)

        val file = File(URI(uri.toString()))
        textFile = file.getTextFile(this)
        notifyStatusChanged()

        binding.textDateDetail.text = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.GERMANY)
            .format(Date(file.lastModified()))

        binding.buttonRetryDetail.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                NetworkOperations.send(file, textFile)
            }
        }
    }

    override fun onStop() {
        NetworkOperations.saveAwaitingFiles(this)
        NetworkOperations.removeNotifiable(this)
        super.onStop()
    }

    override fun notifyStatusChanged() {
        runOnUiThread {
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
    }
}