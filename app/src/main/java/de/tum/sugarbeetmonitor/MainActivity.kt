package de.tum.sugarbeetmonitor

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.divider.MaterialDividerItemDecoration
import de.tum.sugarbeetmonitor.databinding.ActivityMainBinding
import java.io.File
import java.net.URI

class MainActivity : AppCompatActivity(), Notifiable {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainAdapter: MainAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        binding.fab.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        kotlin.runCatching {
            // If there were "awaiting" files left from last time, set them to "error"
            File(filesDir, "awaitingFiles").forEachLine {
                File(filesDir, it).writeText(NetworkOperations.STATUS_ERROR.toString())
            }
        }

        // If there were discarded images from last time that were not deleted, delete them now
        deleteFiles()

        // Get notified when the status of a file is changed
        NetworkOperations.addNotifiable(this)

        mainAdapter = MainAdapter(getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        binding.recyclerView.run {
            adapter = mainAdapter
            addItemDecoration(MaterialDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }

        // initialize preprocessing
        Preprocessing.initialize(this)

        // set landing page text
        val landingText = getString(R.string.text_landing_page)
        val iconIndex = landingText.indexOf("%icon")
        val span = ImageSpan(this, R.drawable.ic_baseline_photo_camera_24)
        SpannableString(landingText).let {
            it.setSpan(span, iconIndex, iconIndex + 6, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            binding.textLanding.text = it
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // if activity is launched from PreviewActivity by clicking the 'Save' button
        intent?.getStringExtra("newFile")?.let {
            mainAdapter.addFile()
            binding.recyclerView.smoothScrollToPosition(0)
            NetworkOperations.send(File(URI(it)), this)
        }

        // if activity is launched from DetailActivity by deleting the image
        intent?.getStringExtra("deletedFile")?.let {
            mainAdapter.deleteFile(it.toInt())
        }
    }

    override fun onStart() {
        super.onStart()
        deleteFiles()
    }

    override fun onResume() {
        super.onResume()
        binding.textLanding.visibility =
            if (mainAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun notifyStatusChanged() = runOnUiThread {
        /* We need to notifyDataSetChanged because when this is called from NetworkOperations,
        there is no way to know the position of the item that is changed. */
        mainAdapter.notifyDataSetChanged()
    }

    /**
     * Deletes all images that were discarded from PreviewActivity or deleted from DetailActivity
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