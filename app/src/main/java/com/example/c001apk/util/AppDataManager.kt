package com.example.c001apk.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object AppDataManager {

    private val DB_NAMES = listOf(
        "recent_emoji.db", "user_blacklist.db", "topic_blacklist.db",
        "search_history.db", "browse_history.db", "feed_favorite.db",
        "home_menu.db", "recent_at_user.db"
    )

    fun backup(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/settings.xml")
                addFile(zip, prefsFile, "shared_prefs/settings.xml")

                val dbDir = context.getDatabasePath(DB_NAMES.first()).parentFile
                DB_NAMES.forEach { name ->
                    listOf(name, "$name-wal", "$name-shm").forEach { fn ->
                        addFile(zip, File(dbDir, fn), "databases/$fn")
                    }
                }
            }
        } ?: return false
    }.isSuccess

    fun restore(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val target = when {
                        entry.name.startsWith("shared_prefs/") ->
                            File(context.applicationInfo.dataDir, entry.name)

                        entry.name.startsWith("databases/") ->
                            File(
                                context.getDatabasePath(DB_NAMES.first()).parentFile,
                                entry.name.removePrefix("databases/")
                            )

                        else -> null
                    }
                    target?.let {
                        it.parentFile?.mkdirs()
                        it.outputStream().use { out -> zip.copyTo(out) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: return false
    }.isSuccess

    fun restartApp(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val restartIntent = Intent.makeRestartActivityTask(launchIntent?.component)
        context.startActivity(restartIntent)
        Runtime.getRuntime().exit(0)
    }

    private fun addFile(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }
}
