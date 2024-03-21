package com.canopas.yourspace.data.service.space

import com.canopas.yourspace.data.models.space.ApiSpaceInvitation
import com.canopas.yourspace.data.utils.Config
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpaceInvitationService @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val spaceInvitationRef =
        db.collection(Config.FIRESTORE_COLLECTION_SPACE_INVITATION)

    suspend fun createInvitation(spaceId: String): String {
        val invitationCode = generateInvitationCode()
        val docRef = spaceInvitationRef.document()
        val invitation =
            ApiSpaceInvitation(id = docRef.id, space_id = spaceId, code = invitationCode)

        docRef.set(invitation).await()
        return invitationCode
    }

    private fun generateInvitationCode(): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { characters.random() }
            .joinToString("")
    }

    suspend fun getSpaceInviteCode(spaceId: String): ApiSpaceInvitation? {
        val query = spaceInvitationRef.whereEqualTo("space_id", spaceId)
        val result = query.get().await()
        return result.documents.firstOrNull()?.toObject(ApiSpaceInvitation::class.java)
    }

    suspend fun regenerateInvitationCode(spaceId: String): String {
        val invitation = getSpaceInviteCode(spaceId)
        if (invitation != null) {
            val newCode = generateInvitationCode()
            val docRef = spaceInvitationRef.document(invitation.id)
            docRef.update("code", newCode, "created_at", System.currentTimeMillis()).await()
            return newCode
        }
        return ""
    }

    suspend fun getInvitation(inviteCode: String): ApiSpaceInvitation? {
        val query = spaceInvitationRef.whereEqualTo("code", inviteCode.uppercase())
        val result = query.get().await()
        val invitation = result.documents.firstOrNull()?.toObject(ApiSpaceInvitation::class.java)
        return invitation?.takeIf { !it.isExpired }
    }

    suspend fun deleteInvitations(spaceId: String) {
        spaceInvitationRef.whereEqualTo("space_id", spaceId).get().await().documents.forEach {
            it.reference.delete().await()
        }
    }
}
