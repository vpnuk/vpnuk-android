package uk.vpn.vpnuk.local

data class Settings(
    val socket: String,
    val port: String,
    val reconnect : Boolean,
    val credentials: Credentials?
)

data class Credentials(
    val login: String,
    val password: String
)