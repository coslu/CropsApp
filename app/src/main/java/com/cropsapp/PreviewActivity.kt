package com.cropsapp

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class PreviewActivity : AppCompatActivity() {
    private lateinit var uri: Uri
    private lateinit var file: File
    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = getString(R.string.title_preview)
        //set image from the saved file uri
        uri = Uri.parse(intent.getStringExtra("uri"))
        file = File(URI(uri.toString()))

        setLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        /* This activity is not destroyed and recreated on orientation change so that
        we can delete onDestroy */
        setLayout()
    }

    override fun onDestroy() {
        super.onDestroy()
        //only keep the file if the save button is clicked, otherwise delete it
        if (!saved)
            file.delete()
    }

    private fun setLayout() {
        setContentView(R.layout.activity_preview)
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
            lifecycleScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
                val url = URL("http://192.168.0.4:5000/image")
                val urlConnection = (url.openConnection() as HttpURLConnection).apply {
                    doOutput = true
                    addRequestProperty("Content-Type", "multipart/form-data;boundary=*****")
                }
                try {
                    DataOutputStream(urlConnection.outputStream).apply {
                        writeBytes("--*****\r\nContent-Disposition: form-data; " +
                                "name=\"test\";filename=\"${file.name}\"\r\n\r\n")
                        write(file.readBytes())
                        writeBytes("\r\n--*****--\r\n")
                        close()
                    }
                    val inputStream = BufferedInputStream(urlConnection.inputStream)
                    val array = inputStream.readBytes()
                    println(String(array))
                } finally {
                    urlConnection.disconnect()
                }
            }
            saved = true
            startActivity(intent)
        }
    }
}