package com.canopas.yourspace.ui.flow.auth.phone

import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.campose.countrypicker.CountryPickerBottomSheet
import com.canopas.campose.countrypicker.countryList
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppLogo
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.theme.AppTheme

const val EXTRA_RESULT_IS_NEW_USER = "is-new-user"

@Composable
fun SignInWithPhoneScreen() {
    val viewModel = hiltViewModel<SignInWithPhoneViewModel>()
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            PhoneSignInAppBar { viewModel.popBack() }
        }
    ) {
        PhoneLoginContent(modifier = Modifier.padding(it))
    }

    if (state.error != null) {
        AppBanner(state.error!!, customMsg = state.error?.message) { viewModel.resetErrorState() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneSignInAppBar(onBackPressed: () -> Unit = {}) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppTheme.colorScheme.surface
        ),
        title = { },
        navigationIcon = {
            IconButton(
                onClick = onBackPressed,
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
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PhoneLoginContent(
    modifier: Modifier
) {
    val context = LocalContext.current
    val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    val viewModel = hiltViewModel<SignInWithPhoneViewModel>()
    val state by viewModel.state.collectAsState()

    val countryLanCode = manager.networkCountryIso.uppercase()
    LaunchedEffect(key1 = Unit) {
        val countryCode =
            if (countryLanCode.isNotEmpty()) {
                countryList(context).single { it.code == countryLanCode }.dial_code
            } else {
                countryList(context).first().dial_code
            }
        viewModel.onCodeChange(countryCode)
    }

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
        PhoneTextField()
        Spacer(modifier = Modifier.height(40.dp))
        NextBtn(enable = state.enableNext, isVerifying = state.verifying) {
            viewModel.verifyPhoneNumber(context)
            keyboardController?.hide()
        }
    }

    if (state.showCountryPicker) {
        val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

        CountryPickerBottomSheet(
            bottomSheetTitle = {
            },
            onItemSelected = {
                viewModel.onCodeChange(it.dial_code)
                viewModel.showCountryPicker(false)
            },
            sheetState = modalBottomSheetState,
            containerColor = AppTheme.colorScheme.surface,
            contentColor = AppTheme.colorScheme.textPrimary,
            onDismissRequest = {
                viewModel.showCountryPicker(false)
            }
        )
    }
}

@Composable
private fun PhoneTextField() {
    val context = LocalContext.current

    val viewModel = hiltViewModel<SignInWithPhoneViewModel>()
    val state by viewModel.state.collectAsState()

    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current

    val phoneFocusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (configuration.screenWidthDp.dp > 320.dp) 40.dp else 16.dp)
            .border(
                border = BorderStroke(
                    1.dp,
                    AppTheme.colorScheme.containerHigh
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    viewModel.showCountryPicker()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.code,
                color = AppTheme.colorScheme.textSecondary,
                textAlign = TextAlign.Center,
                style = AppTheme.appTypography.body2,
                modifier = Modifier
                    .padding(start = 20.dp)
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = null,
                Modifier
                    .padding(start = 12.dp),
                tint = AppTheme.colorScheme.textSecondary
            )
        }

        Divider(
            Modifier
                .padding(start = 20.dp)
                .width(1.dp)
                .height(25.dp),
            thickness = 1.dp,
            color = AppTheme.colorScheme.outline
        )
        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(
                AppTheme.colorScheme.primary,
                AppTheme.colorScheme.primary
            )
        ) {
            TextField(
                value = state.phone,
                modifier = Modifier
                    .focusRequester(phoneFocusRequester),
                onValueChange = { phoneNo ->
                    viewModel.onPhoneChange(phoneNo)
                },
                textStyle = AppTheme.appTypography.body2,
                placeholder = {
                    Text(
                        stringResource(R.string.phone_sign_in_hint_phone_number),
                        style = AppTheme.appTypography.body2,
                        color = AppTheme.colorScheme.textDisabled
                    )
                },
                colors = textFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.verifyPhoneNumber(context)
                }),
                singleLine = true
            )
        }
    }
}

@Composable
private fun TitleContent() {
    Text(
        text = stringResource(R.string.phone_sign_in_title_enter_phone_number),
        color = AppTheme.colorScheme.textPrimary,
        textAlign = TextAlign.Center,
        style = AppTheme.appTypography.header1,
        modifier = Modifier
            .fillMaxWidth()
    )

    Text(
        text = stringResource(R.string.phone_sign_in_subtitle_verification_message),
        color = AppTheme.colorScheme.textDisabled,
        textAlign = TextAlign.Center,
        style = AppTheme.appTypography.label1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 40.dp, end = 40.dp)
    )
}

@Composable
private fun NextBtn(enable: Boolean, isVerifying: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(fraction = 0.9f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colorScheme.primary
        ),
        enabled = enable
    ) {
        if (isVerifying) {
            AppProgressIndicator(color = AppTheme.colorScheme.onPrimary)
        }

        Text(
            text = stringResource(R.string.common_btn_next),
            style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.onPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
        )
    }
}

@Composable
fun textFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        unfocusedLabelColor = AppTheme.colorScheme.textSecondary,
        unfocusedBorderColor = Color.Transparent,
        focusedLabelColor = AppTheme.colorScheme.primary,
        focusedBorderColor = Color.Transparent,
        cursorColor = AppTheme.colorScheme.primary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedTextColor = AppTheme.colorScheme.textPrimary,
        unfocusedTextColor = AppTheme.colorScheme.textPrimary
    )
}

@Preview
@Composable
fun PreviewPhoneSignInView() {
    SignInWithPhoneScreen()
}
