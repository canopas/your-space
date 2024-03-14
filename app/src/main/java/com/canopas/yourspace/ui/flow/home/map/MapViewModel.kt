package com.canopas.yourspace.ui.flow.home.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.location.toLocation
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences,
    private val locationManager: LocationManager,
    private val appDispatcher: AppDispatcher,
    private val navigator: AppNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(MapScreenState())
    val state = _state.asStateFlow()

    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(currentUser = userPreferences.currentUser)
            withContext(appDispatcher.IO) {
                _state.emit(_state.value.copy(defaultCameraPosition = locationManager.getLastLocation()))
            }
            userPreferences.currentSpaceState.collectLatest {
                locationJob?.cancel()
                listenMemberLocation()
            }
        }
    }

    private fun listenMemberLocation() {
        locationJob = viewModelScope.launch(appDispatcher.IO) {
            spaceRepository.getMemberWithLocation().collectLatest { members ->
                var currentCameraPosition = _state.value.defaultCameraPosition
                val selectedUser = _state.value.selectedUser
                if (selectedUser != null) {
                    val updatedLocation =
                        members.find { it.user.id == selectedUser.user.id }?.location?.toLocation()
                    if (updatedLocation != null) {
                        currentCameraPosition = updatedLocation
                    }
                }

                _state.emit(
                    _state.value.copy(
                        members = members,
                        defaultCameraPosition = currentCameraPosition
                    )
                )
                if (members.isEmpty() && _state.value.showUserDetails) {
                    dismissMemberDetail()
                }
            }
        }
    }

    fun showMemberDetail(userInfo: UserInfo) = viewModelScope.launch(appDispatcher.IO) {
        val selectedUser = _state.value.selectedUser
        if (selectedUser != null && selectedUser.user.id == userInfo.user.id) {
            dismissMemberDetail()
        } else {
            val selectedLocation = userInfo.location?.toLocation()
            _state.emit(
                _state.value.copy(
                    selectedUser = userInfo,
                    defaultCameraPosition = selectedLocation,
                    showUserDetails = true
                )
            )
        }
    }

    fun dismissMemberDetail() {
        _state.value = _state.value.copy(showUserDetails = false, selectedUser = null)
    }

    fun addMember() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(loadingInviteCode = true))
            val space = spaceRepository.getCurrentSpace() ?: return@launch
            val inviteCode = spaceRepository.getInviteCode(space.id) ?: return@launch
            _state.emit(_state.value.copy(loadingInviteCode = false))
            navigator.navigateTo(
                AppDestinations.SpaceInvitation.spaceInvitation(inviteCode, space.name).path,
                AppDestinations.createSpace.path,
                inclusive = true
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get invite code")
            _state.emit(_state.value.copy(error = e, loadingInviteCode = false))
        }
    }

    fun navigateToPermissionScreen() {
        navigator.navigateTo(AppDestinations.enablePermissions.path)
    }

    fun startLocationTracking() {
        viewModelScope.launch(appDispatcher.IO) {
            val currentLocation = locationManager.getLastLocation()
            _state.emit(_state.value.copy(defaultCameraPosition = currentLocation))
            locationManager.startService()
        }
    }
}

data class MapScreenState(
    val members: List<UserInfo> = emptyList(),
    val currentUser: ApiUser? = null,
    val defaultCameraPosition: Location? = null,
    val selectedUser: UserInfo? = null,
    val showUserDetails: Boolean = false,
    val loadingInviteCode: Boolean = false,
    val error: Exception? = null
)
