package com.cropsapp

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class NetworkOperations() {
    companion object {
        private const val SERVER_URL = "https://sugarbeet-gbtudlapea-ew.a.run.app"
        private const val LOCAL_URL = "http://192.168.0.4:5000" //TODO remove
        private const val CONNECTION_TIMEOUT = 15000
        private val awaitingFiles = mutableListOf<String>()
        private val notifiables = mutableListOf<Notifiable>()

        /**
         * Sends the given image file to the server and writes the result
         * into the corresponding text file. Updates the list in MainActivity.
         * Given a DetailActivity, updates the status there as well.
         */
        fun send(file: File, textFile: File) {
            textFile.writeText(MainAdapter.STATUS_AWAITING.toString())
            awaitingFiles.add(textFile.name)

            notifiables.forEach {
                it.notifyStatusChanged()
            }

            val url = URL("${LOCAL_URL}/predict")
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
                notifiables.forEach {
                    it.notifyStatusChanged()
                }
                awaitingFiles.remove(textFile.name)
                urlConnection.disconnect()
            }
        }

        /**
         * Saves files that currently have status "Awaiting Response" into a file so that if they remain
         * that way when the app is closed, their status will be set to "Error" next time the app
         * launches. We call this from onStop in every activity.
         */
        fun saveAwaitingFiles(context: Context) {
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

    /*@SuppressLint("NotifyDataSetChanged")
    private fun updateUi(detailActivity: DetailActivity?) {
        mainActivity.runOnUiThread {
            *//* we need to notifyDataSetChanged because there may be multiple of those running
            and there is no reliable way to know the position of the data that changed *//*
            mainActivity.mainAdapter.notifyDataSetChanged()
        }
        detailActivity?.runOnUiThread {
            detailActivity.updateStatus()
        }
    }*/
}