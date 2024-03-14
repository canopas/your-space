package com.canopas.yourspace.ui.flow.home.map.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.utils.AppDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val locationService: ApiLocationService,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(MemberDetailState())
    var state = _state.asStateFlow()

    private var _dataPagingSource: LocationHistoryPagingSource? = null

    val location = Pager(
        config = PagingConfig(
            pageSize = 8
        )
    ) {
        LocationHistoryPagingSource(
            query = locationService.getLocationHistoryQuery(
                _state.value.selectedUser?.user?.id ?: "",
                _state.value.selectedTimeFrom ?: 0,
                _state.value.selectedTimeTo ?: 0
            )
        ).also {
            _dataPagingSource = it
        }
    }.flow.cachedIn(viewModelScope)

    fun fetchUserLocationHistory(
        from: Long,
        to: Long,
        userInfo: UserInfo? = _state.value.selectedUser
    ) = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(
            _state.value.copy(
                selectedUser = userInfo,
                selectedTimeFrom = from,
                selectedTimeTo = to
            )
        )
        try {
            _dataPagingSource?.invalidate()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch location history")
            _state.emit(_state.value.copy(error = e))
        }
    }
}

data class MemberDetailState(
    val selectedUser: UserInfo? = null,
    val selectedTimeFrom: Long? = null,
    val selectedTimeTo: Long? = null,
    val error: Exception? = null
)
