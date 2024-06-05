package com.canopas.yourspace.ui.flow.home.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.domain.utils.getAddress
import com.canopas.yourspace.domain.utils.timeAgo
import com.canopas.yourspace.ui.component.ActionIconButton
import com.canopas.yourspace.ui.component.UserBatteryStatus
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SelectedUserDetail(userInfo: UserInfo?, onDismiss: () -> Unit, onTapTimeline: () -> Unit) {
    if (userInfo?.user == null) return
    val user = userInfo.user
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color = AppTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            MemberProfileView(user.profile_image, user.firstChar, userInfo.session)

            MemberInfoView(userName = user.fullName, userInfo.location) { onTapTimeline() }
        }

        Row(
            modifier = Modifier
                .wrapContentSize()
                .clip(CircleShape)
                .background(color = AppTheme.colorScheme.surface)
                .clickable { onDismiss() }
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_drop_down_icon),
                contentDescription = "",
                tint = AppTheme.colorScheme.textDisabled
            )
        }
    }
}

@Composable
private fun MemberProfileView(profileUrl: String?, name: String, session: ApiUserSession?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color = AppTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (profileUrl.isNullOrEmpty()) {
                Text(
                    name,
                    style = AppTheme.appTypography.header4.copy(color = AppTheme.colorScheme.onPrimary)
                )
            } else {
                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current).data(profileUrl).build()
                )
                Image(
                    painter = painter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "ProfileImage"
                )
                if (painter.state !is AsyncImagePainter.State.Success) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_user_profile_placeholder),
                        contentDescription = null,
                        tint = AppTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        if (session != null) {
            Spacer(modifier = Modifier.padding(top = 2.dp))
            UserBatteryStatus(session = session)
        }
    }
}

@Composable
private fun MemberInfoView(userName: String, location: ApiLocation?, onTapTimeline: () -> Unit) {
    val context = LocalContext.current
    var address by remember { mutableStateOf("") }
    val time = timeAgo(location?.created_at)

    LaunchedEffect(location) {
        if (location == null) {
            address = ""; return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val latLng = LatLng(location.latitude, location.longitude)
            address = latLng.getAddress(context) ?: ""
        }
    }
    Column(modifier = Modifier.padding(start = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = userName,
                style = AppTheme.appTypography.subTitle1.copy(color = AppTheme.colorScheme.textPrimary),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
            ActionIconButton(
                modifier = Modifier,
                iconSize = 24.dp,
                icon = R.drawable.ic_timeline,
                onClick = onTapTimeline
            )
        }
        Text(
            text = address,
            style = AppTheme.appTypography.body2.copy(color = AppTheme.colorScheme.textPrimary),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            modifier = Modifier.padding(top = 12.dp)
        )

        if (time.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_access_time),
                    contentDescription = "",
                    tint = AppTheme.colorScheme.textDisabled,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = time,
                    style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textDisabled)
                )
            }
        }
    }
}
