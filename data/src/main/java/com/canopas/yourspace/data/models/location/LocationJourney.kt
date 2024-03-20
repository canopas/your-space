package com.canopas.yourspace.data.models.location

import com.google.errorprone.annotations.Keep

@Keep
data class LocationJourney(
    val id: String,
    val userId: String,
    val fromLatitude: Double,
    val fromLongitude: Double,
    var toLatitude: Double?,
    var toLongitude: Double?,
    val routeDistance: Double?,
    val routeDuration: String?,
    val currentLocationDuration: String?,
    val isSticky: Boolean,
    val createdAt: Long?
)
