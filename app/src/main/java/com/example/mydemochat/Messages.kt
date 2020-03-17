package com.example.mydemochat

data class Messages(val message: String = "", val seen: Boolean = false, val time: Long = 0, val type: String = "", val from: String = ""){
}