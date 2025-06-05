package com.example.twittermemoapp

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    companion object {
        private const val PREF_NAME = "memo_app_prefs"
        private const val KEY_DEFAULT_MEMO_ID = "default_memo_id"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    var defaultMemoId: Long
        get() = sharedPreferences.getLong(KEY_DEFAULT_MEMO_ID, -1L)
        set(value) = sharedPreferences.edit().putLong(KEY_DEFAULT_MEMO_ID, value).apply()
    
    fun hasDefaultMemo(): Boolean = defaultMemoId != -1L
    
    fun clearDefaultMemo() {
        defaultMemoId = -1L
    }
}