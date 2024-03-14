package com.canopas.yourspace.ui.flow.settings.space

import androidx.lifecycle.SavedStateHandle
import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SpaceProfileViewModelTest {

    private val space = ApiSpace(id = "space1", admin_id = "user1", name = "space_name")
    val user1 = ApiUser(id = "user1", first_name = "first_name", last_name = "last_name")
    private val user2 = ApiUser(id = "user2", first_name = "first_name", last_name = "last_name")
    private val userInfo1 = UserInfo(user1, isLocationEnable = true)
    private val userInfo2 = UserInfo(user2)
    private val members = listOf(userInfo1, userInfo2)

    val space_info1 = SpaceInfo(space = space, members = members)

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val savedStateHandle = mock<SavedStateHandle>()
    private val spaceRepository = mock<SpaceRepository>()
    private val navigator = mock<AppNavigator>()
    private val authService = mock<AuthService>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())

    private lateinit var viewModel: SpaceProfileViewModel

    private fun setup() {
        whenever(savedStateHandle.get<String>(AppDestinations.SpaceProfileScreen.KEY_SPACE_ID)).thenReturn(
            "space1"
        )
        viewModel = SpaceProfileViewModel(
            savedStateHandle,
            spaceRepository,
            navigator,
            authService,
            testDispatcher
        )
    }

    @Test
    fun `fetchSpaceDetail should update state with isLoading true`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
                space_info1
            }
        }
        setup()
        assert(viewModel.state.value.isLoading)
    }

    @Test
    fun `fetchSpaceDetail should update state with spaceInfo`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        setup()
        assert(viewModel.state.value.spaceInfo == space_info1)
    }

    @Test
    fun `fetchSpaceDetail should update state with currentUserId`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        setup()
        assert(viewModel.state.value.currentUserId == user1.id)
    }

    @Test
    fun `fetchSpaceDetail should update state with isAdmin`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        setup()
        assert(viewModel.state.value.isAdmin)
    }

    @Test
    fun `fetchSpaceDetail should update state with spaceName`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        setup()
        assert(viewModel.state.value.spaceName == space_info1.space.name)
    }

    @Test
    fun `fetchSpaceDetail should update state with locationEnabled`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        setup()
        assert(viewModel.state.value.locationEnabled)
    }

    @Test
    fun `fetchSpaceDetails should update state with error when exception is thrown`() = runTest {
        val exception = RuntimeException("Error")
        whenever(spaceRepository.getCurrentSpaceInfo()).thenThrow(exception)
        setup()
        assert(viewModel.state.value.error == exception)
        assert(!viewModel.state.value.isLoading)
    }

    @Test
    fun `popBackStack should navigate back`() {
        setup()
        viewModel.popBackStack()
        verify(navigator).navigateBack()
    }

    @Test
    fun `onNameChanged should update state with spaceName`() {
        setup()
        viewModel.onNameChanged("new_name")
        assert(viewModel.state.value.spaceName == "new_name")
    }

    @Test
    fun `onChange should update state with allowSave`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        setup()
        viewModel.onNameChanged("new_name")
        assert(viewModel.state.value.allowSave)
    }

    @Test
    fun `onLocationEnabledChanged should update state with locationEnabled`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        setup()
        viewModel.onLocationEnabledChanged(false)
        assert(!viewModel.state.value.locationEnabled)
    }

    @Test
    fun `onLocationEnabledChanged should update state with allowSave`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        setup()
        viewModel.onLocationEnabledChanged(false)
        assert(viewModel.state.value.allowSave)
    }

    @Test
    fun `onLocationEnabledChanged should not update state allowSave if it's not updated`() =
        runTest {
            whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
            whenever(authService.currentUser).thenReturn(user1)
            setup()
            viewModel.onLocationEnabledChanged(true)
            assert(!viewModel.state.value.allowSave)
        }

    @Test
    fun `saveSpace should update state with saving true`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        whenever(
            spaceRepository.updateSpace(
                space_info1.space.copy(
                    name = "new_name"
                )
            )
        ).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
            }
        }

        setup()
        viewModel.onNameChanged("new_name")
        viewModel.saveSpace()
        assert(viewModel.state.value.saving)
    }

    @Test
    fun `saveSpace should invoke updateSpace if spaceName is updated`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)

        setup()
        viewModel.onNameChanged("new_name")
        viewModel.saveSpace()
        verify(spaceRepository).updateSpace(
            space_info1.space.copy(
                name = "new_name"
            )
        )
    }

    @Test
    fun `saveSpace should not invoke updateSpace if spaceName is not updated`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)

        setup()
        viewModel.saveSpace()
        verify(spaceRepository, times(0)).updateSpace(
            space_info1.space.copy(
                name = "space_name"
            )
        )
    }

    @Test
    fun `saveSpace should invoke enableLocation if locationEnabled is updated`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)

        setup()
        viewModel.onLocationEnabledChanged(false)
        viewModel.saveSpace()
        verify(spaceRepository).enableLocation(space.id, user1.id, false)
    }

    @Test
    fun `saveSpace should not invoke enableLocation if locationEnabled is not updated`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)

        setup()
        viewModel.saveSpace()
        verify(spaceRepository, times(0)).enableLocation(space.id, user1.id, false)
    }

    @Test
    fun `saveSpace should navigate back after saving`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)

        setup()
        viewModel.saveSpace()
        verify(navigator).navigateBack()
    }

    @Test
    fun `resetState should update state with default values`() {
        setup()
        viewModel.resetErrorState()
        assert(viewModel.state.value.error == null)
    }

    @Test
    fun `showDeleteSpaceConfirmation should update state with showDeleteSpaceConfirmation`() {
        setup()
        viewModel.showDeleteSpaceConfirmation(true)
        assert(viewModel.state.value.showDeleteSpaceConfirmation)
    }

    @Test
    fun `showLeaveSpaceConfirmation should update state with showLeaveSpaceConfirmation`() {
        setup()
        viewModel.showLeaveSpaceConfirmation(true)
        assert(viewModel.state.value.showLeaveSpaceConfirmation)
    }

    @Test
    fun `deleteSpace should update state with deletingSpace true`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        whenever(spaceRepository.deleteSpace(space.id)).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
            }
        }
        setup()
        viewModel.deleteSpace()
        assert(viewModel.state.value.deletingSpace)
    }

    @Test
    fun `deleteSpace should invoke deleteSpace`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        setup()
        viewModel.deleteSpace()
        verify(spaceRepository).deleteSpace(space.id)
    }

    @Test
    fun `deleteSpace should navigate back after deleting`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        setup()
        viewModel.deleteSpace()
        verify(navigator).navigateBack(AppDestinations.home.path)
    }

    @Test
    fun `deleteSpace should update state with error if exception is thrown`() = runTest {
        val exception = RuntimeException("Error")
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        whenever(spaceRepository.deleteSpace(space.id)).thenThrow(exception)
        setup()
        viewModel.deleteSpace()
        assert(viewModel.state.value.error == exception)
    }

    @Test
    fun `leaveSpace should update state with leavingSpace true`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        whenever(spaceRepository.leaveSpace(space.id)).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
            }
        }
        setup()
        viewModel.leaveSpace()
        assert(viewModel.state.value.leavingSpace)
    }

    @Test
    fun `leaveSpace should invoke leaveSpace`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        setup()
        viewModel.leaveSpace()
        verify(spaceRepository).leaveSpace(space.id)
    }

    @Test
    fun `leaveSpace should navigate back after leaving`() = runTest {
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        setup()
        viewModel.leaveSpace()
        verify(navigator).navigateBack(AppDestinations.home.path)
    }

    @Test
    fun `leaveSpace should update state with error if exception is thrown`() = runTest {
        val exception = RuntimeException("Error")
        whenever(spaceRepository.getCurrentSpaceInfo()).thenReturn(space_info1)
        whenever(authService.currentUser).thenReturn(user1)
        whenever(spaceRepository.leaveSpace(space.id)).thenThrow(exception)
        setup()
        viewModel.leaveSpace()
        assert(viewModel.state.value.error == exception)
    }
}
