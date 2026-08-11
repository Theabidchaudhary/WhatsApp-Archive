package app.messagememory.data.db

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * The archive database is opened with SQLCipher rather than plain SQLite
 * (defense-in-depth per ARCHITECTURE.md §4 / brief §19 — "consider Android
 * Keystore encryption... where practical"). The passphrase itself is random
 * per-install and stored only inside [EncryptedSharedPreferences], which
 * wraps it with a key sealed in the Android Keystore
 * (`MasterKey.DEFAULT_MASTER_KEY_ALIAS`) — the passphrase never leaves the
 * device and is not derived from anything guessable.
 */
object DbPassphrase {
    private const val PREFS_FILE = "message_memory_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_hex"
    private const val PASSPHRASE_BYTES = 32

    fun getOrCreate(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return existing.hexToBytes()
        }

        val fresh = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_DB_PASSPHRASE, fresh.toHex()).apply()
        return fresh
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        ByteArray(length / 2) { i -> ((this[i * 2].digitToInt(16) shl 4) + this[i * 2 + 1].digitToInt(16)).toByte() }
}
