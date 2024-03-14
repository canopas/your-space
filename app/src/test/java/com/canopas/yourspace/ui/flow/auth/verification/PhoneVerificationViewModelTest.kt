package com.canopas.yourspace.ui.flow.auth.verification

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.auth.FirebaseAuthService
import com.canopas.yourspace.data.service.auth.PhoneAuthState
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations.OtpVerificationNavigation.KEY_PHONE_NO
import com.canopas.yourspace.ui.navigation.AppDestinations.OtpVerificationNavigation.KEY_VERIFICATION_ID
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class PhoneVerificationViewModelTest {
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: PhoneVerificationViewModel

    private val navigator = mock<AppNavigator>()
    private val firebaseAuth = mock<FirebaseAuthService>()
    private val authService = mock<AuthService>()
    private val savedStateHandle = mock<SavedStateHandle>()

    @Before
    fun setup() {
        whenever(savedStateHandle.get<String>(KEY_VERIFICATION_ID)).thenReturn("verificationId")
        whenever(savedStateHandle.get<String>(KEY_PHONE_NO)).thenReturn("1234567890")

        viewModel = PhoneVerificationViewModel(
            savedStateHandle,
            navigator,
            firebaseAuth,
            authService,
            testDispatcher
        )
    }

    @Test
    fun `init should set phone and verificationId`() {
        assert(viewModel.state.value.phone == "1234567890")
        assert(viewModel.state.value.verificationId == "verificationId")
    }

    @Test
    fun `updateOTP should update otp`() {
        viewModel.updateOTP("1234")
        assert(viewModel.state.value.otp == "1234")
    }

    @Test
    fun `updateOTP should set enableVerify to false if otp length is less than 6`() {
        viewModel.updateOTP("1234")
        assert(!viewModel.state.value.enableVerify)
    }

    @Test
    fun `updateOTP should set enableVerify to true if otp length is 6`() {
        viewModel.updateOTP("123456")
        assert(viewModel.state.value.enableVerify)
    }

    @Test
    fun `updateOTP should call verifyOTP set verifying true if firstAutoVerificationComplete is false`() =
        runTest {
            whenever(
                firebaseAuth.signInWithPhoneAuthCredential(
                    "verificationId",
                    "123456"
                )
            ).doSuspendableAnswer {
                withContext(Dispatchers.IO) { delay(5000) }
                return@doSuspendableAnswer "firebaseIdToken"
            }
            viewModel.updateOTP("123456")

            assert(viewModel.state.value.verifying)
        }

    @Test
    fun `updateOTP should not call verifyOTP if firstAutoVerificationComplete is true`() =
        runTest {
            whenever(
                firebaseAuth.signInWithPhoneAuthCredential(
                    "verificationId",
                    "123456"
                )
            ).doSuspendableAnswer {
                withContext(Dispatchers.IO) { delay(5000) }
                return@doSuspendableAnswer "firebaseIdToken"
            }

            viewModel.updateOTP("123456") // make firstAutoVerificationComplete true
            clearInvocations(firebaseAuth)

            viewModel.updateOTP("123456")
            verifyNoInteractions(firebaseAuth)
        }

    @Test
    fun `verifyOtp should should call verifiedPhoneLogin`() = runTest {
        whenever(
            firebaseAuth.signInWithPhoneAuthCredential(
                "verificationId",
                "12356"
            )
        ).thenReturn("firebaseIdToken")
        whenever(firebaseAuth.currentUserUid).thenReturn("uid")

        viewModel.updateOTP("12356")
        viewModel.verifyOTP()

        verify(authService).verifiedPhoneLogin("uid", "firebaseIdToken", "1234567890")
    }

    @Test
    fun `verifyOtp should navigate back to sign in`() = runTest {
        whenever(
            firebaseAuth.signInWithPhoneAuthCredential(
                "verificationId",
                "12356"
            )
        ).thenReturn("firebaseIdToken")
        whenever(firebaseAuth.currentUserUid).thenReturn("uid")
        whenever(authService.verifiedPhoneLogin("uid", "firebaseIdToken", "1234567890")).thenReturn(
            false
        )

        viewModel.updateOTP("12356")
        viewModel.verifyOTP()

        verify(navigator).navigateBack(
            "sign-in",
            result = mapOf("result_code" to 1, "is-new-user" to false)
        )
    }

    @Test
    fun `verifyOtp should set verifying to false`() = runTest {
        whenever(
            firebaseAuth.signInWithPhoneAuthCredential(
                "verificationId",
                "12356"
            )
        ).thenReturn("firebaseIdToken")

        viewModel.updateOTP("12356")
        viewModel.verifyOTP()

        assert(!viewModel.state.value.verifying)
    }

    @Test
    fun `verifyOtp should set verifying to false and error if exception is thrown`() = runTest {
        val exception = mock<FirebaseException>()
        whenever(
            firebaseAuth.signInWithPhoneAuthCredential(
                "verificationId",
                "12356"
            )
        ).doSuspendableAnswer { throw exception }

        viewModel.updateOTP("12356")
        viewModel.verifyOTP()

        assert(!viewModel.state.value.verifying)
        assert(viewModel.state.value.error == exception)
    }

    @Test
    fun `resendCode should navigate back on signIn on verification completed`() = runTest {
        val context = mock<Context>()
        val credential = mock<PhoneAuthCredential>()
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.VerificationCompleted(credential)))
        whenever(firebaseAuth.currentUserUid).thenReturn("uid")
        whenever(authService.verifiedPhoneLogin("uid", "firebaseIdToken", "1234567890")).thenReturn(false)

        whenever(firebaseAuth.signInWithPhoneAuthCredential(credential)).thenReturn("firebaseIdToken")
        viewModel.resendCode(context)
        verify(navigator).navigateBack(
            "sign-in",
            result = mapOf("result_code" to 1, "is-new-user" to false)
        )
    }

    @Test
    fun `resendCode should call verifiedPhoneLogin on verification completed`() = runTest {
        val context = mock<Context>()
        val credential = mock<PhoneAuthCredential>()
        whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
            .thenReturn(flowOf(PhoneAuthState.VerificationCompleted(credential)))
        whenever(firebaseAuth.currentUserUid).thenReturn("uid")
        whenever(firebaseAuth.signInWithPhoneAuthCredential(credential)).thenReturn("firebaseIdToken")
        whenever(authService.verifiedPhoneLogin("uid", "firebaseIdToken", "1234567890")).thenReturn(true)

        viewModel.resendCode(context)
        verify(authService).verifiedPhoneLogin("uid", "firebaseIdToken", "1234567890")
    }

    @Test
    fun `resetCode should update verifying to false and set error on verification failed`() =
        runTest {
            val context = mock<Context>()
            val exception = mock<FirebaseException>()
            whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
                .thenReturn(flowOf(PhoneAuthState.VerificationFailed(exception)))
            viewModel.resendCode(context)
            assert(!viewModel.state.value.verifying)
            assert(viewModel.state.value.error == exception)
        }

    @Test
    fun `resendCode should update verifying to false and set verificationId on CodeSent`() =
        runTest {
            val context = mock<Context>()
            whenever(firebaseAuth.verifyPhoneNumber(context, "1234567890"))
                .thenReturn(flowOf(PhoneAuthState.CodeSent("verificationId")))
            viewModel.resendCode(context)
            assert(!viewModel.state.value.verifying)
            assert(viewModel.state.value.verificationId == "verificationId")
        }

    @Test
    fun `resetErrorState should set error state to null`() {
        viewModel.resetErrorState()
        assert(viewModel.state.value.error == null)
    }
}
