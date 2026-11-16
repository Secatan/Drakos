package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.cloud.CloudAuthorizedDevice
import com.example.data.cloud.CloudCashSession
import com.example.data.cloud.CloudCompanySettings
import com.example.data.cloud.CloudExpense
import com.example.data.cloud.CloudSale
import com.example.data.cloud.CloudStaffProfile
import com.example.data.cloud.CloudTransfer
import com.example.data.cloud.CloudUserAccount
import com.example.data.cloud.OperationType
import com.example.data.cloud.handleFirestoreError
import com.example.data.entity.AuthorizedDeviceEntity
import com.example.data.entity.BranchEntity
import com.example.data.entity.CashSessionEntity
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.StockTransferEntity
import com.example.data.entity.UserProfileEntity
import com.example.domain.model.DifferenceStatus
import com.example.domain.model.ShiftStatus
import com.example.domain.model.TransferStatus
import com.example.domain.security.PinHasher
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Autenticación con Google requerida para sincronizar.")

    // --- STREAMS EN TIEMPO REAL (AISLADOS POR {userId}) ---

    fun observeWorkspaceAccount(userId: String): Flow<CloudUserAccount?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    CloudUserAccount::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeCloudSales(userId: String): Flow<List<CloudSale>> =
        db.collection("users").document(userId).collection("sales")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudSale::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeCloudCashSessions(userId: String): Flow<List<CloudCashSession>> =
        db.collection("users").document(userId).collection("cash_sessions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudCashSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeCloudTransfers(userId: String): Flow<List<CloudTransfer>> =
        db.collection("users").document(userId).collection("transfers")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudTransfer::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeCloudExpenses(userId: String): Flow<List<CloudExpense>> =
        db.collection("users").document(userId).collection("expenses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudExpense::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeCloudCompanySettings(userId: String): Flow<CloudCompanySettings?> =
        db.collection("users").document(userId).collection("settings").document("company")
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    CloudCompanySettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeCloudStaffProfiles(userId: String): Flow<List<CloudStaffProfile>> =
        db.collection("users").document(userId).collection("staff_profiles")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudStaffProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeCloudDevices(userId: String): Flow<List<CloudAuthorizedDevice>> =
        db.collection("users").document(userId).collection("devices")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudAuthorizedDevice::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    // --- CONSULTAS PUNTUALES ---

    suspend fun getCloudSaleById(userId: String, docId: String): Result<CloudSale?> {
        val docRef = db.collection("users").document(userId).collection("sales").document(docId)
        return try {
            val snap = docRef.get().await()
            val sale = snap.toObject(
                CloudSale::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(sale)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    // --- ESCRITURAS SEGURAS CON VALIDACIÓN DE ESQUEMA ---

    suspend fun ensureWorkspaceProfile(
        displayName: String,
        email: String?,
        chainName: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            val safeName = displayName.ifBlank { "Operador Drakos" }.take(100)
            val safeChain = chainName.ifBlank { "Drakos 24hs • 10 Sucursales" }.take(120)
            if (!existing.exists()) {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", safeName)
                    if (!email.isNullOrBlank()) put("email", email.take(160))
                    put("chainName", safeChain)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val payload = buildMap<String, Any> {
                    put("displayName", safeName)
                    if (!email.isNullOrBlank()) put("email", email.take(160))
                    put("chainName", safeChain)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncSale(
        sale: SaleEntity,
        branchName: String,
        cashierName: String,
        customDocId: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = customDocId ?: "sale_${sale.id}"
        val docRef = db.collection("users").document(uid).collection("sales").document(docId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "branchId" to sale.branchId.coerceAtLeast(1L),
                    "branchName" to branchName.ifBlank { "Sucursal ${sale.branchId}" }.take(100),
                    "receiptNumber" to sale.receiptNumber.ifBlank { "TK-${sale.id}" }.take(60),
                    "totalAmount" to sale.totalAmount.coerceAtLeast(0.0),
                    "paymentMethod" to sale.paymentMethod.name,
                    "status" to sale.status,
                    "cashierName" to cashierName.ifBlank { "Cajero" }.take(100),
                    "localSaleId" to sale.id.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val prevStatus = snap.getString("status")
                if (prevStatus == "COMPLETED" && sale.status == "VOIDED") {
                    docRef.update(
                        mapOf(
                            "status" to "VOIDED",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncCashSession(
        session: CashSessionEntity,
        branchName: String,
        operatorName: String,
        customDocId: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = customDocId ?: "sess_${session.id}"
        val docRef = db.collection("users").document(uid).collection("cash_sessions").document(docId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "branchId" to session.branchId.coerceAtLeast(1L),
                    "branchName" to branchName.ifBlank { "Sucursal ${session.branchId}" }.take(100),
                    "operatorName" to operatorName.ifBlank { "Operador" }.take(100),
                    "openingAmount" to session.openingAmount.coerceAtLeast(0.0),
                    "expectedCashAmount" to session.expectedCashAmount,
                    "countedCashAmount" to session.countedCashAmount.coerceAtLeast(0.0),
                    "cashDifference" to session.cashDifference,
                    "status" to session.status.name,
                    "differenceStatus" to session.differenceStatus.name,
                    "notes" to session.notes.take(300),
                    "localSessionId" to session.id.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val prevStatus = snap.getString("status")
                val prevDiff = snap.getString("differenceStatus")
                if (prevStatus == "OPEN" && session.status == ShiftStatus.CLOSED) {
                    docRef.update(
                        mapOf(
                            "expectedCashAmount" to session.expectedCashAmount,
                            "countedCashAmount" to session.countedCashAmount.coerceAtLeast(0.0),
                            "cashDifference" to session.cashDifference,
                            "status" to ShiftStatus.CLOSED.name,
                            "differenceStatus" to session.differenceStatus.name,
                            "notes" to session.notes.take(300),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else if (prevStatus == "CLOSED" &&
                    prevDiff == DifferenceStatus.PENDING_APPROVAL.name &&
                    session.differenceStatus == DifferenceStatus.APPROVED
                ) {
                    docRef.update(
                        mapOf(
                            "differenceStatus" to DifferenceStatus.APPROVED.name,
                            "notes" to session.notes.take(300),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncTransfer(
        transfer: StockTransferEntity,
        productSummary: String,
        quantity: Int,
        customDocId: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = customDocId ?: "trf_${transfer.id}"
        val docRef = db.collection("users").document(uid).collection("transfers").document(docId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "sourceBranchId" to transfer.sourceBranchId.coerceAtLeast(1L),
                    "targetBranchId" to transfer.targetBranchId.coerceAtLeast(1L),
                    "productSummary" to productSummary.ifBlank { "Transferencia #${transfer.id}" }.take(160),
                    "quantity" to quantity.coerceAtLeast(1),
                    "status" to transfer.status.name,
                    "notes" to transfer.notes.take(300),
                    "localTransferId" to transfer.id.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val prevStatus = snap.getString("status")
                if (prevStatus == TransferStatus.REQUESTED.name &&
                    (transfer.status == TransferStatus.DISPATCHED || transfer.status == TransferStatus.RECEIVED)
                ) {
                    docRef.update(
                        mapOf(
                            "status" to TransferStatus.DISPATCHED.name,
                            "notes" to transfer.notes.take(300),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                val afterDispatch = if (prevStatus == TransferStatus.REQUESTED.name &&
                    transfer.status == TransferStatus.RECEIVED
                ) TransferStatus.DISPATCHED.name else prevStatus
                if (afterDispatch == TransferStatus.DISPATCHED.name &&
                    transfer.status == TransferStatus.RECEIVED
                ) {
                    docRef.update(
                        mapOf(
                            "status" to TransferStatus.RECEIVED.name,
                            "notes" to transfer.notes.take(300),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncExpense(
        expense: ExpenseEntity,
        customDocId: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = customDocId ?: "exp_${expense.id}"
        val docRef = db.collection("users").document(uid).collection("expenses").document(docId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "branchId" to expense.branchId.coerceAtLeast(1L),
                    "category" to expense.category.name.take(40),
                    "amount" to expense.amount.coerceAtLeast(0.01),
                    "description" to expense.description.ifBlank { expense.category.label }.take(200),
                    "paymentSource" to expense.paymentSource,
                    "localExpenseId" to expense.id.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncCompanySettings(settings: CompanySettingsEntity): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("settings").document("company")
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "businessName" to settings.businessName.ifBlank { "Drakos Store S.A." }.take(120),
                    "cuit" to settings.cuit.ifBlank { "30-00000000-0" }.take(30),
                    "taxCondition" to settings.taxCondition.ifBlank { "IVA Responsable Inscripto" }.take(80),
                    "defaultVatRate" to settings.defaultVatRate.coerceIn(0.0, 100.0),
                    "cashDropAlertThreshold" to settings.cashDropAlertThreshold.coerceAtLeast(0.0),
                    "cashAuditTolerance" to settings.cashAuditTolerance.coerceAtLeast(0.0),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "businessName" to settings.businessName.ifBlank { "Drakos Store S.A." }.take(120),
                    "cuit" to settings.cuit.ifBlank { "30-00000000-0" }.take(30),
                    "taxCondition" to settings.taxCondition.ifBlank { "IVA Responsable Inscripto" }.take(80),
                    "defaultVatRate" to settings.defaultVatRate.coerceIn(0.0, 100.0),
                    "cashDropAlertThreshold" to settings.cashDropAlertThreshold.coerceAtLeast(0.0),
                    "cashAuditTolerance" to settings.cashAuditTolerance.coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun sanitizeDocKey(prefix: String, raw: String): String {
        val clean = raw.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return "${prefix}_${clean.take(110).ifBlank { "item" }}"
    }

    suspend fun syncStaffProfile(user: UserProfileEntity): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanUser = user.username.trim().lowercase()
        if (cleanUser.length < 3) {
            return Result.failure(IllegalArgumentException("Usuario inválido para sincronizar."))
        }
        val safeHash = if (PinHasher.isHashed(user.pinCode)) user.pinCode else PinHasher.hash(cleanUser, user.pinCode)
        val docId = sanitizeDocKey("staff", cleanUser)
        val docRef = db.collection("users").document(uid).collection("staff_profiles").document(docId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "username" to cleanUser.take(60),
                    "fullName" to user.fullName.ifBlank { cleanUser }.take(100),
                    "pinHash" to safeHash,
                    "role" to user.role.name,
                    "assignedBranchId" to user.assignedBranchId.coerceIn(1L, 100L),
                    "isActive" to user.isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "fullName" to user.fullName.ifBlank { cleanUser }.take(100),
                    "pinHash" to safeHash,
                    "role" to user.role.name,
                    "assignedBranchId" to user.assignedBranchId.coerceIn(1L, 100L),
                    "isActive" to user.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncAuthorizedDevice(device: AuthorizedDeviceEntity): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanDevId = device.deviceId.trim().uppercase()
        if (cleanDevId.length < 4) {
            return Result.failure(IllegalArgumentException("ID de dispositivo inválido para sincronizar."))
        }
        val docId = sanitizeDocKey("dev", cleanDevId)
        val docRef = db.collection("users").document(uid).collection("devices").document(docId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "deviceId" to cleanDevId.take(80),
                    "deviceName" to device.deviceName.ifBlank { "Terminal Drakos" }.take(100),
                    "assignedBranchId" to device.assignedBranchId.coerceIn(1L, 100L),
                    "status" to device.status.ifBlank { "PENDING" },
                    "pairingCode" to device.pairingCode.take(30),
                    "approvedByUserName" to device.approvedByUserName.take(100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "deviceName" to device.deviceName.ifBlank { "Terminal Drakos" }.take(100),
                    "assignedBranchId" to device.assignedBranchId.coerceIn(1L, 100L),
                    "status" to device.status.ifBlank { "PENDING" },
                    "pairingCode" to device.pairingCode.take(30),
                    "approvedByUserName" to device.approvedByUserName.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchCloudStaffAndDevicesOnce(): Result<Pair<List<CloudStaffProfile>, List<CloudAuthorizedDevice>>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val staffRef = db.collection("users").document(uid).collection("staff_profiles")
        val devicesRef = db.collection("users").document(uid).collection("devices")
        return try {
            val staffSnap = staffRef.get().await()
            val devicesSnap = devicesRef.get().await()
            val staff = staffSnap.toObjects(
                CloudStaffProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val devices = devicesSnap.toObjects(
                CloudAuthorizedDevice::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(staff to devices)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, staffRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncAllStaffAndDevices(
        users: List<UserProfileEntity>,
        devices: List<AuthorizedDeviceEntity>
    ): Result<Pair<Int, Int>> = coroutineScope {
        try {
            requireUserId()
            var syncedUsers = 0
            users.chunked(10).forEach { chunk ->
                val results = chunk.map { profile -> async { syncStaffProfile(profile) } }.awaitAll()
                results.forEach { res ->
                    res.onFailure { return@coroutineScope Result.failure(it) }
                    syncedUsers += 1
                }
            }
            var syncedDevices = 0
            devices.chunked(10).forEach { chunk ->
                val results = chunk.map { dev -> async { syncAuthorizedDevice(dev) } }.awaitAll()
                results.forEach { res ->
                    res.onFailure { return@coroutineScope Result.failure(it) }
                    syncedDevices += 1
                }
            }
            Result.success(syncedUsers to syncedDevices)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncFullLocalSnapshot(
        displayName: String,
        email: String?,
        branches: List<BranchEntity>,
        users: List<UserProfileEntity>,
        sales: List<SaleEntity>,
        sessions: List<CashSessionEntity>,
        transfers: List<StockTransferEntity>,
        expenses: List<ExpenseEntity>,
        settings: CompanySettingsEntity,
        devices: List<AuthorizedDeviceEntity> = emptyList()
    ): Result<Int> {
        val branchMap = branches.associate { it.id to it.name }
        val userMap = users.associate { it.id to it.fullName }
        var syncedCount = 0

        ensureWorkspaceProfile(displayName, email, settings.businessName).onFailure { return Result.failure(it) }
        syncCompanySettings(settings).onFailure { return Result.failure(it) }
        syncedCount += 1

        val (uCount, dCount) = syncAllStaffAndDevices(users, devices).getOrElse { return Result.failure(it) }
        syncedCount += uCount + dCount
        sales.take(50).forEach { sale ->
            val bName = branchMap[sale.branchId] ?: "Sucursal K0${sale.branchId}"
            val cName = userMap[sale.userId] ?: "Cajero #${sale.userId}"
            syncSale(sale, bName, cName).onFailure { return Result.failure(it) }
            syncedCount += 1
        }
        sessions.take(30).forEach { sess ->
            val bName = branchMap[sess.branchId] ?: "Sucursal K0${sess.branchId}"
            val oName = userMap[sess.userId] ?: "Operador #${sess.userId}"
            syncCashSession(sess, bName, oName).onFailure { return Result.failure(it) }
            syncedCount += 1
        }
        transfers.take(30).forEach { trf ->
            syncTransfer(trf, "Transferencia K0${trf.sourceBranchId} -> K0${trf.targetBranchId}", 1)
                .onFailure { return Result.failure(it) }
            syncedCount += 1
        }
        expenses.take(30).forEach { exp ->
            syncExpense(exp).onFailure { return Result.failure(it) }
            syncedCount += 1
        }
        return Result.success(syncedCount)
    }
}
