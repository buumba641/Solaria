package com.solaria.app.data.solana

/**
 * Base58 encoding/decoding (Bitcoin alphabet) for Solana addresses and keys.
 */
object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = ALPHABET.length
    private val INDEXES = IntArray(128) { -1 }.also { arr ->
        ALPHABET.forEachIndexed { i, c -> arr[c.code] = i }
    }

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        // Count leading zeros
        var zeros = 0
        while (zeros < input.size && input[zeros].toInt() == 0) zeros++

        // Convert base-256 to base-58
        val encoded = CharArray(input.size * 2)
        var outputStart = encoded.size
        var inputStart = zeros
        while (inputStart < input.size) {
            outputStart--
            encoded[outputStart] = ALPHABET[divmod(input, inputStart, 256, BASE)]
            if (input[inputStart].toInt() == 0) inputStart++
        }
        while (outputStart < encoded.size && encoded[outputStart] == ALPHABET[0]) outputStart++
        repeat(zeros) { outputStart--; encoded[outputStart] = ALPHABET[0] }
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)
        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = if (c.code < 128) INDEXES[c.code] else -1
            require(digit >= 0) { "Invalid Base58 character: $c" }
            input58[i] = digit.toByte()
        }
        var zeros = 0
        while (zeros < input58.size && input58[zeros].toInt() == 0) zeros++

        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < input58.size) {
            outputStart--
            decoded[outputStart] = divmod(input58, inputStart, BASE, 256).toByte()
            if (input58[inputStart].toInt() == 0) inputStart++
        }
        while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) outputStart++
        val result = ByteArray(zeros + decoded.size - outputStart)
        System.arraycopy(decoded, outputStart, result, zeros, decoded.size - outputStart)
        return result
    }

    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Int {
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder
    }
}
