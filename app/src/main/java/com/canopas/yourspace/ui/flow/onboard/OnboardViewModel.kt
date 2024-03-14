package com.canopas.yourspace.ui.flow.onboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.space.SpaceInvitationService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardViewModel @Inject constructor(
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences,
    private val navigator: AppNavigator,
    private val invitationService: SpaceInvitationService
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardScreenState())
    val state: StateFlow<OnboardScreenState> = _state

    private val currentUser get() = authService.currentUser

    init {
        val user = authService.currentUser
        _state.value = _state.value.copy(
            firstName = user?.first_name ?: "",
            lastName = user?.last_name ?: "",
            currentStep = if (user?.first_name.isNullOrEmpty()) {
                OnboardItems.PickName
            } else {
                OnboardItems.SpaceIntro
            }
        )
    }

    fun onFirstNameChange(name: String) {
        _state.value = _state.value.copy(
            firstName = name
        )
    }

    fun onLastNameChange(name: String) {
        _state.value = _state.value.copy(
            lastName = name
        )
    }

    fun navigateToSpaceInfo() = viewModelScope.launch(appDispatcher.IO) {
        if (currentUser?.first_name != _state.value.firstName || currentUser?.last_name != _state.value.lastName) {
            _state.emit(_state.value.copy(updatingUserName = true))
            val user = currentUser?.copy(
                first_name = _state.value.firstName.trim(),
                last_name = _state.value.lastName.trim()
            )
            user?.let { authService.updateUser(it) }
        }
        _state.emit(
            _state.value.copy(
                updatingUserName = false,
                prevStep = _state.value.currentStep,
                currentStep = OnboardItems.SpaceIntro
            )
        )
    }

    fun navigateToJoinOrCreateSpace() {
        _state.value = _state.value.copy(
            prevStep = _state.value.currentStep,
            currentStep = OnboardItems.JoinOrCreateSpace
        )
    }

    fun navigateToCreateSpace() {
        _state.value = _state.value.copy(
            prevStep = _state.value.currentStep,
            currentStep = OnboardItems.CreateSpace
        )
    }

    fun createSpace(spaceName: String) = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(
                _state.value.copy(
                    spaceName = spaceName,
                    creatingSpace = true
                )
            )
            val invitationCode = spaceRepository.createSpaceAndGetInviteCode(spaceName)
            _state.emit(
                _state.value.copy(
                    creatingSpace = false,
                    spaceInviteCode = invitationCode,
                    prevStep = _state.value.currentStep,
                    currentStep = OnboardItems.ShareSpaceCodeOnboard
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Unable to create space")
            _state.emit(_state.value.copy(creatingSpace = false, error = e))
        }
    }

    fun navigateToHome() = viewModelScope.launch {
        userPreferences.setOnboardShown(true)
        navigator.navigateTo(
            AppDestinations.home.path,
            popUpToRoute = AppDestinations.onboard.path,
            inclusive = true
        )
    }

    fun submitInviteCode() = viewModelScope.launch(appDispatcher.IO) {
        val code = _state.value.spaceInviteCode ?: return@launch
        _state.emit(_state.value.copy(verifyingInviteCode = true))

        try {
            val invitation = invitationService.getInvitation(code)
            if (invitation == null) {
                _state.emit(
                    _state.value.copy(
                        verifyingInviteCode = false,
                        errorInvalidInviteCode = true
                    )
                )
                return@launch
            }

            val space = spaceRepository.getSpace(invitation.space_id)
            val joinedSpaces = currentUser?.space_ids ?: emptyList()

            if (space != null && space.id in joinedSpaces) {
                navigateToHome()
                return@launch
            }

            if (space != null) {
                spaceRepository.joinSpace(invitation.space_id)
            }

            _state.emit(
                _state.value.copy(
                    joinedSpace = space,
                    spaceName = space?.name,
                    spaceId = invitation.space_id,
                    verifyingInviteCode = false,
                    errorInvalidInviteCode = false
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Unable to verify invite code")
            _state.emit(
                _state.value.copy(
                    verifyingInviteCode = false,
                    error = e
                )
            )
        }
    }

    fun onInviteCodeChanged(inviteCode: String) {
        _state.value = _state.value.copy(
            spaceInviteCode = inviteCode,
            errorInvalidInviteCode = false
        )

        if (inviteCode.length == 6) {
            submitInviteCode()
        }
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(
            error = null,
            alreadySpaceMember = false,
            errorInvalidInviteCode = false
        )
    }

    fun popTo(page: OnboardItems) {
        _state.value = _state.value.copy(
            prevStep = _state.value.currentStep,
            currentStep = page
        )
    }
}

data class OnboardScreenState(
    val prevStep: OnboardItems = OnboardItems.PickName,
    val currentStep: OnboardItems = OnboardItems.PickName,
    val firstName: String = "",
    val lastName: String = "",
    val updatingUserName: Boolean = false,
    val spaceName: String? = "",
    val spaceId: String? = null,
    val joinedSpace: ApiSpace? = null,
    val spaceInviteCode: String? = "",
    val creatingSpace: Boolean = false,
    val verifyingInviteCode: Boolean = false,
    val errorInvalidInviteCode: Boolean = false,
    val alreadySpaceMember: Boolean = false,
    val error: Exception? = null
)

sealed class OnboardItems {
    data object PickName : OnboardItems()
    data object SpaceIntro : OnboardItems()
    data object JoinOrCreateSpace : OnboardItems()
    data object CreateSpace : OnboardItems()
    data object ShareSpaceCodeOnboard : OnboardItems()
}
