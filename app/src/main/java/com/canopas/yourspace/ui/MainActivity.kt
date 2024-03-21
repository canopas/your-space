package com.canopas.yourspace.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppAlertDialog
import com.canopas.yourspace.ui.flow.auth.methods.SignInMethodViewModel
import com.canopas.yourspace.ui.flow.auth.methods.SignInMethodsScreen
import com.canopas.yourspace.ui.flow.auth.phone.EXTRA_RESULT_IS_NEW_USER
import com.canopas.yourspace.ui.flow.auth.phone.SignInWithPhoneScreen
import com.canopas.yourspace.ui.flow.auth.verification.PhoneVerificationScreen
import com.canopas.yourspace.ui.flow.home.home.HomeScreen
import com.canopas.yourspace.ui.flow.home.space.create.CreateSpaceHomeScreen
import com.canopas.yourspace.ui.flow.home.space.create.SpaceInvite
import com.canopas.yourspace.ui.flow.home.space.join.JoinSpaceScreen
import com.canopas.yourspace.ui.flow.intro.IntroScreen
import com.canopas.yourspace.ui.flow.messages.chat.MessagesScreen
import com.canopas.yourspace.ui.flow.messages.thread.ThreadsScreen
import com.canopas.yourspace.ui.flow.onboard.OnboardScreen
import com.canopas.yourspace.ui.flow.permission.EnablePermissionsScreen
import com.canopas.yourspace.ui.flow.settings.SettingsScreen
import com.canopas.yourspace.ui.flow.settings.profile.EditProfileScreen
import com.canopas.yourspace.ui.flow.settings.space.SpaceProfileScreen
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.canopas.yourspace.ui.navigation.KEY_RESULT
import com.canopas.yourspace.ui.navigation.RESULT_OKAY
import com.canopas.yourspace.ui.navigation.slideComposable
import com.canopas.yourspace.ui.theme.CatchMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CatchMeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val viewModel = hiltViewModel<MainViewModel>()
    val sessionExpired by viewModel.sessionExpiredState.collectAsState()

    val initialRoute by viewModel.initialRoute.collectAsState()
    if (sessionExpired) {
        SessionExpiredAlertPopup()
    }

    AppNavigator(navController = navController, viewModel.navActions)

    NavHost(navController = navController, startDestination = initialRoute) {
        slideComposable(AppDestinations.intro.path) {
            IntroScreen()
        }
        slideComposable(AppDestinations.onboard.path) {
            OnboardScreen()
        }
        slideComposable(AppDestinations.signIn.path) {
            val signInModel = hiltViewModel<SignInMethodViewModel>()

            val result = navController.currentBackStackEntry
                ?.savedStateHandle?.get<Int>(KEY_RESULT)
            result?.let {
                val isNewUSer = navController.currentBackStackEntry
                    ?.savedStateHandle?.get<Boolean>(EXTRA_RESULT_IS_NEW_USER) ?: true
                navController.currentBackStackEntry
                    ?.savedStateHandle?.apply {
                        remove<Int>(KEY_RESULT)
                        remove<Boolean>(EXTRA_RESULT_IS_NEW_USER)
                    }

                LaunchedEffect(key1 = result) {
                    if (result == RESULT_OKAY) {
                        signInModel.onSignUp(isNewUSer)
                    }
                }
            }
            SignInMethodsScreen()
        }

        slideComposable(AppDestinations.phoneSignIn.path) {
            SignInWithPhoneScreen()
        }

        slideComposable(AppDestinations.OtpVerificationNavigation.path) {
            PhoneVerificationScreen()
        }

        slideComposable(AppDestinations.home.path) {
            HomeScreen()
        }

        slideComposable(AppDestinations.createSpace.path) {
            CreateSpaceHomeScreen()
        }

        slideComposable(AppDestinations.joinSpace.path) {
            JoinSpaceScreen()
        }

        slideComposable(AppDestinations.SpaceInvitation.path) {
            SpaceInvite()
        }

        slideComposable(AppDestinations.enablePermissions.path) {
            EnablePermissionsScreen()
        }

        slideComposable(AppDestinations.settings.path) {
            SettingsScreen()
        }

        slideComposable(AppDestinations.editProfile.path) {
            EditProfileScreen()
        }

        slideComposable(AppDestinations.SpaceProfileScreen.path) {
            SpaceProfileScreen()
        }

        slideComposable(AppDestinations.spaceThreads.path) {
            ThreadsScreen()
        }

        slideComposable(AppDestinations.ThreadMessages.path) {
            MessagesScreen()
        }
    }
}

@Composable
fun SessionExpiredAlertPopup() {
    val viewModel = hiltViewModel<MainViewModel>()

    AppAlertDialog(
        title = stringResource(id = R.string.common_session_expired_title),
        subTitle = stringResource(id = R.string.common_session_expired_message),
        confirmBtnText = stringResource(id = R.string.common_btn_ok),
        onConfirmClick = {
            viewModel.signOut()
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}
