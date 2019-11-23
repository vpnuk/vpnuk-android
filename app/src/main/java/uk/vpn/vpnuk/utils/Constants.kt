package uk.vpn.vpnuk.utils

enum class SocketType(
    val value: String,
    val ports: List<String>
) {
    UDP("udp", listOf("1194", "55194", "65194")),
    TCP("tcp", listOf("443", "8008", "80"));

    companion object {
        fun byValue(value: String): SocketType? {
            return values().find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

enum class ServerType(val value: String) {
    SHARED("shared"),
    DEDICATED("dedicated");

    companion object {
        fun byValue(value: String): ServerType? {
            return values().find { it.value.equals(value, ignoreCase = true) }
        }
    }
}