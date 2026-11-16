package com.example.data.cloud

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class CloudUserAccount(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val chainName: String = "Drakos 24hs • 10 Sucursales",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudSale(
    @DocumentId val docId: String = "",
    val branchId: Long = 1L,
    val branchName: String = "",
    val receiptNumber: String = "",
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val status: String = "COMPLETED",
    val cashierName: String = "",
    val localSaleId: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudCashSession(
    @DocumentId val docId: String = "",
    val branchId: Long = 1L,
    val branchName: String = "",
    val operatorName: String = "",
    val openingAmount: Double = 0.0,
    val expectedCashAmount: Double = 0.0,
    val countedCashAmount: Double = 0.0,
    val cashDifference: Double = 0.0,
    val status: String = "OPEN",
    val differenceStatus: String = "NORMAL",
    val notes: String = "",
    val localSessionId: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudTransfer(
    @DocumentId val docId: String = "",
    val sourceBranchId: Long = 1L,
    val targetBranchId: Long = 2L,
    val productSummary: String = "",
    val quantity: Int = 1,
    val status: String = "REQUESTED",
    val notes: String = "",
    val localTransferId: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudExpense(
    @DocumentId val docId: String = "",
    val branchId: Long = 1L,
    val category: String = "VARIOS",
    val amount: Double = 0.0,
    val description: String = "",
    val paymentSource: String = "CASH_DRAWER",
    val localExpenseId: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudCompanySettings(
    @DocumentId val docId: String = "",
    val businessName: String = "Drakos Store Argentina S.A.",
    val cuit: String = "30-71234567-8",
    val taxCondition: String = "IVA Responsable Inscripto",
    val defaultVatRate: Double = 21.0,
    val cashDropAlertThreshold: Double = 50000.0,
    val cashAuditTolerance: Double = 500.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudStaffProfile(
    @DocumentId val docId: String = "",
    val username: String = "",
    val fullName: String = "",
    val pinHash: String = "",
    val role: String = "EMPLOYEE",
    val assignedBranchId: Long = 1L,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudAuthorizedDevice(
    @DocumentId val docId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val assignedBranchId: Long = 1L,
    val status: String = "PENDING",
    val pairingCode: String = "",
    val approvedByUserName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
