package uk.vpn.vpnuk.utils

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


fun Completable.doOnIoObserveOnMain() =
    this.subscribeOn(Schedulers.io())
        .observeOnMain()

fun <T> Single<T>.doOnIoObserveOnMain() =
    this.subscribeOn(Schedulers.io())
        .observeOnMain()

fun <T> Observable<T>.doOnIoObserveOnMain() =
    this.subscribeOn(Schedulers.io())
        .observeOnMain()

fun Completable.observeOnMain() =
    this.observeOn(AndroidSchedulers.mainThread())

fun <T> Single<T>.observeOnMain() =
    this.observeOn(AndroidSchedulers.mainThread())

fun <T> Observable<T>.observeOnMain() =
    this.observeOn(AndroidSchedulers.mainThread())