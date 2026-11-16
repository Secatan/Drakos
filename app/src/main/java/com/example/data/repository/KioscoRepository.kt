package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.KioscoDatabase
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
import com.example.data.entity.SupplierInvoiceEntity
import com.example.data.entity.UserProfileEntity
import com.example.data.entity.WeeklyOrderEntity
import com.example.domain.analytics.DrakosAnalytics
import com.example.domain.model.BatchExpiryItem
import com.example.domain.model.CartItem
import com.example.domain.model.DifferenceStatus
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.MovementType
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PriceRoundingStep
import com.example.domain.model.ProductWithStock
import com.example.domain.model.SaleItemAnalytics
import com.example.domain.model.ShiftStatus
import com.example.domain.model.TransferStatus
import com.example.domain.model.UserRole
import com.example.domain.security.DeviceProvisioningCodec
import com.example.domain.security.PinHasher
import com.example.domain.security.PortableProvisioningBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class KioscoRepository(private val database: KioscoDatabase) {

    private val branchDao = database.branchDao()
    private val userDao = database.userProfileDao()
    private val productDao = database.productDao()
    private val inventoryDao = database.inventoryDao()
    private val saleDao = database.saleDao()
    private val cashDao = database.cashDao()
    private val transferDao = database.transferDao()
    private val expenseDao = database.expenseDao()
    private val auditDao = database.auditDao()
    private val supplierDao = database.supplierDao()
    private val batchDao = database.batchDao()
    private val companySettingsDao = database.companySettingsDao()
    private val deviceDao = database.authorizedDeviceDao()
    private val messageDao = database.branchMessageDao()
    private val invoiceDao = database.supplierInvoiceDao()
    private val weeklyOrderDao = database.weeklyOrderDao()

    // --- Lecturas Reactivas ---
    fun getBranches(): Flow<List<BranchEntity>> = branchDao.getAllBranches()
    fun getUsers(): Flow<List<UserProfileEntity>> = userDao.getAllUsers()
    fun getUsersAdmin(): Flow<List<UserProfileEntity>> = userDao.getAllUsersAdmin()
    fun getCategories(): Flow<List<CategoryEntity>> = productDao.getAllCategories()
    fun getProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()
    fun getSuppliers(): Flow<List<com.example.data.entity.SupplierEntity>> = supplierDao.getAllSuppliers()

    fun getActiveSessionFlow(branchId: Long): Flow<CashSessionEntity?> = cashDao.getActiveSessionFlow(branchId)
    fun getBranchSessions(branchId: Long): Flow<List<CashSessionEntity>> = cashDao.getAllSessionsForBranch(branchId)
    fun getAllSessions(): Flow<List<CashSessionEntity>> = cashDao.getAllSessions()
    fun getCashMovementsForSession(sessionId: Long): Flow<List<CashMovementEntity>> = cashDao.getCashMovementsForSession(sessionId)

    fun getBranchSales(branchId: Long): Flow<List<SaleEntity>> = saleDao.getSalesForBranch(branchId)
    fun getAllSales(): Flow<List<SaleEntity>> = saleDao.getAllSales()
    fun getAllCompletedSaleAnalytics(): Flow<List<SaleItemAnalytics>> = saleDao.getAllCompletedSaleAnalytics()

    fun getBranchInventory(branchId: Long): Flow<List<BranchInventoryEntity>> = inventoryDao.getInventoryForBranch(branchId)
    fun getAllInventory(): Flow<List<BranchInventoryEntity>> = inventoryDao.getAllInventory()
    fun getAllActiveBatches(): Flow<List<ProductBatchEntity>> = batchDao.getAllActiveBatches()
    fun getBranchMovements(branchId: Long): Flow<List<InventoryMovementEntity>> = inventoryDao.getMovementsForBranch(branchId)
    fun getAllMovements(): Flow<List<InventoryMovementEntity>> = inventoryDao.getAllMovements()

    fun getBranchTransfers(branchId: Long): Flow<List<StockTransferEntity>> = transferDao.getTransfersForBranch(branchId)
    fun getAllTransfers(): Flow<List<StockTransferEntity>> = transferDao.getAllTransfers()

    fun getBranchExpenses(branchId: Long): Flow<List<ExpenseEntity>> = expenseDao.getExpensesForBranch(branchId)
    fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    fun getAuditLogs(): Flow<List<AuditLogEntity>> = auditDao.getAllAuditLogs()
    fun getCompanySettings(): Flow<CompanySettingsEntity?> = companySettingsDao.getSettingsFlow()

    fun getAuthorizedDevices(): Flow<List<AuthorizedDeviceEntity>> = deviceDao.getAllDevices()
    fun observeDevice(deviceId: String): Flow<AuthorizedDeviceEntity?> = deviceDao.observeDeviceById(deviceId)
    fun getMessagesForBranch(branchId: Long): Flow<List<BranchMessageEntity>> = messageDao.getMessagesForBranch(branchId)
    fun getAllBranchMessages(): Flow<List<BranchMessageEntity>> = messageDao.getAllMessages()
    fun getSupplierInvoices(branchId: Long): Flow<List<SupplierInvoiceEntity>> = invoiceDao.getInvoicesForBranch(branchId)
    fun getAllSupplierInvoices(): Flow<List<SupplierInvoiceEntity>> = invoiceDao.getAllInvoices()
    fun getWeeklyOrders(branchId: Long): Flow<List<WeeklyOrderEntity>> = weeklyOrderDao.getOrdersForBranch(branchId)
    fun getAllWeeklyOrders(): Flow<List<WeeklyOrderEntity>> = weeklyOrderDao.getAllOrders()

    fun getBranchBatchExpiryItems(branchId: Long): Flow<List<BatchExpiryItem>> {
        return combine(
            batchDao.getActiveBatchesForBranch(branchId),
            productDao.getAllProducts(),
            productDao.getAllCategories()
        ) { batches, products, categories ->
            val prodMap = products.associateBy { it.id }
            val catMap = categories.associate { it.id to it.name }
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR"))

            batches.mapNotNull { batch ->
                val prod = prodMap[batch.productId] ?: return@mapNotNull null
                val daysLeft = ((batch.expiryDateMillis - now) / dayMs).toInt()
                val formattedDate = sdf.format(Date(batch.expiryDateMillis))
                BatchExpiryItem(
                    batchId = batch.id,
                    productId = prod.id,
                    barcode = prod.barcode,
                    name = prod.name,
                    categoryName = catMap[prod.categoryId] ?: "General",
                    batchNumber = batch.batchNumber,
                    quantity = batch.remainingQuantity,
                    daysUntilExpiry = daysLeft,
                    expiryDateFormatted = if (daysLeft <= 0) "VENCIDO ($formattedDate)" else "En $daysLeft días ($formattedDate)",
                    isCritical = daysLeft <= 15
                )
            }
        }
    }

    // Combinación de productos con su stock real en la sucursal seleccionada
    fun getProductsWithStock(branchId: Long): Flow<List<ProductWithStock>> {
        return combine(
            productDao.getAllProducts(),
            productDao.getAllCategories(),
            inventoryDao.getInventoryForBranch(branchId)
        ) { products, categories, inventory ->
            val catMap = categories.associate { it.id to it.name }
            val stockMap = inventory.associate { it.productId to it.quantityOnHand }

            products.map { prod ->
                ProductWithStock(
                    productId = prod.id,
                    barcode = prod.barcode,
                    sku = prod.sku,
                    name = prod.name,
                    description = prod.description,
                    categoryName = catMap[prod.categoryId] ?: "General",
                    costPrice = prod.costPrice,
                    sellingPrice = prod.sellingPrice,
                    vatRate = prod.vatRate,
                    quantityOnHand = stockMap[prod.id] ?: 0,
                    minStock = prod.minStock,
                    isActive = prod.isActive
                )
            }
        }
    }

    suspend fun getUserByPin(pin: String): UserProfileEntity? {
        return userDao.getAllUsersSync().firstOrNull { user ->
            PinHasher.verify(user.username, pin, user.pinCode)
        }
    }

    suspend fun recordSecurityAudit(
        userId: Long,
        userName: String,
        branchId: Long,
        action: String,
        details: String
    ) {
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = action,
                entityType = "AUTH",
                entityId = userId.toString(),
                details = details
            )
        )
    }

    suspend fun getBranchById(id: Long): BranchEntity? = branchDao.getBranchById(id)

    // --- OPERACIONES TRANSACCIONALES POS Y VENTAS ---

    suspend fun recordSale(
        branchId: Long,
        userId: Long,
        userName: String,
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        discount: Double = 0.0,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<SaleEntity> = database.withTransaction {
        // 1. Verificar turno de caja abierto
        val activeSession = cashDao.getActiveSession(branchId)
            ?: return@withTransaction Result.failure(IllegalStateException("No hay un turno de caja abierto en esta sucursal. Debe abrir caja con fondo inicial para operar el POS."))

        if (cartItems.isEmpty()) {
            return@withTransaction Result.failure(IllegalArgumentException("El carrito está vacío."))
        }

        // 2. Verificar existencias de inventario para cada ítem (no permitir stock negativo)
        for (item in cartItems) {
            val inv = inventoryDao.getBranchInventoryItem(branchId, item.productId)
            val currentQty = inv?.quantityOnHand ?: 0
            if (currentQty < item.quantity) {
                return@withTransaction Result.failure(
                    IllegalStateException("Stock insuficiente para '${item.name}'. Disponible: $currentQty, Solicitado: ${item.quantity}")
                )
            }
        }

        // 3. Calcular totales usando alícuota configurada
        val settings = companySettingsDao.getSettingsOnce()
        val vatRate = settings?.defaultVatRate ?: 21.0
        val rawSubtotal = cartItems.sumOf { it.subtotal }
        val finalTotal = (rawSubtotal - discount).coerceAtLeast(0.0)
        val vatTotal = if (vatRate > 0.0) finalTotal * (vatRate / 100.0) / (1.0 + vatRate / 100.0) else 0.0

        val receiptNumber = "TK-${String.format("%03d", branchId)}-${System.currentTimeMillis() % 1000000}"

        // 4. Crear venta
        val sale = SaleEntity(
            id = 0,
            branchId = branchId,
            cashSessionId = activeSession.id,
            userId = userId,
            receiptNumber = receiptNumber,
            subtotal = rawSubtotal,
            discountAmount = discount,
            vatAmount = vatTotal,
            totalAmount = finalTotal,
            paymentMethod = paymentMethod,
            idempotencyKey = idempotencyKey
        )
        val saleId = saleDao.insertSale(sale)

        // 5. Insertar líneas y descontar stock con movimiento inmutable
        val saleItems = cartItems.map { item ->
            SaleItemEntity(
                id = 0,
                saleId = saleId,
                productId = item.productId,
                productName = item.name,
                barcode = item.barcode,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                unitCost = item.unitCost,
                subtotal = item.subtotal
            )
        }
        saleDao.insertSaleItems(saleItems)

        for (item in cartItems) {
            val inv = inventoryDao.getBranchInventoryItem(branchId, item.productId)!!
            val prev = inv.quantityOnHand
            val next = prev - item.quantity

            inventoryDao.insertOrUpdateInventory(
                inv.copy(quantityOnHand = next)
            )

            inventoryDao.insertMovement(
                InventoryMovementEntity(
                    branchId = branchId,
                    productId = item.productId,
                    movementType = MovementType.SALE,
                    quantityChange = -item.quantity,
                    previousQuantity = prev,
                    newQuantity = next,
                    unitCost = item.unitCost,
                    referenceId = receiptNumber,
                    notes = "Venta POS Ticket #$receiptNumber ($paymentMethod)",
                    createdByUserId = userId
                )
            )

            // Descuento FEFO en lotes activos del producto si existen
            var remainingToDeduct = item.quantity
            val activeBatches = batchDao.getActiveBatchesForProduct(branchId, item.productId)
            for (batch in activeBatches) {
                if (remainingToDeduct <= 0) break
                val take = minOf(remainingToDeduct, batch.remainingQuantity)
                batchDao.updateBatch(batch.copy(remainingQuantity = batch.remainingQuantity - take))
                remainingToDeduct -= take
            }
        }

        // 6. Asentar en auditoría
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = "SALE_CONFIRMED",
                entityType = "SALE",
                entityId = receiptNumber,
                details = "Venta por $ $finalTotal ($paymentMethod). Ítems: ${cartItems.size}."
            )
        )

        Result.success(sale.copy(id = saleId))
    }

    // --- GESTIÓN DE CAJA Y ARQUEOS CIEGOS ---

    suspend fun openCashSession(
        branchId: Long,
        userId: Long,
        userName: String,
        openingAmount: Double,
        notes: String
    ): Result<CashSessionEntity> = database.withTransaction {
        val existing = cashDao.getActiveSession(branchId)
        if (existing != null) {
            return@withTransaction Result.failure(IllegalStateException("Ya existe un turno de caja abierto en esta sucursal."))
        }

        val session = CashSessionEntity(
            id = 0,
            branchId = branchId,
            userId = userId,
            openingAmount = openingAmount,
            status = ShiftStatus.OPEN,
            notes = notes
        )
        val sessionId = cashDao.insertSession(session)

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = "CASH_SESSION_OPENED",
                entityType = "CASH_SESSION",
                entityId = sessionId.toString(),
                details = "Apertura de turno de caja con fondo inicial de $ $openingAmount. Notas: $notes"
            )
        )

        Result.success(session.copy(id = sessionId))
    }

    suspend fun closeCashSessionWithBlindCount(
        sessionId: Long,
        countedAmount: Double,
        userId: Long,
        userName: String,
        notes: String,
        tolerance: Double = 500.0 // Tolerancia estándar en ARS si no hay configuración persistida
    ): Result<CashSessionEntity> = database.withTransaction {
        val session = cashDao.getSessionById(sessionId)
            ?: return@withTransaction Result.failure(IllegalStateException("Turno de caja no encontrado."))

        if (session.status == ShiftStatus.CLOSED) {
            return@withTransaction Result.failure(IllegalStateException("Este turno de caja ya fue cerrado."))
        }

        val cashSales = saleDao.getCashSalesForSession(sessionId) ?: 0.0
        val inflows = cashDao.getCashInflowsForSession(sessionId) ?: 0.0
        val outflows = cashDao.getCashOutflowsForSession(sessionId) ?: 0.0

        val expectedAmount = session.openingAmount + cashSales + inflows - outflows
        val difference = countedAmount - expectedAmount
        val effectiveTolerance = companySettingsDao.getSettingsOnce()?.cashAuditTolerance ?: tolerance
        val diffStatus = if (kotlin.math.abs(difference) > effectiveTolerance) {
            DifferenceStatus.PENDING_APPROVAL
        } else {
            DifferenceStatus.NORMAL
        }

        val updated = session.copy(
            closedAt = System.currentTimeMillis(),
            expectedCashAmount = expectedAmount,
            countedCashAmount = countedAmount,
            cashDifference = difference,
            status = ShiftStatus.CLOSED,
            differenceStatus = diffStatus,
            notes = notes
        )
        cashDao.updateSession(updated)

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = updated.branchId,
                action = "CASH_SESSION_CLOSED",
                entityType = "CASH_SESSION",
                entityId = sessionId.toString(),
                details = "Cierre sucursal ${updated.branchId} con arqueo ciego. Fondo: $ ${session.openingAmount} | Contado: $ $countedAmount | Esperado: $ $expectedAmount | Diferencia: $ $difference ($diffStatus)."
            )
        )

        Result.success(updated)
    }

    suspend fun registerCashDrop(
        sessionId: Long,
        branchId: Long,
        amount: Double,
        reason: String,
        userId: Long,
        userName: String
    ): Result<CashMovementEntity> = database.withTransaction {
        val drop = CashMovementEntity(
            id = 0,
            cashSessionId = sessionId,
            branchId = branchId,
            movementType = "OUTFLOW_DROP",
            amount = amount,
            reason = reason,
            createdByUserId = userId
        )
        val id = cashDao.insertCashMovement(drop)

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = "CASH_DROP",
                entityType = "CASH_MOVEMENT",
                entityId = id.toString(),
                details = "Retiro parcial de efectivo de gaveta por $ $amount. Motivo: $reason"
            )
        )

        Result.success(drop.copy(id = id))
    }

    suspend fun approveCashDifference(
        sessionId: Long,
        notes: String,
        userId: Long,
        userName: String
    ): Result<Unit> = database.withTransaction {
        val session = cashDao.getSessionById(sessionId)
            ?: return@withTransaction Result.failure(IllegalStateException("Turno de caja no encontrado."))

        val updated = session.copy(
            differenceStatus = DifferenceStatus.APPROVED,
            notes = if (session.notes.isBlank()) "Diferencia aprobada: $notes" else "${session.notes} | Aprobado: $notes"
        )
        cashDao.updateSession(updated)

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = session.branchId,
                action = "CASH_DIFFERENCE_APPROVED",
                entityType = "CASH_SESSION",
                entityId = sessionId.toString(),
                details = "Diferencia de caja ($ ${session.cashDifference}) aprobada por encargado/dueño. Motivo: $notes"
            )
        )

        Result.success(Unit)
    }

    // --- MERMAS, ROTURAS Y AJUSTES DE INVENTARIO ---

    suspend fun registerInventoryAdjustment(
        branchId: Long,
        productId: Long,
        type: MovementType,
        quantityChange: Int,
        notes: String,
        userId: Long,
        userName: String
    ): Result<Unit> = database.withTransaction {
        val inv = inventoryDao.getBranchInventoryItem(branchId, productId)
            ?: return@withTransaction Result.failure(IllegalStateException("Producto no encontrado en la sucursal."))

        val prod = productDao.getProductById(productId)
            ?: return@withTransaction Result.failure(IllegalStateException("Producto no existe en el catálogo."))

        val prev = inv.quantityOnHand
        val next = (prev + quantityChange).coerceAtLeast(0)

        inventoryDao.insertOrUpdateInventory(inv.copy(quantityOnHand = next))

        inventoryDao.insertMovement(
            InventoryMovementEntity(
                branchId = branchId,
                productId = productId,
                movementType = type,
                quantityChange = quantityChange,
                previousQuantity = prev,
                newQuantity = next,
                unitCost = prod.costPrice,
                referenceId = "AJUSTE-${System.currentTimeMillis() % 100000}",
                notes = notes,
                createdByUserId = userId
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = "INVENTORY_${type.name}",
                entityType = "INVENTORY",
                entityId = prod.barcode,
                details = "Ajuste de inventario: ${prod.name} (${type.label}). Cambio: $quantityChange (Anterior: $prev -> Nuevo: $next). Motivo: $notes"
            )
        )

        Result.success(Unit)
    }

    // --- TRANSFERENCIAS INTER-SUCURSALES ---

    suspend fun createTransferRequest(
        sourceBranchId: Long,
        targetBranchId: Long,
        productId: Long,
        quantity: Int,
        notes: String,
        userId: Long,
        userName: String
    ): Result<StockTransferEntity> = database.withTransaction {
        if (sourceBranchId == targetBranchId) {
            return@withTransaction Result.failure(IllegalArgumentException("La sucursal origen y destino deben ser distintas."))
        }

        val prod = productDao.getProductById(productId)
            ?: return@withTransaction Result.failure(IllegalStateException("Producto no encontrado."))

        val transfer = StockTransferEntity(
            id = 0,
            sourceBranchId = sourceBranchId,
            targetBranchId = targetBranchId,
            status = TransferStatus.REQUESTED,
            requestedByUserId = userId,
            notes = notes
        )
        val transferId = transferDao.insertTransfer(transfer)

        val item = StockTransferItemEntity(
            id = 0,
            transferId = transferId,
            productId = productId,
            productName = prod.name,
            quantityRequested = quantity
        )
        transferDao.insertTransferItems(listOf(item))

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = targetBranchId,
                action = "TRANSFER_REQUESTED",
                entityType = "TRANSFER",
                entityId = transferId.toString(),
                details = "Solicitud de transferencia de $quantity x '${prod.name}' desde Sucursal $sourceBranchId hacia Sucursal $targetBranchId."
            )
        )

        Result.success(transfer.copy(id = transferId))
    }

    suspend fun dispatchTransfer(
        transfer: StockTransferEntity,
        userId: Long,
        userName: String
    ): Result<Unit> = database.withTransaction {
        val items = transferDao.getTransferItems(transfer.id)
        if (items.isEmpty()) {
            return@withTransaction Result.failure(IllegalStateException("La transferencia no contiene ítems."))
        }

        // Descontar mercadería de la sucursal origen (entra en estado En Tránsito)
        for (it in items) {
            val inv = inventoryDao.getBranchInventoryItem(transfer.sourceBranchId, it.productId)
            val current = inv?.quantityOnHand ?: 0
            if (current < it.quantityRequested) {
                return@withTransaction Result.failure(IllegalStateException("Stock insuficiente en sucursal origen para despachar '${it.productName}'."))
            }
            val next = current - it.quantityRequested
            inventoryDao.insertOrUpdateInventory(inv!!.copy(quantityOnHand = next))

            inventoryDao.insertMovement(
                InventoryMovementEntity(
                    branchId = transfer.sourceBranchId,
                    productId = it.productId,
                    movementType = MovementType.TRANSFER_OUT,
                    quantityChange = -it.quantityRequested,
                    previousQuantity = current,
                    newQuantity = next,
                    unitCost = 0.0,
                    referenceId = "TRF-${transfer.id}",
                    notes = "Despacho hacia Sucursal ${transfer.targetBranchId} (En Tránsito)",
                    createdByUserId = userId
                )
            )
        }

        transferDao.updateTransfer(
            transfer.copy(
                status = TransferStatus.DISPATCHED,
                dispatchedByUserId = userId
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = transfer.sourceBranchId,
                action = "TRANSFER_DISPATCHED",
                entityType = "TRANSFER",
                entityId = transfer.id.toString(),
                details = "Mercadería despachada en tránsito hacia Sucursal ${transfer.targetBranchId}."
            )
        )

        Result.success(Unit)
    }

    suspend fun receiveTransfer(
        transfer: StockTransferEntity,
        userId: Long,
        userName: String
    ): Result<Unit> = database.withTransaction {
        val items = transferDao.getTransferItems(transfer.id)

        // Sumar mercadería a la sucursal destino
        for (it in items) {
            val inv = inventoryDao.getBranchInventoryItem(transfer.targetBranchId, it.productId)
            val current = inv?.quantityOnHand ?: 0
            val next = current + it.quantityRequested

            if (inv != null) {
                inventoryDao.insertOrUpdateInventory(inv.copy(quantityOnHand = next))
            } else {
                inventoryDao.insertOrUpdateInventory(
                    BranchInventoryEntity(
                        id = 0,
                        branchId = transfer.targetBranchId,
                        productId = it.productId,
                        quantityOnHand = next
                    )
                )
            }

            inventoryDao.insertMovement(
                InventoryMovementEntity(
                    branchId = transfer.targetBranchId,
                    productId = it.productId,
                    movementType = MovementType.TRANSFER_IN,
                    quantityChange = it.quantityRequested,
                    previousQuantity = current,
                    newQuantity = next,
                    unitCost = 0.0,
                    referenceId = "TRF-${transfer.id}",
                    notes = "Recepción final de mercadería transferida desde Sucursal ${transfer.sourceBranchId}",
                    createdByUserId = userId
                )
            )
        }

        transferDao.updateTransfer(
            transfer.copy(
                status = TransferStatus.RECEIVED,
                receivedByUserId = userId,
                completedAt = System.currentTimeMillis()
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = transfer.targetBranchId,
                action = "TRANSFER_RECEIVED",
                entityType = "TRANSFER",
                entityId = transfer.id.toString(),
                details = "Transferencia recibida con éxito en destino. Stock sumado al inventario local."
            )
        )

        Result.success(Unit)
    }

    // --- GASTOS OPERATIVOS ---

    suspend fun registerExpense(
        branchId: Long,
        category: ExpenseCategory,
        amount: Double,
        description: String,
        source: String,
        userId: Long,
        userName: String
    ): Result<ExpenseEntity> = database.withTransaction {
        val expense = ExpenseEntity(
            id = 0,
            branchId = branchId,
            category = category,
            amount = amount,
            description = description,
            paymentSource = source,
            createdByUserId = userId
        )
        val id = expenseDao.insertExpense(expense)

        // Si se pagó con dinero de la gaveta de caja, impacta la sesión activa
        if (source == "CASH_DRAWER") {
            val activeSession = cashDao.getActiveSession(branchId)
            if (activeSession != null) {
                cashDao.insertCashMovement(
                    CashMovementEntity(
                        id = 0,
                        cashSessionId = activeSession.id,
                        branchId = branchId,
                        movementType = "EXPENSE",
                        amount = amount,
                        reason = "Gasto de caja chica: ${category.label} - $description",
                        createdByUserId = userId
                    )
                )
            }
        }

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = "EXPENSE_REGISTERED",
                entityType = "EXPENSE",
                entityId = id.toString(),
                details = "Gasto registrado: ${category.label} por $ $amount ($source). Detalle: $description"
            )
        )

        Result.success(expense.copy(id = id))
    }

    // --- ANULACIÓN Y DEVOLUCIÓN CONTROLADA DE VENTAS ---

    suspend fun voidSale(
        saleId: Long,
        reason: String,
        userId: Long,
        userName: String
    ): Result<Unit> = database.withTransaction {
        val sale = saleDao.getSaleById(saleId)
            ?: return@withTransaction Result.failure(IllegalStateException("Venta no encontrada."))

        if (sale.status == "VOIDED") {
            return@withTransaction Result.failure(IllegalStateException("Esta venta ya fue anulada previamente."))
        }

        val items = saleDao.getSaleItems(saleId)
        for (item in items) {
            val inv = inventoryDao.getBranchInventoryItem(sale.branchId, item.productId)
            if (inv != null) {
                val next = inv.quantityOnHand + item.quantity
                inventoryDao.insertOrUpdateInventory(inv.copy(quantityOnHand = next))

                inventoryDao.insertMovement(
                    InventoryMovementEntity(
                        branchId = sale.branchId,
                        productId = item.productId,
                        movementType = MovementType.ADJUSTMENT,
                        quantityChange = item.quantity,
                        previousQuantity = inv.quantityOnHand,
                        newQuantity = next,
                        unitCost = item.unitCost,
                        referenceId = "ANULA-${sale.receiptNumber}",
                        notes = "Reintegro por anulación de ticket #${sale.receiptNumber}. Motivo: $reason",
                        createdByUserId = userId
                    )
                )
            }
        }

        if (sale.paymentMethod == PaymentMethod.CASH) {
            val saleSession = cashDao.getSessionById(sale.cashSessionId)
            if (saleSession != null && saleSession.status == ShiftStatus.OPEN) {
                // La venta anulada deja de sumarse en getCashSalesForSession; registramos reverso informativo sin descontar doble.
                cashDao.insertCashMovement(
                    CashMovementEntity(
                        id = 0,
                        cashSessionId = sale.cashSessionId,
                        branchId = sale.branchId,
                        movementType = "VOID_REVERSAL",
                        amount = sale.totalAmount,
                        reason = "Devolución en efectivo por anulación ticket #${sale.receiptNumber}",
                        createdByUserId = userId
                    )
                )
            } else {
                val currentActiveSession = cashDao.getActiveSession(sale.branchId)
                if (currentActiveSession != null) {
                    cashDao.insertCashMovement(
                        CashMovementEntity(
                            id = 0,
                            cashSessionId = currentActiveSession.id,
                            branchId = sale.branchId,
                            movementType = "OUTFLOW_DROP",
                            amount = sale.totalAmount,
                            reason = "Reintegro efectivo por anulación diferida ticket #${sale.receiptNumber}",
                            createdByUserId = userId
                        )
                    )
                }
            }
        }

        saleDao.updateSale(sale.copy(status = "VOIDED"))

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = sale.branchId,
                action = "SALE_VOIDED",
                entityType = "SALE",
                entityId = sale.receiptNumber,
                details = "Anulación de venta por $ ${sale.totalAmount}. Reintegro de ${items.size} ítems. Motivo: $reason"
            )
        )

        Result.success(Unit)
    }

    // --- RECEPCIÓN DE MERCADERÍA DE PROVEEDORES ---

    suspend fun receiveGoodsFromSupplier(
        branchId: Long,
        supplierId: Long,
        productId: Long,
        quantity: Int,
        unitCost: Double,
        invoiceRef: String,
        userId: Long,
        userName: String,
        batchNumber: String = "",
        expiryDateMillis: Long? = null
    ): Result<Unit> = database.withTransaction {
        val prod = productDao.getProductById(productId)
            ?: return@withTransaction Result.failure(IllegalStateException("Producto no encontrado."))

        val inv = inventoryDao.getBranchInventoryItem(branchId, productId)
        val currentQty = inv?.quantityOnHand ?: 0
        val nextQty = currentQty + quantity

        if (inv != null) {
            inventoryDao.insertOrUpdateInventory(inv.copy(quantityOnHand = nextQty))
        } else {
            inventoryDao.insertOrUpdateInventory(
                BranchInventoryEntity(
                    id = 0,
                    branchId = branchId,
                    productId = productId,
                    quantityOnHand = nextQty
                )
            )
        }

        if (unitCost > 0) {
            productDao.updateProduct(prod.copy(costPrice = unitCost))
        }

        val effectiveBatch = batchNumber.ifBlank { "LOT-${System.currentTimeMillis() % 100000}" }
        val effectiveExpiry = expiryDateMillis ?: (System.currentTimeMillis() + 90L * 86_400_000L)
        batchDao.insertBatch(
            ProductBatchEntity(
                branchId = branchId,
                productId = productId,
                batchNumber = effectiveBatch,
                expiryDateMillis = effectiveExpiry,
                initialQuantity = quantity,
                remainingQuantity = quantity,
                supplierId = supplierId,
                invoiceRef = invoiceRef
            )
        )

        inventoryDao.insertMovement(
            InventoryMovementEntity(
                branchId = branchId,
                productId = productId,
                movementType = MovementType.PURCHASE_RECEIPT,
                quantityChange = quantity,
                previousQuantity = currentQty,
                newQuantity = nextQty,
                unitCost = unitCost,
                referenceId = invoiceRef,
                notes = "Recepción de mercadería de proveedor. Remito/Factura: $invoiceRef • Lote: $effectiveBatch",
                createdByUserId = userId
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = "GOODS_RECEIVED",
                entityType = "INVENTORY",
                entityId = prod.barcode,
                details = "Ingreso de mercadería: +$quantity x '${prod.name}' (Lote $effectiveBatch) a costo $ $unitCost. Remito: $invoiceRef"
            )
        )

        Result.success(Unit)
    }

    suspend fun writeOffExpiredBatch(
        batchId: Long,
        quantity: Int,
        notes: String,
        userId: Long,
        userName: String
    ): Result<Unit> = database.withTransaction {
        val batch = batchDao.getBatchById(batchId)
            ?: return@withTransaction Result.failure(IllegalStateException("Lote no encontrado."))
        if (quantity <= 0 || quantity > batch.remainingQuantity) {
            return@withTransaction Result.failure(IllegalArgumentException("Cantidad inválida para dar de baja en el lote ${batch.batchNumber}."))
        }

        val inv = inventoryDao.getBranchInventoryItem(batch.branchId, batch.productId)
            ?: return@withTransaction Result.failure(IllegalStateException("Producto no encontrado en sucursal."))
        val prod = productDao.getProductById(batch.productId)
            ?: return@withTransaction Result.failure(IllegalStateException("Producto no encontrado en catálogo."))

        val newBatchQty = batch.remainingQuantity - quantity
        batchDao.updateBatch(batch.copy(remainingQuantity = newBatchQty))

        val prevStock = inv.quantityOnHand
        val nextStock = (prevStock - quantity).coerceAtLeast(0)
        inventoryDao.insertOrUpdateInventory(inv.copy(quantityOnHand = nextStock))

        inventoryDao.insertMovement(
            InventoryMovementEntity(
                branchId = batch.branchId,
                productId = batch.productId,
                movementType = MovementType.EXPIRY,
                quantityChange = -quantity,
                previousQuantity = prevStock,
                newQuantity = nextStock,
                unitCost = prod.costPrice,
                referenceId = batch.batchNumber,
                notes = notes.ifBlank { "Baja por vencimiento de lote ${batch.batchNumber}" },
                createdByUserId = userId
            )
        )

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = batch.branchId,
                action = "BATCH_EXPIRY_WRITEOFF",
                entityType = "BATCH",
                entityId = batch.batchNumber,
                details = "Baja por vencimiento: -$quantity u. de '${prod.name}' (Lote ${batch.batchNumber}). Stock: $prevStock -> $nextStock."
            )
        )

        Result.success(Unit)
    }

    suspend fun updateCompanySettings(
        settings: CompanySettingsEntity,
        userId: Long,
        userName: String
    ): Result<Unit> = database.withTransaction {
        val normalized = settings.copy(
            id = 1L,
            updatedAt = System.currentTimeMillis()
        )
        companySettingsDao.upsertSettings(normalized)

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = 1L,
                action = "COMPANY_SETTINGS_UPDATED",
                entityType = "SETTINGS",
                entityId = "1",
                details = "Actualización de parámetros: ${normalized.businessName} (CUIT ${normalized.cuit}), IVA ${normalized.defaultVatRate}%, Tolerancia arqueo $ ${normalized.cashAuditTolerance}, Tope gaveta $ ${normalized.cashDropAlertThreshold}."
            )
        )

        Result.success(Unit)
    }

    // --- CREACIÓN / EDICIÓN DE PRODUCTOS ---

    suspend fun saveProduct(
        product: ProductEntity,
        userId: Long,
        userName: String
    ): Result<Long> = database.withTransaction {
        val id = productDao.insertProduct(product)
        if (product.id == 0L) {
            for (b in 1..10) {
                inventoryDao.insertOrUpdateInventory(
                    BranchInventoryEntity(
                        id = 0,
                        branchId = b.toLong(),
                        productId = id,
                        quantityOnHand = 15,
                        minAlertThreshold = product.minStock
                    )
                )
            }
        }

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = 1,
                action = if (product.id == 0L) "PRODUCT_CREATED" else "PRODUCT_UPDATED",
                entityType = "PRODUCT",
                entityId = product.barcode,
                details = "Catálogo: '${product.name}' (Precio: $ ${product.sellingPrice}, Costo: $ ${product.costPrice})."
            )
        )

        Result.success(id)
    }

    // --- ADMINISTRACIÓN DE ACCESOS Y NIVELES (EXCLUSIVO NIVEL 3 OWNER) ---

    suspend fun createStaffUser(
        username: String,
        fullName: String,
        rawPin: String,
        role: UserRole,
        assignedBranchId: Long,
        ownerUserId: Long,
        ownerUserName: String
    ): Result<Long> = database.withTransaction {
        val cleanUser = username.trim().lowercase()
        if (cleanUser.length < 3 || fullName.isBlank()) {
            return@withTransaction Result.failure(IllegalArgumentException("Usuario y nombre completo son obligatorios."))
        }
        if (rawPin.length != 4 || !rawPin.all { it.isDigit() }) {
            return@withTransaction Result.failure(IllegalArgumentException("El PIN debe tener exactamente 4 dígitos numéricos."))
        }
        if (role == UserRole.OWNER) {
            return@withTransaction Result.failure(IllegalArgumentException("Solo se pueden otorgar accesos Nivel 1 o Nivel 2."))
        }
        val existing = userDao.getUserByUsername(cleanUser)
        if (existing != null) {
            return@withTransaction Result.failure(IllegalStateException("Ya existe un perfil con el usuario '$cleanUser'."))
        }
        val pinConflict = userDao.getAllUsersAdminSync().firstOrNull { u ->
            u.isActive && PinHasher.verify(u.username, rawPin, u.pinCode)
        }
        if (pinConflict != null) {
            return@withTransaction Result.failure(
                IllegalArgumentException("El PIN '$rawPin' ya está asignado a ${pinConflict.fullName}. Elija otro PIN de 4 dígitos.")
            )
        }
        val hashed = PinHasher.hash(cleanUser, rawPin)
        val newId = userDao.insertUser(
            UserProfileEntity(
                username = cleanUser,
                fullName = fullName.trim(),
                pinCode = hashed,
                role = role,
                assignedBranchId = assignedBranchId,
                isActive = true
            )
        )
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = ownerUserId,
                userName = ownerUserName,
                branchId = assignedBranchId,
                action = "ACCESS_USER_CREATED",
                entityType = "AUTH",
                entityId = cleanUser,
                details = "Alta de acceso '${fullName.trim()}' ($cleanUser) con rol ${role.label} en Sucursal K0$assignedBranchId."
            )
        )
        Result.success(newId)
    }

    suspend fun updateStaffAccess(
        targetUserId: Long,
        newRole: UserRole,
        newBranchId: Long,
        isActive: Boolean,
        newRawPin: String?,
        ownerUserId: Long,
        ownerUserName: String
    ): Result<Unit> = database.withTransaction {
        val target = userDao.getUserById(targetUserId)
            ?: return@withTransaction Result.failure(IllegalStateException("Usuario no encontrado."))
        if (target.role == UserRole.OWNER) {
            return@withTransaction Result.failure(IllegalStateException("No se puede degradar ni suspender el perfil del Propietario Nivel 3."))
        }
        if (newRole == UserRole.OWNER) {
            return@withTransaction Result.failure(IllegalArgumentException("Solo se asignan roles de Nivel 1 o Nivel 2."))
        }
        val updatedPin = if (!newRawPin.isNullOrBlank()) {
            if (newRawPin.length != 4 || !newRawPin.all { it.isDigit() }) {
                return@withTransaction Result.failure(IllegalArgumentException("El nuevo PIN debe tener 4 dígitos numéricos."))
            }
            val pinConflict = userDao.getAllUsersAdminSync().firstOrNull { u ->
                u.id != target.id && u.isActive && PinHasher.verify(u.username, newRawPin, u.pinCode)
            }
            if (pinConflict != null) {
                return@withTransaction Result.failure(
                    IllegalArgumentException("El PIN '$newRawPin' ya está asignado a ${pinConflict.fullName}. Elija otro PIN.")
                )
            }
            PinHasher.hash(target.username, newRawPin)
        } else {
            target.pinCode
        }
        userDao.updateUser(
            target.copy(
                role = newRole,
                assignedBranchId = newBranchId,
                isActive = isActive,
                pinCode = updatedPin
            )
        )
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = ownerUserId,
                userName = ownerUserName,
                branchId = newBranchId,
                action = "ACCESS_USER_UPDATED",
                entityType = "AUTH",
                entityId = target.username,
                details = "Permiso actualizado para ${target.fullName}: ${newRole.label}, Sucursal K0$newBranchId, Activo=$isActive."
            )
        )
        Result.success(Unit)
    }

    // --- AUTORIZACIÓN DE DISPOSITIVOS / LISTA BLANCA (EXCLUSIVO NIVEL 3) ---

    suspend fun ensureDeviceRegisteredOnStartup(
        deviceId: String,
        deviceName: String
    ): AuthorizedDeviceEntity = database.withTransaction {
        val existing = deviceDao.getDeviceById(deviceId)
        if (existing != null) {
            val touched = existing.copy(lastAccessAt = System.currentTimeMillis())
            deviceDao.updateDevice(touched)
            return@withTransaction touched
        }
        val authCount = deviceDao.countAuthorizedDevices()
        val initialStatus = if (authCount == 0) "AUTHORIZED" else "PENDING"
        val device = AuthorizedDeviceEntity(
            deviceId = deviceId,
            deviceName = deviceName,
            assignedBranchId = 1L,
            status = initialStatus,
            approvedByUserName = if (authCount == 0) "Bootstrap Terminal Central" else "",
            lastAccessAt = System.currentTimeMillis()
        )
        val id = deviceDao.upsertDevice(device)
        device.copy(id = id)
    }

    suspend fun isDeviceAllowedForStaff(deviceId: String): Boolean {
        val settings = companySettingsDao.getSettingsOnce()
        if (settings != null && !settings.strictDeviceWhitelist) return true
        val dev = deviceDao.getDeviceById(deviceId)
        return dev?.status == "AUTHORIZED"
    }

    suspend fun authorizeDeviceByOwner(
        targetDeviceId: String,
        deviceName: String,
        assignedBranchId: Long,
        ownerUserId: Long,
        ownerUserName: String
    ): Result<Unit> = database.withTransaction {
        val cleanId = targetDeviceId.trim().uppercase()
        if (cleanId.length < 4) {
            return@withTransaction Result.failure(IllegalArgumentException("Código de dispositivo inválido."))
        }
        val existing = deviceDao.getDeviceById(cleanId)
        val record = existing?.copy(
            deviceName = deviceName.ifBlank { existing.deviceName },
            assignedBranchId = assignedBranchId,
            status = "AUTHORIZED",
            pairingCode = "",
            approvedByUserName = ownerUserName,
            lastAccessAt = System.currentTimeMillis()
        ) ?: AuthorizedDeviceEntity(
            deviceId = cleanId,
            deviceName = deviceName.ifBlank { "Terminal Sucursal K0$assignedBranchId" },
            assignedBranchId = assignedBranchId,
            status = "AUTHORIZED",
            approvedByUserName = ownerUserName
        )
        deviceDao.upsertDevice(record)
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = ownerUserId,
                userName = ownerUserName,
                branchId = assignedBranchId,
                action = "DEVICE_AUTHORIZED",
                entityType = "DEVICE",
                entityId = cleanId,
                details = "Dispositivo autorizado para operar Drakos: '${record.deviceName}' en Sucursal K0$assignedBranchId."
            )
        )
        Result.success(Unit)
    }

    suspend fun createPairingInvite(
        deviceAlias: String,
        assignedBranchId: Long,
        ownerUserId: Long,
        ownerUserName: String
    ): Result<String> = database.withTransaction {
        val token = "DRK-" + ((1000..9999).random().toString())
        val placeholderId = "INVITE-$token"
        deviceDao.upsertDevice(
            AuthorizedDeviceEntity(
                deviceId = placeholderId,
                deviceName = deviceAlias.ifBlank { "Nuevo Teléfono Sucursal K0$assignedBranchId" },
                assignedBranchId = assignedBranchId,
                status = "PENDING",
                pairingCode = token,
                approvedByUserName = ownerUserName
            )
        )
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = ownerUserId,
                userName = ownerUserName,
                branchId = assignedBranchId,
                action = "DEVICE_INVITE_CREATED",
                entityType = "DEVICE",
                entityId = token,
                details = "Token de alta de dispositivo generado ($token) para '$deviceAlias' (Sucursal K0$assignedBranchId)."
            )
        )
        Result.success(token)
    }

    suspend fun redeemPairingCodeForDevice(
        currentDeviceId: String,
        currentDeviceName: String,
        pairingCode: String
    ): Result<Long> = database.withTransaction {
        val cleanToken = pairingCode.trim().uppercase()
        val invite = deviceDao.getPendingDeviceByPairingCode(cleanToken)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Código de vinculación '$cleanToken' inválido o ya utilizado."))

        val existingCurrent = deviceDao.getDeviceById(currentDeviceId)
        val authorizedRecord = (existingCurrent ?: invite).copy(
            deviceId = currentDeviceId,
            deviceName = if (invite.deviceName.isNotBlank()) invite.deviceName else currentDeviceName,
            assignedBranchId = invite.assignedBranchId,
            status = "AUTHORIZED",
            pairingCode = "",
            approvedByUserName = invite.approvedByUserName,
            lastAccessAt = System.currentTimeMillis()
        )
        // Consumir la invitación original si era un registro temporal
        if (invite.deviceId != currentDeviceId) {
            deviceDao.updateDevice(invite.copy(status = "REVOKED", pairingCode = ""))
        }
        deviceDao.upsertDevice(authorizedRecord)
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = 1L,
                userName = invite.approvedByUserName.ifBlank { "Propietario Nivel 3" },
                branchId = invite.assignedBranchId,
                action = "DEVICE_PAIRED_WITH_TOKEN",
                entityType = "DEVICE",
                entityId = currentDeviceId,
                details = "Nuevo dispositivo vinculado mediante token autorizado ($cleanToken) para Sucursal K0${invite.assignedBranchId}."
            )
        )
        Result.success(invite.assignedBranchId)
    }

    suspend fun revokeDeviceByOwner(
        targetDeviceId: String,
        ownerUserId: Long,
        ownerUserName: String
    ): Result<Unit> = database.withTransaction {
        val dev = deviceDao.getDeviceById(targetDeviceId)
            ?: return@withTransaction Result.failure(IllegalStateException("Dispositivo no encontrado."))
        deviceDao.updateDevice(dev.copy(status = "REVOKED", pairingCode = ""))
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = ownerUserId,
                userName = ownerUserName,
                branchId = dev.assignedBranchId,
                action = "DEVICE_REVOKED",
                entityType = "DEVICE",
                entityId = targetDeviceId,
                details = "Acceso revocado al dispositivo '${dev.deviceName}' ($targetDeviceId)."
            )
        )
        Result.success(Unit)
    }

    suspend fun getUserByIdSync(userId: Long): UserProfileEntity? = userDao.getUserById(userId)

    suspend fun getAllUsersAdminSync(): List<UserProfileEntity> = userDao.getAllUsersAdminSync()

    suspend fun getDeviceByIdSync(deviceId: String): AuthorizedDeviceEntity? = deviceDao.getDeviceById(deviceId)

    suspend fun getAllDevicesSync(): List<AuthorizedDeviceEntity> = deviceDao.getAllDevicesSync()

    suspend fun upsertSyncedStaffProfiles(remoteProfiles: List<UserProfileEntity>): Int = database.withTransaction {
        var applied = 0
        remoteProfiles.forEach { profile ->
            val cleanUser = profile.username.trim().lowercase()
            if (cleanUser.length >= 3 && PinHasher.isHashed(profile.pinCode)) {
                val existing = userDao.getUserByUsername(cleanUser)
                if (existing == null) {
                    userDao.insertUser(
                        profile.copy(
                            id = 0L,
                            username = cleanUser,
                            fullName = profile.fullName.ifBlank { cleanUser }
                        )
                    )
                    applied++
                } else if (
                    existing.fullName != profile.fullName ||
                    existing.pinCode != profile.pinCode ||
                    existing.role != profile.role ||
                    existing.assignedBranchId != profile.assignedBranchId ||
                    existing.isActive != profile.isActive
                ) {
                    userDao.updateUser(
                        existing.copy(
                            fullName = profile.fullName.ifBlank { existing.fullName },
                            pinCode = profile.pinCode,
                            role = profile.role,
                            assignedBranchId = profile.assignedBranchId,
                            isActive = profile.isActive
                        )
                    )
                    applied++
                }
            }
        }
        applied
    }

    suspend fun upsertSyncedDevices(remoteDevices: List<AuthorizedDeviceEntity>): Int = database.withTransaction {
        var applied = 0
        remoteDevices.forEach { remote ->
            val cleanId = remote.deviceId.trim().uppercase()
            if (cleanId.length >= 4) {
                val existing = deviceDao.getDeviceById(cleanId)
                if (existing == null) {
                    deviceDao.upsertDevice(remote.copy(id = 0L, deviceId = cleanId))
                    applied++
                } else {
                    val shouldUpdate = when {
                        remote.status == "REVOKED" && existing.status != "REVOKED" -> true
                        existing.status == "PENDING" && remote.status == "AUTHORIZED" -> true
                        existing.assignedBranchId != remote.assignedBranchId && !existing.deviceId.startsWith("INVITE-") && remote.status == "AUTHORIZED" -> true
                        else -> false
                    }
                    if (shouldUpdate) {
                        deviceDao.updateDevice(
                            existing.copy(
                                deviceName = remote.deviceName.ifBlank { existing.deviceName },
                                assignedBranchId = remote.assignedBranchId,
                                status = remote.status,
                                pairingCode = remote.pairingCode,
                                approvedByUserName = remote.approvedByUserName.ifBlank { existing.approvedByUserName }
                            )
                        )
                        applied++
                    }
                }
            }
        }
        applied
    }

    suspend fun generatePortableProvisioningBundle(
        pairingCode: String,
        assignedBranchId: Long,
        deviceAlias: String,
        ownerUserName: String
    ): String {
        val allUsers = userDao.getAllUsersAdminSync()
        return DeviceProvisioningCodec.encode(
            pairingCode = pairingCode,
            assignedBranchId = assignedBranchId,
            deviceAlias = deviceAlias,
            approvedBy = ownerUserName,
            profiles = allUsers
        )
    }

    suspend fun applyPortableProvisioningBundle(
        bundle: PortableProvisioningBundle,
        currentDeviceId: String,
        currentDeviceName: String
    ): Result<Pair<Long, Int>> = database.withTransaction {
        var imported = 0
        bundle.profiles.forEach { profile ->
            val cleanUser = profile.username.trim().lowercase()
            if (cleanUser.length >= 3 && PinHasher.isHashed(profile.pinCode)) {
                val existing = userDao.getUserByUsername(cleanUser)
                if (existing == null) {
                    userDao.insertUser(profile.copy(id = 0L, username = cleanUser))
                    imported++
                } else if (
                    existing.pinCode != profile.pinCode ||
                    existing.role != profile.role ||
                    existing.assignedBranchId != profile.assignedBranchId ||
                    existing.isActive != profile.isActive ||
                    existing.fullName != profile.fullName
                ) {
                    userDao.updateUser(
                        existing.copy(
                            fullName = profile.fullName.ifBlank { existing.fullName },
                            pinCode = profile.pinCode,
                            role = profile.role,
                            assignedBranchId = profile.assignedBranchId,
                            isActive = profile.isActive
                        )
                    )
                    imported++
                }
            }
        }

        val cleanDevId = currentDeviceId.trim().uppercase().ifBlank { "DRK-LOCAL" }
        val existingDev = deviceDao.getDeviceById(cleanDevId)
        val devRecord = (existingDev ?: AuthorizedDeviceEntity(deviceId = cleanDevId, deviceName = currentDeviceName))
            .copy(
                deviceId = cleanDevId,
                deviceName = bundle.deviceAlias.ifBlank { currentDeviceName },
                assignedBranchId = bundle.assignedBranchId,
                status = "AUTHORIZED",
                pairingCode = "",
                approvedByUserName = bundle.approvedBy,
                lastAccessAt = System.currentTimeMillis()
            )
        deviceDao.upsertDevice(devRecord)

        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = 1L,
                userName = bundle.approvedBy.ifBlank { "Propietario Nivel 3" },
                branchId = bundle.assignedBranchId,
                action = "DEVICE_PORTABLE_PROVISIONED",
                entityType = "DEVICE",
                entityId = cleanDevId,
                details = "Terminal vinculada e importación de $imported perfil(es)/PIN(s) desde Nivel 3 (${bundle.pairingCode}) para Sucursal K0${bundle.assignedBranchId}."
            )
        )
        Result.success(bundle.assignedBranchId to imported)
    }

    // --- COMUNICACIÓN ENTRE LOCALES (NIVEL 2 Y 3) ---

    suspend fun sendBranchMessage(
        fromBranchId: Long,
        toBranchId: Long,
        senderUserId: Long,
        senderName: String,
        senderRoleLevel: Int,
        priority: String,
        subject: String,
        message: String
    ): Result<Long> = database.withTransaction {
        if (subject.isBlank() || message.isBlank()) {
            return@withTransaction Result.failure(IllegalArgumentException("Asunto y mensaje son obligatorios."))
        }
        val id = messageDao.insertMessage(
            BranchMessageEntity(
                fromBranchId = fromBranchId,
                toBranchId = toBranchId,
                senderUserId = senderUserId,
                senderName = senderName,
                senderRoleLevel = senderRoleLevel,
                priority = priority,
                subject = subject.trim(),
                message = message.trim()
            )
        )
        Result.success(id)
    }

    suspend fun markMessageAsRead(messageId: Long) {
        messageDao.markAsRead(messageId)
    }

    // --- FACTURACIÓN DE PROVEEDORES (NIVEL 2 Y 3) ---

    suspend fun registerSupplierInvoice(
        branchId: Long,
        supplierId: Long,
        supplierName: String,
        invoiceType: String,
        invoiceNumber: String,
        totalAmount: Double,
        paymentStatus: String,
        dueDays: Int,
        notes: String,
        userId: Long,
        userName: String
    ): Result<Long> = database.withTransaction {
        if (invoiceNumber.isBlank() || totalAmount <= 0.0) {
            return@withTransaction Result.failure(IllegalArgumentException("Número de comprobante y monto válido son obligatorios."))
        }
        val dueDate = System.currentTimeMillis() + dueDays.coerceAtLeast(0).toLong() * 86_400_000L
        val id = invoiceDao.insertInvoice(
            SupplierInvoiceEntity(
                branchId = branchId,
                supplierId = supplierId,
                supplierName = supplierName,
                invoiceType = invoiceType,
                invoiceNumber = invoiceNumber.trim().uppercase(),
                totalAmount = totalAmount,
                paymentStatus = paymentStatus,
                dueDateMillis = dueDate,
                notes = notes.trim(),
                createdByUserId = userId,
                createdByUserName = userName
            )
        )
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = "SUPPLIER_INVOICE_CREATED",
                entityType = "INVOICE",
                entityId = invoiceNumber.trim().uppercase(),
                details = "$invoiceType #$invoiceNumber de $supplierName por $ $totalAmount ($paymentStatus)."
            )
        )
        Result.success(id)
    }

    suspend fun markSupplierInvoicePaid(
        invoiceId: Long,
        userId: Long,
        userName: String
    ): Result<Unit> = database.withTransaction {
        val inv = invoiceDao.getInvoiceById(invoiceId)
            ?: return@withTransaction Result.failure(IllegalStateException("Factura no encontrada."))
        invoiceDao.updateInvoice(inv.copy(paymentStatus = "PAID"))
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = inv.branchId,
                action = "SUPPLIER_INVOICE_PAID",
                entityType = "INVOICE",
                entityId = inv.invoiceNumber,
                details = "Factura #${inv.invoiceNumber} (${inv.supplierName}) marcada como PAGADA ($ ${inv.totalAmount})."
            )
        )
        Result.success(Unit)
    }

    // --- PEDIDOS SEMANALES BASADOS EN VENTAS Y STOCK (NIVEL 2 Y 3) ---

    suspend fun saveWeeklyOrder(
        branchId: Long,
        supplierId: Long,
        supplierName: String,
        totalEstimatedCost: Double,
        totalUnits: Int,
        itemsSummary: String,
        notes: String,
        userId: Long,
        userName: String
    ): Result<Long> = database.withTransaction {
        if (totalUnits <= 0 || itemsSummary.isBlank()) {
            return@withTransaction Result.failure(IllegalArgumentException("Debe incluir al menos un producto en el pedido semanal."))
        }
        val id = weeklyOrderDao.insertOrder(
            WeeklyOrderEntity(
                branchId = branchId,
                supplierId = supplierId,
                supplierName = supplierName,
                status = "SENT",
                totalEstimatedCost = totalEstimatedCost,
                totalUnits = totalUnits,
                itemsSummary = itemsSummary,
                notes = notes.trim(),
                createdByUserId = userId,
                createdByUserName = userName
            )
        )
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = "WEEKLY_ORDER_GENERATED",
                entityType = "WEEKLY_ORDER",
                entityId = "PED-$id",
                details = "Pedido semanal a $supplierName: $totalUnits unidades (Costo est.: $ $totalEstimatedCost)."
            )
        )
        Result.success(id)
    }

    suspend fun updateWeeklyOrderStatus(
        orderId: Long,
        newStatus: String,
        userId: Long,
        userName: String
    ): Result<Unit> = database.withTransaction {
        val order = weeklyOrderDao.getOrderById(orderId)
            ?: return@withTransaction Result.failure(IllegalStateException("Pedido no encontrado."))
        weeklyOrderDao.updateOrder(order.copy(status = newStatus))
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = order.branchId,
                action = "WEEKLY_ORDER_STATUS",
                entityType = "WEEKLY_ORDER",
                entityId = "PED-${order.id}",
                details = "Estado de pedido semanal actualizado a $newStatus (${order.supplierName})."
            )
        )
        Result.success(Unit)
    }

    // --- ACTUALIZACIÓN MASIVA DE PRECIOS Y LIQUIDACIÓN POR VENCIMIENTO ---

    suspend fun applyBulkPriceUpdate(
        categoryId: Long?,
        costPercentDelta: Double,
        pricePercentDelta: Double,
        preserveMargin: Boolean,
        roundingStep: PriceRoundingStep,
        userId: Long,
        userName: String
    ): Result<Int> = database.withTransaction {
        val activeProducts = productDao.getAllActiveProductsSync()
        val previewRows = DrakosAnalytics.buildBulkPricePreview(
            products = activeProducts,
            categoryId = categoryId,
            costPercentDelta = costPercentDelta,
            pricePercentDelta = pricePercentDelta,
            preserveMargin = preserveMargin,
            roundingStep = roundingStep
        )
        if (previewRows.isEmpty()) {
            return@withTransaction Result.failure(IllegalStateException("No hay productos activos en el alcance seleccionado."))
        }
        val prodMap = activeProducts.associateBy { it.id }
        var updatedCount = 0
        previewRows.forEach { row ->
            val entity = prodMap[row.productId]
            if (entity != null) {
                productDao.updateProduct(
                    entity.copy(
                        costPrice = row.newCostPrice,
                        sellingPrice = row.newSellingPrice
                    )
                )
                updatedCount++
            }
        }
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = 1L,
                action = "BULK_PRICE_UPDATE",
                entityType = "PRODUCT",
                entityId = categoryId?.toString() ?: "ALL",
                details = "Ajuste masivo de precios a $updatedCount prod. (Costo: $costPercentDelta%, Venta: $pricePercentDelta%, Redondeo: ${roundingStep.label}, Preservar margen: $preserveMargin)."
            )
        )
        Result.success(updatedCount)
    }

    suspend fun applyBatchClearanceMarkdown(
        productId: Long,
        branchId: Long,
        batchNumber: String,
        discountPercent: Double,
        userId: Long,
        userName: String
    ): Result<Double> = database.withTransaction {
        val prod = productDao.getProductById(productId)
            ?: return@withTransaction Result.failure(IllegalStateException("Producto no encontrado."))
        val rawPromoPrice = (prod.sellingPrice * (1.0 - discountPercent / 100.0)).coerceAtLeast(10.0)
        val roundedPromoPrice = DrakosAnalytics.roundPriceToStep(rawPromoPrice, PriceRoundingStep.TEN)
        productDao.updateProduct(prod.copy(sellingPrice = roundedPromoPrice))
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                branchId = branchId,
                action = "EXPIRY_CLEARANCE_MARKDOWN",
                entityType = "BATCH",
                entityId = batchNumber,
                details = "Liquidación -${discountPercent.toInt()}% aplicada a '${prod.name}' (Lote $batchNumber): $${prod.sellingPrice.toInt()} -> $${roundedPromoPrice.toInt()}."
            )
        )
        Result.success(roundedPromoPrice)
    }
}

