package com.canopas.yourspace.data.models.messages

import com.canopas.yourspace.data.models.user.UserInfo

data class ThreadInfo(
    val thread: ApiThread,
    val members: List<UserInfo> = emptyList(),
    val messages: List<ApiThreadMessage> = emptyList(),
)
