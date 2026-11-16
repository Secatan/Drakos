package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.AuthorizedDeviceEntity
import com.example.data.entity.BranchEntity
import com.example.data.entity.BranchInventoryEntity
import com.example.data.entity.BranchMessageEntity
import com.example.data.entity.CashMovementEntity
import com.example.data.entity.CashSessionEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.InventoryMovementEntity
import com.example.data.entity.ProductBatchEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.StockTransferEntity
import com.example.data.entity.StockTransferItemEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.SupplierInvoiceEntity
import com.example.data.entity.UserProfileEntity
import com.example.data.entity.WeeklyOrderEntity
import com.example.domain.model.SaleItemAnalytics
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches WHERE isActive = 1 ORDER BY id ASC")
    fun getAllBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE id = :id LIMIT 1")
    suspend fun getBranchById(id: Long): BranchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranches(branches: List<BranchEntity>)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE isActive = 1 ORDER BY role DESC, fullName ASC")
    fun getAllUsers(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles ORDER BY isActive DESC, role DESC, fullName ASC")
    fun getAllUsersAdmin(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 ORDER BY role DESC, fullName ASC")
    suspend fun getAllUsersSync(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles ORDER BY isActive DESC, role DESC, fullName ASC")
    suspend fun getAllUsersAdminSync(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE pinCode = :pin AND isActive = 1 LIMIT 1")
    suspend fun getUserByPin(pin: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserProfileEntity): Long

    @Update
    suspend fun updateUser(user: UserProfileEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveProductsSync(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM branch_inventory WHERE branchId = :branchId")
    fun getInventoryForBranch(branchId: Long): Flow<List<BranchInventoryEntity>>

    @Query("SELECT * FROM branch_inventory")
    fun getAllInventory(): Flow<List<BranchInventoryEntity>>

    @Query("SELECT * FROM branch_inventory WHERE branchId = :branchId AND productId = :productId LIMIT 1")
    suspend fun getBranchInventoryItem(branchId: Long, productId: Long): BranchInventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateInventory(inventory: BranchInventoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryList(list: List<BranchInventoryEntity>)

    @Insert
    suspend fun insertMovement(movement: InventoryMovementEntity): Long

    @Query("SELECT * FROM inventory_movements WHERE branchId = :branchId ORDER BY timestamp DESC LIMIT 200")
    fun getMovementsForBranch(branchId: Long): Flow<List<InventoryMovementEntity>>

    @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC LIMIT 300")
    fun getAllMovements(): Flow<List<InventoryMovementEntity>>
}

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sales WHERE branchId = :branchId ORDER BY createdAt DESC")
    fun getSalesForBranch(branchId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity>

    @Query(
        """
        SELECT s.branchId AS branchId, si.productId AS productId, si.quantity AS quantity,
               si.unitPrice AS unitPrice, si.unitCost AS unitCost, si.subtotal AS subtotal, s.createdAt AS createdAt
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        WHERE s.status = 'COMPLETED'
        """
    )
    fun getAllCompletedSaleAnalytics(): Flow<List<SaleItemAnalytics>>

    @Query("SELECT SUM(totalAmount) FROM sales WHERE branchId = :branchId AND status = 'COMPLETED' AND createdAt >= :sinceTimestamp")
    suspend fun getSalesSumSince(branchId: Long, sinceTimestamp: Long): Double?

    @Query("SELECT SUM(totalAmount) FROM sales WHERE cashSessionId = :sessionId AND status = 'COMPLETED' AND paymentMethod = 'CASH'")
    suspend fun getCashSalesForSession(sessionId: Long): Double?

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): SaleEntity?

    @Update
    suspend fun updateSale(sale: SaleEntity)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)
}

@Dao
interface CashDao {
    @Query("SELECT * FROM cash_sessions WHERE branchId = :branchId AND status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getActiveSession(branchId: Long): CashSessionEntity?

    @Query("SELECT * FROM cash_sessions WHERE branchId = :branchId AND status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun getActiveSessionFlow(branchId: Long): Flow<CashSessionEntity?>

    @Query("SELECT * FROM cash_sessions WHERE branchId = :branchId ORDER BY openedAt DESC")
    fun getAllSessionsForBranch(branchId: Long): Flow<List<CashSessionEntity>>

    @Query("SELECT * FROM cash_sessions ORDER BY openedAt DESC")
    fun getAllSessions(): Flow<List<CashSessionEntity>>

    @Query("SELECT * FROM cash_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): CashSessionEntity?

    @Insert
    suspend fun insertSession(session: CashSessionEntity): Long

    @Update
    suspend fun updateSession(session: CashSessionEntity)

    @Insert
    suspend fun insertCashMovement(movement: CashMovementEntity): Long

    @Query("SELECT * FROM cash_movements WHERE cashSessionId = :sessionId ORDER BY timestamp DESC")
    fun getCashMovementsForSession(sessionId: Long): Flow<List<CashMovementEntity>>

    @Query("SELECT SUM(amount) FROM cash_movements WHERE cashSessionId = :sessionId AND movementType = 'INFLOW'")
    suspend fun getCashInflowsForSession(sessionId: Long): Double?

    @Query("SELECT SUM(amount) FROM cash_movements WHERE cashSessionId = :sessionId AND movementType = 'OUTFLOW_DROP'")
    suspend fun getCashDropsForSession(sessionId: Long): Double?

    @Query("SELECT SUM(amount) FROM cash_movements WHERE cashSessionId = :sessionId AND movementType IN ('OUTFLOW_DROP', 'EXPENSE')")
    suspend fun getCashOutflowsForSession(sessionId: Long): Double?
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM stock_transfers ORDER BY createdAt DESC")
    fun getAllTransfers(): Flow<List<StockTransferEntity>>

    @Query("SELECT * FROM stock_transfers WHERE sourceBranchId = :branchId OR targetBranchId = :branchId ORDER BY createdAt DESC")
    fun getTransfersForBranch(branchId: Long): Flow<List<StockTransferEntity>>

    @Insert
    suspend fun insertTransfer(transfer: StockTransferEntity): Long

    @Update
    suspend fun updateTransfer(transfer: StockTransferEntity)

    @Insert
    suspend fun insertTransferItems(items: List<StockTransferItemEntity>)

    @Query("SELECT * FROM stock_transfer_items WHERE transferId = :transferId")
    suspend fun getTransferItems(transferId: Long): List<StockTransferItemEntity>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE branchId = :branchId ORDER BY createdAt DESC")
    fun getExpensesForBranch(branchId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE branchId = :branchId ORDER BY timestamp DESC LIMIT 200")
    fun getAuditLogsForBranch(branchId: Long): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertAuditLog(log: AuditLogEntity): Long
}

@Dao
interface BatchDao {
    @Query("SELECT * FROM product_batches WHERE branchId = :branchId AND remainingQuantity > 0 ORDER BY expiryDateMillis ASC")
    fun getActiveBatchesForBranch(branchId: Long): Flow<List<ProductBatchEntity>>

    @Query("SELECT * FROM product_batches WHERE remainingQuantity > 0 ORDER BY expiryDateMillis ASC")
    fun getAllActiveBatches(): Flow<List<ProductBatchEntity>>

    @Query("SELECT * FROM product_batches WHERE branchId = :branchId AND productId = :productId AND remainingQuantity > 0 ORDER BY expiryDateMillis ASC")
    suspend fun getActiveBatchesForProduct(branchId: Long, productId: Long): List<ProductBatchEntity>

    @Query("SELECT * FROM product_batches WHERE id = :id LIMIT 1")
    suspend fun getBatchById(id: Long): ProductBatchEntity?

    @Query("SELECT COUNT(*) FROM product_batches")
    suspend fun countBatches(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: ProductBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<ProductBatchEntity>)

    @Update
    suspend fun updateBatch(batch: ProductBatchEntity)
}

@Dao
interface CompanySettingsDao {
    @Query("SELECT * FROM company_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<CompanySettingsEntity?>

    @Query("SELECT * FROM company_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): CompanySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: CompanySettingsEntity)
}

@Dao
interface AuthorizedDeviceDao {
    @Query("SELECT * FROM authorized_devices ORDER BY CASE status WHEN 'PENDING' THEN 0 WHEN 'AUTHORIZED' THEN 1 ELSE 2 END, lastAccessAt DESC")
    fun getAllDevices(): Flow<List<AuthorizedDeviceEntity>>

    @Query("SELECT * FROM authorized_devices ORDER BY lastAccessAt DESC")
    suspend fun getAllDevicesSync(): List<AuthorizedDeviceEntity>

    @Query("SELECT * FROM authorized_devices WHERE deviceId = :deviceId LIMIT 1")
    fun observeDeviceById(deviceId: String): Flow<AuthorizedDeviceEntity?>

    @Query("SELECT * FROM authorized_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): AuthorizedDeviceEntity?

    @Query("SELECT * FROM authorized_devices WHERE pairingCode = :code AND status = 'PENDING' LIMIT 1")
    suspend fun getPendingDeviceByPairingCode(code: String): AuthorizedDeviceEntity?

    @Query("SELECT COUNT(*) FROM authorized_devices WHERE status = 'AUTHORIZED'")
    suspend fun countAuthorizedDevices(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(device: AuthorizedDeviceEntity): Long

    @Update
    suspend fun updateDevice(device: AuthorizedDeviceEntity)
}

@Dao
interface BranchMessageDao {
    @Query("SELECT * FROM branch_messages ORDER BY createdAt DESC LIMIT 200")
    fun getAllMessages(): Flow<List<BranchMessageEntity>>

    @Query("SELECT * FROM branch_messages WHERE toBranchId = 0 OR toBranchId = :branchId OR fromBranchId = :branchId ORDER BY createdAt DESC LIMIT 200")
    fun getMessagesForBranch(branchId: Long): Flow<List<BranchMessageEntity>>

    @Insert
    suspend fun insertMessage(message: BranchMessageEntity): Long

    @Insert
    suspend fun insertMessages(messages: List<BranchMessageEntity>)

    @Query("UPDATE branch_messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("SELECT COUNT(*) FROM branch_messages")
    suspend fun countMessages(): Int
}

@Dao
interface SupplierInvoiceDao {
    @Query("SELECT * FROM supplier_invoices WHERE branchId = :branchId ORDER BY createdAt DESC")
    fun getInvoicesForBranch(branchId: Long): Flow<List<SupplierInvoiceEntity>>

    @Query("SELECT * FROM supplier_invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<SupplierInvoiceEntity>>

    @Query("SELECT * FROM supplier_invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): SupplierInvoiceEntity?

    @Insert
    suspend fun insertInvoice(invoice: SupplierInvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: SupplierInvoiceEntity)
}

@Dao
interface WeeklyOrderDao {
    @Query("SELECT * FROM weekly_orders WHERE branchId = :branchId ORDER BY createdAt DESC")
    fun getOrdersForBranch(branchId: Long): Flow<List<WeeklyOrderEntity>>

    @Query("SELECT * FROM weekly_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<WeeklyOrderEntity>>

    @Query("SELECT * FROM weekly_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): WeeklyOrderEntity?

    @Insert
    suspend fun insertOrder(order: WeeklyOrderEntity): Long

    @Update
    suspend fun updateOrder(order: WeeklyOrderEntity)
}
