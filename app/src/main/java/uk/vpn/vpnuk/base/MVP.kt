/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk.base

interface Presenter<V: View> {
    var view: V?
}

interface View