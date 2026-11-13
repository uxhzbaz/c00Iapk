package com.example.c001apk.util

import android.content.Context
import android.util.Base64
import com.example.c001apk.constant.Constants
import com.example.c001apk.util.Utils.getMD5
import java.io.IOException
import com.example.c001apk.util.Utils.randomAndroidVersionRelease
import com.example.c001apk.util.Utils.randomBrand
import com.example.c001apk.util.Utils.randomDeviceModel
import com.example.c001apk.util.Utils.randomManufacturer
import com.example.c001apk.util.Utils.randomSdkInt
import org.mindrot.jbcrypt.BCrypt
import java.util.Random
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


object TokenDeviceUtils {

    fun randHexString(@Suppress("SameParameterValue") n: Int): String {
        Random().setSeed(System.currentTimeMillis())
        return (0 until n).joinToString("") {
            Random().nextInt(256).toString(16)
        }.uppercase()
    }

    fun getDeviceCode(regenerate: Boolean): String {
        if (regenerate) {
            PrefManager.apply {
                MANUFACTURER = randomManufacturer()
                BRAND = randomBrand()
                MODEL = randomDeviceModel()
                BUILDNUMBER = randHexString(16)
                SDK_INT = randomSdkInt()
                ANDROID_VERSION = randomAndroidVersionRelease()
                USER_AGENT =
                    "Dalvik/2.1.0 (Linux; U; Android $ANDROID_VERSION; ${MODEL} ${BUILDNUMBER}) (#Build; ${BRAND}; ${MODEL}; ${BUILDNUMBER}; $ANDROID_VERSION) +CoolMarket/${VERSION_NAME}-${VERSION_CODE}-${Constants.MODE}"
            }
        }
        val szlmId = if (PrefManager.SZLMID == "") randHexString(32) else PrefManager.SZLMID
        val mac = Utils.randomMacAddress()
        val manuFactor = PrefManager.MANUFACTURER
        val brand = PrefManager.BRAND
        val model = PrefManager.MODEL
        val buildNumber = PrefManager.BUILDNUMBER
        return DeviceCode.encode("$szlmId; ; ; $mac; $manuFactor; $brand; $model; $buildNumber; null")
    }

        private const val B =
        "VFRCVU9GUXNRMEVsTFVNa1dERWpRU0VzVXlFbUxETkFVeTFEUEZZdUl5MGlNVU01SXpCVUpGY3RNeWtoTVVRMUpTMURPU1V4TXpoUUxWTXdWMDBzSXloU01UUXNXVEZEUkZFd1EwUlZNU1FrVVMwaktTTXRKRGhZTFZRa1VqRXpLU1FzTTBCVkxUUXdVaTAwTlNNc00yQlhMVk1sSkMxRE5GbE5MQ1EwV1RFekxTUXhRMEVoTEVNbElqRXpLRmN3TXowbExpTkVWekVqUFNZdU5Ea2tMRk13Vml4VEtGa3RRemhYTEZNc1dDNGpMU1l0VTJCVFRUQTBMRkV1TXpoVExqTWhJakZETUZNc0kwQlZMQ1FrVUN3ekpTSXNJekJTTUVNOFVDeEVMU0l0STJCUUxVUW9VeTBqTEZNc0l5VWtNRlF3VmswdFJDeFlNVE1sSkM0ME5TRXdVMFJRTEZNaElpMGtOU013VTJCUUxqTmdVeXdqT0ZFc05DeFRMRU13V0N4VUxTRXRJeVJUTEROQkl5MDBKRlpOTVVNc1VEQkVKRmd0TkMwa0xEUWtXQzFETUZNeFJDa2pMVk0wVml4VE1GZ3hJeTBtTFZNeEpTMHpKRlF4TTJCUUxTUTRXVEJETUZNdUkwVWhUU3d6UEZndFJEQlJNRE5FVVMwak9GWXVKRGtsTVNRMFV6QXpOU013UkRra01UTWtWU3hVTUZRdFJDeFlNVE5GSXl4RUxGWXNJelVoTGlRMFYwMHhOQ3hWTENNMFZ5NHpKRlV4TkN4VExGUTRWeTRqWUZZeEpEVWpMVU13VXpGRFBGSXNNMEVsTERNc1ZEQTBPU011TkNrbUxFUWtWaXhFTVNKTkxUTW9VaXd6TEZjdFUwUlFMaVF3VlRGREpTVXdVeWtqTVVNaEl5MHpJU1VzSTBFak1UTXdVUzFVTkZjeFJERWxMVVF0SlM0a05GSXNJemhaVFRCVFlGRXdReVJZTFNNbElTNDBMRmdzVkRSWkxVUW9XVEJEUUZZdFJDeFdMak5FVkRCRE1Ga3RORFVsTEZRNFZTMVVKRmd3TXpSV0xEUTRVVTB3UXkwbUxVUTRXUzFUTVNNc1V5VWlNVU5BVVN4VUxGWXVJMFJVTGlNc1dERkVMRk14STJCUUxWTTBXUzRrT0Zjd00wUlhMVk1vV0MwaktGRXhMQ00wVml4VE5GQXRSRFJXTFVRbEpDMVRKRkF3TkN4Z1lB"
    private const val SB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private const val BC = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private const val F = Base64.NO_WRAP or Base64.NO_PADDING
    private val blob by lazy { Base64.decode(B, 0) }

