/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model.serverList

import com.google.gson.annotations.Expose

import com.google.gson.annotations.SerializedName




data class ServerListModel(
    @SerializedName("country")
    @Expose
    var country: List<Country>? = null
) {
}