package com.cropsapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileUtils
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cropsapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.File
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    companion object {
        private const val SERVER_URL = "https://sugarbeet-gbtudlapea-ew.a.run.app"
        private const val LOCAL_URL = "http://192.168.0.4:5000" //TODO remove
        private const val CONNECTION_TIMEOUT = 30000
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mainAdapter: MainAdapter
    private val newFiles = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { view ->
            val intent = Intent(view.context, CameraActivity::class.java)
            startActivity(intent)
        }

        mainAdapter = MainAdapter(this)
        binding.recyclerView.apply {
            adapter = mainAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }


        lifecycleScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            Looper.prepare()
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0
        ) //TODO remove
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        //if activity is launched from PreviewActivity by clicking the 'Save' button
        mainAdapter.updateFiles()
        binding.recyclerView.smoothScrollToPosition(0)
        intent?.getStringExtra("newFile")?.let { addFile(it) }
    }

    override fun onDestroy() {
        for (file in newFiles) {
            file.writeText(MainAdapter.STATUS_ERROR.toString())
        }

        super.onDestroy()
    }

    /**
     * Adds the newly taken picture to the list, sends the image to the server and updates the list
     * with the received result.
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun addFile(uriString: String) {
        val file = File(URI(uriString))
        val textFile = File(filesDir, "${file.nameWithoutExtension}.txt")
        newFiles.add(textFile)

        lifecycleScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            val url = URL("$LOCAL_URL/predict")
            val urlConnection = (url.openConnection() as HttpURLConnection).apply {
                doOutput = true
                addRequestProperty("Content-Type", "multipart/form-data;boundary=*****")
                connectTimeout = CONNECTION_TIMEOUT
            }
            try {
                DataOutputStream(urlConnection.outputStream).apply {
                    writeBytes(
                        "--*****\r\nContent-Disposition: form-data; " +
                                "name=image;filename=${file.name}\r\n\r\n"
                    )
                    write(file.readBytes())
                    writeBytes("\r\n--*****--\r\n")
                    close()
                }
                val inputStream = BufferedInputStream(urlConnection.inputStream)
                val list = String(inputStream.readBytes()).removeSuffix("\n")
                    .removeSurrounding("\"").split('#')
                val result = list[1].toDouble().coerceIn(0.0..100.0)
                textFile.writeText(result.toString())
            } catch (e: Exception) {
                textFile.writeText(MainAdapter.STATUS_ERROR.toString())
            } finally {
                runOnUiThread {
                    /*
                    we need to notifyDataSetChanged because there may be multiple of those running
                    and there is no reliable way to know the position of the data that changed
                     */
                    binding.recyclerView.adapter?.notifyDataSetChanged()
                }
                newFiles.remove(textFile)
                urlConnection.disconnect()
            }
        }
    }
}