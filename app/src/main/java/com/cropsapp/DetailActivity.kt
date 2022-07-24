package com.cropsapp

import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import com.cropsapp.databinding.ActivityDetailBinding
import java.io.File
import java.net.URI
import java.text.DateFormat
import java.util.*

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val uri = intent.getParcelableExtra<Uri>("uri")
        val result = intent.getDoubleExtra("result", -2.0)
        Log.d("HEYY", result.toString())
        binding.imageViewDetail.setImageURI(uri)
        binding.textDateDetail.text = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.GERMANY)
            .format(Date(File(URI(uri.toString())).lastModified()))
        when (result) {
            -1.0 -> {
                binding.imageStatusDetail.setImageDrawable(AppCompatResources
                    .getDrawable(this, R.drawable.ic_baseline_access_time_24))
                binding.textPredictionDetail.text = getString(R.string.list_item_awaiting)
                binding.buttonRetryDetail.visibility = View.GONE
            }
            in 0.0..100.0 -> {
                binding.imageStatusDetail.setImageDrawable(AppCompatResources
                    .getDrawable(this, R.drawable.ic_baseline_done_24))
                binding.textPredictionDetail.text = getString(R.string.list_item_prediction, result)
                binding.buttonRetryDetail.visibility = View.GONE
            }
            else -> {
                binding.imageStatusDetail.setImageDrawable(AppCompatResources
                    .getDrawable(this, R.drawable.ic_baseline_error_outline_24))
                binding.textPredictionDetail.text = getString(R.string.list_item_error)
                binding.buttonRetryDetail.visibility = View.VISIBLE
            }
        }
    }
}