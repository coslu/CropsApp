package com.cropsapp

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import java.io.File
import java.net.URI

class PreviewActivity : AppCompatActivity() {
    private lateinit var uri: Uri
    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        supportActionBar?.title = getString(R.string.title_preview)

        //set image from the saved file uri
        uri = Uri.parse(intent.getStringExtra("uri"))
        val imageView = findViewById<ImageView>(R.id.preview_image_view)
        imageView.setImageURI(uri)

        //set button click listeners
        val discardButton = findViewById<Button>(R.id.button_discard)
        val saveButton = findViewById<Button>(R.id.button_save)
        val intent = Intent(this, MainActivity::class.java)
        discardButton.setOnClickListener {
            finish()
        }
        saveButton.setOnClickListener {
            saved = true
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //only keep the file if the save button is clicked, otherwise delete it
        if (!saved)
            File(URI(uri.toString())).delete()
    }
}