package uk.vpn.vpnuk.base

interface Presenter<V: View> {
    var view: V?
}

interface View