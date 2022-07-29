package com.cropsapp

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.cropsapp.databinding.ActivityPreviewBinding
import java.io.File
import java.net.URI

class PreviewActivity : AppCompatActivity() {
    private lateinit var uri: Uri
    private lateinit var file: File
    private lateinit var binding: ActivityPreviewBinding
    private lateinit var filesToDelete: File
    private var set = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.title_preview)

        filesToDelete = File(filesDir, "filesToDelete.txt")
        kotlin.runCatching {
            set = filesToDelete.readLines().toMutableSet()
        }

        //set image from the saved file uri
        uri = Uri.parse(intent.getStringExtra("uri"))
        file = File(URI(uri.toString()))

        set.add(file.name)

        Glide.with(this).load(uri).into(binding.previewImageView)
        binding.buttonDiscard.setOnClickListener {
            finish()
        }
        binding.buttonSave.setOnClickListener {
            set.remove(file.name)
            saveFilesToDelete()
            Intent(this, MainActivity::class.java).apply {
                putExtra("newFile", uri.toString())
                startActivity(this)
            }
        }
    }

    override fun onStop() {
        //only keep the file if the save button is clicked, otherwise delete it
        saveFilesToDelete()

        NetworkOperations.saveAwaitingFiles(this)
        super.onStop()
    }

    private fun saveFilesToDelete() {
        filesToDelete.delete()
        set.forEach {
            filesToDelete.appendText("$it\n")
        }
    }
}