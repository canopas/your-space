package com.canopas.yourspace.data.service.place

import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.models.place.ApiPlaceMemberSetting
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class ApiPlaceService @Inject constructor(
    private val db: FirebaseFirestore,
    private val placesClient: PlacesClient,
    private val functions: FirebaseFunctions
) {

    private val spaceRef = db.collection(Config.FIRESTORE_COLLECTION_SPACES)

    private fun spacePlacesRef(spaceId: String) =
        spaceRef.document(spaceId).collection(Config.FIRESTORE_COLLECTION_SPACE_PLACES)

    private fun spacePlacesSettingsRef(spaceId: String, placeId: String) =
        spaceRef.document(spaceId).collection(Config.FIRESTORE_COLLECTION_SPACE_PLACES)
            .document(placeId).collection(
                Config.FIRESTORE_COLLECTION_SPACE_PLACES_MEMBER_SETTINGS
            )

    suspend fun addPlace(
        spaceId: String,
        createdBy: String,
        name: String,
        latitude: Double,
        longitude: Double,
        spaceMemberIds: List<String>
    ) {
        val placeDoc = spacePlacesRef(spaceId).document()
        val place = ApiPlace(
            id = placeDoc.id,
            created_by = createdBy,
            space_id = spaceId,
            name = name,
            latitude = latitude,
            longitude = longitude
        )

        placeDoc.set(place).await()
        val settings = spaceMemberIds.map { memberId ->
            val filterIds = spaceMemberIds.filter { it != memberId }
            ApiPlaceMemberSetting(
                place_id = place.id,
                user_id = memberId,
                arrival_alert_for = filterIds,
                leave_alert_for = filterIds
            )
        }

        settings.forEach { setting ->
            spacePlacesSettingsRef(spaceId, place.id).document(setting.user_id).set(setting).await()
        }

        val data = mapOf(
            "spaceId" to spaceId,
            "placeName" to name,
            "createdBy" to createdBy,
            "spaceMemberIds" to spaceMemberIds
        )
        functions.getHttpsCallable("sendNewPlaceNotification").call(data).addOnSuccessListener {
            Timber.d("Notification sent successfully")
        }.addOnFailureListener {
            Timber.e(it, "Failed to send new place notification")
        }
    }

    suspend fun joinUserToExistingPlaces(
        userId: String,
        spaceId: String,
        spaceMemberIds: List<String>
    ) {
        val filterIds = spaceMemberIds.filter { it != userId }
        val spaces = getPlaces(spaceId)

        spaces.forEach { place ->
            getPlaceMemberSettings(place.id, spaceId).forEach { setting ->
                val newSettings = setting.copy(
                    arrival_alert_for = updateAlertUsersList(setting.arrival_alert_for, userId),
                    leave_alert_for = updateAlertUsersList(setting.leave_alert_for, userId)
                )
                updatePlaceSettings(place, setting.user_id, newSettings)
            }
            val newUserSettings = ApiPlaceMemberSetting(
                place_id = place.id,
                user_id = userId,
                arrival_alert_for = filterIds,
                leave_alert_for = filterIds
            )
            updatePlaceSettings(place, userId, newUserSettings)
        }
    }

    private fun updateAlertUsersList(alertUsers: List<String>, newUserId: String): List<String> {
        return if (alertUsers.contains(newUserId)) alertUsers else alertUsers + newUserId
    }

    suspend fun removedUserFromExistingPlaces(spaceId: String, userId: String) {
        val spaces = getPlaces(spaceId)
        spaces.forEach { place ->
            getPlaceMemberSettings(place.id, spaceId).forEach { setting ->
                if (setting.user_id == userId) {
                    deletePlaceSetting(setting.place_id, spaceId, userId)
                } else {
                    val newSettings = setting.copy(
                        arrival_alert_for = setting.arrival_alert_for.filter { it != userId },
                        leave_alert_for = setting.leave_alert_for.filter { it != userId }
                    )
                    updatePlaceSettings(place, setting.user_id, newSettings)
                }
            }
        }
    }

    suspend fun getPlaces(spaceId: String): List<ApiPlace> {
        val places = spacePlacesRef(spaceId).get().await()
        return places.toObjects(ApiPlace::class.java).sortedByDescending { it.created_at }
    }

    fun listenAllPlaces(spaceId: String): Flow<List<ApiPlace>> {
        if (spaceId.isEmpty()) return emptyFlow()
        return spacePlacesRef(spaceId).snapshotFlow(ApiPlace::class.java)
    }

    suspend fun deletePlace(currentSpaceId: String, id: String) {
        spacePlacesRef(currentSpaceId).document(id).delete().await()
    }

    suspend fun getPlace(placeId: String): ApiPlace? {
        return db.collectionGroup(Config.FIRESTORE_COLLECTION_SPACE_PLACES)
            .whereEqualTo("id", placeId).limit(1)
            .snapshotFlow(ApiPlace::class.java).first().firstOrNull()
    }

    suspend fun getPlaceMemberSettings(
        placeId: String,
        spaceId: String
    ): List<ApiPlaceMemberSetting> {
        val settings = spacePlacesSettingsRef(spaceId, placeId).get().await()
        return settings.toObjects(ApiPlaceMemberSetting::class.java)
    }

    suspend fun getPlaceMemberSetting(
        placeId: String,
        spaceId: String,
        userId: String
    ): ApiPlaceMemberSetting? {
        val settings =
            spacePlacesSettingsRef(spaceId, placeId)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get().await()
        return settings.toObjects(ApiPlaceMemberSetting::class.java).firstOrNull()
    }

    suspend fun updatePlace(place: ApiPlace) {
        spacePlacesRef(place.space_id).document(place.id).set(place).await()
    }

    suspend fun updatePlaceSettings(
        place: ApiPlace,
        userId: String,
        setting: ApiPlaceMemberSetting
    ) {
        spacePlacesSettingsRef(place.space_id, place.id).document(userId).set(setting).await()
    }

    suspend fun deletePlaceSetting(placeId: String, spaceId: String, userId: String) {
        spacePlacesSettingsRef(spaceId, placeId).document(userId).delete().await()
    }

    suspend fun findPlace(query: String): List<Place> {
        if (query.trim().isEmpty()) return emptyList()
        val placeFields =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

        val searchByTextRequest =
            SearchByTextRequest.builder(query, placeFields).setMaxResultCount(20).build()
        val response = placesClient.searchByText(searchByTextRequest).await()
        return response.places
    }
}
