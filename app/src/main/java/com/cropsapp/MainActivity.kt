package com.cropsapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.FileUtils
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.cropsapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { view ->
            val intent = Intent(view.context, CameraActivity::class.java)
            startActivity(intent)
        }

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)

        val uri = Uri.parse("file:///storage/emulated/0/Download/test_images.jpg")
        val file = File(URI(uri.toString()))

        binding.buttonDebug.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
                val url = URL("http://192.168.0.4:5000/predict")
                val urlConnection = (url.openConnection() as HttpURLConnection).apply {
                    doOutput = true
                    addRequestProperty("Content-Type", "multipart/form-data;boundary=*****")
                }
                try {
                    DataOutputStream(urlConnection.outputStream).apply {
                        writeBytes(
                            "--*****\r\nContent-Disposition: form-data; " +
                                    "name=\"image\";filename=\"image.jpg\"\r\n\r\n"
                        )
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
        }
    }
}