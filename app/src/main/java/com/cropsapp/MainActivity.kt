package com.cropsapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileUtils
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.cropsapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.File
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class MainActivity : AppCompatActivity() {
    companion object {
        private const val SERVER_URL = "https://sugarbeet-gbtudlapea-ew.a.run.app"
        private const val LOCAL_URL = "http://192.168.0.4:5000"
    }

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

        val files = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "").listFiles()
            ?: emptyArray()
        binding.recyclerView.adapter = MainAdapter(files)

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0) //TODO remove
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        //if activity is launched from PreviewActivity by clicking the 'Save' button
        intent?.getStringExtra("newFile")?.let { addFile(it) }
    }

    private fun printResult(str: String) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show()
    }

    /**
     * Adds the newly taken picture to the list, sends the image to the server and updates the list
     * with the received result.
     */
    private fun addFile(uriString: String) {
        /*val uri = Uri.parse("file:///storage/emulated/0/Download/test_images.jpg")
        val file2 = File(URI(uri.toString()))*/

        val file = File(URI(uriString))

        lifecycleScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            Looper.prepare()
            val url = URL("$LOCAL_URL/predict")
            val urlConnection = (url.openConnection() as HttpURLConnection).apply {
                doOutput = true
                addRequestProperty("Content-Type", "multipart/form-data;boundary=*****")
            }
            try {
                DataOutputStream(urlConnection.outputStream).apply {
                    writeBytes(
                        "--*****\r\nContent-Disposition: form-data; " +
                                "name=\"image\";filename=${file.name}\r\n\r\n"
                    )
                    write(file.readBytes())
                    writeBytes("\r\n--*****--\r\n")
                    close()
                }
                val inputStream = BufferedInputStream(urlConnection.inputStream)
                val list = String(inputStream.readBytes()).removeSuffix("\n")
                    .removeSurrounding("\"").split('#')
                if (list[0] == file.name)
                    printResult("%.2f".format(list[1].toDouble()))
                else throw Exception()
            } catch (e: Exception) {
                e.printStackTrace()
                printResult("Error")
            } finally {
                urlConnection.disconnect()
            }
        }
    }
}