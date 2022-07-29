package com.cropsapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.cropsapp.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.File
import java.net.URI

class MainActivity : AppCompatActivity(), Notifiable {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainAdapter: MainAdapter

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

        deleteFiles()

        //if activity is launched from PreviewActivity by clicking the 'Save' button
        intent?.getStringExtra("newFile")?.let {
            mainAdapter.addFile()
            binding.recyclerView.smoothScrollToPosition(0)
            addFile(it)
        }

        //if activity is launched from DetailActivity by deleting the image
        intent?.getStringExtra("deletedFile")?.let {
            mainAdapter.deleteFile(it.toInt())
        }
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
    override fun notifyStatusChanged() = runOnUiThread {
        /* We need to notifyDataSetChanged because when this is called from NetworkOperations,
        there is no way to know the position of the item that is changed. */
        mainAdapter.notifyDataSetChanged()
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