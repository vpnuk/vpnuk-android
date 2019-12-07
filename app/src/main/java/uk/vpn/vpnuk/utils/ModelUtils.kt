/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.utils

import android.content.Context
import uk.vpn.vpnuk.remote.Server

fun Server.getIconResourceName(context: Context) =
    context.getImageResByName("${location.icon.toLowerCase()}1")