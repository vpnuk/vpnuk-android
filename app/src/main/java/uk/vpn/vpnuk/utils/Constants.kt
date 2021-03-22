/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk.utils

import androidx.annotation.StringRes
import uk.vpn.vpnuk.R

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

enum class ServerType(val value: String, @StringRes val nameRes: Int) {
    SHARED("shared", R.string.server_type_shared),
    DEDICATED("dedicated", R.string.server_type_dedicated),
    DEDICATED_11("dedicated11", R.string.server_type_11);

    companion object {
        fun byValue(value: String): ServerType? {
            return values().find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

const val CREATED_SUBSCRIPTION = "CREATED_SUBSCRIPTION"