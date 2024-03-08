/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import uk.vpn.vpnuk.model.AppInfo
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


class ApplicationsInfoRepository @Inject constructor(
    @ApplicationContext val appContext: Context
) {

    val pm = appContext.packageManager
    @Inject
    lateinit var localRepository: LocalRepository

    fun getAllApplicationsCached() : List<AppInfo> {
        return localRepository.allAppsInfoList
    }

    suspend fun saveAllAppsLocally() {
        withContext(Dispatchers.IO) {
            val allApps = getApplications()
            val formattedAppsList = mutableListOf<AppInfo>()

            allApps.forEach {
                val appInfo = AppInfo(
                    name = getApplicationName(it),
                    packageName = it.packageName,
                    icon = it.icon,
                    isChecked = true
                )

                formattedAppsList.add(appInfo)
            }

            localRepository.allAppsInfoList = formattedAppsList
        }
    }

    private fun getApplications () : List<ApplicationInfo> {
        return appContext.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
    }

    private fun getApplicationName(applicationInfo: ApplicationInfo) : String {
        return applicationInfo.loadLabel(pm).toString()
    }
}