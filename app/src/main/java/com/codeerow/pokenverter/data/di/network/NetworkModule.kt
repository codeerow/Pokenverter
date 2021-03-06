package com.codeerow.pokenverter.data.di.network

import com.codeerow.pokenverter.BuildConfig
import com.codeerow.pokenverter.data.network.api.RatesAPI
import com.codeerow.pokenverter.data.network.connectivity.ConnectivityProvider
import com.codeerow.pokenverter.data.network.connectivity.ConnectivityProvider.ConnectivityStateListener
import com.codeerow.pokenverter.data.network.connectivity.ConnectivityProvider.NetworkState
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.Observable
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber


val networkModule = module {
    single { provideLoggingInterceptor() }
    single { provideClient(loggingInterceptor = get()) }
    single { provideRetrofit(client = get()) }
    single { provideApi<RatesAPI>(retrofit = get()) }

    /** Connectivity */
    single<Observable<NetworkState>> {
        val context = androidContext()
        val connectivityProvider = ConnectivityProvider.createProvider(context)
        connectivityProvider.subscribe()

        registerCallback(object : ScopeCallback {
            override fun onScopeClose(scope: Scope) {
                connectivityProvider.unsubscribe()
            }
        })

        Observable.create {
            connectivityProvider.addListener(object :
                ConnectivityStateListener {
                override fun onStateChange(state: NetworkState) {
                    it.onNext(state)
                }
            })
        }
    }
}

private fun provideLoggingInterceptor(): Interceptor {
    val interceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            Timber.tag("OkHttp")
            Timber.d(message)
        }
    })
    interceptor.level = HttpLoggingInterceptor.Level.BODY
    return interceptor
}

private fun provideClient(loggingInterceptor: Interceptor): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()
}

private fun provideRetrofit(client: OkHttpClient): Retrofit {
    val factory = GsonConverterFactory.create(GsonBuilder().create())
    return Retrofit.Builder()
        .baseUrl(BuildConfig.URL)
        .client(client)
        .addConverterFactory(factory)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
}

