package com.canopas.yourspace.ui.flow.messages.thread

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.AppAlertDialog
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.component.motionClickEvent
import com.canopas.yourspace.ui.flow.messages.chat.toFormattedTitle
import com.canopas.yourspace.ui.flow.settings.SettingsViewModel
import com.canopas.yourspace.ui.theme.AppTheme
import com.canopas.yourspace.utils.formattedMessageTimeString
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadsScreen() {
    val viewModel = hiltViewModel<ThreadsViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    if (state.currentSpace != null) {
                        Column {
                            Text(
                                text = state.currentSpace?.space?.name ?: "",
                                style = AppTheme.appTypography.header3
                            )
                            Text(
                                text = stringResource(id = R.string.threads_screen_subtitle_messages),
                                style = AppTheme.appTypography.body3
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = ""
                        )
                    }
                }
            )
        },
        contentColor = AppTheme.colorScheme.textPrimary,
        containerColor = AppTheme.colorScheme.surface,
        floatingActionButton = {
            if (state.hasMembers) {
                FloatingActionButton(
                    onClick = { viewModel.createNewThread() },
                    containerColor = AppTheme.colorScheme.primary,
                    contentColor = AppTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, "Floating action button.")
                }
            }
        }
    ) {
        ThreadsContent(modifier = Modifier.padding(it))
    }

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }
}

@Composable
private fun ThreadsContent(modifier: Modifier) {
    val viewModel = hiltViewModel<ThreadsViewModel>()
    val state by viewModel.state.collectAsState()
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.loadingSpace || state.loadingThreads) {
            AppProgressIndicator()
        } else if (state.hasMembers) {
            ThreadList(
                state.threadInfo,
                state.currentSpace?.members ?: emptyList(),
                deletingThread = state.deletingThread,
                state.currentUser,
                onClick = { viewModel.showMessages(it) },
                deleteThread = { viewModel.deleteThread(it) })
        } else {
            NoMemberEmptyContent(state.loadingInviteCode) {
                viewModel.addMember()
            }
        }
    }
}

