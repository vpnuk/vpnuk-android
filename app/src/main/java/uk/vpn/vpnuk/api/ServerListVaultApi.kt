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

interface ServerListVaultApi {
    @GET("country.json")
    suspend fun getServerList(): NetworkResponse<ServerListModel, Any>

    @GET("versions.json")
    suspend fun getVersions(): NetworkResponse<VersionsModel, Any>

    @Headers("Content-Type: text/plain")
    @GET("openvpn-configuration.txt")
    suspend fun getOVPNConfig(): NetworkResponse<ResponseBody, Any>
}