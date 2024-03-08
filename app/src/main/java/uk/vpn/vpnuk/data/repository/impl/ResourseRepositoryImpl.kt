/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.data.repository.impl

import android.content.Context
import android.graphics.drawable.Drawable
import uk.vpn.vpnuk.data.repository.ResourceRepository

class ResourceRepositoryImpl(val context: Context) : ResourceRepository {
    override fun getString(resId: Int): String = context.getString(resId)
    override fun getColor(resId: Int): Int = context.resources.getColor(resId)
    override fun getDrawable(resId: Int): Drawable = context.resources.getDrawable(resId)
}
