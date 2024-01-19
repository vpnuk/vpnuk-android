/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model

import com.google.gson.annotations.Expose

import com.google.gson.annotations.SerializedName




class CustomDnsModel {
    @SerializedName("dns")
    @Expose
    var dns: ArrayList<DnsServer> = arrayListOf()
}