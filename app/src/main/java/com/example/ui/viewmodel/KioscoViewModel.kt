package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.AuthorizedDeviceEntity
import com.example.data.entity.BranchEntity
import com.example.data.entity.BranchInventoryEntity
import com.example.data.entity.BranchMessageEntity
import com.example.data.entity.CashMovementEntity
import com.example.data.entity.CashSessionEntity
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.InventoryMovementEntity
import com.example.data.entity.ProductBatchEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.StockTransferEntity
import com.example.data.entity.SupplierInvoiceEntity
import com.example.data.entity.UserProfileEntity
import com.example.data.entity.WeeklyOrderEntity
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.KioscoRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.domain.model.BatchExpiryItem
import com.example.domain.model.CartItem
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.MovementType
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PriceRoundingStep
import com.example.domain.model.ProductWithStock
import com.example.domain.model.SaleItemAnalytics
import com.example.domain.model.UserRole
import com.example.domain.security.DeviceProvisioningCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class KioscoViewModel(
    private val repository: KioscoRepository,
    private val cloudSyncRepository: CloudSyncRepository? = null
) : ViewModel() {

    val allBranches: StateFlow<List<BranchEntity>> = repository.getBranches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserProfileEntity>> = repository.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminUsers: StateFlow<List<UserProfileEntity>> = repository.getUsersAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers = repository.getSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProductsEntity: StateFlow<List<ProductEntity>> = repository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBranchesInventory: StateFlow<List<BranchInventoryEntity>> = repository.getAllInventory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveBatches: StateFlow<List<ProductBatchEntity>> = repository.getAllActiveBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSaleAnalytics: StateFlow<List<SaleItemAnalytics>> = repository.getAllCompletedSaleAnalytics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val authorizedDevices: StateFlow<List<AuthorizedDeviceEntity>> = repository.getAuthorizedDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDeviceId = MutableStateFlow("")
    val currentDeviceId: StateFlow<String> = _currentDeviceId.asStateFlow()

    private val _currentDeviceName = MutableStateFlow("Terminal Drakos")

    val currentDeviceRecord: StateFlow<AuthorizedDeviceEntity?> = _currentDeviceId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(null) else repository.observeDevice(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentUser = MutableStateFlow<UserProfileEntity?>(null)
    val currentUser: StateFlow<UserProfileEntity?> = _currentUser.asStateFlow()

    private val _selectedBranch = MutableStateFlow<BranchEntity?>(null)
    val selectedBranch: StateFlow<BranchEntity?> = _selectedBranch.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _failedPinAttempts = MutableStateFlow(0)
    val failedPinAttempts: StateFlow<Int> = _failedPinAttempts.asStateFlow()

    private val _lockoutUntilMillis = MutableStateFlow(0L)
    val lockoutUntilMillis: StateFlow<Long> = _lockoutUntilMillis.asStateFlow()

    private val _cloudAccessSyncStatus = MutableStateFlow<String?>(null)
    val cloudAccessSyncStatus: StateFlow<String?> = _cloudAccessSyncStatus.asStateFlow()

    private val _isCloudAccessSyncing = MutableStateFlow(false)
    val isCloudAccessSyncing: StateFlow<Boolean> = _isCloudAccessSyncing.asStateFlow()

    private var activeRealtimeUid: String? = null
    private var realtimeSyncJob: Job? = null

    // Turno activo de caja para la sucursal actual
    val activeCashSession: StateFlow<CashSessionEntity?> = _selectedBranch
        .flatMapLatest { branch ->
            val branchId = branch?.id ?: 1L
            repository.getActiveSessionFlow(branchId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val branchSessions: StateFlow<List<CashSessionEntity>> = _selectedBranch
        .flatMapLatest { branch ->
            val branchId = branch?.id ?: 1L
            repository.getBranchSessions(branchId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<CashSessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSessionMovements: StateFlow<List<CashMovementEntity>> = activeCashSession
        .flatMapLatest { session ->
            if (session == null) flowOf(emptyList()) else repository.getCashMovementsForSession(session.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Catálogo con stock reactivo de la sucursal seleccionada
    private val rawProductsWithStock = _selectedBranch
        .flatMapLatest { branch ->
            val branchId = branch?.id ?: 1L
            repository.getProductsWithStock(branchId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBranchProducts: StateFlow<List<ProductWithStock>> = rawProductsWithStock

    // Catálogo filtrado por categoría y búsqueda
    val filteredProducts: StateFlow<List<ProductWithStock>> = combine(
        rawProductsWithStock,
        _selectedCategoryId,
        _searchQuery
    ) { products, catId, query ->
        products.filter { p ->
            val matchesCategory = catId == null || p.categoryName.equals(
                categories.value.find { it.id == catId }?.name,
                ignoreCase = true
            )
            val matchesQuery = query.isBlank() ||
                p.name.contains(query, ignoreCase = true) ||
                p.barcode.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ventas de la sucursal seleccionada
    val branchSales: StateFlow<List<SaleEntity>> = _selectedBranch
        .flatMapLatest { branch ->
            val branchId = branch?.id ?: 1L
            repository.getBranchSales(branchId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val estimatedDrawerCash: StateFlow<Double> = combine(
        activeCashSession,
        branchSales,
        activeSessionMovements
    ) { session, sales, movements ->
        if (session == null) {
            0.0
        } else {
            val cashSales = sales
                .filter { it.cashSessionId == session.id && it.status == "COMPLETED" && it.paymentMethod == PaymentMethod.CASH }
                .sumOf { it.totalAmount }
            val inflows = movements.filter { it.movementType == "INFLOW" }.sumOf { it.amount }
            val outflows = movements.filter { it.movementType == "OUTFLOW_DROP" || it.movementType == "EXPENSE" }.sumOf { it.amount }
            session.openingAmount + cashSales + inflows - outflows
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Ventas consolidadas de las 10 sucursales (Nivel 3 Propietario)
    val allChainSales: StateFlow<List<SaleEntity>> = repository.getAllSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val branchTransfers: StateFlow<List<StockTransferEntity>> = _selectedBranch
        .flatMapLatest { branch ->
            val branchId = branch?.id ?: 1L
            repository.getBranchTransfers(branchId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransfers: StateFlow<List<StockTransferEntity>> = repository.getAllTransfers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val branchExpenses: StateFlow<List<ExpenseEntity>> = _selectedBranch
        .flatMapLatest { branch ->
            val branchId = branch?.id ?: 1L
            repository.getBranchExpenses(branchId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.getAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companySettings: StateFlow<CompanySettingsEntity> = repository.getCompanySettings()
        .map { it ?: CompanySettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompanySettingsEntity())

    val branchBatches: StateFlow<List<BatchExpiryItem>> = _selectedBranch
        .flatMapLatest { branch ->
            val branchId = branch?.id ?: 1L
            repository.getBranchBatchExpiryItems(branchId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val branchMovements: StateFlow<List<InventoryMovementEntity>> = _selectedBranch
        .flatMapLatest { branch ->
            val branchId = branch?.id ?: 1L
            repository.getBranchMovements(branchId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val branchMessages: StateFlow<List<BranchMessageEntity>> = combine(_selectedBranch, _currentUser) { branch, user ->
        branch?.id to user?.role
    }.flatMapLatest { (branchId, role) ->
        if (role == UserRole.OWNER) {
            repository.getAllBranchMessages()
        } else {
            repository.getMessagesForBranch(branchId ?: 1L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val branchInvoices: StateFlow<List<SupplierInvoiceEntity>> = _selectedBranch
        .flatMapLatest { branch ->
            repository.getSupplierInvoices(branch?.id ?: 1L)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices: StateFlow<List<SupplierInvoiceEntity>> = repository.getAllSupplierInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val branchWeeklyOrders: StateFlow<List<WeeklyOrderEntity>> = _selectedBranch
        .flatMapLatest { branch ->
            repository.getWeeklyOrders(branch?.id ?: 1L)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Inicializar sucursal predeterminada; la sesión de usuario inicia bloqueada hasta validar PIN
        viewModelScope.launch {
            allBranches.collect { branches ->
                if (_selectedBranch.value == null && branches.isNotEmpty()) {
                    _selectedBranch.value = branches.first()
                }
            }
        }
    }

    fun initDeviceIdentity(deviceId: String, deviceName: String) {
        if (deviceId.isBlank()) return
        _currentDeviceName.value = deviceName
        if (_currentDeviceId.value == deviceId) return
        _currentDeviceId.value = deviceId
        viewModelScope.launch {
            val registered = repository.ensureDeviceRegisteredOnStartup(deviceId, deviceName)
            if (registered.status == "AUTHORIZED" && registered.assignedBranchId > 0L) {
                val assigned = allBranches.value.find { it.id == registered.assignedBranchId }
                if (assigned != null && _currentUser.value == null) {
                    _selectedBranch.value = assigned
                }
            }
        }
    }

    // --- Control de Sesión, Autenticación Segura y Sucursales ---

    fun switchUser(user: UserProfileEntity) {
        _currentUser.value = user
        _cart.value = emptyList() // Limpiar carrito al cambiar de cajero
        _failedPinAttempts.value = 0
        _lockoutUntilMillis.value = 0L

        // Regla de Aislamiento Territorial:
        // Si es Empleado o Encargado, se fuerza su sucursal asignada
        if (user.role != UserRole.OWNER) {
            val branch = allBranches.value.find { it.id == user.assignedBranchId }
            if (branch != null) {
                _selectedBranch.value = branch
            }
        } else {
            // Al iniciar sesión como Nivel 3 con Google conectado, asegurar que todos los PINs locales estén subidos a la nube
            viewModelScope.launch {
                pushStaffAndDevicesInternal(silent = true)
            }
        }
        _userMessage.value = "Sesión activa: ${user.fullName} (${user.role.label})"
    }

    fun loginWithPin(pin: String, onResult: (Boolean) -> Unit = {}) {
        val now = System.currentTimeMillis()
        if (now < _lockoutUntilMillis.value) {
            val secondsLeft = ((_lockoutUntilMillis.value - now) / 1000L).coerceAtLeast(1L)
            _errorMessage.value = "Terminal bloqueada por intentos fallidos. Reintente en ${secondsLeft}s."
            onResult(false)
            return
        }

        val cleanPin = pin.trim()
        if (cleanPin.length < 4) {
            _errorMessage.value = "Ingrese un PIN válido de 4 dígitos."
            onResult(false)
            return
        }

        viewModelScope.launch {
            val branchId = _selectedBranch.value?.id ?: 1L
            val matched = repository.getUserByPin(cleanPin)
            if (matched != null) {
                val activeDeviceId = _currentDeviceId.value
                val deviceAllowed = if (activeDeviceId.isBlank() || matched.role == UserRole.OWNER) {
                    true
                } else {
                    repository.isDeviceAllowedForStaff(activeDeviceId)
                }

                if (!deviceAllowed) {
                    repository.recordSecurityAudit(
                        userId = matched.id,
                        userName = matched.fullName,
                        branchId = matched.assignedBranchId,
                        action = "AUTH_DEVICE_BLOCKED",
                        details = "Acceso bloqueado en terminal no autorizada ($activeDeviceId) para ${matched.role.label}."
                    )
                    _errorMessage.value = "Dispositivo no autorizado ($activeDeviceId). El Propietario (Nivel 3) debe habilitar esta terminal."
                    onResult(false)
                    return@launch
                }

                switchUser(matched)
                repository.recordSecurityAudit(
                    userId = matched.id,
                    userName = matched.fullName,
                    branchId = matched.assignedBranchId,
                    action = "AUTH_LOGIN_SUCCESS",
                    details = "Inicio de sesión exitoso (${matched.role.label})."
                )
                onResult(true)
            } else {
                val attempts = _failedPinAttempts.value + 1
                _failedPinAttempts.value = attempts
                if (attempts >= 5) {
                    _lockoutUntilMillis.value = System.currentTimeMillis() + 30_000L
                    _failedPinAttempts.value = 0
                    repository.recordSecurityAudit(
                        userId = 0L,
                        userName = "Desconocido",
                        branchId = branchId,
                        action = "AUTH_LOCKOUT",
                        details = "Bloqueo temporal de 30s tras 5 intentos fallidos de PIN."
                    )
                    _errorMessage.value = "Demasiados intentos fallidos. Terminal bloqueada por 30 segundos."
                } else {
                    repository.recordSecurityAudit(
                        userId = 0L,
                        userName = "Desconocido",
                        branchId = branchId,
                        action = "AUTH_LOGIN_FAILED",
                        details = "Intento fallido de autenticación por PIN ($attempts/5)."
                    )
                    _errorMessage.value = "PIN incorrecto (intento $attempts de 5)."
                }
                onResult(false)
            }
        }
    }

    fun logout() {
        val prev = _currentUser.value
        val branchId = _selectedBranch.value?.id ?: 1L
        _currentUser.value = null
        _cart.value = emptyList()
        if (prev != null) {
            viewModelScope.launch {
                repository.recordSecurityAudit(
                    userId = prev.id,
                    userName = prev.fullName,
                    branchId = branchId,
                    action = "AUTH_LOGOUT",
                    details = "Cierre de sesión manual / bloqueo de terminal."
                )
            }
        }
    }

    fun selectBranch(branch: BranchEntity) {
        val current = _currentUser.value
        if (current?.role != UserRole.OWNER && current?.assignedBranchId != branch.id) {
            _errorMessage.value = "Denegado por política de seguridad: No tiene permisos para acceder a otra sucursal."
            return
        }
        _selectedBranch.value = branch
        _cart.value = emptyList()
        _userMessage.value = "Sucursal activa: ${branch.name}"
    }

    // --- POS y Carrito ---

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: ProductWithStock) {
        if (product.quantityOnHand <= 0) {
            _errorMessage.value = "Producto '${product.name}' sin stock en esta sucursal."
            return
        }

        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == product.productId }

        if (index >= 0) {
            val item = currentList[index]
            if (item.quantity + 1 > product.quantityOnHand) {
                _errorMessage.value = "No hay más unidades disponibles de '${product.name}' (Stock: ${product.quantityOnHand})."
                return
            }
            currentList[index] = item.copy(quantity = item.quantity + 1)
        } else {
            currentList.add(
                CartItem(
                    productId = product.productId,
                    barcode = product.barcode,
                    name = product.name,
                    unitPrice = product.sellingPrice,
                    unitCost = product.costPrice,
                    quantity = 1,
                    stockAvailable = product.quantityOnHand
                )
            )
        }
        _cart.value = currentList
    }

    fun findProductByBarcode(barcode: String): ProductWithStock? {
        val clean = barcode.trim()
        if (clean.isEmpty()) return null
        return rawProductsWithStock.value.find { it.barcode.equals(clean, ignoreCase = true) }
    }

    fun addByBarcode(barcode: String) {
        val prod = findProductByBarcode(barcode)
        if (prod != null) {
            addToCart(prod)
            _userMessage.value = "Añadido: ${prod.name}"
        } else {
            _errorMessage.value = "Código de barras '$barcode' no encontrado en el catálogo."
        }
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId }
        if (index >= 0) {
            val item = currentList[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else if (newQty > item.stockAvailable) {
                _errorMessage.value = "Stock máximo disponible alcanzado (${item.stockAvailable})."
                return
            } else {
                currentList[index] = item.copy(quantity = newQty)
            }
            _cart.value = currentList
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.productId != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun processSale(
        paymentMethod: PaymentMethod,
        discount: Double = 0.0,
        onSuccess: (SaleEntity) -> Unit
    ) {
        val branch = _selectedBranch.value ?: return
        val user = _currentUser.value ?: return
        val items = _cart.value

        if (items.isEmpty()) {
            _errorMessage.value = "El carrito de ventas está vacío."
            return
        }

        viewModelScope.launch {
            val result = repository.recordSale(
                branchId = branch.id,
                userId = user.id,
                userName = user.fullName,
                cartItems = items,
                paymentMethod = paymentMethod,
                discount = discount
            )

            result.fold(
                onSuccess = { sale ->
                    _cart.value = emptyList()
                    _userMessage.value = "Venta ${sale.receiptNumber} confirmada ($ ${sale.totalAmount})"
                    syncIfSignedIn { it.syncSale(sale, branch.name, user.fullName) }
                    onSuccess(sale)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Error al registrar la venta."
                }
            )
        }
    }

    // --- Turno de Caja y Arqueo Ciego ---

    fun openShift(openingAmount: Double, notes: String) {
        val branch = _selectedBranch.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            val res = repository.openCashSession(
                branchId = branch.id,
                userId = user.id,
                userName = user.fullName,
                openingAmount = openingAmount,
                notes = notes
            )
            res.fold(
                onSuccess = { opened ->
                    _userMessage.value = "Turno de caja abierto con éxito ($ $openingAmount)."
                    syncIfSignedIn { it.syncCashSession(opened, branch.name, user.fullName) }
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    fun closeShift(countedAmount: Double, notes: String) {
        val session = activeCashSession.value
        if (session == null) {
            _errorMessage.value = "No hay un turno de caja activo para cerrar."
            return
        }
        val user = _currentUser.value ?: return
        val branchName = _selectedBranch.value?.name ?: "Sucursal ${session.branchId}"

        viewModelScope.launch {
            val res = repository.closeCashSessionWithBlindCount(
                sessionId = session.id,
                countedAmount = countedAmount,
                userId = user.id,
                userName = user.fullName,
                notes = notes
            )
            res.fold(
                onSuccess = { closed ->
                    _userMessage.value = "Turno cerrado. Arqueo ciego registrado (Diferencia: $ ${closed.cashDifference})."
                    syncIfSignedIn { it.syncCashSession(closed, branchName, user.fullName) }
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    fun registerCashDrop(amount: Double, reason: String) {
        val session = activeCashSession.value
        if (session == null) {
            _errorMessage.value = "Debe haber un turno activo para realizar retiros de gaveta."
            return
        }
        val branch = _selectedBranch.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            val res = repository.registerCashDrop(
                sessionId = session.id,
                branchId = branch.id,
                amount = amount,
                reason = reason,
                userId = user.id,
                userName = user.fullName
            )
            res.fold(
                onSuccess = {
                    _userMessage.value = "Retiro de efectivo registrado ($ $amount)."
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    fun approveCashDifference(sessionId: Long, notes: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.approveCashDifference(sessionId, notes, user.id, user.fullName)
            res.fold(
                onSuccess = {
                    _userMessage.value = "Descuadre de caja aprobado formalmente."
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    // --- Inventario y Ajustes ---

    fun registerAdjustment(productId: Long, type: MovementType, quantityChange: Int, notes: String) {
        val branch = _selectedBranch.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            val res = repository.registerInventoryAdjustment(
                branchId = branch.id,
                productId = productId,
                type = type,
                quantityChange = quantityChange,
                notes = notes,
                userId = user.id,
                userName = user.fullName
            )
            res.fold(
                onSuccess = {
                    _userMessage.value = "Movimiento inmutable asentado en libro mayor."
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    // --- Transferencias ---

    fun createTransfer(targetBranchId: Long, productId: Long, quantity: Int, notes: String) {
        val sourceBranch = _selectedBranch.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            val res = repository.createTransferRequest(
                sourceBranchId = sourceBranch.id,
                targetBranchId = targetBranchId,
                productId = productId,
                quantity = quantity,
                notes = notes,
                userId = user.id,
                userName = user.fullName
            )
            res.fold(
                onSuccess = { transfer ->
                    _userMessage.value = "Transferencia solicitada hacia la sucursal de destino."
                    syncIfSignedIn { it.syncTransfer(transfer, "Transferencia Sucursal ${sourceBranch.id} -> $targetBranchId", quantity) }
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    fun dispatchTransfer(transfer: StockTransferEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.dispatchTransfer(transfer, user.id, user.fullName)
            res.fold(
                onSuccess = {
                    _userMessage.value = "Mercadería despachada. Stock restado de origen y colocado en tránsito."
                    syncIfSignedIn {
                        it.syncTransfer(
                            transfer.copy(status = com.example.domain.model.TransferStatus.DISPATCHED),
                            "Transferencia #${transfer.id}",
                            1
                        )
                    }
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    fun receiveTransfer(transfer: StockTransferEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.receiveTransfer(transfer, user.id, user.fullName)
            res.fold(
                onSuccess = {
                    _userMessage.value = "Transferencia recibida. Mercadería incorporada al stock local."
                    syncIfSignedIn {
                        it.syncTransfer(
                            transfer.copy(status = com.example.domain.model.TransferStatus.RECEIVED),
                            "Transferencia #${transfer.id}",
                            1
                        )
                    }
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    // --- Gastos ---

    fun registerExpense(category: ExpenseCategory, amount: Double, description: String, source: String) {
        val branch = _selectedBranch.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            val res = repository.registerExpense(
                branchId = branch.id,
                category = category,
                amount = amount,
                description = description,
                source = source,
                userId = user.id,
                userName = user.fullName
            )
            res.fold(
                onSuccess = { expense ->
                    _userMessage.value = "Gasto registrado por $ $amount (${category.label})."
                    syncIfSignedIn { it.syncExpense(expense) }
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    // --- Anulación de Venta ---

    fun voidSale(saleId: Long, reason: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.voidSale(saleId, reason, user.id, user.fullName)
            res.fold(
                onSuccess = {
                    _userMessage.value = "Venta anulada con éxito. Stock reincorporado y egreso asentado."
                    val sale = allChainSales.value.find { it.id == saleId } ?: branchSales.value.find { it.id == saleId }
                    if (sale != null) {
                        val bName = allBranches.value.find { it.id == sale.branchId }?.name ?: "Sucursal ${sale.branchId}"
                        syncIfSignedIn { it.syncSale(sale.copy(status = "VOIDED"), bName, user.fullName) }
                    }
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    // --- Recepción de Compras de Proveedores ---

    fun receiveGoods(
        productId: Long,
        supplierId: Long,
        quantity: Int,
        unitCost: Double,
        invoiceRef: String,
        batchNumber: String = "",
        expiryDays: Int = 90
    ) {
        val branch = _selectedBranch.value ?: return
        val user = _currentUser.value ?: return
        val expiryMillis = System.currentTimeMillis() + expiryDays.coerceAtLeast(1).toLong() * 86_400_000L

        viewModelScope.launch {
            val res = repository.receiveGoodsFromSupplier(
                branchId = branch.id,
                supplierId = supplierId,
                productId = productId,
                quantity = quantity,
                unitCost = unitCost,
                invoiceRef = invoiceRef,
                userId = user.id,
                userName = user.fullName,
                batchNumber = batchNumber,
                expiryDateMillis = expiryMillis
            )
            res.fold(
                onSuccess = {
                    _userMessage.value = "Mercadería recibida: +$quantity unidades al stock local."
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    fun writeOffExpiredBatch(batchId: Long, quantity: Int, notes: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.writeOffExpiredBatch(
                batchId = batchId,
                quantity = quantity,
                notes = notes,
                userId = user.id,
                userName = user.fullName
            )
            res.fold(
                onSuccess = {
                    _userMessage.value = "Baja de lote por vencimiento asentada en inventario."
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    fun updateCompanySettings(settings: CompanySettingsEntity) {
        val user = _currentUser.value ?: return
        if (user.role != UserRole.OWNER) {
            _errorMessage.value = "Solo el Propietario puede modificar los parámetros fiscales y operativos."
            return
        }
        viewModelScope.launch {
            val res = repository.updateCompanySettings(settings, user.id, user.fullName)
            res.fold(
                onSuccess = {
                    _userMessage.value = "Parámetros de la cadena actualizados correctamente."
                    syncIfSignedIn { it.syncCompanySettings(settings) }
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    // --- Alta / Modificación de Producto ---

    fun saveProduct(product: com.example.data.entity.ProductEntity) {
        val user = _currentUser.value ?: return
        if (user.role.level < 2) {
            _errorMessage.value = "Solo Encargado (Nivel 2) o Propietario (Nivel 3) pueden dar de alta productos nuevos."
            return
        }
        viewModelScope.launch {
            val res = repository.saveProduct(product, user.id, user.fullName)
            res.fold(
                onSuccess = {
                    _userMessage.value = "Producto '${product.name}' guardado en catálogo."
                },
                onFailure = {
                    _errorMessage.value = it.message
                }
            )
        }
    }

    // --- Ingreso Rápido Multi-Producto (Nivel 1, 2 y 3) ---

    fun receiveBatchScanIntake(
        lines: List<Triple<Long, Int, Double>>,
        supplierId: Long,
        invoiceRef: String
    ) {
        val branch = _selectedBranch.value ?: return
        val user = _currentUser.value ?: return
        if (lines.isEmpty()) {
            _errorMessage.value = "Escanee al menos un producto para confirmar el ingreso."
            return
        }
        viewModelScope.launch {
            var successUnits = 0
            lines.forEach { (productId, qty, cost) ->
                if (qty > 0) {
                    val res = repository.receiveGoodsFromSupplier(
                        branchId = branch.id,
                        supplierId = supplierId,
                        productId = productId,
                        quantity = qty,
                        unitCost = cost,
                        invoiceRef = invoiceRef.ifBlank { "ING-${System.currentTimeMillis() % 10000}" },
                        userId = user.id,
                        userName = user.fullName,
                        batchNumber = "LOT-${System.currentTimeMillis() % 100000}",
                        expiryDateMillis = System.currentTimeMillis() + 90L * 86_400_000L
                    )
                    if (res.isSuccess) successUnits += qty
                }
            }
            _userMessage.value = "Ingreso por lote confirmado: +$successUnits unidades sumadas al stock local."
        }
    }

    // --- Administración de Accesos y Dispositivos (Exclusivo Nivel 3 OWNER) ---

    fun createStaffUser(
        username: String,
        fullName: String,
        rawPin: String,
        role: UserRole,
        assignedBranchId: Long
    ) {
        val owner = _currentUser.value ?: return
        if (owner.role != UserRole.OWNER) {
            _errorMessage.value = "Acceso denegado: solo el Propietario (Nivel 3) puede crear permisos de acceso."
            return
        }
        viewModelScope.launch {
            repository.createStaffUser(
                username = username,
                fullName = fullName,
                rawPin = rawPin,
                role = role,
                assignedBranchId = assignedBranchId,
                ownerUserId = owner.id,
                ownerUserName = owner.fullName
            ).fold(
                onSuccess = { newId ->
                    _userMessage.value = "Acceso creado para '$fullName' (${role.label})."
                    syncIfSignedIn(reportError = true) { cloudRepo ->
                        repository.getUserByIdSync(newId)?.let { cloudRepo.syncStaffProfile(it) } ?: Result.success(Unit)
                    }
                },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun updateStaffAccess(
        targetUserId: Long,
        newRole: UserRole,
        newBranchId: Long,
        isActive: Boolean,
        newRawPin: String? = null
    ) {
        val owner = _currentUser.value ?: return
        if (owner.role != UserRole.OWNER) {
            _errorMessage.value = "Acceso denegado: solo el Propietario (Nivel 3) puede modificar accesos."
            return
        }
        viewModelScope.launch {
            repository.updateStaffAccess(
                targetUserId = targetUserId,
                newRole = newRole,
                newBranchId = newBranchId,
                isActive = isActive,
                newRawPin = newRawPin,
                ownerUserId = owner.id,
                ownerUserName = owner.fullName
            ).fold(
                onSuccess = {
                    _userMessage.value = "Permisos de acceso actualizados correctamente."
                    syncIfSignedIn(reportError = true) { cloudRepo ->
                        repository.getUserByIdSync(targetUserId)?.let { cloudRepo.syncStaffProfile(it) } ?: Result.success(Unit)
                    }
                },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun authorizeDevice(targetDeviceId: String, deviceName: String, branchId: Long) {
        val owner = _currentUser.value ?: return
        if (owner.role != UserRole.OWNER) {
            _errorMessage.value = "Solo el Nivel 3 (Propietario) puede autorizar dispositivos nuevos."
            return
        }
        viewModelScope.launch {
            repository.authorizeDeviceByOwner(
                targetDeviceId = targetDeviceId,
                deviceName = deviceName,
                assignedBranchId = branchId,
                ownerUserId = owner.id,
                ownerUserName = owner.fullName
            ).fold(
                onSuccess = {
                    _userMessage.value = "Dispositivo '$targetDeviceId' habilitado para K0$branchId."
                    syncIfSignedIn(reportError = true) { cloudRepo ->
                        repository.getDeviceByIdSync(targetDeviceId.trim().uppercase())?.let {
                            cloudRepo.syncAuthorizedDevice(it)
                        } ?: Result.success(Unit)
                    }
                },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun createDevicePairingInvite(
        deviceAlias: String,
        branchId: Long,
        onTokenCreated: (String) -> Unit = {}
    ) {
        createDevicePairingPackage(deviceAlias, branchId) { shortToken, _ ->
            onTokenCreated(shortToken)
        }
    }

    fun createDevicePairingPackage(
        deviceAlias: String,
        branchId: Long,
        onPackageCreated: (shortToken: String, portableBundle: String) -> Unit
    ) {
        val owner = _currentUser.value ?: return
        if (owner.role != UserRole.OWNER) {
            _errorMessage.value = "Solo el Nivel 3 (Propietario) puede habilitar nuevos dispositivos."
            return
        }
        viewModelScope.launch {
            repository.createPairingInvite(
                deviceAlias = deviceAlias,
                assignedBranchId = branchId,
                ownerUserId = owner.id,
                ownerUserName = owner.fullName
            ).fold(
                onSuccess = { token ->
                    val portable = repository.generatePortableProvisioningBundle(
                        pairingCode = token,
                        assignedBranchId = branchId,
                        deviceAlias = deviceAlias,
                        ownerUserName = owner.fullName
                    )
                    _userMessage.value = "Código $token y llave portátil listos para enviar al nuevo teléfono."
                    pushStaffAndDevicesInternal(silent = true)
                    onPackageCreated(token, portable)
                },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun buildAllStaffPortablePackage(branchId: Long, onPackageReady: (String) -> Unit) {
        val owner = _currentUser.value ?: return
        if (owner.role != UserRole.OWNER) {
            _errorMessage.value = "Solo el Nivel 3 (Propietario) puede exportar accesos."
            return
        }
        viewModelScope.launch {
            val branchName = allBranches.value.find { it.id == branchId }?.name ?: "Sucursal K0$branchId"
            val pkg = repository.generatePortableProvisioningBundle(
                pairingCode = "DRK-SYNC",
                assignedBranchId = branchId,
                deviceAlias = "Terminal $branchName",
                ownerUserName = owner.fullName
            )
            onPackageReady(pkg)
        }
    }

    fun revokeDevice(targetDeviceId: String) {
        val owner = _currentUser.value ?: return
        if (owner.role != UserRole.OWNER) {
            _errorMessage.value = "Solo el Nivel 3 (Propietario) puede revocar dispositivos."
            return
        }
        viewModelScope.launch {
            repository.revokeDeviceByOwner(
                targetDeviceId = targetDeviceId,
                ownerUserId = owner.id,
                ownerUserName = owner.fullName
            ).fold(
                onSuccess = {
                    _userMessage.value = "Dispositivo '$targetDeviceId' bloqueado."
                    syncIfSignedIn(reportError = true) { cloudRepo ->
                        repository.getDeviceByIdSync(targetDeviceId.trim().uppercase())?.let {
                            cloudRepo.syncAuthorizedDevice(it)
                        } ?: Result.success(Unit)
                    }
                },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun redeemPairingCode(code: String) {
        val deviceId = _currentDeviceId.value.ifBlank { "DRK-LOCAL" }
        val cleanInput = code.trim()
        if (cleanInput.isBlank()) return
        viewModelScope.launch {
            if (DeviceProvisioningCodec.isPortableBundle(cleanInput)) {
                DeviceProvisioningCodec.decode(cleanInput).fold(
                    onSuccess = { bundle ->
                        repository.applyPortableProvisioningBundle(
                            bundle = bundle,
                            currentDeviceId = deviceId,
                            currentDeviceName = _currentDeviceName.value
                        ).fold(
                            onSuccess = { (branchId, importedCount) ->
                                allBranches.value.find { it.id == branchId }?.let { _selectedBranch.value = it }
                                _cloudAccessSyncStatus.value = "Llave portátil importada (${bundle.profiles.size} perfiles verificados, $importedCount actualizados)."
                                _userMessage.value = "Terminal vinculada a K0$branchId • $importedCount perfil(es)/PIN(s) nuevos o actualizados."
                            },
                            onFailure = { _errorMessage.value = it.message }
                        )
                    },
                    onFailure = { err ->
                        _errorMessage.value = err.message ?: "Código portátil inválido o incompleto."
                    }
                )
            } else {
                // Si hay cuenta Google conectada, descargar primero usuarios y terminales desde Firestore
                pullCloudAccessSnapshot(silent = true)
                repository.redeemPairingCodeForDevice(
                    currentDeviceId = deviceId,
                    currentDeviceName = _currentDeviceName.value,
                    pairingCode = cleanInput
                ).fold(
                    onSuccess = { branchId ->
                        allBranches.value.find { it.id == branchId }?.let { _selectedBranch.value = it }
                        _userMessage.value = "Terminal autorizada y vinculada a Sucursal K0$branchId."
                        syncIfSignedIn(reportError = false) { cloudRepo ->
                            repository.getDeviceByIdSync(deviceId)?.let { cloudRepo.syncAuthorizedDevice(it) } ?: Result.success(Unit)
                        }
                    },
                    onFailure = {
                        _errorMessage.value = "Código '$cleanInput' no encontrado en la nube ni en este equipo. En el teléfono Nivel 3 tocá 'Subir PINs a Nube' o pegá la Llave Portátil (DRKPRO1:...)."
                    }
                )
            }
        }
    }

    fun onFirebaseUserChanged(uid: String?, isOwner: Boolean) {
        if (uid.isNullOrBlank()) {
            activeRealtimeUid = null
            realtimeSyncJob?.cancel()
            realtimeSyncJob = null
            _cloudAccessSyncStatus.value = null
            return
        }
        val cloudRepo = cloudSyncRepository ?: return
        if (activeRealtimeUid != uid) {
            activeRealtimeUid = uid
            realtimeSyncJob?.cancel()
            realtimeSyncJob = viewModelScope.launch {
                launch {
                    cloudRepo.observeCloudStaffProfiles(uid)
                        .catch { err ->
                            _cloudAccessSyncStatus.value = "Error leyendo PINs de nube: ${err.localizedMessage ?: err.message}"
                        }
                        .collect { cloudStaff ->
                            val localProfiles = cloudStaff.mapNotNull { cp ->
                                val role = runCatching { UserRole.valueOf(cp.role) }.getOrNull() ?: return@mapNotNull null
                                UserProfileEntity(
                                    id = 0L,
                                    username = cp.username,
                                    fullName = cp.fullName,
                                    pinCode = cp.pinHash,
                                    role = role,
                                    assignedBranchId = cp.assignedBranchId,
                                    isActive = cp.isActive
                                )
                            }
                            if (localProfiles.isNotEmpty()) {
                                repository.upsertSyncedStaffProfiles(localProfiles)
                            }
                            _cloudAccessSyncStatus.value = "Nube activa: ${cloudStaff.size} perfil(es) sincronizados."
                        }
                }
                launch {
                    cloudRepo.observeCloudDevices(uid)
                        .catch { }
                        .collect { cloudDevices ->
                            val localDevices = cloudDevices.map { cd ->
                                AuthorizedDeviceEntity(
                                    id = 0L,
                                    deviceId = cd.deviceId,
                                    deviceName = cd.deviceName,
                                    assignedBranchId = cd.assignedBranchId,
                                    status = cd.status,
                                    pairingCode = cd.pairingCode,
                                    approvedByUserName = cd.approvedByUserName
                                )
                            }
                            if (localDevices.isNotEmpty()) {
                                repository.upsertSyncedDevices(localDevices)
                            }
                        }
                }
            }
        }
        viewModelScope.launch {
            if (isOwner) {
                pushStaffAndDevicesInternal(silent = true)
            } else {
                pullCloudAccessSnapshot(silent = true)
            }
        }
    }

    fun pushLocalAccessToCloud(silent: Boolean = false) {
        viewModelScope.launch {
            pushStaffAndDevicesInternal(silent = silent)
        }
    }

    private suspend fun pushStaffAndDevicesInternal(silent: Boolean) {
        val cloudRepo = cloudSyncRepository ?: return
        val isSignedIn = try {
            Firebase.auth.currentUser != null
        } catch (_: Exception) {
            false
        }
        if (!isSignedIn) {
            if (!silent) {
                _errorMessage.value = "Conecte una cuenta Google primero en 'Nube' o desde la pantalla de PIN."
            }
            return
        }
        _isCloudAccessSyncing.value = true
        try {
            val users = repository.getAllUsersAdminSync()
            val devices = repository.getAllDevicesSync()
            cloudRepo.syncAllStaffAndDevices(users, devices).fold(
                onSuccess = { (uCount, dCount) ->
                    _cloudAccessSyncStatus.value = "Nube al día: $uCount perfil(es) y $dCount terminal(es) respaldados."
                    if (!silent) {
                        _userMessage.value = "Subidos a la nube: $uCount perfil(es)/PIN(s) y $dCount terminal(es)."
                    }
                },
                onFailure = { err ->
                    val detail = err.localizedMessage ?: err.message ?: "Error al subir PINs a Firestore."
                    _cloudAccessSyncStatus.value = "Error nube: $detail"
                    if (!silent) {
                        _errorMessage.value = "Error subiendo a la nube: $detail"
                    }
                }
            )
        } finally {
            _isCloudAccessSyncing.value = false
        }
    }

    fun syncAccessProfilesFromCloud(silent: Boolean = false) {
        viewModelScope.launch {
            pullCloudAccessSnapshot(silent = silent)
        }
    }

    private suspend fun pullCloudAccessSnapshot(silent: Boolean) {
        val cloudRepo = cloudSyncRepository ?: return
        val isSignedIn = try {
            Firebase.auth.currentUser != null
        } catch (_: Exception) {
            false
        }
        if (!isSignedIn) {
            if (!silent) {
                _errorMessage.value = "Conecte primero la cuenta Google de la cadena o pegue una Llave Portátil (DRKPRO1:...)."
            }
            return
        }
        _isCloudAccessSyncing.value = true
        try {
            cloudRepo.fetchCloudStaffAndDevicesOnce().fold(
                onSuccess = { (cloudStaff, cloudDevices) ->
                    val localProfiles = cloudStaff.mapNotNull { cp ->
                        val role = runCatching { UserRole.valueOf(cp.role) }.getOrNull() ?: return@mapNotNull null
                        UserProfileEntity(
                            id = 0L,
                            username = cp.username,
                            fullName = cp.fullName,
                            pinCode = cp.pinHash,
                            role = role,
                            assignedBranchId = cp.assignedBranchId,
                            isActive = cp.isActive
                        )
                    }
                    val localDevices = cloudDevices.map { cd ->
                        AuthorizedDeviceEntity(
                            id = 0L,
                            deviceId = cd.deviceId,
                            deviceName = cd.deviceName,
                            assignedBranchId = cd.assignedBranchId,
                            status = cd.status,
                            pairingCode = cd.pairingCode,
                            approvedByUserName = cd.approvedByUserName
                        )
                    }
                    val updatedUsers = repository.upsertSyncedStaffProfiles(localProfiles)
                    val updatedDevs = repository.upsertSyncedDevices(localDevices)
                    _cloudAccessSyncStatus.value = "Nube verificada: ${cloudStaff.size} perfil(es) / ${cloudDevices.size} terminal(es)."
                    if (!silent) {
                        _userMessage.value = when {
                            cloudStaff.isEmpty() -> "La nube tiene 0 perfiles todavía. En el celular Nivel 3 tocá 'Subir PINs a la Nube'."
                            updatedUsers + updatedDevs > 0 -> "Nube sincronizada: $updatedUsers PIN(s)/usuario(s) y $updatedDevs terminal(es) actualizados."
                            else -> "Accesos al día con la nube (${cloudStaff.size} perfiles verificados)."
                        }
                    }
                },
                onFailure = { err ->
                    val detail = err.localizedMessage ?: err.message ?: "No se pudieron descargar los accesos de la nube."
                    _cloudAccessSyncStatus.value = "Error nube: $detail"
                    if (!silent) {
                        _errorMessage.value = detail
                    }
                }
            )
        } finally {
            _isCloudAccessSyncing.value = false
        }
    }

    fun toggleStrictDeviceWhitelist(enabled: Boolean) {
        val owner = _currentUser.value ?: return
        if (owner.role != UserRole.OWNER) {
            _errorMessage.value = "Solo el Propietario (Nivel 3) puede cambiar la política de dispositivos."
            return
        }
        val current = companySettings.value
        updateCompanySettings(current.copy(strictDeviceWhitelist = enabled))
    }

    // --- Comunicación entre Locales (Nivel 2 y 3) ---

    fun sendBranchMessage(toBranchId: Long, priority: String, subject: String, message: String) {
        val user = _currentUser.value ?: return
        val branch = _selectedBranch.value ?: return
        if (user.role.level < 2) {
            _errorMessage.value = "Solo Encargado (Nivel 2) o Propietario (Nivel 3) pueden enviar comunicaciones entre locales."
            return
        }
        viewModelScope.launch {
            repository.sendBranchMessage(
                fromBranchId = branch.id,
                toBranchId = toBranchId,
                senderUserId = user.id,
                senderName = user.fullName,
                senderRoleLevel = user.role.level,
                priority = priority,
                subject = subject,
                message = message
            ).fold(
                onSuccess = { _userMessage.value = "Mensaje enviado a la red Drakos." },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun markMessageAsRead(messageId: Long) {
        viewModelScope.launch { repository.markMessageAsRead(messageId) }
    }

    // --- Facturación de Proveedores (Nivel 2 y 3) ---

    fun registerSupplierInvoice(
        supplierId: Long,
        supplierName: String,
        invoiceType: String,
        invoiceNumber: String,
        totalAmount: Double,
        paymentStatus: String,
        dueDays: Int,
        notes: String
    ) {
        val user = _currentUser.value ?: return
        val branch = _selectedBranch.value ?: return
        if (user.role.level < 2) {
            _errorMessage.value = "Solo Nivel 2 o Nivel 3 pueden ingresar facturación de proveedores."
            return
        }
        viewModelScope.launch {
            repository.registerSupplierInvoice(
                branchId = branch.id,
                supplierId = supplierId,
                supplierName = supplierName,
                invoiceType = invoiceType,
                invoiceNumber = invoiceNumber,
                totalAmount = totalAmount,
                paymentStatus = paymentStatus,
                dueDays = dueDays,
                notes = notes,
                userId = user.id,
                userName = user.fullName
            ).fold(
                onSuccess = { _userMessage.value = "Comprobante $invoiceNumber registrado." },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun markInvoicePaid(invoiceId: Long) {
        val user = _currentUser.value ?: return
        if (user.role.level < 2) return
        viewModelScope.launch {
            repository.markSupplierInvoicePaid(invoiceId, user.id, user.fullName).fold(
                onSuccess = { _userMessage.value = "Factura marcada como pagada." },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    // --- Pedidos Semanales (Nivel 2 y 3) ---

    fun saveWeeklyOrder(
        supplierId: Long,
        supplierName: String,
        totalEstimatedCost: Double,
        totalUnits: Int,
        itemsSummary: String,
        notes: String
    ) {
        val user = _currentUser.value ?: return
        val branch = _selectedBranch.value ?: return
        if (user.role.level < 2) {
            _errorMessage.value = "Solo Nivel 2 o Nivel 3 pueden emitir pedidos semanales."
            return
        }
        viewModelScope.launch {
            repository.saveWeeklyOrder(
                branchId = branch.id,
                supplierId = supplierId,
                supplierName = supplierName,
                totalEstimatedCost = totalEstimatedCost,
                totalUnits = totalUnits,
                itemsSummary = itemsSummary,
                notes = notes,
                userId = user.id,
                userName = user.fullName
            ).fold(
                onSuccess = { _userMessage.value = "Pedido semanal emitido a $supplierName ($totalUnits u.)." },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun markWeeklyOrderReceived(orderId: Long) {
        val user = _currentUser.value ?: return
        if (user.role.level < 2) return
        viewModelScope.launch {
            repository.updateWeeklyOrderStatus(orderId, "RECEIVED", user.id, user.fullName).fold(
                onSuccess = { _userMessage.value = "Pedido semanal marcado como recibido." },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    // --- Exportación CSV ---

    fun generateCsvExport(type: String): String {
        val user = _currentUser.value

        return when (type) {
            "SALES" -> {
                val list = if (user?.role == UserRole.OWNER) allChainSales.value else branchSales.value
                val header = "ID;Ticket;Fecha;Sucursal;Total;MedioPago;Estado\n"
                val rows = list.joinToString("\n") {
                    "${it.id};${it.receiptNumber};${it.createdAt};${it.branchId};${it.totalAmount};${it.paymentMethod.name};${it.status}"
                }
                header + rows
            }
            "STOCK" -> {
                val list = filteredProducts.value
                val header = "ID;CodigoBarras;Producto;Categoria;PrecioVenta;Costo;Stock;PuntoPedido\n"
                val rows = list.joinToString("\n") {
                    "${it.productId};${it.barcode};${it.name};${it.categoryName};${it.sellingPrice};${it.costPrice};${it.quantityOnHand};${it.minStock}"
                }
                header + rows
            }
            else -> {
                val list = if (user?.role == UserRole.OWNER) allExpenses.value else branchExpenses.value
                val header = "ID;Categoria;Monto;Detalle;Origen;Fecha\n"
                val rows = list.joinToString("\n") {
                    "${it.id};${it.category.name};${it.amount};${it.description};${it.paymentSource};${it.createdAt}"
                }
                header + rows
            }
        }
    }

    fun notifyInfo(message: String) {
        _userMessage.value = message
    }

    fun applyBulkPriceUpdate(
        categoryId: Long?,
        costPercentDelta: Double,
        pricePercentDelta: Double,
        preserveMargin: Boolean,
        roundingStep: PriceRoundingStep
    ) {
        val user = _currentUser.value ?: return
        if (user.role.level < 2) {
            _errorMessage.value = "Solo Encargado (Nivel 2) o Propietario (Nivel 3) pueden ajustar precios masivamente."
            return
        }
        viewModelScope.launch {
            repository.applyBulkPriceUpdate(
                categoryId = categoryId,
                costPercentDelta = costPercentDelta,
                pricePercentDelta = pricePercentDelta,
                preserveMargin = preserveMargin,
                roundingStep = roundingStep,
                userId = user.id,
                userName = user.fullName
            ).fold(
                onSuccess = { count ->
                    _userMessage.value = "Precios actualizados en $count producto(s) (${roundingStep.label})."
                },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun applyExpiryClearanceMarkdown(
        productId: Long,
        batchNumber: String,
        discountPercent: Double = 25.0
    ) {
        val user = _currentUser.value ?: return
        val branch = _selectedBranch.value ?: return
        if (user.role.level < 2) {
            _errorMessage.value = "Solo Encargado (Nivel 2) o Propietario (Nivel 3) pueden aplicar liquidaciones por vencimiento."
            return
        }
        viewModelScope.launch {
            repository.applyBatchClearanceMarkdown(
                productId = productId,
                branchId = branch.id,
                batchNumber = batchNumber,
                discountPercent = discountPercent,
                userId = user.id,
                userName = user.fullName
            ).fold(
                onSuccess = { newPrice ->
                    _userMessage.value = "Liquidación -${discountPercent.toInt()}% aplicada al lote $batchNumber (Nuevo precio: $${newPrice.toInt()})."
                },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun notifyError(message: String) {
        _errorMessage.value = message
    }

    fun clearMessages() {
        _userMessage.value = null
        _errorMessage.value = null
    }

    private fun syncIfSignedIn(
        reportError: Boolean = false,
        block: suspend (CloudSyncRepository) -> Result<Any>
    ) {
        val cloudRepo = cloudSyncRepository ?: return
        val signedIn = try {
            Firebase.auth.currentUser != null
        } catch (_: Exception) {
            false
        }
        if (!signedIn) return
        viewModelScope.launch {
            runCatching { block(cloudRepo) }
                .onSuccess { res ->
                    res.onFailure { err ->
                        val msg = err.localizedMessage ?: err.message ?: "Error al sincronizar con Firestore."
                        _cloudAccessSyncStatus.value = "Error nube: $msg"
                        if (reportError) {
                            _errorMessage.value = "Guardado localmente, pero falló subida a nube: $msg"
                        }
                    }
                }
                .onFailure { err ->
                    val msg = err.localizedMessage ?: err.message ?: "Error de red en sincronización."
                    _cloudAccessSyncStatus.value = "Error nube: $msg"
                    if (reportError) {
                        _errorMessage.value = "Guardado localmente, pero falló subida a nube: $msg"
                    }
                }
        }
    }
}

class KioscoViewModelFactory(
    private val repository: KioscoRepository,
    private val cloudSyncRepository: CloudSyncRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KioscoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KioscoViewModel(repository, cloudSyncRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
