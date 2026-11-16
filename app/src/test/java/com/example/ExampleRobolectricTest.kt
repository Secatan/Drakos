package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.KioscoDatabase
import com.example.data.entity.BranchEntity
import com.example.data.entity.BranchInventoryEntity
import com.example.data.entity.CashSessionEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.ProductBatchEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.UserProfileEntity
import com.example.data.repository.KioscoRepository
import com.example.domain.analytics.DrakosAnalytics
import com.example.domain.model.CartItem
import com.example.domain.model.DifferenceStatus
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PriceRoundingStep
import com.example.domain.model.SaleItemAnalytics
import com.example.domain.model.ShiftStatus
import com.example.domain.model.StockHealthStatus
import com.example.domain.model.UserRole
import com.example.domain.security.PinHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: KioscoDatabase
    private lateinit var repository: KioscoRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KioscoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = KioscoRepository(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context validates KioscoPOS name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Drakos", appName)
    }

    @Test
    fun `test sale decrements stock exactly once in atomic transaction`() = runBlocking {
        // 1. Preparar sucursal, producto, inventario y turno
        val branch = BranchEntity(1, "Sucursal Centro", "K01", "Corrientes 1240", "1143712210")
        db.branchDao().insertBranches(listOf(branch))

        val product = ProductEntity(
            id = 10,
            barcode = "7790010001",
            sku = "ALF-HAV-01",
            name = "Alfajor Havanna 70% Cacao",
            description = "Alfajor",
            categoryId = 1,
            costPrice = 1150.0,
            sellingPrice = 1800.0,
            vatRate = 21.0
        )
        db.productDao().insertProducts(listOf(product))

        val initialStock = 20
        db.inventoryDao().insertOrUpdateInventory(
            BranchInventoryEntity(id = 1, branchId = 1, productId = 10, quantityOnHand = initialStock)
        )

        val session = CashSessionEntity(
            id = 1,
            branchId = 1,
            userId = 3,
            openingAmount = 15000.0,
            status = ShiftStatus.OPEN
        )
        db.cashDao().insertSession(session)

        // 2. Ejecutar venta de 3 alfajores
        val cartItems = listOf(
            CartItem(
                productId = 10,
                barcode = "7790010001",
                name = "Alfajor Havanna 70% Cacao",
                unitPrice = 1800.0,
                unitCost = 1150.0,
                quantity = 3,
                stockAvailable = initialStock
            )
        )

        val result = repository.recordSale(
            branchId = 1,
            userId = 3,
            userName = "Lucía Fernández",
            cartItems = cartItems,
            paymentMethod = PaymentMethod.CASH
        )

        assertTrue("La venta debe confirmarse con éxito", result.isSuccess)
        val sale = result.getOrNull()
        assertNotNull(sale)
        assertEquals(5400.0, sale!!.totalAmount, 0.01)

        // 3. Verificar que el inventario se descontó exactamente a 17 (20 - 3)
        val updatedStock = db.inventoryDao().getBranchInventoryItem(1, 10)?.quantityOnHand
        assertEquals(17, updatedStock)
    }

    @Test
    fun `test sale fails if inventory is insufficient and does not produce negative stock`() = runBlocking {
        val branch = BranchEntity(1, "Sucursal Centro", "K01", "Corrientes 1240", "1143712210")
        db.branchDao().insertBranches(listOf(branch))

        val product = ProductEntity(
            id = 11,
            barcode = "7790020001",
            sku = "BEB-COCA-500",
            name = "Coca-Cola 500ml",
            description = "Bebida",
            categoryId = 2,
            costPrice = 980.0,
            sellingPrice = 1600.0
        )
        db.productDao().insertProducts(listOf(product))

        db.inventoryDao().insertOrUpdateInventory(
            BranchInventoryEntity(id = 2, branchId = 1, productId = 11, quantityOnHand = 2)
        )

        db.cashDao().insertSession(
            CashSessionEntity(id = 2, branchId = 1, userId = 3, openingAmount = 10000.0, status = ShiftStatus.OPEN)
        )

        // Intentar vender 5 cuando solo hay 2
        val cartItems = listOf(
            CartItem(
                productId = 11,
                barcode = "7790020001",
                name = "Coca-Cola 500ml",
                unitPrice = 1600.0,
                unitCost = 980.0,
                quantity = 5,
                stockAvailable = 2
            )
        )

        val result = repository.recordSale(
            branchId = 1,
            userId = 3,
            userName = "Lucía Fernández",
            cartItems = cartItems,
            paymentMethod = PaymentMethod.CASH
        )

        assertTrue("La venta debe fallar por stock insuficiente", result.isFailure)
        val stockRemains = db.inventoryDao().getBranchInventoryItem(1, 11)?.quantityOnHand
        assertEquals("El stock debe mantenerse en 2", 2, stockRemains)
    }

    @Test
    fun `test user roles hierarchy and permission levels`() {
        assertEquals(1, UserRole.EMPLOYEE.level)
        assertEquals(2, UserRole.BRANCH_MANAGER.level)
        assertEquals(3, UserRole.OWNER.level)
    }

    @Test
    fun `test void sale atomically rolls back inventory and sets status to VOIDED`() = runBlocking {
        val branch = BranchEntity(1, "Sucursal Centro", "K01", "Corrientes 1240", "1143712210")
        db.branchDao().insertBranches(listOf(branch))

        val product = ProductEntity(
            id = 20,
            barcode = "7790030001",
            sku = "GOL-BELD-01",
            name = "Chicles Beldent Menta",
            description = "Golosina",
            categoryId = 3,
            costPrice = 400.0,
            sellingPrice = 700.0
        )
        db.productDao().insertProducts(listOf(product))

        val initialStock = 15
        db.inventoryDao().insertOrUpdateInventory(
            BranchInventoryEntity(id = 20, branchId = 1, productId = 20, quantityOnHand = initialStock)
        )

        db.cashDao().insertSession(
            CashSessionEntity(id = 5, branchId = 1, userId = 3, openingAmount = 15000.0, status = ShiftStatus.OPEN)
        )

        // 1. Vender 2 unidades
        val cartItems = listOf(
            CartItem(
                productId = 20,
                barcode = "7790030001",
                name = "Chicles Beldent Menta",
                unitPrice = 700.0,
                unitCost = 400.0,
                quantity = 2,
                stockAvailable = initialStock
            )
        )

        val saleRes = repository.recordSale(1, 3, "Lucía", cartItems, PaymentMethod.CASH)
        assertTrue(saleRes.isSuccess)
        val sale = saleRes.getOrNull()!!
        assertEquals(13, db.inventoryDao().getBranchInventoryItem(1, 20)?.quantityOnHand)

        // 2. Anular la venta
        val voidRes = repository.voidSale(sale.id, "Error de tipeo de cliente", 1, "Roberto Dueño")
        assertTrue("La anulación debe completarse", voidRes.isSuccess)

        // 3. Verificar que el stock volvió a 15 y el status es VOIDED
        val restoredStock = db.inventoryDao().getBranchInventoryItem(1, 20)?.quantityOnHand
        assertEquals(15, restoredStock)
        val updatedSale = db.saleDao().getSaleById(sale.id)
        assertEquals("VOIDED", updatedSale?.status)
    }

    @Test
    fun `test goods receipt increases inventory and updates cost price`() = runBlocking {
        val branch = BranchEntity(1, "Sucursal Centro", "K01", "Corrientes 1240", "1143712210")
        db.branchDao().insertBranches(listOf(branch))

        val product = ProductEntity(
            id = 30,
            barcode = "7790040001",
            sku = "CIG-MARL-20",
            name = "Cigarrillos Marlboro",
            description = "Cigarrillos",
            categoryId = 4,
            costPrice = 3000.0,
            sellingPrice = 3800.0
        )
        db.productDao().insertProducts(listOf(product))

        db.inventoryDao().insertOrUpdateInventory(
            BranchInventoryEntity(id = 30, branchId = 1, productId = 30, quantityOnHand = 10)
        )

        val res = repository.receiveGoodsFromSupplier(
            branchId = 1,
            supplierId = 3,
            productId = 30,
            quantity = 20,
            unitCost = 3200.0,
            invoiceRef = "REM-999",
            userId = 1,
            userName = "Roberto"
        )

        assertTrue(res.isSuccess)
        val newStock = db.inventoryDao().getBranchInventoryItem(1, 30)?.quantityOnHand
        assertEquals(30, newStock) // 10 + 20

        val updatedProd = db.productDao().getProductById(30)
        assertEquals(3200.0, updatedProd?.costPrice)
    }

    @Test
    fun `test sale with customer account fiado records correctly and decrements inventory`() = runBlocking {
        val branch = BranchEntity(2, "Sucursal Palermo", "K02", "Honduras 4800", "1148325511")
        db.branchDao().insertBranches(listOf(branch))

        val product = ProductEntity(
            id = 40,
            barcode = "7790080001",
            sku = "ENE-REDB-250",
            name = "Red Bull 250ml",
            description = "Energizante",
            categoryId = 2,
            costPrice = 1400.0,
            sellingPrice = 2400.0
        )
        db.productDao().insertProducts(listOf(product))

        db.inventoryDao().insertOrUpdateInventory(
            BranchInventoryEntity(id = 40, branchId = 2, productId = 40, quantityOnHand = 15)
        )

        db.cashDao().insertSession(
            CashSessionEntity(id = 10, branchId = 2, userId = 2, openingAmount = 20000.0, status = ShiftStatus.OPEN)
        )

        val cart = listOf(
            CartItem(
                productId = 40,
                barcode = "7790080001",
                name = "Red Bull 250ml",
                unitPrice = 2400.0,
                unitCost = 1400.0,
                quantity = 2,
                stockAvailable = 15
            )
        )

        val result = repository.recordSale(
            branchId = 2,
            userId = 2,
            userName = "Carlos Benítez",
            cartItems = cart,
            paymentMethod = PaymentMethod.ACCOUNT
        )

        assertTrue("La venta en cuenta corriente debe registrarse exitosamente", result.isSuccess)
        val sale = result.getOrNull()!!
        assertEquals(PaymentMethod.ACCOUNT, sale.paymentMethod)
        assertEquals(4800.0, sale.totalAmount, 0.01)

        val stock = db.inventoryDao().getBranchInventoryItem(2, 40)?.quantityOnHand
        assertEquals(13, stock)
    }

    @Test
    fun `test blind cash closing preserves branch opening amount, subtracts drawer expenses, and applies persisted tolerance`() = runBlocking {
        val branch = BranchEntity(4, "Sucursal 04 - Recoleta", "K04", "Av. Las Heras 2310", "1148039088")
        db.branchDao().insertBranches(listOf(branch))

        val product = ProductEntity(
            id = 50,
            barcode = "7790090001",
            sku = "ALF-JOR-01",
            name = "Alfajor Jorgito",
            description = "Alfajor",
            categoryId = 1,
            costPrice = 480.0,
            sellingPrice = 1200.0
        )
        db.productDao().insertProducts(listOf(product))
        db.inventoryDao().insertOrUpdateInventory(
            BranchInventoryEntity(id = 50, branchId = 4, productId = 50, quantityOnHand = 30)
        )

        // Configurar tolerancia de empresa a $300
        repository.updateCompanySettings(
            CompanySettingsEntity(cashAuditTolerance = 300.0),
            userId = 1,
            userName = "Roberto Dueño"
        )

        // Abrir turno en Sucursal 4 con $22.000 (distinto de K01 y $15.000)
        val openRes = repository.openCashSession(
            branchId = 4,
            userId = 8,
            userName = "Encargado Recoleta",
            openingAmount = 22000.0,
            notes = "Apertura Recoleta"
        )
        assertTrue(openRes.isSuccess)
        val sessionId = openRes.getOrNull()!!.id

        // Venta en efectivo por $3.600
        val saleRes = repository.recordSale(
            branchId = 4,
            userId = 8,
            userName = "Encargado Recoleta",
            cartItems = listOf(CartItem(50, "7790090001", "Alfajor Jorgito", 1200.0, 480.0, 3, 30)),
            paymentMethod = PaymentMethod.CASH
        )
        assertTrue(saleRes.isSuccess)

        // Gasto pagado desde gaveta por $1.200 y retiro parcial por $4.000
        repository.registerExpense(4, ExpenseCategory.VARIOS, 1200.0, "Bolsas", "CASH_DRAWER", 8, "Encargado Recoleta")
        repository.registerCashDrop(sessionId, 4, 4000.0, "Buzón", 8, "Encargado Recoleta")

        // Esperado = 22000 + 3600 - 1200 - 4000 = 20400. Contado = 20000 -> diferencia -400 (supera tolerancia $300)
        val closeRes = repository.closeCashSessionWithBlindCount(
            sessionId = sessionId,
            countedAmount = 20000.0,
            userId = 8,
            userName = "Encargado Recoleta",
            notes = "Cierre nocturno"
        )
        assertTrue(closeRes.isSuccess)
        val closed = closeRes.getOrNull()!!
        assertEquals(4L, closed.branchId)
        assertEquals(22000.0, closed.openingAmount, 0.01)
        assertEquals(20400.0, closed.expectedCashAmount, 0.01)
        assertEquals(-400.0, closed.cashDifference, 0.01)
        assertEquals(DifferenceStatus.PENDING_APPROVAL, closed.differenceStatus)
    }

    @Test
    fun `test voiding open cash sale does not double subtract from expected cash`() = runBlocking {
        val branch = BranchEntity(3, "Sucursal 03 - Belgrano", "K03", "Cabildo 2180", "1147841133")
        db.branchDao().insertBranches(listOf(branch))
        val product = ProductEntity(60, "7790090002", "BEB-SPR-500", "Sprite 500ml", "Bebida", 2, 900.0, 1500.0)
        db.productDao().insertProducts(listOf(product))
        db.inventoryDao().insertOrUpdateInventory(BranchInventoryEntity(id = 60, branchId = 3, productId = 60, quantityOnHand = 10))

        val session = repository.openCashSession(3, 7, "Cajero K03", 10000.0, "Turno").getOrNull()!!
        val sale = repository.recordSale(
            branchId = 3,
            userId = 7,
            userName = "Cajero K03",
            cartItems = listOf(CartItem(60, "7790090002", "Sprite 500ml", 1500.0, 900.0, 2, 10)),
            paymentMethod = PaymentMethod.CASH
        ).getOrNull()!!

        val voidRes = repository.voidSale(sale.id, "Cliente canceló", 7, "Cajero K03")
        assertTrue(voidRes.isSuccess)

        val closed = repository.closeCashSessionWithBlindCount(session.id, 10000.0, 7, "Cajero K03", "Ok").getOrNull()!!
        assertEquals(10000.0, closed.expectedCashAmount, 0.01)
        assertEquals(0.0, closed.cashDifference, 0.01)
    }

    @Test
    fun `test persistent batch creation on receipt, FEFO sale deduction, and expired write-off`() = runBlocking {
        val branch = BranchEntity(1, "Sucursal Centro", "K01", "Corrientes 1240", "1143712210")
        db.branchDao().insertBranches(listOf(branch))
        val product = ProductEntity(70, "7790090003", "GAL-CHO-250", "Chocolinas 250g", "Galletitas", 5, 1000.0, 1750.0)
        db.productDao().insertProducts(listOf(product))
        db.inventoryDao().insertOrUpdateInventory(BranchInventoryEntity(id = 70, branchId = 1, productId = 70, quantityOnHand = 0))
        repository.openCashSession(1, 3, "Lucía", 15000.0, "Turno")

        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        // Ingresar lote próximo a vencer (5 días) y lote largo (60 días)
        repository.receiveGoodsFromSupplier(1, 1, 70, 10, 1000.0, "REM-1", 1, "Roberto", "LOT-CORTO", now + 5 * dayMs)
        repository.receiveGoodsFromSupplier(1, 1, 70, 10, 1050.0, "REM-2", 1, "Roberto", "LOT-LARGO", now + 60 * dayMs)

        // Vender 4 unidades -> debe descontar primero del lote que vence antes (LOT-CORTO: 10 -> 6)
        repository.recordSale(
            branchId = 1,
            userId = 3,
            userName = "Lucía",
            cartItems = listOf(CartItem(70, "7790090003", "Chocolinas 250g", 1750.0, 1050.0, 4, 20)),
            paymentMethod = PaymentMethod.MP_QR
        )

        val batchesAfterSale = db.batchDao().getActiveBatchesForProduct(1, 70)
        assertEquals(2, batchesAfterSale.size)
        assertEquals("LOT-CORTO", batchesAfterSale[0].batchNumber)
        assertEquals(6, batchesAfterSale[0].remainingQuantity)
        assertEquals(10, batchesAfterSale[1].remainingQuantity)

        // Dar de baja 2 unidades vencidas del lote corto
        val writeOffRes = repository.writeOffExpiredBatch(batchesAfterSale[0].id, 2, "Vencidas", 1, "Roberto")
        assertTrue(writeOffRes.isSuccess)

        val batchesAfterWriteOff = db.batchDao().getActiveBatchesForProduct(1, 70)
        assertEquals(4, batchesAfterWriteOff[0].remainingQuantity)
        assertEquals(14, db.inventoryDao().getBranchInventoryItem(1, 70)?.quantityOnHand)

        val expiryUiItems = repository.getBranchBatchExpiryItems(1).first()
        assertEquals(2, expiryUiItems.size)
        assertTrue(expiryUiItems.first().isCritical)
    }

    @Test
    fun `test PinHasher produces salted 64-char SHA-256 hash and enforces username salt`() {
        val hashA = PinHasher.hash("roberto", "1234")
        val hashB = PinHasher.hash("carlos", "1234")

        assertTrue(PinHasher.isHashed(hashA))
        assertEquals(64, hashA.length)
        assertNotEquals("Salted hashes must differ across usernames", hashA, hashB)

        assertTrue(PinHasher.verify("roberto", "1234", hashA))
        assertFalse(PinHasher.verify("carlos", "1234", hashA))
        assertFalse(PinHasher.verify("roberto", "9999", hashA))

        // Compatibilidad con registros legacy antes de migración
        assertTrue(PinHasher.verify("legacy", "4321", "4321"))
        assertFalse(PinHasher.verify("legacy", "0000", "4321"))
    }

    @Test
    fun `test getUserByPin authenticates hashed users and recordSecurityAudit logs auth events`() = runBlocking {
        val owner = UserProfileEntity(
            id = 1,
            username = "roberto.dueno",
            fullName = "Roberto Dueño",
            pinCode = PinHasher.hash("roberto.dueno", "1234"),
            role = UserRole.OWNER,
            assignedBranchId = 1L
        )
        val cashier = UserProfileEntity(
            id = 3,
            username = "lucia.k01",
            fullName = "Lucía Cajera",
            pinCode = PinHasher.hash("lucia.k01", "3456"),
            role = UserRole.EMPLOYEE,
            assignedBranchId = 1L
        )
        db.userProfileDao().insertUsers(listOf(owner, cashier))

        val authOwner = repository.getUserByPin("1234")
        assertNotNull(authOwner)
        assertEquals("roberto.dueno", authOwner?.username)

        val authCashier = repository.getUserByPin("3456")
        assertNotNull(authCashier)
        assertEquals(UserRole.EMPLOYEE, authCashier?.role)

        assertNull(repository.getUserByPin("9999"))

        repository.recordSecurityAudit(
            userId = authOwner!!.id,
            userName = authOwner.fullName,
            branchId = 1L,
            action = "AUTH_LOGIN_SUCCESS",
            details = "Inicio de sesión verificado con hash SHA-256."
        )

        val logs = repository.getAuditLogs().first()
        assertEquals(1, logs.size)
        assertEquals("AUTH_LOGIN_SUCCESS", logs.first().action)
        assertEquals("AUTH", logs.first().entityType)
    }

    @Test
    fun `test scanned barcode lookup resolves product and stock intake increases on-hand quantity`() = runBlocking {
        val branch = BranchEntity(1, "Sucursal Centro", "K01", "Corrientes 1240", "1143712210")
        db.branchDao().insertBranches(listOf(branch))

        val product = ProductEntity(
            id = 88,
            barcode = "7790099887",
            sku = "DRK-BULL-01",
            name = "Drakos Energy 473ml",
            description = "Energizante",
            categoryId = 2,
            costPrice = 1200.0,
            sellingPrice = 2100.0
        )
        db.productDao().insertProducts(listOf(product))
        db.inventoryDao().insertOrUpdateInventory(
            BranchInventoryEntity(id = 88, branchId = 1, productId = 88, quantityOnHand = 0)
        )

        val scannedRaw = "  7790099887  "
        val branchCatalog = repository.getProductsWithStock(1L).first()
        val matched = branchCatalog.find { it.barcode.equals(scannedRaw.trim(), ignoreCase = true) }
        assertNotNull("El producto escaneado debe encontrarse en el catálogo", matched)
        assertEquals(0, matched!!.quantityOnHand)

        val receiptResult = repository.receiveGoodsFromSupplier(
            branchId = 1L,
            supplierId = 1L,
            productId = matched.productId,
            quantity = 24,
            unitCost = 1250.0,
            invoiceRef = "SCAN-001",
            userId = 1L,
            userName = "Operador Drakos",
            batchNumber = "LOTE-SCAN-24",
            expiryDateMillis = null
        )
        assertTrue("El ingreso por escaneo debe completarse con éxito", receiptResult.isSuccess)

        val updatedStock = db.inventoryDao().getBranchInventoryItem(1L, matched.productId)?.quantityOnHand
        assertEquals(24, updatedStock)

        val updatedProd = db.productDao().getProductByBarcode("7790099887")
        assertNotNull(updatedProd)
        assertEquals(1250.0, updatedProd!!.costPrice, 0.01)
    }

    @Test
    fun `test level 3 device whitelist pairing, staff access management, and strict blocking`() = runBlocking {
        // Bootstrap primer equipo central autorizado automáticamente
        val central = repository.ensureDeviceRegisteredOnStartup("DRK-CENTRAL1", "Tablet Caja Central")
        assertEquals("AUTHORIZED", central.status)
        assertTrue(repository.isDeviceAllowedForStaff("DRK-CENTRAL1"))

        // Segundo dispositivo externo entra como PENDING y queda bloqueado para Nivel 1/2
        val external = repository.ensureDeviceRegisteredOnStartup("DRK-EXT99999", "Samsung A54 Externo")
        assertEquals("PENDING", external.status)
        assertFalse(repository.isDeviceAllowedForStaff("DRK-EXT99999"))

        // Dueño (Nivel 3) genera token de vinculación y el teléfono externo lo canjea
        val tokenRes = repository.createPairingInvite("Teléfono Sucursal Palermo", 2L, 1L, "Roberto Dueño")
        assertTrue(tokenRes.isSuccess)
        val token = tokenRes.getOrNull()!!

        val pairRes = repository.redeemPairingCodeForDevice("DRK-EXT99999", "Samsung A54 Externo", token)
        assertTrue(pairRes.isSuccess)
        assertEquals(2L, pairRes.getOrNull())
        assertTrue(repository.isDeviceAllowedForStaff("DRK-EXT99999"))

        // Dueño revoca el teléfono y vuelve a bloquearse
        val revokeRes = repository.revokeDeviceByOwner("DRK-EXT99999", 1L, "Roberto Dueño")
        assertTrue(revokeRes.isSuccess)
        assertFalse(repository.isDeviceAllowedForStaff("DRK-EXT99999"))

        // Alta y edición de accesos Nivel 1 y Nivel 2 desde Nivel 3
        val createRes = repository.createStaffUser("marta.k02", "Marta Gómez", "5678", UserRole.EMPLOYEE, 2L, 1L, "Roberto Dueño")
        assertTrue(createRes.isSuccess)
        val staffUser = repository.getUserByPin("5678")
        assertNotNull(staffUser)
        assertEquals(UserRole.EMPLOYEE, staffUser?.role)

        val updateRes = repository.updateStaffAccess(staffUser!!.id, UserRole.BRANCH_MANAGER, 2L, true, "8765", 1L, "Roberto Dueño")
        assertTrue(updateRes.isSuccess)
        val promoted = repository.getUserByPin("8765")
        assertNotNull(promoted)
        assertEquals(UserRole.BRANCH_MANAGER, promoted?.role)
    }

    @Test
    fun `test financial bar chart analytics, Excel stock alerts, and weekly order replenishment`() {
        val now = 1_760_000_000_000L
        val dayMs = 86_400_000L

        val sales = listOf(
            SaleEntity(1L, 1L, 1L, 1L, "T-001", 4000.0, 0.0, 840.0, 4840.0, PaymentMethod.CASH, "COMPLETED", idempotencyKey = "k-1", createdAt = now - 2 * dayMs),
            SaleEntity(2L, 2L, 1L, 2L, "T-002", 2000.0, 0.0, 420.0, 2420.0, PaymentMethod.MP_QR, "COMPLETED", idempotencyKey = "k-2", createdAt = now - 2 * dayMs),
            SaleEntity(3L, 1L, 1L, 1L, "T-003", 1000.0, 0.0, 210.0, 1210.0, PaymentMethod.CASH, "VOIDED", idempotencyKey = "k-3", createdAt = now - dayMs)
        )
        val analytics = listOf(
            SaleItemAnalytics(1L, 10L, 4, 1210.0, 600.0, 4840.0, now - 2 * dayMs),
            SaleItemAnalytics(2L, 10L, 2, 1210.0, 600.0, 2420.0, now - 2 * dayMs)
        )

        // Cadena completa vs Filtro Sucursal 1
        val chainDaily = DrakosAnalytics.buildDailySalesBars(sales, analytics, filterBranchId = null, days = 5, nowMillis = now)
        assertEquals(5, chainDaily.size)
        assertEquals(7260.0, chainDaily.sumOf { it.revenueAmount }, 0.01)

        val branch1Daily = DrakosAnalytics.buildDailySalesBars(sales, analytics, filterBranchId = 1L, days = 5, nowMillis = now)
        assertEquals(4840.0, branch1Daily.sumOf { it.revenueAmount }, 0.01)
        assertEquals(2400.0, branch1Daily.sumOf { it.costAmount }, 0.01)

        val branch1Monthly = DrakosAnalytics.buildMonthlySalesBars(sales, analytics, filterBranchId = 1L, months = 3, nowMillis = now)
        assertEquals(3, branch1Monthly.size)
        assertEquals(4840.0, branch1Monthly.sumOf { it.revenueAmount }, 0.01)

        // Grilla Excel con alerta de bajo mínimo y vencimiento próximo + Pedido Semanal
        val cat = CategoryEntity(1L, "Bebidas", "")
        val prodLow = ProductEntity(10L, "7791", "SKU-1", "Coca 500", "", 1L, 900.0, 1600.0, minStock = 10)
        val inv = BranchInventoryEntity(1L, 1L, 10L, quantityOnHand = 4)
        val batchNearExpiry = ProductBatchEntity(1L, 1L, 10L, "LOT-1", now + 5 * dayMs, 4, 4)

        val excelRows = DrakosAnalytics.buildExcelStockRows(listOf(prodLow), listOf(cat), listOf(inv), listOf(batchNearExpiry), analytics, 1L, now)
        assertEquals(1, excelRows.size)
        assertEquals(StockHealthStatus.CRITICAL, excelRows.first().healthStatus)

        val suggestions = DrakosAnalytics.buildWeeklyOrderSuggestions(listOf(prodLow), listOf(cat), listOf(inv), listOf(batchNearExpiry), analytics, 1L, coverageDays = 7, nowMillis = now)
        assertEquals(1, suggestions.size)
        assertTrue(suggestions.first().suggestedQty >= 10)
    }

    @Test
    fun `test supplier invoice workflow, weekly orders persistence, and inter-branch messages`() = runBlocking {
        val invId = repository.registerSupplierInvoice(
            branchId = 2L,
            supplierId = 1L,
            supplierName = "Arcor",
            invoiceType = "FACTURA_A",
            invoiceNumber = "0001-00088123",
            totalAmount = 145000.0,
            paymentStatus = "PENDING",
            dueDays = 15,
            notes = "Reposición golosinas",
            userId = 2L,
            userName = "Carlos Encargado"
        ).getOrNull()!!

        val invBefore = repository.getSupplierInvoices(2L).first().first()
        assertEquals("PENDING", invBefore.paymentStatus)

        assertTrue(repository.markSupplierInvoicePaid(invId, 2L, "Carlos Encargado").isSuccess)
        val invAfter = repository.getSupplierInvoices(2L).first().first()
        assertEquals("PAID", invAfter.paymentStatus)

        val orderRes = repository.saveWeeklyOrder(
            branchId = 2L,
            supplierId = 1L,
            supplierName = "Arcor",
            totalEstimatedCost = 62000.0,
            totalUnits = 48,
            itemsSummary = "Chocolinas x24, Bon o Bon x24",
            notes = "Pedido semanal",
            userId = 2L,
            userName = "Carlos Encargado"
        )
        assertTrue(orderRes.isSuccess)
        val orders = repository.getWeeklyOrders(2L).first()
        assertEquals(1, orders.size)
        assertEquals("SENT", orders.first().status)

        val msgRes = repository.sendBranchMessage(
            fromBranchId = 2L,
            toBranchId = 1L,
            senderUserId = 2L,
            senderName = "Carlos Encargado",
            senderRoleLevel = 2,
            priority = "STOCK_REQUEST",
            subject = "Préstamo de Red Bull",
            message = "¿Tienen 12 latas disponibles para transferir hoy?"
        )
        assertTrue(msgRes.isSuccess)
        val inboxBranch1 = repository.getMessagesForBranch(1L).first()
        assertEquals(1, inboxBranch1.size)
        assertFalse(inboxBranch1.first().isRead)
        repository.markMessageAsRead(inboxBranch1.first().id)
        assertTrue(repository.getMessagesForBranch(1L).first().first().isRead)
    }

    @Test
    fun `test bulk price rounding, preview, transactional update, and expiry clearance markdown`() = runBlocking {
        val branch = BranchEntity(1, "Sucursal Centro", "K01", "Corrientes 1240", "1143712210")
        db.branchDao().insertBranches(listOf(branch))
        db.productDao().insertCategories(
            listOf(
                CategoryEntity(1L, "Bebidas", ""),
                CategoryEntity(2L, "Golosinas", "")
            )
        )
        val prodBebida = ProductEntity(100L, "779100", "SKU-BEB", "Coca 500", "", 1L, costPrice = 1000.0, sellingPrice = 1600.0)
        val prodGolosina = ProductEntity(101L, "779101", "SKU-GOL", "Alfajor", "", 2L, costPrice = 500.0, sellingPrice = 900.0)
        db.productDao().insertProducts(listOf(prodBebida, prodGolosina))
        db.inventoryDao().insertOrUpdateInventory(BranchInventoryEntity(100L, 1L, 100L, quantityOnHand = 20))

        // Redondeo psicológico a $50 y $100
        assertEquals(1850.0, DrakosAnalytics.roundPriceToStep(1842.0, PriceRoundingStep.FIFTY), 0.01)
        assertEquals(1900.0, DrakosAnalytics.roundPriceToStep(1860.0, PriceRoundingStep.HUNDRED), 0.01)

        val preview = DrakosAnalytics.buildBulkPricePreview(
            products = listOf(prodBebida, prodGolosina),
            categoryId = 1L,
            costPercentDelta = 15.0,
            pricePercentDelta = 15.0,
            preserveMargin = true,
            roundingStep = PriceRoundingStep.FIFTY
        )
        assertEquals(1, preview.size)
        assertEquals(1150.0, preview.first().newCostPrice, 0.01)
        assertEquals(1850.0, preview.first().newSellingPrice, 0.01)

        val bulkRes = repository.applyBulkPriceUpdate(
            categoryId = 1L,
            costPercentDelta = 15.0,
            pricePercentDelta = 15.0,
            preserveMargin = true,
            roundingStep = PriceRoundingStep.FIFTY,
            userId = 2L,
            userName = "Encargado K01"
        )
        assertTrue(bulkRes.isSuccess)
        assertEquals(1, bulkRes.getOrNull())
        assertEquals(1850.0, db.productDao().getProductById(100L)!!.sellingPrice, 0.01)
        assertEquals(900.0, db.productDao().getProductById(101L)!!.sellingPrice, 0.01)

        // Liquidación -25% sobre lote próximo a vencer (1850 * 0.75 = 1387.5 -> redondeado a $10 = 1390)
        val batchRes = repository.applyBatchClearanceMarkdown(
            productId = 100L,
            branchId = 1L,
            batchNumber = "LOT-VENC-01",
            discountPercent = 25.0,
            userId = 2L,
            userName = "Encargado K01"
        )
        assertTrue(batchRes.isSuccess)
        assertEquals(1390.0, batchRes.getOrNull()!!, 0.01)
        assertEquals(1390.0, db.productDao().getProductById(100L)!!.sellingPrice, 0.01)

        val auditActions = repository.getAuditLogs().first().map { it.action }
        assertTrue(auditActions.contains("BULK_PRICE_UPDATE"))
        assertTrue(auditActions.contains("EXPIRY_CLEARANCE_MARKDOWN"))
    }

    @Test
    fun `test 24h hourly heatmap, ABC profitability matrix, and cashier variance summary`() {
        val peakHourMs = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 19)
            set(java.util.Calendar.MINUTE, 30)
        }.timeInMillis
        val offPeakMs = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 3)
            set(java.util.Calendar.MINUTE, 15)
        }.timeInMillis

        val sales = listOf(
            SaleEntity(1L, 1L, 1L, 1L, "T-10", 10000.0, 0.0, 0.0, 10000.0, PaymentMethod.CASH, "COMPLETED", idempotencyKey = "h1", createdAt = peakHourMs),
            SaleEntity(2L, 1L, 1L, 1L, "T-11", 5000.0, 0.0, 0.0, 5000.0, PaymentMethod.MP_QR, "COMPLETED", idempotencyKey = "h2", createdAt = peakHourMs),
            SaleEntity(3L, 1L, 1L, 1L, "T-12", 2000.0, 0.0, 0.0, 2000.0, PaymentMethod.CASH, "COMPLETED", idempotencyKey = "h3", createdAt = offPeakMs)
        )
        val heatmap = DrakosAnalytics.buildHourlyHeatmap(sales, filterBranchId = 1L)
        assertEquals(6, heatmap.size)
        val slot18to22 = heatmap.first { it.slotLabel == "18-22h" }
        assertTrue(slot18to22.isPeak)
        assertEquals(2, slot18to22.ticketCount)
        assertEquals(15000.0, slot18to22.revenueAmount, 0.01)

        val cats = listOf(
            CategoryEntity(1L, "Energizantes", ""),
            CategoryEntity(2L, "Cigarrillos", "")
        )
        val prods = listOf(
            ProductEntity(1L, "111", "S1", "Monster", "", 1L, 1000.0, 2200.0),
            ProductEntity(2L, "222", "S2", "Marlboro", "", 2L, 3500.0, 3800.0)
        )
        val items = listOf(
            SaleItemAnalytics(1L, 1L, 10, 2200.0, 1000.0, 22000.0, peakHourMs),
            SaleItemAnalytics(1L, 2L, 10, 3800.0, 3500.0, 38000.0, peakHourMs)
        )
        val abc = DrakosAnalytics.buildCategoryProfitability(items, prods, cats, sales, filterBranchId = 1L)
        assertEquals(2, abc.size)
        assertEquals("A", abc.first().abcClass)
        val cig = abc.first { it.categoryName == "Cigarrillos" }
        assertEquals("Volumen / Tráfico", cig.strategicTag)

        val users = listOf(
            UserProfileEntity(10L, "cajero.uno", "Marcos Cajero", "hash", UserRole.EMPLOYEE, 1L)
        )
        val sessions = listOf(
            CashSessionEntity(
                id = 1L,
                branchId = 1L,
                userId = 10L,
                openingAmount = 15000.0,
                expectedCashAmount = 20000.0,
                countedCashAmount = 19500.0,
                cashDifference = -500.0,
                status = ShiftStatus.CLOSED,
                differenceStatus = DifferenceStatus.PENDING_APPROVAL
            ),
            CashSessionEntity(
                id = 2L,
                branchId = 1L,
                userId = 10L,
                openingAmount = 15000.0,
                expectedCashAmount = 18000.0,
                countedCashAmount = 18000.0,
                cashDifference = 0.0,
                status = ShiftStatus.CLOSED,
                differenceStatus = DifferenceStatus.NORMAL
            )
        )
        val variance = DrakosAnalytics.buildCashierVarianceSummary(sessions, users, filterBranchId = 1L)
        assertEquals(1, variance.size)
        assertEquals("Marcos Cajero", variance.first().cashierName)
        assertEquals(-500.0, variance.first().netDifference, 0.01)
        assertEquals(1, variance.first().pendingApprovals)
    }

    @Test
    fun `test portable provisioning bundle transfers custom Level 3 PINs to a second phone database`() = runBlocking {
        // Teléfono 1 (Dueño Nivel 3 crea un usuario Nivel 1 con PIN 7410)
        val createRes = repository.createStaffUser(
            username = "sofia.k04",
            fullName = "Sofía Recoleta",
            rawPin = "7410",
            role = UserRole.EMPLOYEE,
            assignedBranchId = 4L,
            ownerUserId = 1L,
            ownerUserName = "Roberto Dueño"
        )
        assertTrue(createRes.isSuccess)

        val portableToken = repository.generatePortableProvisioningBundle(
            pairingCode = "DRK-9012",
            assignedBranchId = 4L,
            deviceAlias = "Moto Edge Recoleta",
            ownerUserName = "Roberto Dueño"
        )
        assertTrue(com.example.domain.security.DeviceProvisioningCodec.isPortableBundle(portableToken))

        // Teléfono 2 (BD completamente nueva y aislada, sin Google Login del dueño)
        val phone2Db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KioscoDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            val phone2Repo = KioscoRepository(phone2Db)
            // Antes de vincular, el PIN 7410 no existe en el teléfono 2
            assertNull(phone2Repo.getUserByPin("7410"))

            val decoded = com.example.domain.security.DeviceProvisioningCodec.decode(portableToken).getOrThrow()
            val applyRes = phone2Repo.applyPortableProvisioningBundle(
                bundle = decoded,
                currentDeviceId = "DRK-PHONE2-EXT",
                currentDeviceName = "Motorola Recoleta"
            )
            assertTrue(applyRes.isSuccess)
            val (branchId, importedCount) = applyRes.getOrThrow()
            assertEquals(4L, branchId)
            assertTrue(importedCount >= 1)

            // Ahora el teléfono 2 inicia sesión con el PIN 7410 y queda autorizado en K04
            val syncedUser = phone2Repo.getUserByPin("7410")
            assertNotNull(syncedUser)
            assertEquals("sofia.k04", syncedUser?.username)
            assertEquals(UserRole.EMPLOYEE, syncedUser?.role)
            assertEquals(4L, syncedUser?.assignedBranchId)
            assertTrue(phone2Repo.isDeviceAllowedForStaff("DRK-PHONE2-EXT"))
        } finally {
            phone2Db.close()
        }
    }

    @Test
    fun `test portable provisioning codec extracts token from wrapped WhatsApp message and rejects duplicate PINs`() = runBlocking {
        val uniqueAttempt = repository.createStaffUser(
            username = "lucas.k05",
            fullName = "Lucas Caballito",
            rawPin = "8520",
            role = UserRole.BRANCH_MANAGER,
            assignedBranchId = 5L,
            ownerUserId = 1L,
            ownerUserName = "Roberto Dueño"
        )
        assertTrue(uniqueAttempt.isSuccess)

        val dupAttempt = repository.createStaffUser(
            username = "duplicado.pin",
            fullName = "Usuario Duplicado",
            rawPin = "8520",
            role = UserRole.EMPLOYEE,
            assignedBranchId = 1L,
            ownerUserId = 1L,
            ownerUserName = "Roberto Dueño"
        )
        assertTrue(dupAttempt.isFailure)

        val rawToken = repository.generatePortableProvisioningBundle(
            pairingCode = "DRK-5520",
            assignedBranchId = 5L,
            deviceAlias = "Samsung A55 K05",
            ownerUserName = "Roberto Dueño"
        )
        val wrappedWhatsApp = "Hola equipo! Pegar esto en Drakos:\n  ${rawToken.chunked(40).joinToString("\n")}  \nGracias!"
        assertTrue(com.example.domain.security.DeviceProvisioningCodec.isPortableBundle(wrappedWhatsApp))
        val decoded = com.example.domain.security.DeviceProvisioningCodec.decode(wrappedWhatsApp).getOrThrow()
        assertEquals(5L, decoded.assignedBranchId)
        assertTrue(decoded.profiles.any { it.username == "lucas.k05" })
    }

    @Test
    fun `test drakos promotions save, toggle active, delete, and audit logging`() = runBlocking {
        // Validación de campos requeridos
        val invalidBlank = repository.savePromotion(
            title = " ",
            badge = "COMBO",
            description = "",
            discountPercent = 10.0,
            fixedPromoPrice = null,
            itemsSpec = "1:2",
            validSchedule = "24hs",
            isActive = true,
            userId = 1L,
            userName = "Roberto Dueño",
            branchId = 1L
        )
        assertTrue(invalidBlank.isFailure)

        // Creación de combo con precio fijo
        val createRes = repository.savePromotion(
            title = "Dúo Nocturno Drakos",
            badge = "NOCHE",
            description = "2 Energizantes + Alfajor",
            discountPercent = 0.0,
            fixedPromoPrice = 4200.0,
            itemsSpec = "1:2,2:1",
            validSchedule = "22:00 a 06:00",
            isActive = true,
            userId = 2L,
            userName = "Carlos Encargado",
            branchId = 1L
        )
        assertTrue(createRes.isSuccess)
        val promoId = createRes.getOrNull()!!

        val allAfterCreate = repository.getAllPromotions().first()
        assertEquals(1, allAfterCreate.size)
        assertEquals("Dúo Nocturno Drakos", allAfterCreate.first().title)
        assertEquals("NOCHE", allAfterCreate.first().badge)
        assertEquals(4200.0, allAfterCreate.first().fixedPromoPrice!!, 0.01)

        val activeAfterCreate = repository.getActivePromotions().first()
        assertEquals(1, activeAfterCreate.size)

        // Pausar promoción -> desaparece de activas en POS pero sigue en catálogo
        assertTrue(repository.togglePromotionActive(promoId, false, 2L, "Carlos Encargado", 1L).isSuccess)
        assertTrue(repository.getActivePromotions().first().isEmpty())
        assertFalse(repository.getAllPromotions().first().first().isActive)

        // Eliminar promoción -> desaparece del catálogo y registra auditoría
        assertTrue(repository.deletePromotion(promoId, 1L, "Roberto Dueño", 1L).isSuccess)
        assertTrue(repository.getAllPromotions().first().isEmpty())

        val auditActions = repository.getAuditLogs().first().map { it.action }
        assertTrue(auditActions.contains("PROMOTION_CREATED"))
        assertTrue(auditActions.contains("PROMOTION_TOGGLED"))
        assertTrue(auditActions.contains("PROMOTION_DELETED"))
    }
}
