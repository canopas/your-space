package com.canopas.yourspace.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.io.IOException

class ServerException(message: String? = null) : Exception(message)

@Composable
fun AppBanner(msg: String, onDismiss: (() -> Unit)?) {
    if (msg.isEmpty()) return

    var show by remember { mutableStateOf(false) }
    val textStyle = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.onPrimary)
    LaunchedEffect(key1 = msg) {
        delay(200)
        show = true
        delay(5000)
        show = false
        onDismiss?.invoke()
    }

    Popup(
        properties = PopupProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        onDismissRequest = { onDismiss?.invoke() }
    ) {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { -it }
            ),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        color = AppTheme.colorScheme.primary
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = msg,
                    style = textStyle,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AppBanner(msg: Exception, customMsg: String? = null, onDismiss: (() -> Unit)? = {}) {
    var show by remember { mutableStateOf(false) }
    val textStyle = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.onPrimary)
    LaunchedEffect(key1 = Unit) {
        delay(200)
        show = true
        delay(5000)
        show = false
        onDismiss?.invoke()
    }

    Popup(
        properties = PopupProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        onDismissRequest = { onDismiss?.invoke() }
    ) {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { -it }
            ),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        color = AppTheme.colorScheme.primary
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                when (msg) {
                    is IOException -> {
                        Text(
                            text = stringResource(R.string.network_error_text),
                            style = textStyle
                        )
                    }

                    is ServerException -> {
                        Text(
                            text = stringResource(R.string.server_error_text),
                            style = textStyle
                        )
                    }

                    else -> {
                        Text(
                            text = customMsg ?: stringResource(R.string.unexpected_error_text),
                            style = textStyle,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
