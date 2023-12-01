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
import uk.vpn.vpnuk.utils.getIconResourceName

class ServersAdapter(context: Context, val listener: (Server) -> Unit) :
    RecyclerView.Adapter<ServerViewHolder>() {
    val inflater = LayoutInflater.from(context)
    private val servers = mutableListOf<Server>()

    fun updateData(list: List<Server>) {
        servers.clear()
        servers.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        return ServerViewHolder(inflater.inflate(R.layout.item_server, parent, false)).apply {
            itemView.setOnClickListener {
                listener(servers[adapterPosition])
            }
        }
    }

    override fun getItemCount(): Int {
        return servers.size
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.itemView.isFocusable = true
        holder.bind(servers[position])
    }
}

class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvCity: TextView = itemView.findViewById(R.id.tvCity)
    val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
    val ivCountry: ImageView = itemView.findViewById(R.id.ivCountry)

    fun bind(server: Server) {
        //ivCountry.setImageResource(server.getIconResourceName(itemView.context))
        //server.location.icon //== "uk"
        var iso = server.location.icon.toLowerCase()
        when(iso){
            "uk" -> iso = "gb"
        }

        val drawable = FlagKit.getDrawable(itemView.context, iso)
        ivCountry.setImageDrawable(drawable)

        tvCity.text = "${server.location.city}"

        tvAddress.text = "${server.dns}"
    }

}