package com.codeerow.pokenverter.data.network.connectivity.impl

import com.codeerow.pokenverter.data.network.connectivity.ConnectivityProvider
import com.codeerow.pokenverter.data.network.connectivity.ConnectivityProvider.ConnectivityStateListener
import com.codeerow.pokenverter.data.network.connectivity.ConnectivityProvider.NetworkState
import java.util.*


abstract class BaseConnectivityProvider : ConnectivityProvider {
    private val listeners = Collections.synchronizedList(mutableListOf<ConnectivityStateListener>())
    private var subscribed = false


    override fun addListener(listener: ConnectivityStateListener) {
        listeners.add(listener)
        listener.onStateChange(getNetworkState()) // propagate an initial state
        verifySubscription()
    }

    override fun removeListener(listener: ConnectivityStateListener) {
        listeners.remove(listener)
        verifySubscription()
    }

    private fun verifySubscription() {
        if (!subscribed && listeners.isNotEmpty()) {
            subscribe()
            subscribed = true
        } else if (subscribed && listeners.isEmpty()) {
            unsubscribe()
            subscribed = false
        }
    }

    @Synchronized
    protected fun dispatchChange(state: NetworkState) {
        for (listener in listeners) {
            listener.onStateChange(state)
        }
    }
}