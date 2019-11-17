package uk.vpn.vpnuk.remote

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml

//import javax.xml.bind.annotation.*

@Xml(name = "servers")
//@XmlAccessorType(XmlAccessType.FIELD)
class Servers {
    @Element(name = "server")
    var servers: List<Server>? = null

    override fun toString(): String {
        return "Servers(servers=$servers)"
    }
}

@Xml(name = "server")
//@XmlAccessorType(XmlAccessType.FIELD)
class Server {
    @Attribute(name = "type")
    var type: String? = null
    @Element
    var address: String? = null
    @Element
    var dns: String? = null
    @Element
    var location: Location? = null

    constructor(
        type: String,
        address: String,
        dns: String,
        icon: String,
        city: String,
        value: String
    ) {
        this.type = type
        this.address = address
        this.dns = dns
        this.location = Location().apply {
            this.icon = icon
            this.city = city
            this.value = value
        }
    }

    override fun toString(): String {
        return "Server(type=$type, address=$address, dns=$dns, location=$location)"
    }
}

@Xml(name = "location")
//@XmlAccessorType(XmlAccessType.FIELD)
class Location {
    @Attribute
    var icon: String? = null
    @Attribute
    var city: String? = null
    @PropertyElement
    var value: String? = null

    override fun toString(): String {
        return "Location(icon=$icon, city=$city, value=$value)"
    }
}
