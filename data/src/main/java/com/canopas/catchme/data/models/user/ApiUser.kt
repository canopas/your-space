package com.canopas.catchme.data.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import java.util.UUID

const val LOGIN_TYPE_GOOGLE = 1
const val LOGIN_TYPE_PHONE = 2
const val LOGIN_DEVICE_TYPE_ANDROID = 1

@Keep
data class ApiUser(
    val id: String = UUID.randomUUID().toString(),
    val phone: String? = null,
    val email: String? = null,
    val auth_type: Int? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val profile_image: String? = null,
    val location_enabled: Boolean = true,
    val space_ids: List<String>? = null,
    val provider_firebase_id_token: String? = null,
    val created_at: Long? = System.currentTimeMillis()
) {
    @get:Exclude
    val fullName: String get() = "$first_name $last_name"
}

@Keep
data class ApiUserSession(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String,
    val device_id: String? = null,
    val fcm_token: String? = null,
    val device_name: String? = null,
    val platform: Int = LOGIN_DEVICE_TYPE_ANDROID,
    val session_active: Boolean = true,
    val app_version: Long? = null,
    val battery_status: String? = null,
    val created_at: Long? = System.currentTimeMillis()
)
