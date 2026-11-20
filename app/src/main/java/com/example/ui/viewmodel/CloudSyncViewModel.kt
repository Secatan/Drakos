package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.CloudCashSession
import com.example.data.cloud.CloudCompanySettings
import com.example.data.cloud.CloudExpense
import com.example.data.cloud.CloudSale
import com.example.data.cloud.CloudTransfer
import com.example.data.cloud.CloudUserAccount
import com.example.data.entity.CompanySettingsEntity
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.KioscoRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

data class CloudDashboardSummary(
    val account: CloudUserAccount?,
    val sales: List<CloudSale>,
    val sessions: List<CloudCashSession>,
    val transfers: List<CloudTransfer>,
    val expenses: List<CloudExpense>,
    val companySettings: CloudCompanySettings?
) {
    val activeSalesTotal: Double
        get() = sales.filter { it.status == "COMPLETED" }.sumOf { it.totalAmount }

    val activeExpensesTotal: Double
        get() = expenses.sumOf { it.amount }
}

class CloudSyncViewModel(
    private val cloudRepository: CloudSyncRepository,
    private val localRepository: KioscoRepository,
    private val currentUserId: String,
    private val accountDisplayName: String,
    private val accountEmail: String?
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val uiState: StateFlow<CloudUiState<CloudDashboardSummary>> = combine(
        cloudRepository.observeWorkspaceAccount(currentUserId),
        cloudRepository.observeCloudSales(currentUserId),
        cloudRepository.observeCloudCashSessions(currentUserId),
        cloudRepository.observeCloudTransfers(currentUserId),
        cloudRepository.observeCloudExpenses(currentUserId),
        cloudRepository.observeCloudCompanySettings(currentUserId)
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CloudDashboardSummary(
            account = values[0] as CloudUserAccount?,
            sales = values[1] as List<CloudSale>,
            sessions = values[2] as List<CloudCashSession>,
            transfers = values[3] as List<CloudTransfer>,
            expenses = values[4] as List<CloudExpense>,
            companySettings = values[5] as CloudCompanySettings?
        )
    }
        .map<CloudDashboardSummary, CloudUiState<CloudDashboardSummary>> { CloudUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing cloud data", rootError)
            emit(CloudUiState.Error(rootError.localizedMessage ?: "Error al sincronizar con Firestore."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CloudUiState.Loading
        )

    init {
        viewModelScope.launch {
            val localSettings = localRepository.getCompanySettings().first() ?: CompanySettingsEntity()
            cloudRepository.ensureWorkspaceProfile(
                displayName = accountDisplayName,
                email = accountEmail,
                chainName = localSettings.businessName
            )
        }
    }

    fun syncAllLocalToCloud() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val branches = localRepository.getBranches().first()
                val users = localRepository.getUsersAdmin().first()
                val devices = localRepository.getAuthorizedDevices().first()
                val sales = localRepository.getAllSales().first()
                val sessions = localRepository.getAllSessions().first()
                val transfers = localRepository.getAllTransfers().first()
                val expenses = localRepository.getAllExpenses().first()
                val settings = localRepository.getCompanySettings().first() ?: CompanySettingsEntity()

                val transferSummaries = transfers.associate { trf ->
                    trf.id to localRepository.buildTransferCloudPayloadInfo(trf)
                }

                val res = cloudRepository.syncFullLocalSnapshot(
                    displayName = accountDisplayName,
                    email = accountEmail,
                    branches = branches,
                    users = users,
                    sales = sales,
                    sessions = sessions,
                    transfers = transfers,
                    expenses = expenses,
                    settings = settings,
                    devices = devices,
                    transferSummaries = transferSummaries
                )
                res.fold(
                    onSuccess = { count ->
                        _syncMessage.value = "Sincronización completada: $count registros (incluidos PINs y terminales) actualizados en la nube."
                    },
                    onFailure = { err ->
                        _syncMessage.value = "Error de sincronización: ${err.localizedMessage ?: err.message}"
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun restoreSettingsFromCloud(operatorId: Long, operatorName: String) {
        val currentState = uiState.value
        val cloudSettings = (currentState as? CloudUiState.Success)?.data?.companySettings
        if (cloudSettings == null) {
            _syncMessage.value = "Aún no hay parámetros fiscales guardados en la nube."
            return
        }
        viewModelScope.launch {
            val entity = CompanySettingsEntity(
                id = 1L,
                businessName = cloudSettings.businessName,
                cuit = cloudSettings.cuit,
                taxCondition = cloudSettings.taxCondition,
                defaultVatRate = cloudSettings.defaultVatRate,
                cashDropAlertThreshold = cloudSettings.cashDropAlertThreshold,
                cashAuditTolerance = cloudSettings.cashAuditTolerance
            )
            localRepository.updateCompanySettings(entity, operatorId, operatorName).fold(
                onSuccess = {
                    _syncMessage.value = "Parámetros fiscales restaurados desde la nube al terminal local."
                },
                onFailure = { err ->
                    _syncMessage.value = err.message ?: "No se pudieron aplicar los parámetros."
                }
            )
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    private companion object {
        const val TAG = "CloudSyncVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
