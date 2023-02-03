package com.wahyurhy.transactiontracker.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

fun encryptAES(plainText: String, key: String): String {
    val secretKey = SecretKeySpec(key.toByteArray(), "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val encryptedByteValue = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(encryptedByteValue, Base64.DEFAULT)
}

fun decryptAES(encryptedText: String, key: String): String {
    val encryptedByteValue = Base64.decode(encryptedText, Base64.DEFAULT)
    val secretKey = SecretKeySpec(key.toByteArray(), "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    val decryptedByteValue = cipher.doFinal(encryptedByteValue)
    return String(decryptedByteValue, Charsets.UTF_8)
}
