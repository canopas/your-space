package com.canopas.yourspace.ui.flow.auth.verification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppLogo
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.flow.auth.phone.textFieldColors
import com.canopas.yourspace.ui.theme.AppTheme
import com.canopas.yourspace.ui.theme.InterFontFamily
import kotlinx.coroutines.delay

private const val CODE_TIME_COUNTDOWN: Int = 30
private const val CODE_TIMER_NOTIFIER_INTERVAL = 1000L

@Composable
fun PhoneVerificationScreen() {
    Scaffold(
        topBar = {
            VerificationAppBar()
        }
    ) {
        PhoneVerificationViewContent(modifier = Modifier.padding(it))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerificationAppBar() {
    val viewModel = hiltViewModel<PhoneVerificationViewModel>()
    val state by viewModel.state.collectAsState()

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppTheme.colorScheme.surface
        ),
        title = { },
        navigationIcon = {
            IconButton(
                onClick = {
                    viewModel.popBack()
                },
                modifier = Modifier
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = null,
                    tint = AppTheme.colorScheme.textSecondary
                )
            }
        }
    )

    if (state.error != null) {
        AppBanner(state.error!!, customMsg = state.error?.message) {
            viewModel.resetErrorState()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PhoneVerificationViewContent(modifier: Modifier) {
    val scrollState = rememberScrollState()

    val keyboardController = LocalSoftwareKeyboardController.current
    DisposableEffect(key1 = Unit, effect = {
        onDispose {
            keyboardController?.hide()
        }
    })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.surface)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        AppLogo(colorTint = AppTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(80.dp))
        TitleContent()
        Spacer(modifier = Modifier.height(40.dp))
        VerificationCodeTextField()
        Spacer(modifier = Modifier.height(40.dp))
        NextBtn()
        Spacer(modifier = Modifier.height(10.dp))
        ResendCodeView()
    }
}

@Composable
private fun TitleContent() {
    Text(
        text = stringResource(R.string.phone_sign_in_otp_verification_title),
        color = AppTheme.colorScheme.textPrimary,
        textAlign = TextAlign.Center,
        style = AppTheme.appTypography.header1,
        modifier = Modifier
            .fillMaxWidth()
    )

    Text(
        text = stringResource(R.string.phone_sign_in_otp_subtitle_verification_message),
        color = AppTheme.colorScheme.textDisabled,
        textAlign = TextAlign.Center,
        style = AppTheme.appTypography.label1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 40.dp, end = 40.dp)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NextBtn() {
    val keyboardController = LocalSoftwareKeyboardController.current

    val viewModel = hiltViewModel<PhoneVerificationViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.verifying) {
        if (state.verifying) {
            keyboardController?.hide()
        }
    }

    Button(
        onClick = {
            viewModel.verifyOTP()
        },
        modifier = Modifier
            .fillMaxWidth(fraction = 0.9f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colorScheme.primary
        ),
        enabled = state.enableVerify
    ) {
        if (state.verifying) {
            AppProgressIndicator(color = AppTheme.colorScheme.onPrimary)
        }

        Text(
            text = stringResource(R.string.phone_sign_in_otp_btn_verify),
            style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.onPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
        )
    }
}

@Composable
private fun ResendCodeView() {
    val viewModel = hiltViewModel<PhoneVerificationViewModel>()
    val context = LocalContext.current
    var currentTime by remember { mutableIntStateOf(CODE_TIME_COUNTDOWN) }
    val runTimer by remember {
        mutableStateOf(true)
    }
    AnimatedVisibility(visible = runTimer) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = {
                    currentTime = CODE_TIME_COUNTDOWN
                    viewModel.resendCode(context)
                },
                enabled = currentTime <= 0
            ) {
                LaunchedEffect(key1 = currentTime, key2 = runTimer, block = {
                    if (currentTime > 0 && runTimer) {
                        delay(CODE_TIMER_NOTIFIER_INTERVAL)
                        currentTime--
                    }
                })

                Text(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.phone_sign_in_otp_label_resend_code))
                        if (currentTime > 0) {
                            withStyle(
                                style = SpanStyle(
                                    color = AppTheme.colorScheme.textPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append(" 00:${if (currentTime >= 10) currentTime else "0$currentTime"}")
                            }
                        }
                    },
                    style = AppTheme.appTypography.label2,
                    color = if (currentTime > 0) {
                        AppTheme.colorScheme.primary.copy(
                            0.6f
                        )
                    } else {
                        AppTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

@Composable
private fun VerificationCodeTextField() {
    val viewModel = hiltViewModel<PhoneVerificationViewModel>()
    val state by viewModel.state.collectAsState()

    Box(contentAlignment = Alignment.Center) {
        TextField(
            value = state.otp,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            colors = textFieldColors(),
            textStyle = TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 34.sp,
                letterSpacing = 8.5.sp,
                textAlign = TextAlign.Center
            ),
            onValueChange = { otp ->
                viewModel.updateOTP(otp)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )

        Divider(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 90.dp)
                .align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = AppTheme.colorScheme.outline
        )
    }
}

@Preview
@Composable
fun PreviewOtpVerificationView() {
    PhoneVerificationScreen()
}
