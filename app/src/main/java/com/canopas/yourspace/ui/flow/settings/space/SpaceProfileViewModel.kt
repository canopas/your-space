package com.canopas.yourspace.ui.flow.settings.space

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SpaceProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val spaceRepository: SpaceRepository,
    private val navigator: AppNavigator,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val spaceID =
        savedStateHandle.get<String>(AppDestinations.SpaceProfileScreen.KEY_SPACE_ID) ?: ""

    private val _state = MutableStateFlow(SpaceProfileState())
    val state = _state.asStateFlow()

    init {
        fetchSpaceDetail()
    }

    private fun fetchSpaceDetail() = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(isLoading = true))
        try {
            val spaceInfo = spaceRepository.getCurrentSpaceInfo()
            val locationEnabled =
                spaceInfo?.members?.firstOrNull { it.user.id == authService.currentUser?.id }?.isLocationEnable
                    ?: false
            _state.emit(
                _state.value.copy(
                    isLoading = false,
                    spaceInfo = spaceInfo,
                    currentUserId = authService.currentUser?.id,
                    isAdmin = spaceInfo?.space?.admin_id == authService.currentUser?.id,
                    spaceName = spaceInfo?.space?.name,
                    locationEnabled = locationEnabled
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch space detail")
            _state.emit(_state.value.copy(error = e, isLoading = false))
        }
    }

    fun popBackStack() {
        navigator.navigateBack()
    }

    fun onNameChanged(name: String) {
        _state.value = state.value.copy(spaceName = name)
        onChange()
    }

    private fun onChange() {
        val spaceName = _state.value.spaceInfo?.space?.name
        val validFirstName = (_state.value.spaceName ?: "").trim().length >= 3

        val locationEnabled =
            _state.value.spaceInfo?.members?.firstOrNull { it.user.id == authService.currentUser?.id }?.isLocationEnable
                ?: false

        val changes =
            spaceName != _state.value.spaceName || locationEnabled != _state.value.locationEnabled

        _state.value = state.value.copy(allowSave = validFirstName && changes)
    }

    fun onLocationEnabledChanged(enable: Boolean) {
        _state.value = state.value.copy(locationEnabled = enable)
        onChange()
    }

    fun saveSpace() = viewModelScope.launch(appDispatcher.IO) {
        if (state.value.saving) return@launch
        val space = _state.value.spaceInfo?.space ?: return@launch

        val locationEnabled =
            _state.value.spaceInfo?.members?.firstOrNull { it.user.id == authService.currentUser?.id }?.isLocationEnable
                ?: false

        val isLocationStateUpdated = locationEnabled != _state.value.locationEnabled
        val isNameUpdated = space.name != _state.value.spaceName?.trim()

        try {
            _state.emit(_state.value.copy(saving = true))
            if (isNameUpdated) {
                spaceRepository.updateSpace(
                    space.copy(name = _state.value.spaceName?.trim() ?: "")
                )
            }
            if (isLocationStateUpdated) {
                spaceRepository.enableLocation(
                    spaceID,
                    authService.currentUser?.id ?: "",
                    _state.value.locationEnabled
                )
            }
            _state.emit(_state.value.copy(saving = false))
            navigator.navigateBack()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save space")
            _state.emit(_state.value.copy(saving = false, error = e))
        }
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun showDeleteSpaceConfirmation(show: Boolean) {
        _state.value = state.value.copy(showDeleteSpaceConfirmation = show)
    }

    fun showLeaveSpaceConfirmation(show: Boolean) {
        _state.value = state.value.copy(showLeaveSpaceConfirmation = show)
    }

    fun deleteSpace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(
                _state.value.copy(
                    deletingSpace = true,
                    showDeleteSpaceConfirmation = false
                )
            )
            spaceRepository.deleteSpace(spaceID)
            navigator.navigateBack(AppDestinations.home.path)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete space")
            _state.emit(
                _state.value.copy(
                    error = e,
                    deletingSpace = false,
                    showDeleteSpaceConfirmation = false
                )
            )
        }
    }

    fun leaveSpace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(
                _state.value.copy(
                    leavingSpace = true,
                    showLeaveSpaceConfirmation = false
                )
            )
            spaceRepository.leaveSpace(spaceID)
            navigator.navigateBack(AppDestinations.home.path)
        } catch (e: Exception) {
            Timber.e(e, "Failed to leave space")
            _state.emit(
                _state.value.copy(
                    error = e,
                    leavingSpace = false,
                    showLeaveSpaceConfirmation = false
                )
            )
        }
    }
}

data class SpaceProfileState(
    val isLoading: Boolean = false,
    val saving: Boolean = false,
    val currentUserId: String? = null,
    val isAdmin: Boolean = false,
    val spaceInfo: SpaceInfo? = null,
    val spaceName: String? = null,
    val deletingSpace: Boolean = false,
    val leavingSpace: Boolean = false,
    val locationEnabled: Boolean = false,
    val showDeleteSpaceConfirmation: Boolean = false,
    val showLeaveSpaceConfirmation: Boolean = false,
    val allowSave: Boolean = false,
    val error: Exception? = null
)
