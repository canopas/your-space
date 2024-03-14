package com.canopas.yourspace.ui.flow.home.map

import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MapViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val spaceRepository = mock<SpaceRepository>()
    private val userPreferences = mock<UserPreferences>()
    private val locationManager = mock<LocationManager>()
    private val navigator = mock<AppNavigator>()
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())

    private lateinit var viewModel: MapViewModel

    private fun setUp() {
        viewModel = MapViewModel(
            spaceRepository = spaceRepository,
            userPreferences = userPreferences,
            locationManager = locationManager,
            appDispatcher = testDispatcher,
            navigator = navigator
        )
    }

    @Test
    fun `when userPreferences currentSpaceState emits, then listenMemberLocation is called`() =
        runTest {
            val user = ApiUser(id = "user1")
            val flow = flow {
                emit("space1")
            }
            whenever(userPreferences.currentUser).thenReturn(user)
            whenever(userPreferences.currentSpaceState).thenReturn(flow)
            whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
            setUp()
            verify(spaceRepository).getMemberWithLocation()
        }

    @Test
    fun `showMemberDetail should set selectedUser`() = runTest {
        val user = ApiUser(id = "user1")
        val info = UserInfo(user)
        val flow = flow {
            emit("space1")
        }
        whenever(userPreferences.currentUser).thenReturn(user)
        whenever(userPreferences.currentSpaceState).thenReturn(flow)
        whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
        setUp()
        viewModel.showMemberDetail(info)
        assert(viewModel.state.value.selectedUser == info)
        assert(viewModel.state.value.showUserDetails)
    }

    @Test
    fun `dismissMemberDetail should set selectedUser to null`() = runTest {
        val user = ApiUser(id = "user1")
        val info = UserInfo(user)
        val flow = flow {
            emit("space1")
        }
        whenever(userPreferences.currentUser).thenReturn(user)
        whenever(userPreferences.currentSpaceState).thenReturn(flow)
        whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
        setUp()
        viewModel.showMemberDetail(info)
        viewModel.dismissMemberDetail()
        assert(viewModel.state.value.selectedUser == null)
        assert(!viewModel.state.value.showUserDetails)
    }

    @Test
    fun `showMemberDetail should dismissMemberDetail if selectedUser is not null`() = runTest {
        val user = ApiUser(id = "user1")
        val info = UserInfo(user)
        val flow = flow {
            emit("space1")
        }
        whenever(userPreferences.currentUser).thenReturn(user)
        whenever(userPreferences.currentSpaceState).thenReturn(flow)
        whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
        setUp()
        viewModel.showMemberDetail(info)
        assert(viewModel.state.value.selectedUser == info)
        assert(viewModel.state.value.showUserDetails)
        viewModel.showMemberDetail(info)
        assert(viewModel.state.value.selectedUser == null)
        assert(!viewModel.state.value.showUserDetails)
    }

    @Test
    fun `addMember should set loadingInviteCode to true`() = runTest {
        val user = ApiUser(id = "user1")
        val flow = flow {
            emit("space1")
        }
        whenever(userPreferences.currentUser).thenReturn(user)
        whenever(userPreferences.currentSpaceState).thenReturn(flow)
        whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
        whenever(spaceRepository.getCurrentSpace()).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
                null
            }
        }
        setUp()
        viewModel.addMember()
        assert(viewModel.state.value.loadingInviteCode)
    }

    @Test
    fun `addMember should call spaceRepository getCurrentSpace`() = runTest {
        val user = ApiUser(id = "user1")
        val flow = flow {
            emit("space1")
        }
        whenever(userPreferences.currentUser).thenReturn(user)
        whenever(userPreferences.currentSpaceState).thenReturn(flow)
        whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
        setUp()
        viewModel.addMember()
        verify(spaceRepository).getCurrentSpace()
    }

    @Test
    fun `addMember should call getInviteCode`() = runTest {
        val user = ApiUser(id = "user1")
        val space = ApiSpace(id = "space1")
        val flow = flow {
            emit("space1")
        }
        whenever(userPreferences.currentUser).thenReturn(user)
        whenever(userPreferences.currentSpaceState).thenReturn(flow)
        whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
        whenever(spaceRepository.getCurrentSpace()).thenReturn(space)
        setUp()
        viewModel.addMember()
        verify(spaceRepository).getInviteCode("space1")
    }

    @Test
    fun `addMember should navigate to spaceInvitation`() = runTest {
        val user = ApiUser(id = "user1")
        val space = ApiSpace(id = "space1", name = "space1")
        val flow = flow {
            emit("space1")
        }
        whenever(userPreferences.currentUser).thenReturn(user)
        whenever(userPreferences.currentSpaceState).thenReturn(flow)
        whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
        whenever(spaceRepository.getCurrentSpace()).thenReturn(space)
        whenever(spaceRepository.getInviteCode("space1")).thenReturn("inviteCode")
        setUp()
        viewModel.addMember()
        verify(navigator).navigateTo(
            "space-invite/inviteCode/space1",
            "create-space",
            true
        )
    }

    @Test
    fun `addMember should set error if getCurrentSpace throws exception`() = runTest {
        val user = ApiUser(id = "user1")
        val flow = flow {
            emit("space1")
        }
        val exception = RuntimeException("error")
        whenever(userPreferences.currentUser).thenReturn(user)
        whenever(userPreferences.currentSpaceState).thenReturn(flow)
        whenever(spaceRepository.getMemberWithLocation()).thenReturn(flowOf(emptyList()))
        whenever(spaceRepository.getCurrentSpace()).thenThrow(exception)
        setUp()
        viewModel.addMember()
        assert(viewModel.state.value.error == exception)
    }
}
