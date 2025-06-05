package com.example.twittermemoapp

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Memo(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
) : Serializable {
    
    // タイムスタンプ付きコンテンツを返す
    fun getContentWithTimestamp(): String {
        return if (content.isNotEmpty()) {
            "$content\n[${timestamp}]"
        } else {
            "[${timestamp}]"
        }
    }
    
    // ソフト削除用メソッド
    fun markAsDeleted(): Memo {
        return copy(isDeleted = true, deletedAt = System.currentTimeMillis())
    }
    
    // 復元用メソッド
    fun restore(): Memo {
        return copy(isDeleted = false, deletedAt = null)
    }
    
    // 1ヶ月経過チェック
    fun canPermanentlyDelete(): Boolean {
        val oneMonth = 30L * 24 * 60 * 60 * 1000 // 30日をミリ秒で
        return deletedAt?.let { 
            System.currentTimeMillis() - it > oneMonth 
        } ?: false
    }
}