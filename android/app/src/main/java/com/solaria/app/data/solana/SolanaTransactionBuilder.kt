package com.solaria.app.data.solana

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and signs raw Solana transactions for System Program transfers.
 * Implements the Solana transaction wire format:
 *   compact-u16(num_signatures) | signatures | message
 *
 * The message format:
 *   header(3 bytes) | compact-u16(num_accounts) | account keys | recent_blockhash | compact-u16(num_instructions) | instructions
 */
@Singleton
class SolanaTransactionBuilder @Inject constructor(
    private val keypairManager: SolanaKeypairManager
) {
    companion object {
        /** System Program address (all zeros in Base58) */
        val SYSTEM_PROGRAM_ID = Base58.decode("11111111111111111111111111111111")

        /** Transfer instruction index in System Program */
        const val TRANSFER_INSTRUCTION_INDEX = 2
    }

    /**
     * Build and sign a SOL transfer transaction.
     *
     * @param fromAddress Sender's Base58 public key
     * @param toAddress Recipient's Base58 public key
     * @param lamports Amount to transfer in lamports
     * @param recentBlockhash Recent blockhash from the RPC
     * @return Base64-encoded signed transaction ready for sendTransaction
     */
    fun buildTransferTransaction(
        fromAddress: String,
        toAddress: String,
        lamports: Long,
        recentBlockhash: String
    ): String {
        val fromPubkey = Base58.decode(fromAddress)
        val toPubkey = Base58.decode(toAddress)
        val blockhashBytes = Base58.decode(recentBlockhash)

        require(fromPubkey.size == 32) { "Invalid sender address" }
        require(toPubkey.size == 32) { "Invalid recipient address" }
        require(blockhashBytes.size == 32) { "Invalid blockhash" }

        // Build the message
        val message = buildTransferMessage(fromPubkey, toPubkey, lamports, blockhashBytes)

        // Sign the message
        val signature = keypairManager.sign(fromAddress, message)

        // Assemble the full transaction
        val txBytes = ByteArrayOutputStream()
        txBytes.write(encodeCompactU16(1)) // 1 signature
        txBytes.write(signature)            // 64-byte signature
        txBytes.write(message)              // message bytes
        
        return android.util.Base64.encodeToString(txBytes.toByteArray(), android.util.Base64.NO_WRAP)
    }

    /**
     * Build the message portion of a System Program Transfer transaction.
     *
     * Message layout:
     *   Header: [num_required_sigs=1, num_readonly_signed=0, num_readonly_unsigned=1]
     *   Accounts: [sender(signer+writable), receiver(writable), SystemProgram(readonly)]
     *   Recent blockhash: 32 bytes
     *   Instructions: 1 transfer instruction
     */
    private fun buildTransferMessage(
        fromPubkey: ByteArray,
        toPubkey: ByteArray,
        lamports: Long,
        blockhashBytes: ByteArray
    ): ByteArray {
        val msg = ByteArrayOutputStream()

        // ── Header ──
        msg.write(1) // num_required_signatures
        msg.write(0) // num_readonly_signed_accounts
        msg.write(1) // num_readonly_unsigned_accounts (System Program)

        // ── Account keys (ordered: signer+writable first, then writable, then readonly) ──
        msg.write(encodeCompactU16(3)) // 3 accounts
        msg.write(fromPubkey)          // index 0: sender (signer + writable)
        msg.write(toPubkey)            // index 1: receiver (writable)
        msg.write(SYSTEM_PROGRAM_ID)   // index 2: System Program (readonly)

        // ── Recent blockhash ──
        msg.write(blockhashBytes)

        // ── Instructions ──
        msg.write(encodeCompactU16(1)) // 1 instruction

        // Transfer instruction:
        msg.write(2) // program_id_index = 2 (System Program)

        // Account indices for this instruction
        msg.write(encodeCompactU16(2)) // 2 accounts
        msg.write(0) // from account index
        msg.write(1) // to account index

        // Instruction data: [u32 LE instruction_index, u64 LE lamports]
        val instructionData = ByteBuffer.allocate(12)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(TRANSFER_INSTRUCTION_INDEX)
            .putLong(lamports)
            .array()
        msg.write(encodeCompactU16(instructionData.size))
        msg.write(instructionData)

        return msg.toByteArray()
    }

    /**
     * Encode an integer as Solana's compact-u16 format.
     * Values 0-127 use 1 byte; 128-16383 use 2 bytes; 16384+ use 3 bytes.
     */
    private fun encodeCompactU16(value: Int): ByteArray {
        val bytes = mutableListOf<Byte>()
        var v = value
        while (true) {
            val b = v and 0x7F
            v = v shr 7
            if (v == 0) {
                bytes.add(b.toByte())
                break
            } else {
                bytes.add((b or 0x80).toByte())
            }
        }
        return bytes.toByteArray()
    }
}
