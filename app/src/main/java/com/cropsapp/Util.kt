package com.cropsapp

import android.content.Context
import java.io.File

fun File.getTextFile(context: Context): File =
    File(context.filesDir, "${nameWithoutExtension}.txt")