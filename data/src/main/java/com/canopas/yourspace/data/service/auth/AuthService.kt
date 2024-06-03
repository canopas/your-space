package com.canopas.yourspace.data.service.auth

import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val userPreferences: UserPreferences,
    private val apiUserService: ApiUserService,
    private val firebaseAuth: FirebaseAuth,
    private val locationManager: LocationManager
) {
    private val authStateChangeListeners = HashSet<AuthStateChangeListener>()

    fun addListener(authStateChangeListener: AuthStateChangeListener) {
        this.authStateChangeListeners.add(authStateChangeListener)
    }

    fun removeListener(authStateChangeListener: AuthStateChangeListener) {
        this.authStateChangeListeners.remove(authStateChangeListener)
    }

    suspend fun verifiedPhoneLogin(
        uid: String?,
        firebaseToken: String?,
        phoneNumber: String
    ): Boolean {
        return processLogin(uid, firebaseToken, null, phoneNumber)
    }

    suspend fun verifiedGoogleLogin(
        uid: String?,
        firebaseToken: String?,
        account: GoogleSignInAccount
    ): Boolean {
        return processLogin(uid, firebaseToken, account, null)
    }

    private suspend fun processLogin(
        uid: String?,
        firebaseToken: String?,
        account: GoogleSignInAccount? = null,
        phoneNumber: String? = null
    ): Boolean {
        val (isNewUser, user, session) =
            apiUserService.saveUser(uid, firebaseToken, account, phoneNumber)
        notifyAuthChange()
        saveUser(user, session)
        return isNewUser
    }

    private fun notifyAuthChange() {
        authStateChangeListeners.forEach { it.onAuthStateChanged() }
    }

    var currentUser: ApiUser?
        get() {
            return userPreferences.currentUser
        }
        private set(newUser) {
            userPreferences.currentUser = newUser
        }

    private var currentUserSession: ApiUserSession?
        get() {
            return userPreferences.currentUserSession
        }
        private set(newSession) {
            userPreferences.currentUserSession = newSession
        }

    private fun saveUser(user: ApiUser, session: ApiUserSession) {
        currentUser = user
        currentUserSession = session
    }

    fun saveUser(user: ApiUser) {
        currentUser = user
    }

    fun saveUserSession(session: ApiUserSession) {
        currentUserSession = session
    }

    suspend fun updateUser(user: ApiUser) {
        apiUserService.updateUser(user)
        currentUser = user
    }

    fun signOut() {
        locationManager.stopLocationTracking()
        currentUser = null
        currentUserSession = null
        userPreferences.isFCMRegistered = false
        userPreferences.setOnboardShown(false)
        userPreferences.currentSpace = ""
        firebaseAuth.signOut()
        locationManager.stopService()
    }

    suspend fun deleteAccount() {
        val currentUser = currentUser ?: return
        apiUserService.deleteUser(currentUser.id)
        signOut()
    }

    suspend fun getUser(): ApiUser? = apiUserService.getUser(currentUser?.id ?: "")
    suspend fun getUserFlow() = apiUserService.getUserFlow(currentUser?.id ?: "")

    suspend fun updateBatteryStatus(batteryPercentage: Float) {
        val currentUser = currentUser ?: return
        val session = currentUserSession ?: return
        apiUserService.updateBatteryPct(currentUser.id, session.id, batteryPercentage)
    }

    suspend fun updateUserSessionState(state: Int) {
        val currentUser = currentUser ?: return
        val session = currentUserSession ?: return
        apiUserService.updateSessionState(currentUser.id, session.id, state)
    }
}

interface AuthStateChangeListener {
    fun onAuthStateChanged()
}

sealed class AuthState {
    data object UNAUTHORIZED : AuthState()
    data class VERIFIED(val user: ApiUser) : AuthState()

    val isAuthorised: Boolean
        get() = this !is UNAUTHORIZED

    val isVerified: Boolean
        get() = this is VERIFIED
}
