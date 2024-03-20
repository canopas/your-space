package com.canopas.yourspace.ui.flow.home.map.member

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.theme.AppTheme
import com.canopas.yourspace.utils.getAddress
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MemberDetailBottomSheetContent(
    userInfo: UserInfo
) {
    val viewModel = hiltViewModel<MemberDetailViewModel>()
    val state by viewModel.state.collectAsState()
    val locations = viewModel.locations.collectAsLazyPagingItems()

    LaunchedEffect(userInfo) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val timestamp = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        viewModel.fetchUserLocationHistory(
            from = timestamp,
            to = calendar.timeInMillis,
            userInfo
        )
    }

    Column(modifier = Modifier.fillMaxHeight(0.9f)) {
        UserInfoContent(userInfo)
        Spacer(modifier = Modifier.height(16.dp))
        Divider(thickness = 1.dp, color = AppTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(10.dp))

        FilterOption(
            selectedFromTimestamp = state.selectedTimeFrom ?: 0
        ) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = it
            }
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            val timestamp = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            viewModel.fetchUserLocationHistory(from = timestamp, to = calendar.timeInMillis)
        }
        LocationHistory(locations)
    }
}

@Composable
fun LocationHistory(
    locations: LazyPagingItems<LocationJourney>
) {
    Box {
        when {
            locations.loadState.refresh == LoadState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { AppProgressIndicator() }
            }

            locations.itemCount == 0 -> {
                EmptyHistory()
            }

            else -> {
                LazyColumn {
                    items(locations.itemCount) { index ->
                        val location = locations[index]
                        LocationHistoryItem(
                            location!!,
                            index,
                            isLastItem = index == locations.itemCount - 1
                        )
                    }

                    if (locations.loadState.append == LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) { AppProgressIndicator() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_location_history),
            contentDescription = "",
            modifier = Modifier
                .padding(bottom = 30.dp)
                .alpha(0.8f)
        )

        Text(
            text = stringResource(id = R.string.member_detail_empty_location_history),
            style = AppTheme.appTypography.body1.copy(
                color = AppTheme.colorScheme.containerHigh.copy(
                    alpha = 0.8f
                )
            ),
            modifier = Modifier.padding(bottom = 30.dp)
        )
    }
}

@Composable
private fun LocationHistoryItem(
    location: LocationJourney,
    index: Int,
    isLastItem: Boolean
) {
    val context = LocalContext.current
    var fromAddress by remember { mutableStateOf("") }
    var toAddress by remember {
        mutableStateOf("")
    }
    LaunchedEffect(location) {
        withContext(Dispatchers.IO) {
            val latLng =
                LatLng(location.fromLatitude, location.fromLongitude)
            val toLatLng = LatLng(location.toLatitude ?: 0.0, location.toLongitude ?: 0.0)
            fromAddress = latLng.getAddress(context) ?: ""
            toAddress = toLatLng.getAddress(context) ?: ""
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = if (index % 2 == 0) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (location.isSticky) {
                            AppTheme.colorScheme.primary.copy(
                                alpha = 0.5f
                            )
                        } else {
                            AppTheme.colorScheme.alertColor.copy(
                                alpha = 0.5f
                            )
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (location.isSticky) {
                    Icon(
                        if (fromAddress.isEmpty()) Icons.Outlined.Refresh else Icons.Default.LocationOn,
                        contentDescription = "",
                        tint = AppTheme.colorScheme.surface,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(2.dp)
                    )
                } else {
                    Icon(
                        painterResource(id = R.drawable.ic_location_journey),
                        contentDescription = "",
                        tint = AppTheme.colorScheme.surface,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(2.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = AppTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            if (fromAddress.isEmpty()) {
                Shimmer()
            } else {
                if (location.isSticky) {
                    Text(
                        text = fromAddress,
                        style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary),
                        modifier = Modifier
                            .padding(16.dp)
                    )
                } else {
                    Text(
                        text = "$fromAddress--->$toAddress",
                        style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary),
                        modifier = Modifier
                            .padding(16.dp)
                    )
                }
            }
            location.createdAt?.let {
                Text(
                    text = getFormattedCreatedAt(it),
                    style = AppTheme.appTypography.label2.copy(color = AppTheme.colorScheme.textSecondary),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_access_time),
                    contentDescription = "",
                    tint = AppTheme.colorScheme.textSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = location.currentLocationDuration ?: location.routeDuration ?: "",
                    style = AppTheme.appTypography.label2.copy(color = AppTheme.colorScheme.textSecondary)
                )
            }
        }
    }

    if (index % 2 == 0 && !isLastItem) {
        Icon(
            painter = painterResource(id = R.drawable.ic_right_drawn_icon),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(6f)
                .rotate(230f)
                .offset(y = (50).dp, x = -(50).dp)
        )
    } else if (index % 2 != 0 && !isLastItem) {
        Icon(
            painter = painterResource(id = R.drawable.ic_left_drawn_icon),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(6f)
                .rotate(150f)
                .offset(x = 50.dp, y = 20.dp)
        )
    }
}

@Composable
fun Shimmer() {
    val gradient = listOf(
        Color.LightGray.copy(alpha = 0.9f), // darker grey (90% opacity)
        Color.LightGray.copy(alpha = 0.5f) // lighter grey (30% opacity)
    )

    val transition = rememberInfiniteTransition(label = "") // animate infinite times

    val translateAnimation = transition.animateFloat( // animate the transition
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000, // duration for the animation
                easing = FastOutLinearInEasing
            )
        ),
        label = ""
    )
    val brush = Brush.linearGradient(
        colors = gradient,
        start = Offset(200f, 200f),
        end = Offset(
            x = translateAnimation.value,
            y = translateAnimation.value
        )
    )
    Spacer(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
    )
}

