package com.canopas.yourspace.ui.flow.auth.phone

import android.content.Context
import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.auth.FirebaseAuthService
import com.canopas.yourspace.data.service.auth.PhoneAuthState
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SignInWithPhoneViewModelTest {
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SignInWithPhoneViewModel

    private val navigator = mock<AppNavigator>()
    private val firebaseAuth = mock<FirebaseAuthService>()
    private val authService = mock<AuthService>()

    @Before
    fun setup() {
        viewModel = SignInWithPhoneViewModel(
            navigator,
            firebaseAuth,
            authService,
            testDispatcher
        )
    }

    @Test
    fun `onPhoneChange should update phone`() {
        viewModel.onPhoneChange("1234567890")
        assert(viewModel.state.value.phone == "1234567890")
    }

    @Test
    fun `onCodeChange should update code`() {
        viewModel.onCodeChange("1234567890")
        assert(viewModel.state.value.code == "1234567890")
    }

    @Test
    fun `verifyPhoneNumber should update verifying to false on verification completed`() = runTest {
        val context = mock<Context>()
        val credential = mock<PhoneAuthCredential>()
        viewModel.onPhoneChange("1234567890")
        whenever(firebaseAuth.currentUserUid).thenReturn("uid")
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.VerificationCompleted(credential)))
        whenever(firebaseAuth.signInWithPhoneAuthCredential(credential)).thenReturn("firebaseIdToken")
        whenever(authService.verifiedPhoneLogin("uid", "firebaseIdToken", "1234567890")).thenReturn(
            true
        )
        viewModel.verifyPhoneNumber(context)
        assert(!viewModel.state.value.verifying)
    }

    @Test
    fun `verifyPhoneNumber should navigate back on signIn on verification completed`() = runTest {
        val context = mock<Context>()
        val credential = mock<PhoneAuthCredential>()
        viewModel.onPhoneChange("1234567890")
        whenever(firebaseAuth.currentUserUid).thenReturn("uid")
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.VerificationCompleted(credential)))
        whenever(firebaseAuth.signInWithPhoneAuthCredential(credential)).thenReturn("firebaseIdToken")
        whenever(authService.verifiedPhoneLogin("uid", "firebaseIdToken", "1234567890")).thenReturn(
            true
        )

        viewModel.verifyPhoneNumber(context)
        verify(navigator).navigateBack(
            "sign-in",
            result = mapOf("result_code" to 1, "is-new-user" to true)
        )
    }

    @Test
    fun `verifyPhoneNumber should call verifiedPhoneLogin on verification completed`() = runTest {
        val context = mock<Context>()
        val credential = mock<PhoneAuthCredential>()
        viewModel.onPhoneChange("1234567890")
        whenever(firebaseAuth.currentUserUid).thenReturn("uid")
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.VerificationCompleted(credential)))
        whenever(firebaseAuth.signInWithPhoneAuthCredential(credential)).thenReturn("firebaseIdToken")
        whenever(authService.verifiedPhoneLogin("uid", "firebaseIdToken", "1234567890")).thenReturn(
            true
        )
        viewModel.verifyPhoneNumber(context)
        verify(authService).verifiedPhoneLogin("uid", "firebaseIdToken", "1234567890")
    }

    @Test
    fun `verifyPhoneNumber should update verifying to false on verification failed`() = runTest {
        val context = mock<Context>()
        val exception = mock<FirebaseException>()
        viewModel.onPhoneChange("1234567890")
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.VerificationFailed(exception)))
        viewModel.verifyPhoneNumber(context)
        assert(!viewModel.state.value.verifying)
    }

    @Test
    fun `verifyPhoneNumber should update error state on verification failed`() = runTest {
        val context = mock<Context>()
        val exception = mock<FirebaseException>()
        viewModel.onPhoneChange("1234567890")
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.VerificationFailed(exception)))
        viewModel.verifyPhoneNumber(context)
        assert(viewModel.state.value.error == exception)
    }

    @Test
    fun `verifyPhoneNumber should update verifying to false on code sent`() = runTest {
        val context = mock<Context>()
        viewModel.onPhoneChange("1234567890")
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.CodeSent("verificationId")))
        viewModel.verifyPhoneNumber(context)
        assert(!viewModel.state.value.verifying)
    }

    @Test
    fun `verifyPhoneNumber should navigate to otp verification on code sent`() = runTest {
        val context = mock<Context>()
        viewModel.onPhoneChange("1234567890")
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.CodeSent("verificationId")))
        viewModel.verifyPhoneNumber(context)
        verify(navigator).navigateTo(route = "otp-verification/1234567890/verificationId")
    }

    @Test
    fun `verifyPhoneNumber should set verificationId on code sent`() = runTest {
        val context = mock<Context>()
        viewModel.onPhoneChange("1234567890")
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.CodeSent("verificationId")))
        viewModel.verifyPhoneNumber(context)
        assert(viewModel.state.value.verificationId == "verificationId")
    }

    @Test
    fun `popBack should navigate back`() = runBlocking {
        viewModel.popBack()
        verify(navigator).navigateBack()
    }

    @Test
    fun `showCountryPicker should update showCountryPicker state to true`() {
        viewModel.showCountryPicker()
        assert(viewModel.state.value.showCountryPicker)
    }

    @Test
    fun `showCountryPicker should update showCountryPicker state to false`() {
        viewModel.showCountryPicker(false)
        assert(!viewModel.state.value.showCountryPicker)
    }

    @Test
    fun `resetErrorState should set error state to null`() {
        viewModel.resetErrorState()
        assert(viewModel.state.value.error == null)
    }
}
