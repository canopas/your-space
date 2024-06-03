package com.canopas.yourspace.domain.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.canopas.yourspace.data.models.user.USER_STATE_LOCATION_PERMISSION_DENIED
import com.canopas.yourspace.data.models.user.USER_STATE_NO_NETWORK_OR_PHONE_OFF
import com.canopas.yourspace.data.models.user.USER_STATE_UNKNOWN
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.isLocationPermissionGranted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NetworkConnectionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var authService: AuthService

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (authService.currentUser == null) return
        if (intent.action != NETWORK_STATUS_ACTION) return

        val connected = intent.getBooleanExtra(NETWORK_STATUS, false)
        val hasLocationPermission = context.isLocationPermissionGranted
        val state = if (connected && hasLocationPermission) {
            USER_STATE_UNKNOWN
        } else if (!connected) {
            USER_STATE_NO_NETWORK_OR_PHONE_OFF
        } else {
            USER_STATE_LOCATION_PERMISSION_DENIED
        }

        scope.launch { authService.updateUserSessionState(state) }
    }

    companion object {
        const val NETWORK_STATUS_ACTION = "com.example.NETWORK_STATUS"
        const val NETWORK_STATUS = "networkStatus"
    }
}
