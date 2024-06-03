package com.canopas.yourspace.ui.flow.onboard.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.component.PrimaryOutlinedButton
import com.canopas.yourspace.ui.flow.onboard.OnboardViewModel
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun SpaceInfoOnboard() {
    val viewModel = hiltViewModel<OnboardViewModel>()

    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        TitleContent()
        Spacer(modifier = Modifier.weight(0.5f))
        Image(
            painter = painterResource(id = R.drawable.ic_solo_traveler_image),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillHeight
        )
        Spacer(modifier = Modifier.weight(0.5f))
        SubTitleTitleContent()
        Spacer(modifier = Modifier.height(30.dp))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = stringResource(R.string.common_btn_continue),
            onClick = {
                viewModel.navigateToJoinOrCreateSpace()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryOutlinedButton(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = stringResource(R.string.common_btn_skip),
            onClick = { viewModel.navigateToHome() }
        )
    }
}

@Composable
private fun TitleContent() {
    Text(
        text = stringResource(R.string.onboard_space_info_title),
        style = AppTheme.appTypography.header3,
        color = AppTheme.colorScheme.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@Composable
private fun SubTitleTitleContent() {
    Text(
        text = stringResource(R.string.onboard_space_info_subtitle),
        style = AppTheme.appTypography.subTitle1,
        color = AppTheme.colorScheme.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}
