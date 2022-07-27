package com.cropsapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.*
import android.provider.ContactsContract
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
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.File
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.ArrayList
import kotlin.math.max

class MainActivity : AppCompatActivity(), Notifiable {
    companion object {
        private const val SERVER_URL = "https://sugarbeet-gbtudlapea-ew.a.run.app"
        private const val LOCAL_URL = "http://192.168.0.4:5000" //TODO remove
        private const val CONNECTION_TIMEOUT = 30000
    }

    private lateinit var binding: ActivityMainBinding
    lateinit var mainAdapter: MainAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { view ->
            val intent = Intent(view.context, CameraActivity::class.java)
            startActivity(intent)
        }

        kotlin.runCatching {
            //If there were "awaiting" files left from last time, set them to "error"
            File(filesDir, "awaitingFiles").forEachLine {
                File(filesDir, it).writeText(MainAdapter.STATUS_ERROR.toString())
            }
        }

        //If there were discarded images from last time that were not deleted, delete them now
        deleteFiles()

        //Get notified when the status of a file is changed
        NetworkOperations.addNotifiable(this)

        mainAdapter = MainAdapter(getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        binding.recyclerView.apply {
            adapter = mainAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        //if activity is launched from PreviewActivity by clicking the 'Save' button
        deleteFiles()
        mainAdapter.updateFiles()
        binding.recyclerView.smoothScrollToPosition(0)
        intent?.getStringExtra("newFile")?.let { addFile(it) }
    }

    override fun onResume() {
        super.onResume()
        deleteFiles()
    }

    override fun onStop() {
        NetworkOperations.saveAwaitingFiles(this)
        super.onStop()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun notifyStatusChanged() {
        /* We need to notifyDataSetChanged because when this is called from NetworkOperations,
        there is no way to know the position of the item that is changed. */
        runOnUiThread {
            mainAdapter.notifyDataSetChanged()
        }
    }

    private fun addFile(uriString: String) {
        val file = File(URI(uriString))
        val textFile = file.getTextFile(this)

        CoroutineScope(Dispatchers.IO).launch {
            NetworkOperations.send(file, textFile)
        }
    }

    /**
     * Deletes all images that were discarded from PreviewActivity
     */
    private fun deleteFiles() {
        kotlin.runCatching {
            File(filesDir, "filesToDelete.txt").apply {
                forEachLine {
                    File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), it).apply {
                        getTextFile(this@MainActivity).delete()
                        delete()
                    }
                }
                delete()
            }
        }
    }
}