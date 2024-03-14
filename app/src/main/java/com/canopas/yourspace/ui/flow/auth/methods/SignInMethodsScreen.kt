package com.canopas.yourspace.ui.flow.auth.methods

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppLogo
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.theme.AppTheme
import com.canopas.yourspace.ui.theme.CatchMeTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import timber.log.Timber

@Composable
fun SignInMethodsScreen() {
    Scaffold {
        SignInContent(
            modifier = Modifier
                .padding(it)
        )
    }
}

@Composable
private fun SignInContent(modifier: Modifier) {
    val viewModel = hiltViewModel<SignInMethodViewModel>()
    val scrollState = rememberScrollState()
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                Brush.verticalGradient(
                    listOf(
                        AppTheme.colorScheme.primary.copy(0f),
                        AppTheme.colorScheme.primary.copy(0.12F),
                        AppTheme.colorScheme.primary.copy(0f)
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        AppLogo(colorTint = AppTheme.colorScheme.primary)
        Spacer(modifier = Modifier.weight(1f))
        GoogleSignInBtn()
        Spacer(modifier = Modifier.height(20.dp))
        PhoneLoginBtn(onClick = { viewModel.signInWithPhone() })
        Spacer(modifier = Modifier.weight(1f))
    }

    if (state.error != null) {
        AppBanner(
            msg = state.error!!,
            customMsg = stringResource(id = R.string.sign_in_with_google_failed_error_msg)
        ) { viewModel.resetErrorState() }
    }
}

@Composable
private fun PhoneLoginBtn(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(fraction = 0.8f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colorScheme.primary
        )
    ) {
        Text(
            text = stringResource(id = R.string.sign_in_btn_continue_with_phone),
            style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.onPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

@Composable
private fun GoogleSignInBtn() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<SignInMethodViewModel>()
    val state by viewModel.state.collectAsState()

    val signInClientLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)

                    viewModel.proceedGoogleSignIn(account)
                } catch (e: ApiException) {
                    Timber.e(e, "Unable to sign in with google")
                }
            }
        }

    Button(
        onClick = {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleSignInClient.signOut()
            signInClientLauncher.launch(googleSignInClient.signInIntent)
        },
        modifier = Modifier
            .fillMaxWidth(fraction = 0.8f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White
        )
    ) {
        if (state.showGoogleLoading) {
            AppProgressIndicator()
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_sign_in_google_logo),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(id = R.string.sign_in_btn_continue_with_google),
                style = AppTheme.appTypography.label1.copy(color = Color.Black),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
            )
        }
    }
}

@Preview
@Composable
fun PreviewSignInView() {
    CatchMeTheme {
        SignInMethodsScreen()
    }
}
