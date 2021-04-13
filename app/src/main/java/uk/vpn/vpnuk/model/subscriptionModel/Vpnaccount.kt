/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model.subscriptionModel

import android.os.Parcelable
import com.google.gson.annotations.Expose

import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Vpnaccount(
    @SerializedName("username")
    @Expose
    var username: String? = null,

    @SerializedName("password")
    @Expose
    var password: String? = null,

    @SerializedName("ip")
    @Expose
    var ip: String? = null,

    @SerializedName("server")
    @Expose
    var server: Server? = null,



    @Transient
    var subscriptionId: Int
) : Parcelable