    private fun gen(d: String, t: Long, c: Long): String? {
        val i = ((t + c) % 100).toInt() * 4 + 0x80
        val seg = runCatching { Base64.decode(blob.copyOfRange(i, i + 0x80), 0) }.getOrNull()
            ?: return null
        val p = "${Constants.APP_ID}&${String(seg)}&${d.getMD5()}&$t&$c"
        val s = Base64.encodeToString("${t.toString(16)}/${p.getMD5()}".toByteArray(), F).take(22)
        val z = s.dropLast(1) + SB[(SB.indexOf(s.last()) + 59) % 64]
        if (z.any { it !in BC } || BC.indexOf(z.last()) % 16 != 0) return null
        val h = BCrypt.hashpw(
            Base64.encodeToString(p.toByteArray(), Base64.NO_WRAP).getMD5(),
            "\$2a\$04\$$z"
        )
        return "v3" + Base64.encodeToString(("\$2y" + h.substring(3)).toByteArray(), F)
    }

    fun String.getTokenV3(): String {
        val c = PrefManager.VERSION_CODE.toLongOrNull() ?: Constants.VERSION_CODE.toLong()
        val t = System.currentTimeMillis() / 1000
        for (i in 0L..15L) gen(this, t + i, c)?.let { return it }
        throw IOException("token gen failed")
    }

    private var lf = 0L
    private const val U = "https://auni.telecome.cn/a/mdna/report?v=8.7&t=m&p=F9BD0FBDACFE80FCF34370FBCFEE1A98&r=5bb7efdd4c794f0e9563317b7802b995&n=0&l=2&h=0"

    private fun xor(b: ByteArray): ByteArray {
        val k = "xp7j@&!v5]2k#+3z{n9\$q^r%[8f1*d4e:c>0l?m/6)yb-=w}".toByteArray()
        return ByteArray(b.size) { (b[it].toInt() xor k[it % k.size].toInt()).toByte() }
    }

    // 移植自 coolapk-desktop shuzilm.rs：向数盟申请合法 DUID
    private fun genDuid(): String? = runCatching {
        val r = ByteArray(76).also { SecureRandom().nextBytes(it) }
        fun h(a: Int, n: Int) = r.copyOfRange(a, a + n).joinToString("") { "%02x".format(it) }
        val t = System.currentTimeMillis()
        val j = JSONObject().put("2cO", "${t / 1000}.${"%09d".format(t % 1000 * 1000000)}")
            .put("3mS", "v9.0.1").put("6yY", h(44, 32))
            .put("75c", "11e7b222083a4b732b4b14811f6fc05995a01415eb")
            .put("AAA", "v1.0").put("DUv", "com.coolapk.market").put("foO", h(4, 8).take(15))
            .put("gEd", h(28, 16)).put("sfOo", h(0, 4).uppercase().take(7))
            .put("w91", h(12, 16).uppercase())
        val o = ByteArrayOutputStream()
        DeflaterOutputStream(o, Deflater(6)).use { it.write(j.toString().toByteArray()) }
        val raw = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build().newCall(
            Request.Builder().url(U)
                .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 8.1; TK Watch Build/OPM2.171019.012)")
                .header("Accept-Encoding", "identity")
                .post(xor(o.toByteArray()).toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
        ).execute().use { it.body!!.bytes() }
        val d = xor(raw)
        val s = runCatching { InflaterInputStream(d.inputStream()).readBytes() }.getOrDefault(d)
        JSONObject(String(s)).optString("n_a").takeIf { Regex("DU[A-Za-z0-9_-]{8,126}").matches(it) }
    }.getOrNull()

    @Synchronized
    fun getLastingDeviceCode(): String {
        if (PrefManager.SZLMID.isEmpty() && System.currentTimeMillis() - lf > 60000)
            genDuid()?.let {
                PrefManager.SZLMID = it
                if (PrefManager.xAppDevice != "") PrefManager.xAppDevice = getDeviceCode(false)
            } ?: run { lf = System.currentTimeMillis() }
        if (PrefManager.xAppDevice == "")
            PrefManager.xAppDevice = getDeviceCode(true)
        return PrefManager.xAppDevice
    }

    fun getLastingInstallTime(context: Context): String {
        val sp = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        return sp.getString("INSTALL_TIME", null).let {
            it ?: System.currentTimeMillis().toString().apply {
                sp.edit().putString("INSTALL_TIME", this).apply()
            }
        }
    }

}
