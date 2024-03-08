/*
 * Copyright (c) 2022 VPNUK
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 *
 */

package uk.vpn.vpnuk.di

import android.content.Context
import androidx.annotation.NonNull
import com.google.gson.GsonBuilder
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.vpn.vpnuk.api.ServerListVaultApi
import uk.vpn.vpnuk.api.VpnUkInfoApi
import uk.vpn.vpnuk.data.repository.LocalRepository
import uk.vpn.vpnuk.data.repository.ResourceRepository
import uk.vpn.vpnuk.data.repository.impl.ResourceRepositoryImpl
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLSession

import javax.net.ssl.HostnameVerifier





@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideLocalRepository(@ApplicationContext context: Context) : LocalRepository {
        return LocalRepository(context)
    }

    @Provides
    @Singleton
    fun provideResourceRepository(
        @ApplicationContext
        context: Context,
    ): ResourceRepository {
        return ResourceRepositoryImpl(context)
    }


    @Provides
    @Singleton
    fun provideVpnUkInfoApi() : VpnUkInfoApi {
        val gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(VPNUK_INFO_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .client(provideOkHttpClient())
            .build()
            .create(VpnUkInfoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideServerListVaultApi() : ServerListVaultApi {
        val gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(SERVERLISTVAULT_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .client(provideOkHttpClient())
            .build()
            .create(ServerListVaultApi::class.java)
    }



    @NonNull
    private fun provideOkHttpClient() : OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY


        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> { return arrayOf() }
            }
        )
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { hostname, session -> true }
            .addInterceptor(provideInterceptor())
            .readTimeout(2, TimeUnit.MINUTES)
            .addInterceptor(logging)
            .build()
    }

    private fun provideInterceptor() : Interceptor {
        return Interceptor { chain ->
            val apiBuilder = chain
                .request()
                .newBuilder()
                .addHeader("Accept", "application/json")

            val request = apiBuilder
                .build()

            chain.proceed(request)
        }
    }

    companion object {
        val VPNUK_INFO_BASE_URL = "https://vpnuk.info/"
        val SERVERLISTVAULT_BASE_URL = "https://www.serverlistvault.com/"
    }
}