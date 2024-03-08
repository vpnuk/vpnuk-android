/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.data.repository

import android.graphics.drawable.Drawable

interface ResourceRepository {

    fun getString(resId: Int): String
    fun getColor(resId: Int): Int
    fun getDrawable(resId: Int): Drawable

}