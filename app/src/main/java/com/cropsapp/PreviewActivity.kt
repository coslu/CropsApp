package com.cropsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cropsapp.databinding.ActivityPreviewBinding
import java.io.File
import java.net.URI

class PreviewActivity : AppCompatActivity() {
    private lateinit var uri: Uri
    private lateinit var file: File
    private lateinit var binding: ActivityPreviewBinding
    private lateinit var deletionFile: File
    private var filesToDelete = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.title = getString(R.string.title_preview)

        deletionFile = File(filesDir, "filesToDelete.txt")
        kotlin.runCatching {
            filesToDelete = deletionFile.readLines().toMutableSet()
        }

        //set image from the saved file uri
        uri = Uri.parse(intent.getStringExtra("uri"))
        file = File(URI(uri.toString()))

        filesToDelete.add(file.name)
        saveFilesToDelete()

        Glide.with(this).load(uri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(binding.previewImageView)
        binding.buttonDiscard.setOnClickListener {
            finish()
        }
        binding.buttonSave.setOnClickListener {
            filesToDelete.remove(file.name)
            saveFilesToDelete()
            Intent(this, MainActivity::class.java).apply {
                putExtra("newFile", uri.toString())
                startActivity(this)
            }
        }
    }

    private fun saveFilesToDelete() {
        deletionFile.delete()
        filesToDelete.forEach {
            deletionFile.appendText("$it\n")
        }
    }
}