package uk.vpn.vpnuk.utils

import androidx.lifecycle.*


fun <T> LifecycleOwner.observe(liveData: LiveData<T>, observer: Observer<T>) {
    liveData.observe(this, observer)
}

@Suppress("detekt.UnsafeCast")
fun <T> MutableLiveData<T>.asLiveData() = this as LiveData<T>

fun <T, K, R>LifecycleOwner.observeCombine(firstLive: LiveData<T>, secondLive: LiveData<K>, callback: (T?, K?) -> R){
    firstLive.combineWith(secondLive){ first, second ->
        callback(first, second)
    }
}

fun <T, K, R> LiveData<T>.combineWith(
    liveData: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block(this.value, liveData.value)
    }
    return result
}