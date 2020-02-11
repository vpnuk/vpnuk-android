/*
 * Copyright (c) 2019 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import uk.vpn.vpnuk.local.Settings
import uk.vpn.vpnuk.utils.Logger
import uk.vpn.vpnuk.utils.SocketType
import java.util.concurrent.TimeUnit

interface Requests {
    @GET("servers.json")
    fun getServers(): Single<Servers>

    @GET("versions.json")
    fun getServerVersion(): Single<ServerVersion>
}

data class Wrapper(val server: Server?)
class Repository(context: Context) {
    var serversUpdated: Boolean = false
        private set
    private val prefs = context.getSharedPreferences("servers", Context.MODE_PRIVATE)
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.serverlistvault.com/")
        .client(
            OkHttpClient.Builder()
//                .addInterceptor(HttpLoggingInterceptor().apply {
//                    this.level = HttpLoggingInterceptor.Level.BODY
//                })
                .connectTimeout(20, TimeUnit.SECONDS)
                .build()
        )
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val api = retrofit.create(Requests::class.java)
    private val gson = Gson()

    private val currentServer = BehaviorSubject.create<Wrapper>().toSerialized().apply {
        onNext(Wrapper(null))
    }

    fun updateSettings(settings: Settings) {
        prefs.edit()
            .putString("settings", gson.toJson(settings))
            .commit()
    }

    fun getSettings() =
        prefs.getString("settings", null)?.let { gson.fromJson(it, Settings::class.java) }
            ?: Settings(SocketType.UDP.value, SocketType.UDP.ports.first(), false, null, null)

    fun getSelectedServer(): Server? = currentServer.blockingFirst()?.server

    fun getCurrentServerObservable(): Observable<Wrapper> = currentServer

    fun setServerId(id: String): Completable {
        return Single.fromCallable {
            prefs.edit()
                .putString("id", id)
                .commit()
        }.flatMapCompletable {
            updateCurrentServer()
        }
    }

    private fun updateCurrentServer(): Completable {
        return getServersCache()
            .map { list -> Wrapper(list.find { getCurrentServerId() == it.address }) }
            .doOnSuccess {
                Logger.e("subscribe", "put new $it")
                currentServer.onNext(it)
            }
            .ignoreElement()
    }

    fun getServersCache(): Single<List<Server>> =
        Single.fromCallable {
            prefs.getString("servers", null)?.let {
                gson.fromJson(it, Servers::class.java)?.servers
            } ?: emptyList()
        }

    fun updateServers(): Completable =
        api.getServerVersion()
            .flatMapCompletable { version ->
                if (getCurrentServerVersion() != version.servers) {
                    api.getServers()
                        .doOnSuccess { servers ->
                            prefs.edit()
                                .putString("servers", gson.toJson(servers))
                                .putString("version", version.servers)
                                .commit()
                        }
                        .ignoreElement()
                } else {
                    Completable.complete()
                }
            }
            .andThen(updateCurrentServer())
            .onErrorResumeNext { throwable ->
                updateCurrentServer().andThen(Completable.error(throwable))
            }
            .doOnComplete {
                serversUpdated = true
            }

    private fun getCurrentServerId() =
        prefs.getString("id", null)

    private fun getCurrentServerVersion() =
        prefs.getString("version", null)

    companion object {
        private var repository: Repository? = null
        fun instance(context: Context): Repository {
            if (repository == null) {
                repository = Repository(context.applicationContext)
            }
            return repository!!
        }
    }
}