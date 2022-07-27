package com.cropsapp

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
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
    private lateinit var file: File
    private lateinit var textFile: File
    private var position: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NetworkOperations.addNotifiable(this)
        position = intent.getStringExtra("position")

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val uri = intent.getParcelableExtra<Uri>("uri")
        Glide.with(this).load(uri).into(binding.imageViewDetail)

        file = File(URI(uri.toString()))
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
        super.onStop()
    }

    override fun onDestroy() {
        NetworkOperations.removeNotifiable(this)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> confirmDelete()
            android.R.id.home -> finish()
        }
        return true
    }

    override fun notifyStatusChanged() = runOnUiThread {
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
                binding.textPredictionDetail.text =
                    getString(R.string.list_item_prediction, result)
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

    private fun confirmDelete() = AlertDialog.Builder(this).apply {
        setTitle(R.string.alert_title_delete)
        setMessage(R.string.alert_message_delete)
        setNegativeButton(R.string.alert_cancel) { dialog, _ ->
            dialog.cancel()
        }
        setPositiveButton(R.string.alert_positive_delete) { dialog, _ ->
            delete()
        }
        show()
    }


    private fun delete() {
        File(filesDir, "filesToDelete.txt").writeText(file.name)
        Intent(this, MainActivity::class.java).apply {
            putExtra("deletedFile", position)
            startActivity(this)
        }
    }
}