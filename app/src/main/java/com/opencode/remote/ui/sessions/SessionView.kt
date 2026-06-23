package com.opencode.remote.ui.sessions

data class SessionView(
    val id: String,
    val title: String,
    val directory: String,
    val updated: Long,
    val status: String,
    val files: Int,
    val additions: Int,
    val deletions: Int,
    val modelLabel: String? = null
)
