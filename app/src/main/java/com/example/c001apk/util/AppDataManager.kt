package com.example.c001apk.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AppDataManager {

    private const val MAGIC = "C01E"
    private const val PBKDF2_ITER = 100_000
    private const val KEY_LEN = 256

    private val DB_NAMES = listOf(
        "recent_emoji.db", "user_blacklist.db", "topic_blacklist.db",
        "search_history.db", "browse_history.db", "feed_favorite.db",
        "home_menu.db", "recent_at_user.db"
    )

    fun backup(context: Context, uri: Uri, password: String? = null): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { raw ->
            val out: OutputStream = if (password.isNullOrEmpty()) raw else {
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
                raw.write(MAGIC.toByteArray()); raw.write(salt); raw.write(iv)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
                }
                CipherOutputStream(raw, cipher)
            }
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

    fun restore(context: Context, uri: Uri, password: String? = null): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { raw ->
            val buffered = raw.buffered().apply { mark(4) }
            val magic = ByteArray(4)
            val isEncrypted = buffered.read(magic) == 4 && String(magic) == MAGIC
            val input: InputStream = if (!isEncrypted) {
                buffered.reset(); buffered
            } else {
                if (password.isNullOrEmpty()) return false
                val salt = ByteArray(16); val iv = ByteArray(12)
                buffered.read(salt); buffered.read(iv)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
                }
                CipherInputStream(buffered, cipher)
            }
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

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITER, KEY_LEN)
        val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }
}
