/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.ui.mainScreen.googleVersion

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.ui.splash.SplashScreenVM
import javax.inject.Inject

class GoogleMainVM @Inject constructor(
    private val localRepository: LocalRepository,
) : ViewModel() {

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    private val _oneShotEvents = MutableSharedFlow<OneShotEvent>()
    val oneShotEvents = _oneShotEvents.asSharedFlow()


    init {

    }

    fun onAction(action: SplashScreenVM.UiAction) {

    }




    data class ViewState(
        val loadingTextToDisplay: String = "Checking for update...",
    )
    sealed class OneShotEvent {
        object NavigateToQuickLaunch : OneShotEvent()
        class ErrorToast(val message: String = "") : OneShotEvent()
    }
    sealed class UiAction {
        class Nothing() : UiAction()
    }
}