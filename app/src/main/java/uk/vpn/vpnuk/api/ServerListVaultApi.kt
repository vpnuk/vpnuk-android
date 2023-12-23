/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.api

import com.haroldadmin.cnradapter.NetworkResponse
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import uk.vpn.vpnuk.model.serverList.ServerListModel
import uk.vpn.vpnuk.model.versions.VersionsModel
import uk.vpn.vpnuk.remote.Servers

interface ServerListVaultApi {
    @GET("servers.json")
    suspend fun getServerList(): NetworkResponse<Servers, Any>

    @GET("versions.json")
    suspend fun getVersions(): NetworkResponse<VersionsModel, Any>

    @Headers("Content-Type: text/plain")
    @GET("android.txt")
    suspend fun getOVPNConfig(): NetworkResponse<ResponseBody, Any>
}