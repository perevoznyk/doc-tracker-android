package com.example.doctracker.data

import android.content.Context
import android.net.Uri
import java.io.File

object PhotoStore {
    private fun dir(ctx: Context) = File(ctx.filesDir, "photos").apply { mkdirs() }

    fun newFile(ctx: Context): File = File(dir(ctx), "doc_${System.currentTimeMillis()}.jpg")

    /** Copies a picked image into private storage so it stays available after the picker permission ends. */
    fun import(ctx: Context, uri: Uri): String {
        val f = newFile(ctx)
        ctx.contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it) } }
        return f.absolutePath
    }

    fun delete(path: String?) { path?.let { File(it).delete() } }
}
