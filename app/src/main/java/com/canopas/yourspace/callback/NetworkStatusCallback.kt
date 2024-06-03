package com.canopas.yourspace.callback

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.canopas.yourspace.domain.receiver.NetworkConnectionReceiver

class NetworkStatusCallback(private val context: Context) : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        updateNetworkStatus(true)
    }

    override fun onLost(network: Network) {
        updateNetworkStatus(false)
    }

    private fun updateNetworkStatus(isConnected: Boolean) {
        val intent = Intent(NetworkConnectionReceiver.NETWORK_STATUS_ACTION).apply {
            putExtra(NetworkConnectionReceiver.NETWORK_STATUS, isConnected)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}
