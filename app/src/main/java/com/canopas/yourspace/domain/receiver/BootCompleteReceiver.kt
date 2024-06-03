package com.canopas.yourspace.domain.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.canopas.yourspace.data.models.user.USER_STATE_LOCATION_PERMISSION_DENIED
import com.canopas.yourspace.data.models.user.USER_STATE_NO_NETWORK_OR_PHONE_OFF
import com.canopas.yourspace.data.models.user.USER_STATE_UNKNOWN
import com.canopas.yourspace.data.repository.GeofenceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.utils.isLocationPermissionGranted
import com.canopas.yourspace.domain.utils.NetworkUtils.isNetWorkConnected
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompleteReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationManager: LocationManager

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var geofenceRepository: GeofenceRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if ((Intent.ACTION_BOOT_COMPLETED == intent.action) && authService.currentUser != null) {
            locationManager.startService()
            scope.launch { geofenceRepository.registerAllPlaces() }

            val connected = isNetWorkConnected(context)
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
    }
}
