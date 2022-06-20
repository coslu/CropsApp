package com.cropsapp

import android.content.Intent
import android.os.Bundle
import android.os.FileUtils
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        binding.buttonDebug.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
                val url = URL("http://192.168.0.4:5000/add")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.doOutput = true
                try {
                    urlConnection.outputStream.write("data=3;4".toByteArray())
//                    val inputStream = BufferedInputStream(urlConnection.inputStream)
                    val array = urlConnection.inputStream.readBytes()
                    println(String(array))
                } catch (e:Exception) {
                    e.printStackTrace()
                } finally {
                    urlConnection.disconnect()
                }
            }
        }
    }
}