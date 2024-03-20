package com.canopas.yourspace.ui.flow.home.map.member

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.LocationJourney
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class LocationHistoryPagingSource(private var query: Query) :
    PagingSource<QuerySnapshot, LocationJourney>() {

    private var isLoadedFirstTime = MutableStateFlow(true)

    override fun getRefreshKey(state: PagingState<QuerySnapshot, LocationJourney>): QuerySnapshot? =
        null

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, LocationJourney> =
        try {
            val currentPage = params.key ?: query.get().await()
            if (currentPage.isEmpty) {
                val lists =
                    currentPage.toObjects(ApiLocation::class.java).createLocationJourney()
                LoadResult.Page(data = lists, prevKey = null, nextKey = null)
            } else {
                val lastVisibleProduct = currentPage.documents[currentPage.size() - 1]
                val nextPage = query.startAfter(lastVisibleProduct).get().await()
                val lists =
                    currentPage.toObjects(ApiLocation::class.java).createLocationJourney()
                LoadResult.Page(
                    data = lists,
                    prevKey = null,
                    nextKey = nextPage
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    private fun List<ApiLocation>.createLocationJourney(): List<LocationJourney> {
        val locationJourney = mutableListOf<LocationJourney>()

        if (isEmpty()) {
            return emptyList()
        }

        var stickyLocation =
            LocationJourney(
                id = this[0].id,
                userId = this[0].user_id,
                fromLatitude = this[0].latitude,
                fromLongitude = this[0].longitude,
                toLatitude = null,
                toLongitude = null,
                routeDistance = null,
                routeDuration = null,
                currentLocationDuration = durationBetweenLocationWithFormattedTime(
                    this[0].created_at,
                    null
                ),
                isSticky = true,
                createdAt = this[0].created_at
            )

        for (i in indices) {
            val nextLocation = this.getOrNull(i + 1)
            val currentLocation = this[i]
            val currentTime = System.currentTimeMillis()
            if (nextLocation != null) {
                if (checkElapsedTimeIsMoreThanFiveMinutes(
                        currentLocation.created_at ?: currentTime,
                        nextLocation.created_at ?: currentTime
                    ) || isLoadedFirstTime.value || distanceBetweenLocations(
                            currentLocation,
                            nextLocation
                        ) > 5000
                ) {
                    val distance = distanceBetweenLocations(
                        currentLocation,
                        nextLocation
                    )
                    val duration = durationBetweenLocationWithFormattedTime(
                        currentLocation.created_at,
                        stickyLocation.createdAt
                    )
                    if (isLoadedFirstTime.value) {
                        locationJourney.add(stickyLocation)
                        isLoadedFirstTime.value = false
                    } else {
                        locationJourney.add(
                            LocationJourney(
                                id = currentLocation.id,
                                userId = currentLocation.user_id,
                                fromLatitude = currentLocation.latitude,
                                fromLongitude = currentLocation.longitude,
                                toLatitude = nextLocation.latitude,
                                toLongitude = nextLocation.longitude,
                                routeDistance = distance,
                                routeDuration = duration,
                                currentLocationDuration = null,
                                isSticky = false,
                                createdAt = currentLocation.created_at
                            )
                        )
                        locationJourney.add(
                            LocationJourney(
                                id = nextLocation.id,
                                userId = nextLocation.user_id,
                                fromLatitude = nextLocation.latitude,
                                fromLongitude = nextLocation.longitude,
                                toLatitude = null,
                                toLongitude = null,
                                routeDistance = null,
                                routeDuration = null,
                                currentLocationDuration = durationBetweenLocationWithFormattedTime(
                                    currentLocation.created_at,
                                    nextLocation.created_at
                                ),
                                isSticky = true,
                                createdAt = nextLocation.created_at
                            )
                        )
                    }
                    stickyLocation = locationJourney.last()
                }
            }
        }

        return locationJourney
    }

    private fun distanceBetweenLocations(location1: ApiLocation, location2: ApiLocation): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            location1.latitude,
            location1.longitude,
            location2.latitude,
            location2.longitude,
            results
        )
        return results[0].toDouble()
    }

    private fun durationBetweenLocationWithFormattedTime(
        duration1: Long?,
        duration2: Long?
    ): String {
        val currentTime = System.currentTimeMillis()
        val maxBetweenLocation1And2 = maxOf(duration1 ?: currentTime, duration2 ?: currentTime)
        val minBetweenLocation1And2 =
            minOf(duration1 ?: currentTime, duration2 ?: currentTime, currentTime)
        val duration = (maxBetweenLocation1And2).minus(minBetweenLocation1And2)
        val hours = duration / 3600000
        val minutes = (duration % 3600000) / 60000
        val seconds = (duration % 60000) / 1000
        val daysIfGreaterThan24Hrs = hours / 24
        val hoursIfGreaterThan24Hrs = hours % 24
        return if (daysIfGreaterThan24Hrs > 0) {
            "$daysIfGreaterThan24Hrs day(s) $hoursIfGreaterThan24Hrs hr(s) $minutes min(s)"
        } else if (hours > 0) {
            "$hours hr(s) $minutes min(s)"
        } else if (minutes > 0) {
            "$minutes min(s)"
        } else {
            "$seconds sec(s)"
        }
    }

    private fun checkElapsedTimeIsMoreThanFiveMinutes(timestamp1: Long, timestamp2: Long): Boolean {
        val minTimeStamp = minOf(timestamp1, timestamp2)
        val maxTimeStamp = maxOf(timestamp1, timestamp2)
        val calendar1 = Calendar.getInstance().apply { timeInMillis = minTimeStamp }
        val calendar2 = Calendar.getInstance().apply { timeInMillis = maxTimeStamp }
        return calendar2.timeInMillis - calendar1.timeInMillis > 5 * 60 * 1000
    }
}
