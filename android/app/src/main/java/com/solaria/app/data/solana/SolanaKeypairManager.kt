package com.solaria.app.data.solana

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Ed25519 keypairs for Solana wallets.
 * Private keys are stored in Android EncryptedSharedPreferences.
 * Equivalent to `solana-keygen new` from the CLI.
 */
@Singleton
class SolanaKeypairManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "solaria_wallet_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Generate a new Ed25519 keypair. Returns the public key as Base58.
     * The private seed is securely stored in EncryptedSharedPreferences.
     */
    fun generateKeypair(): String {
        val seed = ByteArray(32)
        SecureRandom().nextBytes(seed)

        val privKeySpec = EdDSAPrivateKeySpec(seed, spec)
        val publicKeyBytes = privKeySpec.a.toByteArray()
        val publicKeyBase58 = Base58.encode(publicKeyBytes)

        // Store the 32-byte seed (from which both private & public keys derive)
        encryptedPrefs.edit()
            .putString("seed_$publicKeyBase58", android.util.Base64.encodeToString(seed, android.util.Base64.NO_WRAP))
            .putString("active_wallet", publicKeyBase58)
            .apply()

        return publicKeyBase58
    }

    /**
     * Import a wallet from a Base58-encoded private key (64-byte expanded key)
     * or a 32-byte seed.
     */
    fun importFromPrivateKey(base58Key: String): String {
        val keyBytes = Base58.decode(base58Key)
        val seed = if (keyBytes.size == 64) keyBytes.copyOfRange(0, 32) else keyBytes
        require(seed.size == 32) { "Invalid key: expected 32 or 64 bytes, got ${keyBytes.size}" }

        val privKeySpec = EdDSAPrivateKeySpec(seed, spec)
        val publicKeyBytes = privKeySpec.a.toByteArray()
        val publicKeyBase58 = Base58.encode(publicKeyBytes)

        encryptedPrefs.edit()
            .putString("seed_$publicKeyBase58", android.util.Base64.encodeToString(seed, android.util.Base64.NO_WRAP))
            .putString("active_wallet", publicKeyBase58)
            .apply()

        return publicKeyBase58
    }

    /** Get the currently active wallet public key, or null if none exists. */
    fun getActiveWalletAddress(): String? = encryptedPrefs.getString("active_wallet", null)

    /** Check if we have a locally-stored keypair for the given address. */
    fun hasKeypair(address: String): Boolean =
        encryptedPrefs.getString("seed_$address", null) != null

    /** Get the 32-byte public key bytes for an address. */
    fun getPublicKeyBytes(address: String): ByteArray = Base58.decode(address)

    /**
     * Sign a message with the keypair for the given wallet address.
     * Returns a 64-byte Ed25519 signature.
     */
    fun sign(address: String, message: ByteArray): ByteArray {
        val seedBase64 = encryptedPrefs.getString("seed_$address", null)
            ?: throw IllegalStateException("No keypair found for $address")
        val seed = android.util.Base64.decode(seedBase64, android.util.Base64.NO_WRAP)

        val privKeySpec = EdDSAPrivateKeySpec(seed, spec)
        val privateKey = EdDSAPrivateKey(privKeySpec)

        val engine = EdDSAEngine(MessageDigest.getInstance(spec.hashAlgorithm))
        engine.initSign(privateKey)
        engine.update(message)
        return engine.sign()
    }

    /** Remove all stored keys (for wallet disconnect). */
    fun clearKeypair(address: String) {
        encryptedPrefs.edit()
            .remove("seed_$address")
            .apply()
        // If this was the active wallet, clear it
        if (encryptedPrefs.getString("active_wallet", null) == address) {
            encryptedPrefs.edit().remove("active_wallet").apply()
        }
    }

    /** Export the private key as Base58 for backup (64 bytes: seed + pubkey). */
    fun exportPrivateKey(address: String): String? {
        val seedBase64 = encryptedPrefs.getString("seed_$address", null) ?: return null
        val seed = android.util.Base64.decode(seedBase64, android.util.Base64.NO_WRAP)
        val privKeySpec = EdDSAPrivateKeySpec(seed, spec)
        val pubKeyBytes = privKeySpec.a.toByteArray()
        // Solana CLI format: 64-byte array [seed(32) + pubkey(32)]
        return Base58.encode(seed + pubKeyBytes)
    }
}
