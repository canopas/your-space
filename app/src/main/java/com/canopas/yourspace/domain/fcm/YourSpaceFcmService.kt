package com.canopas.yourspace.domain.fcm

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.USER_STATE_UNKNOWN
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

const val YOURSPACE_CHANNEL_MESSAGES = "your_space_notification_channel_messages"
const val YOURSPACE_CHANNEL_PLACES = "your_space_notification_channel_places"
const val YOURSPACE_CHANNEL_GEOFENCE = "your_space_notification_channel_geofence"

const val NOTIFICATION_ID = 101

const val KEY_NOTIFICATION_TYPE = "type"

object NotificationChatConst {
    const val NOTIFICATION_TYPE_CHAT = "chat"
    const val KEY_PROFILE_URL = "senderProfileUrl"
    const val KEY_IS_GROUP = "isGroup"
    const val KEY_GROUP_NAME = "groupName"
    const val KEY_SENDER_ID = "senderId"
    const val KEY_THREAD_ID = "threadId"
}

object NotificationPlaceConst {
    const val NOTIFICATION_TYPE_NEW_PLACE_ADDED = "new_place_added"
    const val KEY_SPACE_ID = "spaceId"
}

object NotificationGeofenceConst {
    const val NOTIFICATION_TYPE_GEOFENCE = "geofence"
    const val KEY_SPACE_ID = "spaceId"
    const val KEY_PLACE_ID = "placeId"
    const val KEY_PLACE_NAME = "placeName"
    const val eventBy = "eventBy"
    const val KEY_EVENT_BY = "eventBy"
}

object NotificationNetworkStatusConst {
    const val NOTIFICATION_TYPE_NETWORK_CHECK = "network_status"
}

@AndroidEntryPoint
class YourSpaceFcmService : FirebaseMessagingService() {
    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var authService: AuthService

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        userPreferences.isFCMRegistered = false
        if (userPreferences.currentUser != null) {
            FcmRegisterWorker.startService(applicationContext)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val notification = message.notification
        notification?.let {
            val title = notification.title
            val body = notification.body
            val profile = message.data[NotificationChatConst.KEY_PROFILE_URL]
            val type = message.data[KEY_NOTIFICATION_TYPE]

            if (title != null && body != null) {
                when (type) {
                    NotificationChatConst.NOTIFICATION_TYPE_CHAT -> {
                        scope.launch {
                            val bitmap =
                                if (profile.isNullOrEmpty()) null else getTrackBitmapFromUrl(profile)
                            sendNotification(
                                this@YourSpaceFcmService,
                                title,
                                body,
                                message.data,
                                bitmap
                            )
                        }
                    }
                    NotificationPlaceConst.NOTIFICATION_TYPE_NEW_PLACE_ADDED -> {
                        sendPlaceNotification(this, title, body, message.data)
                    }
                    NotificationGeofenceConst.NOTIFICATION_TYPE_GEOFENCE -> {
                        sendGeoFenceNotification(this, title, body, message.data)
                    }
                    NotificationNetworkStatusConst.NOTIFICATION_TYPE_NETWORK_CHECK -> {
                        // Update user session state
                        scope.launch { authService.updateUserSessionState(USER_STATE_UNKNOWN) }
                    }
                }
            }
        }
    }

    private fun sendGeoFenceNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val spaceId = data[NotificationGeofenceConst.KEY_SPACE_ID]
        val userId = data[NotificationGeofenceConst.KEY_EVENT_BY]

        val notificationId = userId?.hashCode() ?: NOTIFICATION_ID

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(NotificationGeofenceConst.KEY_SPACE_ID, spaceId)
            putExtra(NotificationGeofenceConst.KEY_EVENT_BY, userId)
            putExtra(
                KEY_NOTIFICATION_TYPE,
                NotificationGeofenceConst.NOTIFICATION_TYPE_GEOFENCE
            )
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT

        val clickAction = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            pendingIntentFlag
        )

        val nBuilder = NotificationCompat.Builder(this, YOURSPACE_CHANNEL_PLACES)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(clickAction)

        notificationManager.notify(notificationId, nBuilder.build())
    }

    private fun sendPlaceNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val spaceId = data[NotificationPlaceConst.KEY_SPACE_ID]
        val notificationId = spaceId?.hashCode() ?: NOTIFICATION_ID

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(NotificationPlaceConst.KEY_SPACE_ID, spaceId)
            putExtra(
                KEY_NOTIFICATION_TYPE,
                NotificationPlaceConst.NOTIFICATION_TYPE_NEW_PLACE_ADDED
            )
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT

        val clickAction = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            pendingIntentFlag
        )

        val nBuilder = NotificationCompat.Builder(this, YOURSPACE_CHANNEL_PLACES)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(clickAction)

        notificationManager.notify(notificationId, nBuilder.build())
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(
        context: Context,
        title: String,
        body: String,
        data: MutableMap<String, String>,
        profile: Bitmap?
    ) {
        val isGroup = data[NotificationChatConst.KEY_IS_GROUP].toBoolean()
        val groupName = data[NotificationChatConst.KEY_GROUP_NAME]
        val senderId = data[NotificationChatConst.KEY_SENDER_ID]
        val threadId = data[NotificationChatConst.KEY_THREAD_ID]
        val notificationId = threadId?.hashCode() ?: NOTIFICATION_ID

        val user = Person.Builder().apply {
            setKey(senderId)
            setName(title)
            profile?.let { setIcon(IconCompat.createWithAdaptiveBitmap(it)) }
        }.build()

        val style =
            findActiveNotification(this, notificationId)
                ?.let {
                    NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(
                        it
                    )
                }
                ?: NotificationCompat.MessagingStyle(user)

        style.also {
            if (isGroup) {
                it.setGroupConversation(true)
                it.setConversationTitle(groupName)
            }
            it.addMessage(body, System.currentTimeMillis(), user)
        }

        val nBuilder =
            context.messageNotificationBuilder(notificationId, threadId, title, body, style)
        notificationManager.notify(notificationId, nBuilder.build())
    }

    private suspend fun getTrackBitmapFromUrl(
        url: String
    ): Bitmap? {
        try {
            val size = resources.getDimensionPixelSize(R.dimen.size_notification_large)

            val loader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .size(size, size)
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap
            return bitmap
        } catch (throwable: Throwable) {
            return null
        }
    }
}
