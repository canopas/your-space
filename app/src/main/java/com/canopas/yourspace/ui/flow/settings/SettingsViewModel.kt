package com.canopas.yourspace.ui.flow.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val navigator: AppNavigator,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val spaceRepository: SpaceRepository

) : ViewModel() {

    private val _state = MutableStateFlow(SettingsScreenState())
    val state = _state.asStateFlow()

    init {
        getUser()
        getUserSpaces()
    }

    private fun getUser() = viewModelScope.launch(appDispatcher.IO) {
        authService.getUserFlow().collectLatest { user ->
            _state.emit(_state.value.copy(user = user))
        }
    }

    private fun getUserSpaces() = viewModelScope.launch(appDispatcher.IO) {
        val userId = authService.currentUser?.id ?: return@launch
        try {
            _state.emit(_state.value.copy(loadingSpaces = state.value.spaces.isEmpty()))

            spaceRepository.getUserSpaces(userId).collectLatest {
                _state.emit(_state.value.copy(loadingSpaces = false, spaces = it))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get spaces")
            _state.emit(_state.value.copy(loadingSpaces = false, error = e.localizedMessage))
        }
    }

    fun popBackStack() {
        navigator.navigateBack()
    }

    fun showSignOutConfirmation(show: Boolean) {
        _state.value = _state.value.copy(openSignOutDialog = show)
    }

    fun signOutUser() = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(signingOut = true, openSignOutDialog = false))
        authService.signOut()
        navigator.navigateTo(
            AppDestinations.signIn.path,
            clearStack = true
        )
        _state.emit(_state.value.copy(signingOut = false))
    }

    fun editProfile() {
        navigator.navigateTo(AppDestinations.editProfile.path)
    }

    fun navigateToSpaceSettings(spaceId: String) {
        navigator.navigateTo(AppDestinations.SpaceProfileScreen.spaceSettings(spaceId).path)
    }

    fun showContactSupport() {
        navigator.navigateTo(AppDestinations.contactSupport.path)
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }
}

data class SettingsScreenState(
    val user: ApiUser? = null,
    val spaces: List<ApiSpace> = emptyList(),
    val loadingSpaces: Boolean = false,
    var openSignOutDialog: Boolean = false,
    var openDeleteAccountDialog: Boolean = false,
    var deletingAccount: Boolean = false,
    var signingOut: Boolean = false,
    var error: String? = null
)
