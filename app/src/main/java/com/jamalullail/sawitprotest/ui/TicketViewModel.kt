package com.jamalullail.sawitprotest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamalullail.sawitprotest.data.local.TicketEntity
import com.jamalullail.sawitprotest.data.repository.TicketRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TicketUiState {
    object Loading : TicketUiState()
    data class Success(val tickets: List<TicketEntity>) : TicketUiState()
    data class Error(val message: String) : TicketUiState()
}

sealed class SaveTicketUiState {
    object Idle : SaveTicketUiState()
    object Loading : SaveTicketUiState()
    object Success : SaveTicketUiState()
    data class Error(val message: String) : SaveTicketUiState()
}

class TicketViewModel(private val repository: TicketRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TicketUiState>(TicketUiState.Loading)
    val uiState: StateFlow<TicketUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveTicketUiState>(SaveTicketUiState.Idle)
    val saveState: StateFlow<SaveTicketUiState> = _saveState.asStateFlow()

    val licenseNumber = MutableStateFlow("")
    val driverName = MutableStateFlow("")
    val inboundWeight = MutableStateFlow("")
    val outboundWeight = MutableStateFlow("")

    val netWeight: StateFlow<Double> = combine(inboundWeight, outboundWeight) { inW, outW ->
        val inVal = inW.toDoubleOrNull() ?: 0.0
        val outVal = outW.toDoubleOrNull() ?: 0.0
        (inVal - outVal).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow("date")

    val unsyncedCount: StateFlow<Int> = repository.getUnsyncedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        observeTickets()
        repository.startRealtimeSync(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTickets() {
        combine(_searchQuery, _sortBy) { query, sort ->
            query to sort
        }.flatMapLatest { (query, sort) ->
            repository.getAllTickets(query, sort)
        }.onEach { tickets ->
            _uiState.value = TicketUiState.Success(tickets)
        }.catch { e ->
            _uiState.value = TicketUiState.Error(e.message ?: "Unknown Error")
        }.launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(sortBy: String) {
        _sortBy.value = sortBy
    }

    suspend fun getTicketById(id: String): TicketEntity? = repository.getTicketById(id)

    fun setFormData(ticket: TicketEntity) {
        licenseNumber.value = ticket.licenseNumber
        driverName.value = ticket.driverName
        inboundWeight.value = ticket.inboundWeight.toString()
        outboundWeight.value = ticket.outboundWeight.toString()
    }

    fun clearFormData() {
        licenseNumber.value = ""
        driverName.value = ""
        inboundWeight.value = ""
        outboundWeight.value = ""
    }

    fun saveTicket(id: String? = null) {
        viewModelScope.launch {
            _saveState.value = SaveTicketUiState.Loading
            
            val result = repository.saveTicket(
                id = id,
                licenseNumber = licenseNumber.value,
                driverName = driverName.value,
                inboundWeight = inboundWeight.value.toDoubleOrNull() ?: -1.0,
                outboundWeight = outboundWeight.value.toDoubleOrNull() ?: -1.0
            )
            
            handleSaveResult(result)
        }
    }

    private suspend fun handleSaveResult(result: Result<Boolean>) {
        result.onSuccess { syncSuccess ->
            _saveState.value = SaveTicketUiState.Success
            if (!syncSuccess) {
                _errorMessage.emit("Local save success, sync pending.")
            }
        }.onFailure { e ->
            _saveState.value = SaveTicketUiState.Error(e.message ?: "Save failed")
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val result = repository.syncUnsyncedTickets()
            result.onFailure { e ->
                _errorMessage.emit("Sync failed: ${e.message}")
            }
        }
    }
    
    fun resetSaveState() {
        _saveState.value = SaveTicketUiState.Idle
    }
}
