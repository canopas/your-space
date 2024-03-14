package com.canopas.yourspace.ui.flow.home.home

import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.flow.home.home.HomeViewModelTestData.space_info1
import com.canopas.yourspace.ui.flow.home.home.HomeViewModelTestData.space_info2
import com.canopas.yourspace.ui.flow.home.home.HomeViewModelTestData.user1
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

object HomeViewModelTestData {
    private val space = ApiSpace(id = "space1", admin_id = "user1", name = "space_name")
    private val space2 = ApiSpace(id = "space2", admin_id = "user2", name = "space_name")
    val user1 = ApiUser(id = "user1", first_name = "first_name", last_name = "last_name")
    private val user2 = ApiUser(id = "user2", first_name = "first_name", last_name = "last_name")
    private val userInfo1 = UserInfo(user1)
    private val userInfo2 = UserInfo(user2)
    private val members = listOf<UserInfo>(userInfo1, userInfo2)

    val space_info1 = SpaceInfo(space = space, members = members)
    val space_info2 = SpaceInfo(space = space2, members = members)
}

@ExperimentalCoroutinesApi
class HomeScreenViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val navigator = mock<AppNavigator>()
    private val locationManager = mock<LocationManager>()
    private val spaceRepository = mock<SpaceRepository>()
    private val userPreferences = mock<UserPreferences>()
    private val authService = mock<AuthService>()

    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())

    private lateinit var viewModel: HomeScreenViewModel

    private fun setUp() {
        whenever(userPreferences.currentUser).thenReturn(user1)
        viewModel = HomeScreenViewModel(
            navigator = navigator,
            locationManager = locationManager,
            spaceRepository = spaceRepository,
            userPreferences = userPreferences,
            authService = authService,
            appDispatcher = testDispatcher
        )
    }

    @Test
    fun `getAllSpace should set isLoadingSpace to true`() = runTest {
        whenever(spaceRepository.getAllSpaceInfo()).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
                flowOf(emptyList())
            }
        }

        setUp()
        assert(viewModel.state.value.isLoadingSpaces)
    }

    @Test
    fun `getAllSpace should update the state`() = runTest {
        val result = listOf(space_info1)
        whenever(spaceRepository.currentSpaceId).thenReturn("space1")
        whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(result))
        setUp()

        with(viewModel.state.value) {
            assert(spaces == result)
            assert(selectedSpace == space_info1)
            assert(selectedSpaceId == "space1")
            assert(!isLoadingSpaces)
        }
    }

    @Test
    fun `getAllSpace should set currentSpaceId to first space id if currentSpaceId is empty`() =
        runTest {
            val result = listOf(space_info1)
            whenever(spaceRepository.currentSpaceId).thenReturn("")
            whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(result))
            setUp()

            verify(spaceRepository).currentSpaceId = "space1"
        }

    @Test
    fun `getAllSpace should arrange the spaces list to have current space at first position`() =
        runTest {
            val result = listOf(space_info1, space_info2)
            whenever(spaceRepository.currentSpaceId).thenReturn("space2")
            whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(result))
            setUp()

            with(viewModel.state.value) {
                assert(spaces == listOf(space_info2, space_info1))
                assert(selectedSpace == space_info2)
                assert(selectedSpaceId == "space2")
                assert(!isLoadingSpaces)
            }
        }

    @Test
    fun `getAllSpace should set error if getAllSpaceInfo throws exception`() = runTest {
        val exception = RuntimeException("error")
        whenever(spaceRepository.getAllSpaceInfo()).thenThrow(exception)

        setUp()
        assert(viewModel.state.value.error == exception)
    }

    @Test
    fun `onTabChange should set currentTab to selected tab`() = runTest {
        setUp()
        viewModel.onTabChange(1)
        assert(viewModel.state.value.currentTab == 1)
    }

    @Test
    fun `toggleSpaceSelection should toggle showSpaceSelectionPopup`() = runTest {
        setUp()
        viewModel.toggleSpaceSelection()
        assert(viewModel.state.value.showSpaceSelectionPopup)
    }

    @Test
    fun `startTracking should call locationManager startService`() = runTest {
        setUp()
        viewModel.startTracking()
        verify(locationManager).startService()
    }

    @Test
    fun `navigateToCreateSpace should navigate to create-space`() = runTest {
        setUp()
        viewModel.navigateToCreateSpace()
        verify(navigator).navigateTo("create-space")
    }

    @Test
    fun `selectSpace should set currentSpaceId to selected space id`() = runTest {
        setUp()
        viewModel.selectSpace("space1")
        verify(spaceRepository).currentSpaceId = "space1"
    }

    @Test
    fun `selectSpace should update the state`() = runTest {
        val result = listOf(space_info1, space_info2)
        whenever(spaceRepository.currentSpaceId).thenReturn("space2")
        whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(result))
        setUp()

        viewModel.selectSpace("space1")

        with(viewModel.state.value) {
            assert(spaces == listOf(space_info2, space_info1))
            assert(selectedSpace == space_info1)
            assert(selectedSpaceId == "space1")
            assert(!isLoadingSpaces)
        }
    }

    @Test
    fun `addMember should set isLoadingSpaces to true`() = runTest {
        val result = listOf(space_info1, space_info2)
        whenever(spaceRepository.currentSpaceId).thenReturn("space1")
        whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(result))
        whenever(spaceRepository.getInviteCode("space1")).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
                "code"
            }
        }

        setUp()
        viewModel.addMember()
        assert(viewModel.state.value.isLoadingSpaces)
    }

    @Test
    fun `addMember should navigate to spaceInvitation with invite code and space name`() = runTest {
        val result = listOf(space_info1, space_info2)
        whenever(spaceRepository.currentSpaceId).thenReturn("space1")
        whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(result))
        whenever(spaceRepository.getInviteCode("space1")).thenReturn("code")

        setUp()
        viewModel.addMember()
        verify(navigator).navigateTo("space-invite/code/space_name", "create-space", true)
    }

    @Test
    fun `addMember should set error if getInviteCode throws exception`() = runTest {
        val exception = RuntimeException("error")
        val result = listOf(space_info1, space_info2)
        whenever(spaceRepository.currentSpaceId).thenReturn("space1")
        whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(result))
        whenever(spaceRepository.getInviteCode("space1")).thenThrow(exception)

        setUp()
        viewModel.addMember()
        assert(viewModel.state.value.error == exception)
    }

    @Test
    fun `joinSpace should navigate to join-space`() = runTest {
        setUp()
        viewModel.joinSpace()
        verify(navigator).navigateTo("join-space")
    }

    @Test
    fun `toggleLocation should update enablingLocation to true`() = runTest {
        whenever(spaceRepository.currentSpaceId).thenReturn("space1")
        whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(listOf(space_info1)))
        whenever(userPreferences.currentUser).thenReturn(user1)
        whenever(spaceRepository.enableLocation("space1", "user1", false)).doSuspendableAnswer {
            withContext(Dispatchers.IO) {
                delay(1000)
            }
        }
        setUp()
        viewModel.toggleLocation()
        assert(viewModel.state.value.enablingLocation)
    }

    @Test
    fun `toggleLocation should invoke enableLocation with locationEnable false`() =
        runTest {
            whenever(spaceRepository.currentSpaceId).thenReturn("space1")
            whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(listOf(space_info1)))
            whenever(userPreferences.currentUser).thenReturn(user1)
            setUp()
            viewModel.toggleLocation()
            verify(spaceRepository).enableLocation("space1", "user1", false)
        }

    @Test
    fun `toggleLocation should invoke enableLocation with locationEnable true`() =
        runTest {
            whenever(spaceRepository.currentSpaceId).thenReturn("space1")
            whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(listOf(space_info1)))
            whenever(userPreferences.currentUser).thenReturn(user1)
            setUp()
            viewModel.toggleLocation()
            verify(spaceRepository).enableLocation("space1", "user1", false)
            viewModel.toggleLocation()
            verify(spaceRepository).enableLocation("space1", "user1", true)
        }

    @Test
    fun `toggleLocation should set error state if enableLocation throws exception`() =
        runTest {
            val exception = RuntimeException("error")
            whenever(spaceRepository.currentSpaceId).thenReturn("space1")
            whenever(spaceRepository.getAllSpaceInfo()).thenReturn(flowOf(listOf(space_info1)))
            whenever(userPreferences.currentUser).thenReturn(user1)
            whenever(spaceRepository.enableLocation("space1", "user1", false)).thenThrow(
                exception
            )
            setUp()
            viewModel.toggleLocation()
            assert(viewModel.state.value.error == exception)
        }
}