@Composable
fun ThreadList(
    threadInfos: List<ThreadInfo>,
    members: List<UserInfo>,
    deletingThread: ThreadInfo?,
    currentUser: ApiUser?,
    onClick: (thread: ThreadInfo) -> Unit,
    deleteThread: (thread: ThreadInfo) -> Unit
) {
    if (threadInfos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.threads_screen_no_threads),
                textAlign = TextAlign.Center,
                style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            itemsIndexed(
                threadInfos,
                key = { index, item -> item.thread.id }
            ) { index, threadInfo ->
                val threadMembers = members.filter { member ->
                    threadInfo.thread.member_ids.contains(member.user.id) && member.user.id != currentUser?.id
                }.map { it.user }

                SwipeToDelete(deleting = deletingThread == threadInfo,
                    content = {
                        ThreadItem(threadInfo, threadMembers, currentUser) {
                            onClick(
                                threadInfo
                            )
                        }
                    }, onDelete = {
                        deleteThread(threadInfo)
                    })
                if (index != threadInfos.size - 1) {
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = AppTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

enum class DragAnchors {
    Center,
    End,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeToDelete(
    deleting: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
    onDelete: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val defaultActionSize = 80.dp
    val endActionSizePx = with(density) { defaultActionSize.toPx() }
    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    val state = remember {
        AnchoredDraggableState(
            initialValue = DragAnchors.Center,
            anchors = DraggableAnchors {
                DragAnchors.Center at 0f
                DragAnchors.End at endActionSizePx
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween(),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.alertColor)
            .clip(RectangleShape)
    ) {

        IconButton(
            onClick = { showDeleteConfirmation = true }, modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            enabled = !deleting
        ) {
            if (deleting) AppProgressIndicator()
            else
                Icon(
                    imageVector = Icons.Default.Delete, contentDescription = null,
                    tint = AppTheme.colorScheme.onPrimary
                )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart)
                .offset {
                    IntOffset(
                        x = -state
                            .requireOffset()
                            .roundToInt(),
                        y = 0,
                    )
                }
                .anchoredDraggable(state, Orientation.Horizontal, reverseDirection = true),
            content = content
        )
    }

    if (showDeleteConfirmation) {
        ShowDeleteThreadDialog(onDelete = {
            onDelete()
            showDeleteConfirmation = false
        }) {
            scope.launch { state.animateTo(DragAnchors.Center) }
            showDeleteConfirmation = false
        }
    }


}

@Composable
fun ShowDeleteThreadDialog(onDelete: () -> Unit, onDismiss: () -> Unit) {
    AppAlertDialog(
        title = stringResource(R.string.threads_screen_delete_dialogue_title_text),
        subTitle = stringResource(R.string.threads_screen_delete_dialogue_message_text),
        confirmBtnText = stringResource(R.string.threads_screen_delete_dialogue_delete_btn),
        dismissBtnText = stringResource(R.string.common_btn_cancel),
        onConfirmClick = onDelete,
        onDismissClick = onDismiss,
        isConfirmDestructive = true
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.ThreadItem(
    threadInfo: ThreadInfo,
    members: List<ApiUser>,
    currentUser: ApiUser?,
    onClick: () -> Unit
) {
    val message = threadInfo.messages.firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
            .animateItemPlacement()
            .motionClickEvent { onClick() }
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThreadProfile(members)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = members.map { it.first_name ?: "" }.toFormattedTitle(),
                style = AppTheme.appTypography.body1,
                color = AppTheme.colorScheme.textPrimary
            )
            Text(
                text = message?.message ?: "",
                style = AppTheme.appTypography.label2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AppTheme.colorScheme.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasUnreadMsg = message?.seen_by?.contains(currentUser?.id) == false
            Text(
                text = message?.created_at?.formattedMessageTimeString(
                    LocalContext.current
                ) ?: "",
                style = AppTheme.appTypography.label3,
                color = if (hasUnreadMsg) AppTheme.colorScheme.primary else AppTheme.colorScheme.textSecondary
            )

            if (hasUnreadMsg) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(8.dp)
                        .background(AppTheme.colorScheme.primary, shape = CircleShape),
                )

            }
        }
    }
}


@Composable
fun ThreadProfile(members: List<ApiUser>) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(
                AppTheme.colorScheme.containerNormalOnSurface,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        when (members.size) {
            1 -> {
                members.firstOrNull()?.let {
                    UserProfile(modifier = Modifier.fillMaxSize(), user = it, fontSize = 18.sp)
                }
            }

            2 -> {
                members.forEachIndexed { index, apiUser ->
                    UserProfile(
                        modifier = Modifier
                            .size(34.dp)
                            .border(
                                width = 2.dp,
                                color = AppTheme.colorScheme.containerNormalOnSurface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(if (index == 0) Alignment.TopStart else Alignment.BottomEnd),
                        user = apiUser,
                        shape = RoundedCornerShape(8.dp),
                        fontSize = 16.sp
                    )
                }
            }

            else -> {
                val filter = members.take(3)
                filter.forEachIndexed { index, apiUser ->
                    UserProfile(
                        modifier = Modifier
                            .size(30.dp)
                            .border(
                                width = 2.dp,
                                color = AppTheme.colorScheme.containerNormalOnSurface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(if (index == 0) Alignment.TopStart else if (index == 1) Alignment.TopEnd else Alignment.BottomCenter),
                        user = apiUser,
                        shape = RoundedCornerShape(8.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NoMemberEmptyContent(
    loadingInviteCode: Boolean,
    addMember: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_thread_no_member),
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = AppTheme.colorScheme.textPrimary
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text(
            text = stringResource(id = R.string.threads_screen_no_members_title),
            style = AppTheme.appTypography.header4
        )
        Text(
            text = stringResource(id = R.string.threads_screen_no_members_subtitle),
            style = AppTheme.appTypography.label1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.padding(10.dp))

        PrimaryButton(
            label = stringResource(id = R.string.thread_screen_add_new_member),
            onClick = addMember,
            showLoader = loadingInviteCode
        )
    }
}
