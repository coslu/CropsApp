package de.tum.sugarbeetmonitor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import de.tum.sugarbeetmonitor.databinding.ActivityPreviewBinding
import java.io.File
import java.net.URI

class PreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.title = getString(R.string.title_preview)

        val deletionFile = File(filesDir, "filesToDelete.txt")
        val filesToDelete = mutableSetOf<String>()
        kotlin.runCatching {
            filesToDelete.addAll(deletionFile.readLines())
        }

        //set image from the saved file uri
        val uri = Uri.parse(intent.getStringExtra("uri"))
        val file = File(URI(uri.toString()))

        filesToDelete.add(file.name)
        saveFilesToDelete(deletionFile, filesToDelete)

        Glide.with(this).load(uri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(binding.previewImageView)
        binding.buttonDiscard.setOnClickListener {
            finish()
        }
        binding.buttonSave.setOnClickListener {
            filesToDelete.remove(file.name)
            saveFilesToDelete(deletionFile, filesToDelete)
            Intent(this, MainActivity::class.java).apply {
                putExtra("newFile", uri.toString())
                startActivity(this)
            }
        }
    }

    private fun saveFilesToDelete(deletionFile: File, filesToDelete: Set<String>) {
        deletionFile.delete()
        filesToDelete.forEach {
            deletionFile.appendText("$it\n")
        }
    }
}