package com.example.c001apk.util

import android.content.Context
import android.net.Uri
import android.os.Build
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WebDavManager {

    private val client by lazy { OkHttpClient() }

    fun fileName(): String {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "c001apk_${Build.MANUFACTURER}_${Build.MODEL}_$time.zip".replace(" ", "_")
    }

    private fun Request.Builder.withAuth() = apply {
        if (PrefManager.webdavUser.isNotEmpty())
            header("Authorization", Credentials.basic(PrefManager.webdavUser, PrefManager.webdavPass))
    }

    fun upload(context: Context, password: String?): Boolean = runCatching {
        val tmp = File(context.cacheDir, "webdav_backup.tmp")
        check(AppDataManager.backup(context, Uri.fromFile(tmp), password))
        val url = PrefManager.webdavUrl.trimEnd('/') + "/" + fileName()
        val request = Request.Builder().url(url).withAuth()
            .put(tmp.asRequestBody("application/octet-stream".toMediaType()))
            .build()
        client.newCall(request).execute().use { it.isSuccessful }
    }.getOrDefault(false)

    fun listBackups(): List<String> = runCatching {
        val request = Request.Builder()
            .url(PrefManager.webdavUrl)
            .method("PROPFIND", null)
            .header("Depth", "1")
            .withAuth()
            .build()
        val body = client.newCall(request).execute().use { it.body?.string() }.orEmpty()
        Regex("""<[^>]*:?href>([^<]*\.zip)</[^>]*:?href>""").findAll(body)
            .map { it.groupValues[1].substringAfterLast('/') }
            .distinct().toList()
    }.getOrDefault(emptyList())

    fun download(context: Context, name: String, password: String?): Boolean = runCatching {
        val url = PrefManager.webdavUrl.trimEnd('/') + "/" + name
        val request = Request.Builder().url(url).withAuth().build()
        val tmp = File(context.cacheDir, "webdav_restore.tmp")
        val ok = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@use false
            resp.body?.byteStream()?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            true
        }
        ok && AppDataManager.restore(context, Uri.fromFile(tmp), password)
    }.getOrDefault(false)
}
