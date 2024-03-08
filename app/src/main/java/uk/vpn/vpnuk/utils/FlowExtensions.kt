/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun <T> Flow<T>.launchWhenStarted(lifecycleScope: LifecycleCoroutineScope){
    lifecycleScope.launchWhenStarted {
        this@launchWhenStarted.collect()
    }
}

fun <T> Flow<T>.launchWhenCreated(lifecycleScope: LifecycleCoroutineScope){
    lifecycleScope.launchWhenCreated {
        this@launchWhenCreated.collect()
    }
}

fun <T> LifecycleOwner.observeFlow(flow: Flow<T>, observer: Observer<T>){
    this.lifecycleScope.launchWhenStarted {
        flow.collect {
            observer.onChanged(it)
        }
    }
}

fun <T> Fragment.observeFlow(flow: Flow<T>, observer: Observer<T>){
    this.viewLifecycleOwner.lifecycleScope.launchWhenStarted {
        flow.collect {
            observer.onChanged(it)
        }
    }
}

fun <T> ViewModel.emitFlow(flow: MutableSharedFlow<T>, data: T){
    this.viewModelScope.launch {
        flow.emit(data)
    }
}

fun <T> ViewModel.emitFlow(flow: MutableStateFlow<T>, data: T){
    this.viewModelScope.launch {
        flow.emit(data)
    }
}

fun <T> MutableStateFlow<T>.emit(data: T, scope: CoroutineScope) {
    val stateFlow = this
    scope.launch { stateFlow.emit(data) }
}