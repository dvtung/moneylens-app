package com.dvt.moneylens.util

import java.security.MessageDigest

object SecurityUtils {

    private const val DEFAULT_SALT = "moneylens_secure_salt_2026"

    /**
     * Băm mã PIN người dùng bằng thuật toán SHA-256 kết hợp Salt.
     */
    fun hashPin(pin: String, salt: String = DEFAULT_SALT): String {
        if (pin.isBlank()) return ""
        val input = "$salt:$pin"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Xác thực mã PIN nhập vào so với chuỗi Hash đã lưu trong SharedPreferences.
     * Hỗ trợ tương thích ngược cho mã PIN thô cũ (6 chữ số).
     */
    fun verifyPin(inputPin: String, storedHashOrRawPin: String, salt: String = DEFAULT_SALT): Boolean {
        if (storedHashOrRawPin.isBlank()) return true
        if (inputPin == storedHashOrRawPin) return true // Tương thích ngược với PIN thô cũ
        val hashedInput = hashPin(inputPin, salt)
        return hashedInput == storedHashOrRawPin
    }
}
