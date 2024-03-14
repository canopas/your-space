package com.canopas.yourspace.ui.flow.home.space.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.repository.SpaceRepository
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
class CreateSpaceHomeViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val spaceRepository: SpaceRepository,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(CreateSpaceHomeState())
    val state = _state.asStateFlow()

    fun navigateBack() {
        appNavigator.navigateBack()
    }

    fun onSpaceNameChange(spaceName: String) {
        _state.value = _state.value.copy(spaceName = spaceName)
    }

    fun createSpace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(creatingSpace = true))
            val invitationCode = spaceRepository.createSpaceAndGetInviteCode(_state.value.spaceName)
            appNavigator.navigateTo(
                AppDestinations.SpaceInvitation.spaceInvitation(
                    invitationCode,
                    _state.value.spaceName
                ).path,
                AppDestinations.createSpace.path,
                inclusive = true
            )
        } catch (e: Exception) {
            Timber.e(e, "Unable to create space")
            _state.emit(_state.value.copy(creatingSpace = false, error = e))
        }
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }
}

data class CreateSpaceHomeState(
    val spaceName: String = "",
    val spaceInviteCode: String? = "",
    val creatingSpace: Boolean = false,
    val error: Exception? = null
)