@Composable
fun FilterOption(
    selectedFromTimestamp: Long,
    onTimeSelected: (Long) -> Unit = {}
) {
    var showDatePicker by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 6.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.member_detail_location_history),
            style = AppTheme.appTypography.body2.copy(color = AppTheme.colorScheme.textSecondary)
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = {
            showDatePicker = true
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = getFormattedFilterLabel(selectedFromTimestamp),
                    style = AppTheme.appTypography.body2.copy(color = AppTheme.colorScheme.textSecondary),
                    modifier = Modifier.padding(end = 5.dp)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter_by),
                    contentDescription = "",
                    tint = AppTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        ShowDatePicker(
            confirmButtonClick = { timestamp ->
                showDatePicker = false

                onTimeSelected(timestamp)
            },
            dismissButtonClick = {
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePicker(
    confirmButtonClick: (Long) -> Unit,
    dismissButtonClick: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    DatePickerDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = {
                confirmButtonClick(
                    datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                )
            }) {
                Text(text = "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = dismissButtonClick) {
                Text(text = "Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            dateValidator = { date -> date <= System.currentTimeMillis() }
        )
    }
}

@Composable
fun UserInfoContent(userInfo: UserInfo) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        UserProfile(modifier = Modifier.size(54.dp), user = userInfo.user)

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = userInfo.user.fullName,
                style = AppTheme.appTypography.header3,
                maxLines = 1
            )
            if (!userInfo.isLocationEnable) {
                Text(
                    text = stringResource(id = R.string.map_user_item_location_off),
                    style = AppTheme.appTypography.label1.copy(
                        color = Color.Red,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }

//        Box(
//            modifier = Modifier
//                .size(38.dp)
//                .background(
//                    color = AppTheme.colorScheme.containerNormal,
//                    shape = CircleShape
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                painter = painterResource(id = R.drawable.ic_messages),
//                contentDescription = "",
//                tint = AppTheme.colorScheme.textSecondary,
//                modifier = Modifier
//                    .size(24.dp)
//                    .padding(2.dp)
//            )
//        }
    }
}

fun getFormattedFilterLabel(startTimestamp: Long): String {
    val startDate = Date(startTimestamp)
    val startDateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    return startDateFormat.format(startDate)
}

fun getFormattedCreatedAt(createdAt: Long): String {
    val createdAtTime = Date(createdAt)
    val createdAtFormat = SimpleDateFormat("d MMM HH:mm", Locale.getDefault())
    return createdAtFormat.format(createdAtTime)
}
