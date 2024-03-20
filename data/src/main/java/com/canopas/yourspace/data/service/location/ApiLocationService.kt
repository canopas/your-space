package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLocationService @Inject constructor(
    db: FirebaseFirestore,
    private val locationManager: LocationManager
) {
    private val userRef = db.collection(Config.FIRESTORE_COLLECTION_USERS)
    private fun locationRef(userId: String) = userRef.document(userId).collection(Config.FIRESTORE_COLLECTION_USER_LOCATIONS)

    suspend fun saveLastKnownLocation(
        userId: String
    ) {
        val lastLocation = locationManager.getLastLocation() ?: return
        val docRef = locationRef(userId).document()

        val location = ApiLocation(
            id = docRef.id,
            user_id = userId,
            latitude = lastLocation.latitude,
            longitude = lastLocation.longitude,
            created_at = System.currentTimeMillis()
        )

        docRef.set(location).await()
    }

    suspend fun saveCurrentLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        recordedAt: Long
    ) {
        val docRef = locationRef(userId).document()

        val location = ApiLocation(
            id = docRef.id,
            user_id = userId,
            latitude = latitude,
            longitude = longitude,
            created_at = recordedAt
        )

        docRef.set(location).await()
    }

    suspend fun getCurrentLocation(userId: String) =
        locationRef(userId).whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
            .snapshotFlow(ApiLocation::class.java)

    suspend fun getLastLocation(userId: String) = try {
        locationRef(userId).whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
            .get().await().documents.firstOrNull()?.toObject(ApiLocation::class.java)
    } catch (e: Exception) {
        Timber.e(e, "Error while getting last location")
        null
    }

    fun getLocationHistoryQuery(userId: String, from: Long, to: Long) =
        locationRef(userId).whereEqualTo("user_id", userId)
            .whereGreaterThanOrEqualTo("created_at", from)
            .whereLessThan("created_at", to)
            .orderBy("created_at", Query.Direction.DESCENDING).limit(8)

    suspend fun deleteLocations(userId: String) {
        locationRef(userId).whereEqualTo("user_id", userId).get().await().documents.forEach {
            it.reference.delete().await()
        }
    }
}
