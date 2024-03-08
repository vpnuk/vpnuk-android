/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model

data class AppInfo(
    val name: String = "",
    val packageName: String = "",
    val icon: Int = 0,
    var isChecked: Boolean = true,
)
