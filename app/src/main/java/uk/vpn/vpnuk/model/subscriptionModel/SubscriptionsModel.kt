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
data class SubscriptionsModel(
    @SerializedName("id")
    @Expose
    var id: Int? = null,

    @SerializedName("product_name")
    @Expose
    var productName: String? = null,

    @SerializedName("product_id")
    @Expose
    var productId: Int? = null,

    @SerializedName("quantity")
    @Expose
    var quantity: Int? = null,

    @SerializedName("country")
    @Expose
    var country: String? = null,

    @SerializedName("sessions")
    @Expose
    var sessions: Int? = null,

    @SerializedName("period")
    @Expose
    var period: Int? = null,

    @SerializedName("type")
    @Expose
    var type: String? = null,

    @SerializedName("status")
    @Expose
    var status: String? = null,

    @SerializedName("trial_end")
    @Expose
    var trialEnd: String? = null,

    @SerializedName("next_payment_date")
    @Expose
    var nextPaymentDate: String? = null,

    @SerializedName("vpnaccounts")
    @Expose
    var vpnaccounts: List<Vpnaccount>? = null,




    @SerializedName("pending_orders")
    @Expose
    var pending_orders: List<PendingOrder>? = null
) : Parcelable