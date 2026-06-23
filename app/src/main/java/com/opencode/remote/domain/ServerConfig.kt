package com.opencode.remote.domain

data class ServerConfig(
    val host: String = "192.168.1.100",
    val port: Int = 4096,
    val username: String = "opencode",
    val password: String = ""
)
