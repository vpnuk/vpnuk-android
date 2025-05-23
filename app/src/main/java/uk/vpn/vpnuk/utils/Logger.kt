/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.utils

import android.util.Log
import uk.vpn.vpnuk.BuildConfig

object Logger {
    val vpnLogs = mutableListOf<String>()

    fun e(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message)
        }
        if(tag == "asdasd"){
            vpnLogs.add("- " + message + "\n")
        }
        if(tag == "OpenVPN"){
            vpnLogs.add("* " + message + "\n")
        }
    }
}