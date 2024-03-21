package com.canopas.yourspace.ui.flow.messages.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.flow.messages.chat.components.MemberList
import com.canopas.yourspace.ui.flow.messages.chat.components.MessageList
import com.canopas.yourspace.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen() {
    val viewModel = hiltViewModel<MessagesViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    ToolbarTitle(state.selectedMember)
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
        containerColor = AppTheme.colorScheme.surface
    ) {
        MessagesContent(modifier = Modifier.padding(it))
    }

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }
}

@Composable
fun ToolbarTitle(selectedMember: List<UserInfo>) {
    if (selectedMember.isNotEmpty()) {
        Text(
            text = selectedMember.map { it.user.first_name ?: "" }.toFormattedTitle(),
            style = AppTheme.appTypography.header3
        )
    }
}

fun List<String>.toFormattedTitle(): String {
    if (isEmpty()) return ""

    val firstTwo = this.take(2)
    val remaining = this.drop(2)

    val users = firstTwo.joinToString(", ") { it }
    val count = if (remaining.isNotEmpty()) " +${remaining.size}" else ""

    return users + count
}

@Composable
fun MessagesContent(modifier: Modifier) {
    val viewModel = hiltViewModel<MessagesViewModel>()
    val state by viewModel.state.collectAsState()
    val lazyState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    if (state.loading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AppProgressIndicator()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            MessageList(
                lazyState,
                state.loading,
                state.append,
                state.messagesByDate,
                state.threadMembers,
                state.currentUserId,
                loadMore = { viewModel.loadMore() }
            )

            NewMessageInput(
                state.newMessage,
                onValueChanged = {
                    viewModel.onMessageChanged(it)
                },
                onSend = {
                    viewModel.sendNewMessage()
                    scope.launch {
                        delay(200)
                        lazyState.animateScrollToItem(0)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = state.isNewThread,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            MemberList(
                state.currentSpace?.space?.name ?: "",
                state.currentSpace?.members?.filter { it.user.id != state.currentUserId }
                    ?: emptyList(),
                state.selectedMember,
                state.selectAll,
                onMemberSelect = {
                    viewModel.toggleMemberSelection(it)
                },
                selectAllMember = {
                    viewModel.selectAllMember()
                }
            )
        }
    }
}

@Composable
fun NewMessageInput(
    message: String,
    onValueChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            modifier = Modifier.weight(1f),
            value = message, onValueChange = onValueChanged,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            maxLines = 5,
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            textStyle = AppTheme.appTypography.body1.copy(color = AppTheme.colorScheme.textPrimary),
            cursorBrush = SolidColor(AppTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            AppTheme.colorScheme.containerNormalOnSurface,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 10.dp)
                        .padding(start = 16.dp, end = 10.dp)
                ) {
                    if (message.isEmpty()) {
                        Text(
                            text = stringResource(R.string.messages_new_message_hint),
                            style = AppTheme.appTypography.body2,
                            color = AppTheme.colorScheme.textDisabled
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { onSend() },
            modifier = Modifier
                .align(Alignment.Bottom)
                .size(40.dp)
                .clip(shape = CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = AppTheme.colorScheme.primary,
                contentColor = AppTheme.colorScheme.onPrimary,
                disabledContainerColor = AppTheme.colorScheme.containerNormalOnSurface,
                disabledContentColor = AppTheme.colorScheme.textDisabled
            ),
            enabled = message.trim().isNotEmpty(),
            content = {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "",
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}
