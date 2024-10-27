package com.example.twittermemoapp

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Memo(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val timestamp: String = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
) : Serializable