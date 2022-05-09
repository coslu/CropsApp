package com.cropsapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class PreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        val uri = Uri.parse(intent.getStringExtra("uri"))
        val imageView = findViewById<ImageView>(R.id.preview_image_view)
        imageView.setImageURI(uri)
    }
}