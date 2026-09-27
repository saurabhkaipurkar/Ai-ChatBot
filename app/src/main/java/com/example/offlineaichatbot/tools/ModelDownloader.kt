package com.example.offlineaichatbot.tools

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ModelDownloader {

    fun modelFile(context: Context) : File = File(context.filesDir, "model.litertlm")
    suspend fun ensureModel(context: Context) : String = withContext(Dispatchers.IO) {

        val targetFile = modelFile(context)
        if(targetFile.exists().not() && targetFile.length() == 0L) {
            context.assets.open("model.litertlm").use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        targetFile.absolutePath
    }
}