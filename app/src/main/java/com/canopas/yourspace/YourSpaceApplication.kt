package com.canopas.yourspace

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.IntentFilter
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Configuration
import com.canopas.yourspace.callback.NetworkStatusCallback
import com.canopas.yourspace.data.repository.GeofenceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.auth.AuthStateChangeListener
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.domain.fcm.FcmRegisterWorker
import com.canopas.yourspace.domain.fcm.YOURSPACE_CHANNEL_GEOFENCE
import com.canopas.yourspace.domain.fcm.YOURSPACE_CHANNEL_MESSAGES
import com.canopas.yourspace.domain.fcm.YOURSPACE_CHANNEL_PLACES
import com.canopas.yourspace.domain.receiver.NetworkConnectionReceiver
import com.canopas.yourspace.domain.utils.NetworkUtils
import com.google.android.libraries.places.api.Places
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class YourSpaceApplication :
    Application(),
    DefaultLifecycleObserver,
    Configuration.Provider,
    AuthStateChangeListener {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var geoFenceRepository: GeofenceRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    private lateinit var networkStatusCallback: NetworkStatusCallback
    private lateinit var networkConnectionReceiver: NetworkConnectionReceiver

    override fun onCreate() {
        Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.PLACE_API_KEY)

        super<Application>.onCreate()
        Timber.plant(Timber.DebugTree())
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        authService.addListener(this)
        setNotificationChannel()

        networkStatusCallback = NetworkStatusCallback(this)
        networkConnectionReceiver = NetworkConnectionReceiver()

        NetworkUtils.registerNetworkCallback(this, networkStatusCallback)
        LocalBroadcastManager.getInstance(this).registerReceiver(
            networkConnectionReceiver,
            IntentFilter(NetworkConnectionReceiver.NETWORK_STATUS_ACTION)
        )

        if (userPreferences.currentUser != null) {
            geoFenceRepository.init()
        }
    }

    private fun setNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                YOURSPACE_CHANNEL_MESSAGES,
                getString(R.string.title_notification_channel_messages),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    getString(R.string.description_notification_channel_messages)
                enableLights(true)
                notificationManager.createNotificationChannel(this)
            }

            NotificationChannel(
                YOURSPACE_CHANNEL_PLACES,
                getString(R.string.title_notification_channel_places),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    getString(R.string.description_notification_channel_places)
                enableLights(true)
                notificationManager.createNotificationChannel(this)
            }

            NotificationChannel(
                YOURSPACE_CHANNEL_GEOFENCE,
                getString(R.string.title_notification_channel_geofence),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    getString(R.string.description_notification_channel_geofence)
                enableLights(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) this.setAllowBubbles(true)
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        if (userPreferences.currentUser != null && !userPreferences.isFCMRegistered) {
            FcmRegisterWorker.startService(this)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onAuthStateChanged() {
        if (userPreferences.currentUser != null && !userPreferences.isFCMRegistered) {
            FcmRegisterWorker.startService(this)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        NetworkUtils.unregisterNetworkCallback(this, networkStatusCallback)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(networkConnectionReceiver)
    }
}
