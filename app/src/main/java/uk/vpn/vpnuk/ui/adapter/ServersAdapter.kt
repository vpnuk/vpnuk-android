/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *  
 */

package uk.vpn.vpnuk.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.murgupluoglu.flagkit.FlagKit
import uk.vpn.vpnuk.R
import uk.vpn.vpnuk.remote.Server

class ServersAdapter(
    context: Context,
    val listener: (Server) -> Unit
) : RecyclerView.Adapter<ServerViewHolder>() {

    val inflater = LayoutInflater.from(context)
    private val servers = mutableListOf<Server>()

    fun updateData(list: List<Server>) {
        servers.clear()
        servers.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        return ServerViewHolder(inflater.inflate(R.layout.item_server, parent, false))
    }

    override fun getItemCount(): Int {
        return servers.size
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.itemView.isFocusable = true

        holder.bind(servers[position], listener)
    }
}

class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvCity: TextView = itemView.findViewById(R.id.tvCity)
    val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
    val ivCountry: ImageView = itemView.findViewById(R.id.ivCountry)

    fun bind(server: Server, listener: (Server) -> Unit) {
        itemView.setOnClickListener { listener(server) }

        tvCity.text = server.location?.city
        tvAddress.text = server.dns

        var iso = server.location?.icon?.toLowerCase() ?: ""
        when(iso){
            "uk" -> iso = "gb"
        }

        val drawable = FlagKit.getDrawable(itemView.context, iso)
        ivCountry.setImageDrawable(drawable)
    }

}