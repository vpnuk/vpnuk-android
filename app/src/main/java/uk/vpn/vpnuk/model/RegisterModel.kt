/*
 * Copyright (c) 2021 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class RegisterModel(
    @SerializedName("user_name")
    @Expose
    var userName: String? = null,

    @SerializedName("password")
    @Expose
    var password: String? = null,

    @SerializedName("first_name")
    @Expose
    var firstName: String? = null,

    @SerializedName("last_name")
    @Expose
    var lastName: String? = null,

    @SerializedName("email")
    @Expose
    var email: String? = null
)