package com.cropsapp

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.cropsapp.databinding.ActivityDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(binding.root)
        val uri = intent.getStringExtra("uriString")?.toUri()
        Glide.with(this).load(uri).into(binding.imageViewDetail)

        file = File(URI(uri.toString()))
        textFile = file.getTextFile(this)
        notifyStatusChanged()

        binding.textDateDetail.text = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.GERMANY)
            .format(Date(file.lastModified()))

        binding.buttonRetryDetail.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                NetworkOperations.send(file, this@DetailActivity)
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
                        .getDrawable(this, R.drawable.ic_schedule_24)
                )

                binding.textPredictionDetail.text = getString(R.string.list_item_awaiting)
                binding.buttonRetryDetail.visibility = View.GONE
            }
            in 0.0..100.0 -> {
                binding.imageStatusDetail.setImageDrawable(
                    AppCompatResources
                        .getDrawable(this, R.drawable.ic_check_circle_24)
                )
                binding.textPredictionDetail.text =
                    getString(R.string.list_item_prediction, result)
                binding.buttonRetryDetail.visibility = View.GONE
            }
            else -> {
                binding.imageStatusDetail.setImageDrawable(
                    AppCompatResources
                        .getDrawable(this, R.drawable.ic_error_24)
                )
                binding.textPredictionDetail.text = getString(R.string.list_item_error)
                binding.buttonRetryDetail.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Creates and shows an AlertDialog to confirm deletion of the file
     */
    private fun confirmDelete() =
        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
        )
            .setTitle(R.string.alert_title_delete)
            .setMessage(R.string.alert_message_delete)
            .setNeutralButton(R.string.alert_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(R.string.alert_positive_delete) { _, _ ->
                delete()
            }
            .setIcon(R.drawable.ic_delete_24)
            .show()


    /**
     * Deletes the file associated with this activity
     */
    private fun delete() {
        File(filesDir, "filesToDelete.txt").writeText(file.name)
        Intent(this, MainActivity::class.java).apply {
            putExtra("deletedFile", position)
            startActivity(this)
        }
    }
}