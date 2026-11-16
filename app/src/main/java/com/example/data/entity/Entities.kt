package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.DifferenceStatus
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.MovementType
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ShiftStatus
import com.example.domain.model.TransferStatus
import com.example.domain.model.UserRole

@Entity(
    tableName = "branches",
    indices = [Index(value = ["code"], unique = true)]
)
data class BranchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val address: String,
    val phone: String,
    val isActive: Boolean = true
)

@Entity(
    tableName = "user_profiles",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val pinCode: String,
    val role: UserRole,
    val assignedBranchId: Long,
    val isActive: Boolean = true
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val sku: String,
    val name: String,
    val description: String,
    val categoryId: Long,
    val costPrice: Double,
    val sellingPrice: Double,
    val vatRate: Double = 21.0,
    val unit: String = "UN",
    val minStock: Int = 10,
    val isActive: Boolean = true
)

@Entity(
    tableName = "branch_inventory",
    indices = [Index(value = ["branchId", "productId"], unique = true)]
)
data class BranchInventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val productId: Long,
    val quantityOnHand: Int,
    val minAlertThreshold: Int = 5
)

@Entity(
    tableName = "inventory_movements",
    indices = [Index(value = ["branchId"]), Index(value = ["productId"])]
)
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val productId: Long,
    val movementType: MovementType,
    val quantityChange: Int,
    val previousQuantity: Int,
    val newQuantity: Int,
    val unitCost: Double,
    val referenceId: String = "",
    val notes: String = "",
    val createdByUserId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_transfers")
data class StockTransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceBranchId: Long,
    val targetBranchId: Long,
    val status: TransferStatus,
    val requestedByUserId: Long,
    val dispatchedByUserId: Long? = null,
    val receivedByUserId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "stock_transfer_items")
data class StockTransferItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transferId: Long,
    val productId: Long,
    val productName: String,
    val quantityRequested: Int,
    val quantityDispatched: Int = 0,
    val quantityReceived: Int = 0
)

@Entity(tableName = "cash_sessions")
data class CashSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val userId: Long,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val openingAmount: Double,
    val expectedCashAmount: Double = 0.0,
    val countedCashAmount: Double = 0.0,
    val cashDifference: Double = 0.0,
    val status: ShiftStatus = ShiftStatus.OPEN,
    val differenceStatus: DifferenceStatus = DifferenceStatus.NORMAL,
    val notes: String = ""
)

@Entity(tableName = "cash_movements")
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashSessionId: Long,
    val branchId: Long,
    val movementType: String, // INFLOW, OUTFLOW_DROP, EXPENSE
    val amount: Double,
    val reason: String,
    val createdByUserId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales",
    indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val cashSessionId: Long,
    val userId: Long,
    val receiptNumber: String,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val vatAmount: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val status: String = "COMPLETED", // COMPLETED, VOIDED
    val createdAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val barcode: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double,
    val subtotal: Double
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val cuit: String,
    val contactPhone: String,
    val email: String,
    val address: String,
    val isActive: Boolean = true
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val category: ExpenseCategory,
    val amount: Double,
    val description: String,
    val paymentSource: String, // CASH_DRAWER, BANK
    val createdByUserId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val userName: String,
    val branchId: Long,
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "product_batches",
    indices = [
        Index(value = ["branchId", "productId"]),
        Index(value = ["expiryDateMillis"])
    ]
)
data class ProductBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val productId: Long,
    val batchNumber: String,
    val expiryDateMillis: Long,
    val initialQuantity: Int,
    val remainingQuantity: Int,
    val supplierId: Long? = null,
    val invoiceRef: String = "",
    val receivedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_settings")
data class CompanySettingsEntity(
    @PrimaryKey val id: Long = 1L,
    val businessName: String = "Drakos Store Argentina S.A.",
    val cuit: String = "30-71234567-8",
    val taxCondition: String = "IVA Responsable Inscripto",
    val currencyCode: String = "ARS",
    val defaultVatRate: Double = 21.0,
    val cashDropAlertThreshold: Double = 50000.0,
    val cashAuditTolerance: Double = 500.0,
    val strictDeviceWhitelist: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "authorized_devices",
    indices = [Index(value = ["deviceId"], unique = true)]
)
data class AuthorizedDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val deviceName: String,
    val assignedBranchId: Long = 1L,
    val status: String = "PENDING", // AUTHORIZED, PENDING, REVOKED
    val pairingCode: String = "",
    val approvedByUserName: String = "",
    val lastAccessAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "branch_messages",
    indices = [Index(value = ["toBranchId"]), Index(value = ["createdAt"])]
)
data class BranchMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromBranchId: Long,
    val toBranchId: Long, // 0 = Toda la cadena Drakos
    val senderUserId: Long,
    val senderName: String,
    val senderRoleLevel: Int,
    val priority: String = "NORMAL", // NORMAL, STOCK_REQUEST, URGENT
    val subject: String,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "supplier_invoices",
    indices = [Index(value = ["branchId"]), Index(value = ["supplierId"])]
)
data class SupplierInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val supplierId: Long,
    val supplierName: String,
    val invoiceType: String = "FACTURA_A", // FACTURA_A, FACTURA_B, FACTURA_C, REMITO_X
    val invoiceNumber: String,
    val totalAmount: Double,
    val paymentStatus: String = "PENDING", // PENDING, PAID, CREDIT_ACCOUNT
    val dueDateMillis: Long,
    val notes: String = "",
    val createdByUserId: Long,
    val createdByUserName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weekly_orders",
    indices = [Index(value = ["branchId"]), Index(value = ["createdAt"])]
)
data class WeeklyOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: Long,
    val supplierId: Long,
    val supplierName: String,
    val status: String = "DRAFT", // DRAFT, SENT, RECEIVED
    val totalEstimatedCost: Double,
    val totalUnits: Int,
    val itemsSummary: String,
    val notes: String = "",
    val createdByUserId: Long,
    val createdByUserName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "promotions",
    indices = [Index(value = ["isActive"]), Index(value = ["createdAt"])]
)
data class PromotionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val badge: String = "COMBO",
    val description: String = "",
    val discountPercent: Double = 0.0,
    val fixedPromoPrice: Double? = null,
    val itemsSpec: String, // Formato "productId:qty,productId:qty"
    val validSchedule: String = "24hs • Todos los días",
    val isActive: Boolean = true,
    val createdByUserId: Long = 1L,
    val createdByUserName: String = "Sistema Drakos",
    val createdAt: Long = System.currentTimeMillis()
)


