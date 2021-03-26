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
data class Product(
    @SerializedName("product_id")
    @Expose
    var productId: Int? = null,

    @SerializedName("quantity")
    @Expose
    var quantity: Int? = null
) : Parcelable {
}