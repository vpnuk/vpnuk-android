package uk.vpn.vpnuk.utils

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


fun Completable.doOnIoSubscribeOnMain() =
    this.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())

fun <T> Single<T>.doOnIoSubscribeOnMain() =
    this.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())

fun <T> Observable<T>.doOnIoSubscribeOnMain() =
    this.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())