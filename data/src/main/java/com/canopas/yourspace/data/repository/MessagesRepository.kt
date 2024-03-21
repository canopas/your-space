package com.canopas.yourspace.data.repository

import com.canopas.yourspace.data.models.messages.ApiThread
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.messages.ApiMessagesService
import com.canopas.yourspace.data.service.user.ApiUserService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MessagesRepository @Inject constructor(
    private val apiMessagesService: ApiMessagesService,
    private val userService: ApiUserService,
    private val authService: AuthService

) {

    suspend fun createThread(spaceId: String, adminId: String, memberIds: List<String>): String {
        val threadId = apiMessagesService.createThread(spaceId, adminId)
        apiMessagesService.joinThread(threadId, memberIds)
        return threadId
    }

    suspend fun sendMessage(
        message: String,
        senderId: String,
        threadId: String
    ) {
        apiMessagesService.sendMessage(threadId, senderId, message)
    }

    suspend fun getThreads(spaceId: String): Flow<List<ApiThread>> {
        val userId = authService.currentUser?.id ?: return emptyFlow()
        return apiMessagesService.getThreads(spaceId, userId)
    }

    suspend fun getThread(threadId: String) =
        apiMessagesService.getThread(threadId).map {
            val thread = it ?: return@map null
            val members =
                thread.member_ids.map { memberId -> userService.getUser(memberId) }.filterNotNull()
                    .map { UserInfo(it) }
            ThreadInfo(it, members)
        }

    fun getMessages(threadId: String, from: Long, limit: Int) =
        apiMessagesService.getMessages(threadId, from, limit)

    fun getLatestMessages(threadId: String, limit: Int) =
        apiMessagesService.getLatestMessages(threadId, limit)

    suspend  fun markMessagesAsSeen(threadId: String, messageIds:List<String>, currentUserId: String) {
        apiMessagesService.markMessagesAsSeen(threadId, messageIds, currentUserId)
    }
}
