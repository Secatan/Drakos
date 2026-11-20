package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AuditDao
import com.example.data.dao.AuthorizedDeviceDao
import com.example.data.dao.BatchDao
import com.example.data.dao.BranchDao
import com.example.data.dao.BranchMessageDao
import com.example.data.dao.CashDao
import com.example.data.dao.CompanySettingsDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.InventoryDao
import com.example.data.dao.ProductDao
import com.example.data.dao.PromotionDao
import com.example.data.dao.SaleDao
import com.example.data.dao.SupplierInvoiceDao
import com.example.data.dao.TransferDao
import com.example.data.dao.UserProfileDao
import com.example.data.dao.WeeklyOrderDao
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
import com.example.data.entity.PromotionEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.StockTransferEntity
import com.example.data.entity.StockTransferItemEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.SupplierInvoiceEntity
import com.example.data.entity.UserProfileEntity
import com.example.data.entity.WeeklyOrderEntity
import com.example.domain.model.DifferenceStatus
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.MovementType
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ShiftStatus
import com.example.domain.model.TransferStatus
import com.example.domain.model.UserRole
import com.example.domain.security.PinHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

@Database(
    entities = [
        BranchEntity::class,
        UserProfileEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        BranchInventoryEntity::class,
        InventoryMovementEntity::class,
        StockTransferEntity::class,
        StockTransferItemEntity::class,
        CashSessionEntity::class,
        CashMovementEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SupplierEntity::class,
        ExpenseEntity::class,
        AuditLogEntity::class,
        ProductBatchEntity::class,
        CompanySettingsEntity::class,
        AuthorizedDeviceEntity::class,
        BranchMessageEntity::class,
        SupplierInvoiceEntity::class,
        WeeklyOrderEntity::class,
        PromotionEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KioscoDatabase : RoomDatabase() {
    abstract fun branchDao(): BranchDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun productDao(): ProductDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun saleDao(): SaleDao
    abstract fun cashDao(): CashDao
    abstract fun transferDao(): TransferDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun auditDao(): AuditDao
    abstract fun supplierDao(): com.example.data.dao.SupplierDao
    abstract fun batchDao(): BatchDao
    abstract fun companySettingsDao(): CompanySettingsDao
    abstract fun authorizedDeviceDao(): AuthorizedDeviceDao
    abstract fun branchMessageDao(): BranchMessageDao
    abstract fun supplierInvoiceDao(): SupplierInvoiceDao
    abstract fun weeklyOrderDao(): WeeklyOrderDao
    abstract fun promotionDao(): PromotionDao

    companion object {
        @Volatile
        private var INSTANCE: KioscoDatabase? = null
        private val seedMutex = Mutex()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `product_batches` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `branchId` INTEGER NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `batchNumber` TEXT NOT NULL,
                        `expiryDateMillis` INTEGER NOT NULL,
                        `initialQuantity` INTEGER NOT NULL,
                        `remainingQuantity` INTEGER NOT NULL,
                        `supplierId` INTEGER,
                        `invoiceRef` TEXT NOT NULL,
                        `receivedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_batches_branchId_productId` ON `product_batches` (`branchId`, `productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_batches_expiryDateMillis` ON `product_batches` (`expiryDateMillis`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `company_settings` (
                        `id` INTEGER NOT NULL,
                        `businessName` TEXT NOT NULL,
                        `cuit` TEXT NOT NULL,
                        `taxCondition` TEXT NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `defaultVatRate` REAL NOT NULL,
                        `cashDropAlertThreshold` REAL NOT NULL,
                        `cashAuditTolerance` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `company_settings`
                    (`id`, `businessName`, `cuit`, `taxCondition`, `currencyCode`, `defaultVatRate`, `cashDropAlertThreshold`, `cashAuditTolerance`, `updatedAt`)
                    VALUES (1, 'Drakos Store Argentina S.A.', '30-71234567-8', 'IVA Responsable Inscripto', 'ARS', 21.0, 50000.0, 500.0, ${System.currentTimeMillis()})
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `company_settings` ADD COLUMN `strictDeviceWhitelist` INTEGER NOT NULL DEFAULT 1")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `authorized_devices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `deviceName` TEXT NOT NULL,
                        `assignedBranchId` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `pairingCode` TEXT NOT NULL,
                        `approvedByUserName` TEXT NOT NULL,
                        `lastAccessAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_authorized_devices_deviceId` ON `authorized_devices` (`deviceId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `branch_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fromBranchId` INTEGER NOT NULL,
                        `toBranchId` INTEGER NOT NULL,
                        `senderUserId` INTEGER NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `senderRoleLevel` INTEGER NOT NULL,
                        `priority` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_branch_messages_toBranchId` ON `branch_messages` (`toBranchId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_branch_messages_createdAt` ON `branch_messages` (`createdAt`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `supplier_invoices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `branchId` INTEGER NOT NULL,
                        `supplierId` INTEGER NOT NULL,
                        `supplierName` TEXT NOT NULL,
                        `invoiceType` TEXT NOT NULL,
                        `invoiceNumber` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `paymentStatus` TEXT NOT NULL,
                        `dueDateMillis` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdByUserId` INTEGER NOT NULL,
                        `createdByUserName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_invoices_branchId` ON `supplier_invoices` (`branchId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_invoices_supplierId` ON `supplier_invoices` (`supplierId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `weekly_orders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `branchId` INTEGER NOT NULL,
                        `supplierId` INTEGER NOT NULL,
                        `supplierName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `totalEstimatedCost` REAL NOT NULL,
                        `totalUnits` INTEGER NOT NULL,
                        `itemsSummary` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdByUserId` INTEGER NOT NULL,
                        `createdByUserName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_weekly_orders_branchId` ON `weekly_orders` (`branchId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_weekly_orders_createdAt` ON `weekly_orders` (`createdAt`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `promotions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `badge` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `discountPercent` REAL NOT NULL,
                        `fixedPromoPrice` REAL,
                        `itemsSpec` TEXT NOT NULL,
                        `validSchedule` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdByUserId` INTEGER NOT NULL,
                        `createdByUserName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_promotions_isActive` ON `promotions` (`isActive`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_promotions_createdAt` ON `promotions` (`createdAt`)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): KioscoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KioscoDatabase::class.java,
                    "kioscopos_master.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    ensureDatabaseReady(database)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    ensureDatabaseReady(database)
                }
            }
        }

        private suspend fun ensureDatabaseReady(database: KioscoDatabase) {
            seedMutex.withLock {
                if (database.userProfileDao().getAllUsersAdminSync().isEmpty()) {
                    populateInitialData(database)
                }
                if (database.companySettingsDao().getSettingsOnce() == null) {
                    database.companySettingsDao().upsertSettings(CompanySettingsEntity())
                }
                if (database.batchDao().countBatches() == 0) {
                    seedInitialBatches(database)
                }
                if (database.branchMessageDao().countMessages() == 0) {
                    seedInitialBranchMessages(database)
                }
                if (database.promotionDao().countPromotions() == 0) {
                    seedInitialPromotions(database)
                }
                // Migrar PINs legados en texto plano a hash SHA-256 (incluye perfiles inactivos)
                val existingUsers = database.userProfileDao().getAllUsersAdminSync()
                val unhashed = existingUsers.filter { !PinHasher.isHashed(it.pinCode) }
                if (unhashed.isNotEmpty()) {
                    database.userProfileDao().insertUsers(
                        unhashed.map { it.copy(pinCode = PinHasher.hash(it.username, it.pinCode)) }
                    )
                }
            }
        }

        suspend fun populateInitialData(db: KioscoDatabase) {
            val branchDao = db.branchDao()
            val userDao = db.userProfileDao()
            val productDao = db.productDao()
            val inventoryDao = db.inventoryDao()
            val cashDao = db.cashDao()
            val auditDao = db.auditDao()

            // 1. Las 10 Sucursales de la cadena de Kioscos en Buenos Aires
            val branches = listOf(
                BranchEntity(1, "Sucursal 01 - Centro", "K01", "Av. Corrientes 1240, CABA", "011 4371-2210"),
                BranchEntity(2, "Sucursal 02 - Palermo", "K02", "Av. Santa Fe 3420, CABA", "011 4822-5590"),
                BranchEntity(3, "Sucursal 03 - Belgrano", "K03", "Av. Cabildo 2180, CABA", "011 4784-1133"),
                BranchEntity(4, "Sucursal 04 - Recoleta", "K04", "Av. Las Heras 2310, CABA", "011 4803-9088"),
                BranchEntity(5, "Sucursal 05 - Caballito", "K05", "Av. Rivadavia 5100, CABA", "011 4902-6712"),
                BranchEntity(6, "Sucursal 06 - Villa Crespo", "K06", "Av. Scalabrini Ortiz 750, CABA", "011 4771-3344"),
                BranchEntity(7, "Sucursal 07 - Almagro", "K07", "Av. Medrano 410, CABA", "011 4865-2022"),
                BranchEntity(8, "Sucursal 08 - San Telmo", "K08", "Defensa 890, CABA", "011 4361-9988"),
                BranchEntity(9, "Sucursal 09 - Flores", "K09", "Av. Nazca 120, CABA", "011 4612-8877"),
                BranchEntity(10, "Sucursal 10 - Retiro", "K10", "Av. del Libertador 180, CABA", "011 4311-4455")
            )
            branchDao.insertBranches(branches)

            // 2. Personal: Propietario (Nivel 3), Encargados (Nivel 2) y Empleados (Nivel 1) con PINs hasheados
            val users = mutableListOf(
                UserProfileEntity(1, "propietario", "Roberto Gómez (Dueño General)", PinHasher.hash("propietario", "1234"), UserRole.OWNER, 1),
                UserProfileEntity(2, "encargado_centro", "Martín Palermo (Encargado Centro)", PinHasher.hash("encargado_centro", "2345"), UserRole.BRANCH_MANAGER, 1),
                UserProfileEntity(3, "empleado_centro", "Lucía Fernández (Cajera Mañana)", PinHasher.hash("empleado_centro", "3456"), UserRole.EMPLOYEE, 1),
                UserProfileEntity(4, "encargado_palermo", "Juan Román (Encargado Palermo)", PinHasher.hash("encargado_palermo", "2346"), UserRole.BRANCH_MANAGER, 2),
                UserProfileEntity(5, "empleado_palermo", "Carlos Tevez (Cajero Turno Tarde)", PinHasher.hash("empleado_palermo", "3457"), UserRole.EMPLOYEE, 2)
            )
            // Añadir personal complementario para reflejar los ~50 empleados de las 10 sucursales
            var nextUserId = 6L
            for (branchId in 1..10) {
                if (branchId > 2) {
                    val username = "encargado_k${branchId}"
                    users.add(UserProfileEntity(nextUserId++, username, "Encargado Sucursal $branchId", PinHasher.hash(username, "200$branchId"), UserRole.BRANCH_MANAGER, branchId.toLong()))
                }
                val uA = "cajero_a_k${branchId}"
                val uB = "cajero_b_k${branchId}"
                val uC = "cajero_c_k${branchId}"
                users.add(UserProfileEntity(nextUserId++, uA, "Cajero Turno A (K$branchId)", PinHasher.hash(uA, "300$branchId"), UserRole.EMPLOYEE, branchId.toLong()))
                users.add(UserProfileEntity(nextUserId++, uB, "Cajero Turno B (K$branchId)", PinHasher.hash(uB, "310$branchId"), UserRole.EMPLOYEE, branchId.toLong()))
                users.add(UserProfileEntity(nextUserId++, uC, "Cajero Noche (K$branchId)", PinHasher.hash(uC, "320$branchId"), UserRole.EMPLOYEE, branchId.toLong()))
            }
            userDao.insertUsers(users)

            // 3. Categorías
            val categories = listOf(
                CategoryEntity(1, "Alfajores", "cake"),
                CategoryEntity(2, "Bebidas", "local_drink"),
                CategoryEntity(3, "Golosinas & Chocolates", "cookie"),
                CategoryEntity(4, "Cigarrillos & Fumar", "smoke_free"),
                CategoryEntity(5, "Galletitas & Snacks", "bakery_dining"),
                CategoryEntity(6, "Servicios & Varios", "category")
            )
            productDao.insertCategories(categories)

            // 4. Catálogo de Productos Reales de Kioscos Argentinos
            val products = listOf(
                ProductEntity(1, "7790010001", "ALF-HAV-01", "Alfajor Havanna 70% Cacao", "Alfajor premium chocolate amargo puro", 1, 1150.0, 1800.0, 21.0, "UN", 12),
                ProductEntity(2, "7790010002", "ALF-GUA-01", "Alfajor Guaymallén Blanco", "Clásico alfajor escolar cobertura blanca", 1, 240.0, 450.0, 21.0, "UN", 24),
                ProductEntity(3, "7790010003", "ALF-JOR-01", "Alfajor Jorgito Chocolate", "Alfajor tradicional relleno con dulce de leche", 1, 480.0, 800.0, 21.0, "UN", 20),
                ProductEntity(4, "7790010004", "ALF-CAP-01", "Alfajor Capitán del Espacio Triple", "Mítico alfajor triple de Quilmes", 1, 750.0, 1200.0, 21.0, "UN", 15),

                ProductEntity(5, "7790020001", "BEB-COCA-500", "Coca-Cola Original 500ml", "Gaseosa cola botella retornable/descartable", 2, 980.0, 1600.0, 21.0, "UN", 18),
                ProductEntity(6, "7790020002", "BEB-COCA-ZERO", "Coca-Cola Sin Azúcar 500ml", "Bebida sin azúcar botella 500ml", 2, 980.0, 1600.0, 21.0, "UN", 15),
                ProductEntity(7, "7790020003", "BEB-SPR-500", "Sprite Lima Limón 500ml", "Gaseosa lima limón refrescante", 2, 920.0, 1500.0, 21.0, "UN", 12),
                ProductEntity(8, "7790020004", "BEB-AGU-VIL", "Agua Villavicencio con Gas 500ml", "Agua mineral de manantial", 2, 650.0, 1100.0, 21.0, "UN", 20),
                ProductEntity(9, "7790020005", "BEB-MONS-473", "Monster Energy Mango Loco 473ml", "Bebida energizante lata grande", 2, 1550.0, 2400.0, 21.0, "UN", 10),
                ProductEntity(10, "7790020006", "BEB-RED-250", "Red Bull Energy Drink 250ml", "Energizante lata clásica", 2, 1400.0, 2200.0, 21.0, "UN", 10),

                ProductEntity(11, "7790030001", "GOL-BELD-01", "Chicles Beldent Menta Fuerte", "Chicle sin azúcar blister", 3, 400.0, 700.0, 21.0, "UN", 30),
                ProductEntity(12, "7790030002", "GOL-SUG-50", "Caramelos Sugus Confitados 50g", "Bolsita confites frutales", 3, 360.0, 650.0, 21.0, "UN", 20),
                ProductEntity(13, "7790030003", "GOL-COF-38", "Chocolatín Block Cofler 38g", "Chocolate con maní entero", 3, 780.0, 1300.0, 21.0, "UN", 16),
                ProductEntity(14, "7790030004", "GOL-MOG-100", "Gomitas Mogul Frutales 100g", "Gomitas suaves sabor frutas", 3, 550.0, 950.0, 21.0, "UN", 20),

                ProductEntity(15, "7790040001", "CIG-MARL-20", "Cigarrillos Marlboro Box 20", "Cajetilla clásica box 20", 4, 3400.0, 3800.0, 21.0, "UN", 25),
                ProductEntity(16, "7790040002", "CIG-PHIL-20", "Cigarrillos Philip Morris Box 20", "Cajetilla box 20", 4, 3050.0, 3400.0, 21.0, "UN", 25),
                ProductEntity(17, "7790040003", "FUM-BIC-MAX", "Encendedor Bic Maxi", "Encendedor descartable estándar", 4, 850.0, 1500.0, 21.0, "UN", 20),
                ProductEntity(18, "7790040004", "FUM-OCB-PRE", "Papelillos OCB Premium Negro", "Librillo papel para armar", 4, 600.0, 1100.0, 21.0, "UN", 25),

                ProductEntity(19, "7790050001", "GAL-CHO-250", "Galletitas Chocolinas 250g", "Galletitas de chocolate para chocotorta", 5, 1100.0, 1750.0, 21.0, "UN", 15),
                ProductEntity(20, "7790050002", "GAL-ORE-118", "Galletitas Oreo 118g", "Paquete galletitas rellenas crema", 5, 900.0, 1450.0, 21.0, "UN", 20),
                ProductEntity(21, "7790050003", "SNA-LAY-85", "Papas Fritas Lay's Clásicas 85g", "Snack papas fritas saladas", 5, 1350.0, 2100.0, 21.0, "UN", 12),

                ProductEntity(22, "7790060001", "SER-SUBE-1K", "Carga Tarjeta SUBE $1.000", "Servicio recarga transporte público", 6, 990.0, 1000.0, 21.0, "UN", 999),
                ProductEntity(23, "7790060002", "VAR-ELI-PAQ", "Pañuelos Descartables Elite", "Paquetito bolsillo 10 unidades", 6, 320.0, 600.0, 21.0, "UN", 25)
            )
            productDao.insertProducts(products)

            // 5. Inventario independiente para las 10 sucursales
            val inventoryList = mutableListOf<BranchInventoryEntity>()
            for (b in 1..10) {
                for (p in products) {
                    val baseQty = when {
                        p.id == 22L -> 999 // SUBE siempre disponible
                        b == 1 -> 35 // Sucursal 1 bien abastecida
                        b == 3 && p.id == 1L -> 2 // Belgrano con quiebre de Havanna para probar alertas y transferencias
                        b == 7 && p.id == 5L -> 3 // Almagro con quiebre de Coca-Cola
                        else -> 15 + (b * 3 + p.id.toInt()) % 25
                    }
                    inventoryList.add(
                        BranchInventoryEntity(
                            id = 0,
                            branchId = b.toLong(),
                            productId = p.id,
                            quantityOnHand = baseQty,
                            minAlertThreshold = p.minStock
                        )
                    )
                }
            }
            inventoryDao.insertInventoryList(inventoryList)

            // 6. Turno activo inicial en Sucursal 1 (con fondo de caja de $15.000 ARS)
            val initialSession = CashSessionEntity(
                id = 1,
                branchId = 1,
                userId = 3, // Lucía Fernández (Cajera)
                openedAt = System.currentTimeMillis() - 7200000L, // Abierto hace 2 horas
                openingAmount = 15000.0,
                status = ShiftStatus.OPEN,
                notes = "Apertura de turno mañana con fondo de cambio en billetes"
            )
            cashDao.insertSession(initialSession)

            // 7. Proveedores Habituales de Kioscos en Argentina
            val suppliers = listOf(
                SupplierEntity(1, "Arcor S.A.I.C.", "30-50279317-6", "0800-444-2726", "ventas@arcor.com", "Av. Fulvio Salvador Pagani 487, Córdoba"),
                SupplierEntity(2, "Cervecería Quilmes / PepsiCo", "30-50346800-4", "011 4349-1200", "pedidos@quilmes.com.ar", "Av. 12 de Octubre, Quilmes"),
                SupplierEntity(3, "Massalin Particulares S.A.", "30-50013978-9", "011 4735-8000", "contacto@massalin.com", "Av. Libertador 350, Vicente López"),
                SupplierEntity(4, "Havanna S.A.", "30-50392300-8", "0810-333-4282", "kioscos@havanna.com.ar", "Calle 14 Nº 2550, Mar del Plata"),
                SupplierEntity(5, "Distribuidora Mayorista El Kiosquero", "30-71234567-8", "011 4862-9900", "mayorista@elkiosquero.com.ar", "Av. Jujuy 1420, CABA")
            )
            db.supplierDao().insertSuppliers(suppliers)

            // 8. Parámetros de Empresa
            db.companySettingsDao().upsertSettings(CompanySettingsEntity())

            // 9. Lotes y vencimientos iniciales reales por sucursal
            seedInitialBatches(db)

            // 10. Registro de Auditoría Inicial
            auditDao.insertAuditLog(
                AuditLogEntity(
                    userId = 1,
                    userName = "Roberto Gómez",
                    branchId = 1,
                    action = "INITIAL_BOOTSTRAP",
                    entityType = "SYSTEM",
                    entityId = "ALL",
                    details = "Inicialización del sistema Drakos 24hs para 10 sucursales con 50 perfiles, 23 productos, lotes iniciales y fondos de caja."
                )
            )
        }

        suspend fun seedInitialBatches(db: KioscoDatabase) {
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L
            val batches = mutableListOf<ProductBatchEntity>()

            // Lotes representativos para productos perecederos (alfajores, bebidas, golosinas, galletitas)
            val daysPattern = listOf(6, 18, 42, 95, 160)
            for (b in 1..10) {
                for (pid in 1..21) {
                    val daysOffset = daysPattern[(b + pid) % daysPattern.size]
                    val qty = if (pid == 1 && b == 3) 2 else if (pid == 5 && b == 7) 3 else 12 + (b + pid) % 15
                    batches.add(
                        ProductBatchEntity(
                            branchId = b.toLong(),
                            productId = pid.toLong(),
                            batchNumber = "LOT-${String.format(Locale.US, "%02d", b)}${String.format(Locale.US, "%03d", pid)}",
                            expiryDateMillis = now + daysOffset * dayMs,
                            initialQuantity = qty,
                            remainingQuantity = qty,
                            supplierId = ((pid % 5) + 1).toLong(),
                            invoiceRef = "REM-INI-${String.format(Locale.US, "%02d", b)}",
                            receivedAt = now - 5 * dayMs
                        )
                    )
                }
            }
            db.batchDao().insertBatches(batches)
        }

        suspend fun seedInitialBranchMessages(db: KioscoDatabase) {
            val now = System.currentTimeMillis()
            val msgs = listOf(
                BranchMessageEntity(
                    fromBranchId = 3L,
                    toBranchId = 1L,
                    senderUserId = 6L,
                    senderName = "Encargado Sucursal 3",
                    senderRoleLevel = 2,
                    priority = "STOCK_REQUEST",
                    subject = "Reposición urgente Alfajor Havanna 70%",
                    message = "Centro, estamos con quiebre crítico en K03 (2 u.). ¿Tienen 12 unidades disponibles para traslado hoy?",
                    createdAt = now - 3_600_000L
                ),
                BranchMessageEntity(
                    fromBranchId = 2L,
                    toBranchId = 0L,
                    senderUserId = 4L,
                    senderName = "Juan Román (Encargado Palermo)",
                    senderRoleLevel = 2,
                    priority = "NORMAL",
                    subject = "Entrega Quilmes / PepsiCo confirmada",
                    message = "El camión de bebidas pasó por Palermo sin faltantes. Dejamos factura ingresada en sistema.",
                    createdAt = now - 7_200_000L
                )
            )
            db.branchMessageDao().insertMessages(msgs)
        }

        suspend fun seedInitialPromotions(db: KioscoDatabase) {
            val now = System.currentTimeMillis()
            val promos = listOf(
                PromotionEntity(
                    title = "Desayuno (Havanna + Coca)",
                    badge = "COMBO",
                    description = "Alfajor Havanna 70% + Coca-Cola 500ml con 10% OFF",
                    discountPercent = 10.0,
                    fixedPromoPrice = null,
                    itemsSpec = "1:1,5:1",
                    validSchedule = "24hs • Todos los días",
                    isActive = true,
                    createdByUserId = 1L,
                    createdByUserName = "Drakos Store",
                    createdAt = now
                ),
                PromotionEntity(
                    title = "2x Beldent Menta",
                    badge = "DUO",
                    description = "2 unidades de Chicles Beldent Menta con 15% OFF",
                    discountPercent = 15.0,
                    fixedPromoPrice = null,
                    itemsSpec = "11:2",
                    validSchedule = "24hs • Todos los días",
                    isActive = true,
                    createdByUserId = 1L,
                    createdByUserName = "Drakos Store",
                    createdAt = now - 1000L
                ),
                PromotionEntity(
                    title = "Red Bull + Chocolinas",
                    badge = "NOCHE",
                    description = "Energizante Red Bull 250ml + Galletitas Chocolinas 250g",
                    discountPercent = 12.0,
                    fixedPromoPrice = null,
                    itemsSpec = "10:1,19:1",
                    validSchedule = "Turno Tarde / Noche 24hs",
                    isActive = true,
                    createdByUserId = 1L,
                    createdByUserName = "Drakos Store",
                    createdAt = now - 2000L
                )
            )
            db.promotionDao().insertPromotions(promos)
        }
    }
}
