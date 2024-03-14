package com.canopas.yourspace.ui.flow.home.space.create

import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CreateSpaceHomeViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val spaceRepository = mock<SpaceRepository>()
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())
    private val appNavigator = mock<AppNavigator>()

    private lateinit var viewModel: CreateSpaceHomeViewModel

    @Before
    fun setUp() {
        viewModel = CreateSpaceHomeViewModel(
            appNavigator = appNavigator,
            spaceRepository = spaceRepository,
            appDispatcher = testDispatcher
        )
    }

    @Test
    fun `navigateBack should call appNavigator to navigateBack`() {
        viewModel.navigateBack()
        verify(appNavigator).navigateBack()
    }

    @Test
    fun `onSpaceNameChange should update state with spaceName`() {
        viewModel.onSpaceNameChange("spaceName")
        assert(viewModel.state.value.spaceName == "spaceName")
    }

    @Test
    fun `createSpace should call spaceRepository to createSpaceAndGetInviteCode`() = runTest {
        viewModel.onSpaceNameChange("spaceName")
        viewModel.createSpace()
        verify(spaceRepository).createSpaceAndGetInviteCode("spaceName")
    }

    @Test
    fun `createSpace should call appNavigator to navigateTo SpaceInvitation`() = runTest {
        whenever(spaceRepository.createSpaceAndGetInviteCode("spaceName"))
            .thenReturn("inviteCode")
        viewModel.onSpaceNameChange("spaceName")
        viewModel.createSpace()
        verify(appNavigator).navigateTo(
            "space-invite/inviteCode/spaceName",
            "create-space",
            true
        )
    }

    @Test
    fun `createSpace should update state with error when spaceRepository throws exception`() = runTest {
        val error = RuntimeException("error")
        whenever(spaceRepository.createSpaceAndGetInviteCode("spaceName"))
            .thenThrow(error)
        viewModel.onSpaceNameChange("spaceName")
        viewModel.createSpace()
        assert(viewModel.state.value.error == error)
    }
}
