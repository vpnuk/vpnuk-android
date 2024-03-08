/*
 * Copyright (c) 2024 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.settingsScreen.manageApps

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.vpn.vpnuk.data.repository.ApplicationsInfoRepository
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.data.repository.ResourceRepository
import uk.vpn.vpnuk.model.AppInfo
import uk.vpn.vpnuk.utils.emitFlow
import javax.inject.Inject


@HiltViewModel
class ManageAppsViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val appsRepository: ApplicationsInfoRepository,
    private val resourceRepository: ResourceRepository,
) : ViewModel() {

    private val _viewState = MutableStateFlow(ManageAppsViewState())
    val viewState = _viewState.asStateFlow()

    private var allAppsList = mutableListOf<AppInfo>()


    fun getApplications() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            Log.d("kek", "AppsSize111 - ${localRepository.allAppsInfoList.size}")
            while (localRepository.allAppsInfoList.isEmpty()){}
            Log.d("kek", "AppsSize222 - ${localRepository.allAppsInfoList.size}")

            val allApps = appsRepository.getAllApplicationsCached()
            val excludedApps = localRepository.excludedApps
            val formattedAppsList = mutableListOf<AppInfo>()

            allApps.forEach {
                if(excludedApps.map { it.packageName }.contains(it.packageName)){
                    it.isChecked = false
                }
                formattedAppsList.add(it)
            }

            allAppsList = formattedAppsList.toMutableList()


            _viewState.emit(_viewState.value.copy(applicationsList = formattedAppsList.sortedBy { it.isChecked }, isProgress = false))
        }
    }

    fun onAppItemChecked(app: AppInfo, checked: Boolean) {
        val excludedApps = localRepository.excludedApps.toMutableList()
        val isExcludedAppsContain = excludedApps.map { it.packageName }.contains(app.packageName)

        app.isChecked = checked

        if (!checked){
            if(!isExcludedAppsContain){
                excludedApps.add(app)
            }
        }else{
            if(isExcludedAppsContain){
                val newExcludedAppsList = mutableListOf<AppInfo>()

                excludedApps.forEachIndexed { index, appInfo ->
                    if(appInfo.packageName != app.packageName)
                        newExcludedAppsList.add(appInfo)
                }

                excludedApps.clear()
                excludedApps.addAll(newExcludedAppsList)
            }
        }

        localRepository.excludedApps = excludedApps
        val allList = _viewState.value.applicationsList.toMutableList()

        allList.forEachIndexed { index, appInfo ->
            if(excludedApps.map { it.packageName }.contains(appInfo.packageName)){
                val oldItem = allList[index]
                oldItem.isChecked = false
                allList[index] = oldItem
            }
        }

        allAppsList = allList

        emitFlow(_viewState, _viewState.value.copy(applicationsList = allList))
    }

    fun onSearchTextChange(text: CharSequence?) {
        var filteredList = mutableListOf<AppInfo>()

        if(!text.isNullOrBlank()){
            _viewState.value.applicationsList.forEach {
                if(it.name.contains(text, true)){
                    filteredList.add(it)
                }
            }
        }else{
            filteredList = allAppsList
        }

        emitFlow(_viewState, _viewState.value.copy(applicationsList = filteredList))
    }

    data class ManageAppsViewState(
        val applicationsList: List<AppInfo> = listOf(),
        val isProgress: Boolean = true,
    )
}










