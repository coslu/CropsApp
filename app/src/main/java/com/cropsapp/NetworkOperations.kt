package com.cropsapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object NetworkOperations {
    const val STATUS_AWAITING = -1.0
    const val STATUS_ERROR = -2.0
    private const val SERVER_URL = "https://sugarbeet-gbtudlapea-ew.a.run.app"
    private const val LOCAL_URL = "http://192.168.0.2:5000" //TODO remove
    private const val CONNECTION_TIMEOUT = 15000
    private val awaitingFiles = mutableListOf<String>()
    private val notifiables = mutableListOf<Notifiable>()

    /**
     * Sends the given image file to the server and writes the result
     * into the corresponding text file. Notifies all notifiables
     * on each status change.
     * @param file The image file to send to the server
     * @param context A context of the application package
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun send(file: File, context: Context) = CoroutineScope(Dispatchers.IO).launch {
        val textFile = file.getTextFile(context)
        textFile.writeText(STATUS_AWAITING.toString())
        awaitingFiles.add(textFile.name)
        saveAwaitingFiles(context)

        notifiables.forEach {
            it.notifyStatusChanged()
        }

        val processedBitmap = Preprocessing.instance.crop(BitmapFactory.decodeFile(file.path))
        val newFile = File(context.filesDir, file.name)
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, newFile.outputStream())

        val url = URL("${SERVER_URL}/predict")
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
                write(newFile.readBytes())
                writeBytes("\r\n--*****--\r\n")
                close()
            }
            val inputStream = BufferedInputStream(urlConnection.inputStream)
            val result = String(inputStream.readBytes()).toDouble().coerceIn(0.0..100.0)
            textFile.writeText(result.toString())
        } catch (e: Exception) {
            textFile.writeText(STATUS_ERROR.toString())
        } finally {
            notifiables.forEach {
                it.notifyStatusChanged()
            }
            awaitingFiles.remove(textFile.name)
            saveAwaitingFiles(context)
            urlConnection.disconnect()
            newFile.delete()
        }
    }

    /**
     * Saves files that currently have status "Awaiting Response" into a file so that if they
     * remain that way when the app is closed, their status will be set to "Error"
     * next time the app launches.
     */
    private fun saveAwaitingFiles(context: Context) {
        File(context.filesDir, "awaitingFiles").apply {
            delete()
            awaitingFiles.forEach {
                appendText("${it}\n")
            }
        }
    }

    fun addNotifiable(notifiable: Notifiable) = notifiables.add(notifiable)

    fun removeNotifiable(notifiable: Notifiable) = notifiables.remove(notifiable)
}