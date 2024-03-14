package com.canopas.yourspace.ui.flow.home.space.join

import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.space.ApiSpaceInvitation
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.space.SpaceInvitationService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class JoinSpaceViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val spaceRepository = mock<SpaceRepository>()
    private val appNavigator = mock<AppNavigator>()
    private val authService = mock<AuthService>()
    private val invitationService = mock<SpaceInvitationService>()

    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())

    private lateinit var viewModel: JoinSpaceViewModel

    @Before
    fun setUp() {
        viewModel = JoinSpaceViewModel(
            appNavigator = appNavigator,
            appDispatcher = testDispatcher,
            invitationService = invitationService,
            spaceRepository = spaceRepository,
            authService = authService
        )
    }

    @Test
    fun `popBackStack calls appNavigator to navigateBack`() {
        viewModel.popBackStack()
        verify(appNavigator).navigateBack()
    }

    @Test
    fun `onCodeChanged updates inviteCode in state`() {
        val code = "123456"
        viewModel.onCodeChanged(code)
        assert(viewModel.state.value.inviteCode == code)
    }

    @Test
    fun `verifyAndJoinSpace should set verifying state to true`() = runTest {
        val code = "123456"
        whenever(invitationService.getInvitation(code))
            .doSuspendableAnswer {
                withContext(Dispatchers.IO) {
                    delay(1000)
                    null
                }
            }
        viewModel.onCodeChanged(code)
        viewModel.verifyAndJoinSpace()
        assert(viewModel.state.value.verifying)
    }

    @Test
    fun `verifyAndJoinSpace should set errorInvalidInviteCode state to true when invitation is null`() =
        runTest {
            val code = "123456"
            whenever(invitationService.getInvitation(code)).thenReturn(null)
            viewModel.onCodeChanged(code)
            viewModel.verifyAndJoinSpace()
            assert(viewModel.state.value.errorInvalidInviteCode)
            assert(!viewModel.state.value.verifying)
        }

    @Test
    fun `verifyAndJoinSpace should invoke joinSpace when invitation is not null`() = runTest {
        val code = "123456"
        val invitation = mock<ApiSpaceInvitation>()
        whenever(invitation.space_id).thenReturn("space1")
        whenever(invitationService.getInvitation(code)).thenReturn(invitation)
        viewModel.onCodeChanged(code)
        viewModel.verifyAndJoinSpace()
        verify(spaceRepository).joinSpace("space1")
    }

    @Test
    fun `verifyAndJoinSpace should set joinedSpace state when joinSpace is successful`() = runTest {
        val code = "123456"
        val space = mock<ApiSpace>()
        val invitation = mock<ApiSpaceInvitation>()
        whenever(invitation.space_id).thenReturn("space1")
        whenever(invitationService.getInvitation(code)).thenReturn(invitation)
        whenever(spaceRepository.getSpace("space1")).thenReturn(space)
        viewModel.onCodeChanged(code)
        viewModel.verifyAndJoinSpace()
        assert(viewModel.state.value.joinedSpace != null)
        assert(viewModel.state.value.joinedSpace == space)
        assert(!viewModel.state.value.verifying)
    }

    @Test
    fun `verifyAndJoinSpace should set error state when exception is thrown`() = runTest {
        val code = "123456"
        val exception = RuntimeException("Error")
        whenever(invitationService.getInvitation(code)).thenThrow(exception)
        viewModel.onCodeChanged(code)
        viewModel.verifyAndJoinSpace()
        assert(viewModel.state.value.error == exception)
        assert(!viewModel.state.value.verifying)
    }

    @Test
    fun `resetErrorState should updare error state`() {
        viewModel.resetErrorState()
        assert(viewModel.state.value.error == null)
        assert(!viewModel.state.value.errorInvalidInviteCode)
    }
}
