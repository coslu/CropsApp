package com.cropsapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CameraActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1
        private val REQUIRED_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private val imageCapture =
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
    private lateinit var imageView: ImageView
    private lateinit var previewView: PreviewView
    private lateinit var listener: OrientationEventListener
    private var permissionsDenied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        previewView = findViewById(R.id.preview_view)
        imageView = findViewById(R.id.camera_image_view)

        /* OrientationEventListener to determine target image rotation of imageCapture.
        Is enabled onStart, disabled onStop */
        listener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                imageCapture.targetRotation = when (orientation) {
                    in 225..315 -> Surface.ROTATION_90
                    in 135..225 -> Surface.ROTATION_180
                    in 45..135 -> Surface.ROTATION_270
                    else -> Surface.ROTATION_0
                }
                Log.d("HEY", "orientationSet")
            }
        }

        if (allPermissionsGranted())
            startCamera()
        else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onStart() {
        super.onStart()

        listener.enable()

        /* if we had denied permissions but they are granted now, stop showing the warning
        and start the camera */
        if (permissionsDenied && allPermissionsGranted()) {
            permissionsDenied = false
            setContentView(R.layout.activity_camera)
            previewView = findViewById(R.id.preview_view)
            imageView = findViewById(R.id.camera_image_view)
            startCamera()
        }
        //remove freeze
        imageView.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        listener.disable()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera()
        } else {
            permissionsDenied = true
            //alternative warning layout to ask for permissions
            setContentView(R.layout.activity_camera_alt)
            findViewById<Button>(R.id.button_permission_back).setOnClickListener {
                onBackPressed()
            }
            findViewById<Button>(R.id.button_grant_permission).setOnClickListener {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:com.cropsapp")
                )
                startActivity(intent)
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        //go fullscreen
        supportActionBar?.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = window.insetsController
            windowInsetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController?.hide(WindowInsets.Type.statusBars())
        }

        //bind camera to lifecycle
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))

        val takePictureButton = findViewById<FloatingActionButton>(R.id.button_take_picture)
        takePictureButton.setOnClickListener { takePicture() }
    }

    private fun freezePreview() {
        //play white border animation
        val animation = AnimationUtils.loadAnimation(this, R.anim.take_picture_border_fade)
        val borderView = findViewById<View>(R.id.take_picture_border)
        borderView.alpha = 1.0F
        borderView.startAnimation(animation)

        //static image to replace previewView
        imageView.apply {
            setImageBitmap(previewView.bitmap)
            visibility = View.VISIBLE
        }
    }

    private fun takePicture() {
        freezePreview()

        //data of the saved image
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.GERMANY)
            .format(System.currentTimeMillis()) + ".jpg"

        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), name)
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    //start preview activity when the image is saved
                    val intent = Intent(applicationContext, PreviewActivity::class.java)
                    intent.putExtra("uri", outputFileResults.savedUri.toString())
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {
                    //what TODO here?
                    Toast.makeText(applicationContext, exception.message, Toast.LENGTH_LONG).show()
                }
            })
    }
}