package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.attemptAutoSignIn
import com.example.data.cloud.authStateFlow
import com.example.domain.model.DifferenceStatus
import com.example.domain.model.UserRole
import com.example.ui.components.DrakosAlertItem
import com.example.ui.components.DrakosSaasTopBar
import com.example.ui.components.DrakosSidebarContent
import com.example.ui.components.DrakosSidebarDest
import com.example.ui.components.PinLoginModal
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ManagementHubScreen
import com.example.ui.screens.PinLoginScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ShiftsScreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosLightCanvas
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.KioscoPOSTheme
import com.example.ui.viewmodel.KioscoViewModel
import com.example.ui.viewmodel.KioscoViewModelFactory
import com.example.util.DeviceIdentity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class KioscoNavScreen(
    val title: String,
    val icon: ImageVector,
    val minRoleLevel: Int // 1: Empleado, 2: Encargado, 3: Propietario
) {
    DASHBOARD("Inicio", Icons.Default.Dashboard, 1),
    POS("POS", Icons.Default.PointOfSale, 1),
    INVENTORY("Stock", Icons.Default.Inventory, 1),
    SHIFTS("Caja", Icons.Default.Lock, 1),
    MANAGEMENT("Gestión", Icons.Default.Shield, 2)
}

class MainActivity : ComponentActivity() {

    private val viewModel: KioscoViewModel by viewModels {
        val app = application as KioscoApp
        KioscoViewModelFactory(app.repository, app.cloudSyncRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KioscoPOSTheme {
                KioscoMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KioscoMainApp(viewModel: KioscoViewModel) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val firebaseAuth = remember { Firebase.auth }
    val firebaseUser by remember { firebaseAuth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = firebaseAuth.currentUser)

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var selectedScreen by remember { mutableStateOf(KioscoNavScreen.DASHBOARD) }
    var managementSubTab by remember { mutableIntStateOf(0) }
    var intakeScannerSignal by remember { mutableIntStateOf(0) }
    var showUserSwitchModal by remember { mutableStateOf(false) }
    var showAccessDeniedDialog by remember { mutableStateOf(false) }
    var sidebarExpandedOnDesktop by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeCashSession.collectAsStateWithLifecycle()
    val estimatedDrawerCash by viewModel.estimatedDrawerCash.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val branchProducts by viewModel.allBranchProducts.collectAsStateWithLifecycle()
    val branchBatches by viewModel.branchBatches.collectAsStateWithLifecycle()
    val branchMessages by viewModel.branchMessages.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val authorizedDevices by viewModel.authorizedDevices.collectAsStateWithLifecycle()
    val failedAttempts by viewModel.failedPinAttempts.collectAsStateWithLifecycle()
    val lockoutUntilMillis by viewModel.lockoutUntilMillis.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.initDeviceIdentity(
            deviceId = DeviceIdentity.getOrCreateDeviceId(context),
            deviceName = DeviceIdentity.getDeviceModelLabel()
        )
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = this
        )
    }

    LaunchedEffect(firebaseUser?.uid, currentUser?.role) {
        viewModel.onFirebaseUserChanged(
            uid = firebaseUser?.uid,
            isOwner = currentUser?.role == UserRole.OWNER
        )
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Puerta de seguridad PIN al inicio o tras bloqueo de terminal
    if (currentUser == null) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = DrakosLightCanvas,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            PinLoginScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    val userLevel = currentUser?.role?.level ?: 1
    val visibleScreens = remember(userLevel) {
        KioscoNavScreen.entries.filter { userLevel >= it.minRoleLevel }
    }

    val headerAlerts = remember(
        activeSession,
        estimatedDrawerCash,
        companySettings,
        branchProducts,
        branchBatches,
        branchMessages,
        allSessions,
        authorizedDevices,
        userLevel
    ) {
        buildList {
            if (activeSession == null) {
                add(
                    DrakosAlertItem(
                        title = "Caja Cerrada en Sucursal",
                        detail = "Abra el turno en Caja para habilitar cobros POS.",
                        isCritical = true,
                        actionLabel = "Abrir",
                        onAction = { selectedScreen = KioscoNavScreen.SHIFTS }
                    )
                )
            } else if (estimatedDrawerCash >= companySettings.cashDropAlertThreshold) {
                add(
                    DrakosAlertItem(
                        title = "Gaveta con Efectivo Alto",
                        detail = "Se recomienda retiro parcial de seguridad.",
                        isCritical = true,
                        actionLabel = "Retirar",
                        onAction = { selectedScreen = KioscoNavScreen.SHIFTS }
                    )
                )
            }

            val lowStockCount = branchProducts.count { it.quantityOnHand <= it.minStock }
            if (lowStockCount > 0) {
                add(
                    DrakosAlertItem(
                        title = "$lowStockCount Producto(s) Bajo Mínimo",
                        detail = "Requieren reposición de inventario en esta sucursal.",
                        isCritical = false,
                        actionLabel = "Stock",
                        onAction = { selectedScreen = KioscoNavScreen.INVENTORY }
                    )
                )
            }

            val criticalBatchesCount = branchBatches.count { it.isCritical }
            if (criticalBatchesCount > 0) {
                add(
                    DrakosAlertItem(
                        title = "$criticalBatchesCount Lote(s) Próximo(s) a Vencer",
                        detail = "Revise fechas de vencimiento y aplique liquidación.",
                        isCritical = true,
                        actionLabel = "Lotes",
                        onAction = { selectedScreen = KioscoNavScreen.INVENTORY }
                    )
                )
            }

            if (userLevel >= 2) {
                val unreadMsgs = branchMessages.count { !it.isRead }
                if (unreadMsgs > 0) {
                    add(
                        DrakosAlertItem(
                            title = "$unreadMsgs Mensaje(s) de Locales",
                            detail = "Comunicación interna sin leer entre sucursales.",
                            isCritical = false,
                            actionLabel = "Leer",
                            onAction = {
                                managementSubTab = 1
                                selectedScreen = KioscoNavScreen.MANAGEMENT
                            }
                        )
                    )
                }
            }

            if (userLevel >= 3) {
                val pendingVariances = allSessions.count { it.differenceStatus == DifferenceStatus.PENDING_APPROVAL }
                if (pendingVariances > 0) {
                    add(
                        DrakosAlertItem(
                            title = "$pendingVariances Cierre(s) con Descuadre",
                            detail = "Diferencia pendiente de aprobación de Dueño.",
                            isCritical = true,
                            actionLabel = "Auditar",
                            onAction = { selectedScreen = KioscoNavScreen.SHIFTS }
                        )
                    )
                }
                val pendingDevices = authorizedDevices.count { it.status == "PENDING" }
                if (pendingDevices > 0) {
                    add(
                        DrakosAlertItem(
                            title = "$pendingDevices Equipo(s) Solicitando Permiso",
                            detail = "Autorice terminales nuevas desde Gestión > Accesos.",
                            isCritical = false,
                            actionLabel = "Revisar",
                            onAction = {
                                managementSubTab = 4
                                selectedScreen = KioscoNavScreen.MANAGEMENT
                            }
                        )
                    )
                }
            }
        }
    }

    val unreadMsgCount = remember(branchMessages) { branchMessages.count { !it.isRead } }
    val pendingDeviceCount = remember(authorizedDevices) { authorizedDevices.count { it.status == "PENDING" } }

    fun navigateFromDrawer(screen: KioscoNavScreen, mgmtTab: Int? = null) {
        if (mgmtTab != null) managementSubTab = mgmtTab
        selectedScreen = screen
        scope.launch { drawerState.close() }
    }

    val sidebarDestinations = listOf(
        DrakosSidebarDest(
            id = "dashboard",
            label = if (userLevel >= 2) "Dashboard" else "Operación Rápida",
            icon = Icons.Default.Dashboard,
            minLevel = 1,
            selected = selectedScreen == KioscoNavScreen.DASHBOARD,
            onClick = { navigateFromDrawer(KioscoNavScreen.DASHBOARD) }
        ),
        DrakosSidebarDest(
            id = "pos",
            label = "Punto de Venta",
            icon = Icons.Default.PointOfSale,
            minLevel = 1,
            selected = selectedScreen == KioscoNavScreen.POS,
            onClick = { navigateFromDrawer(KioscoNavScreen.POS) }
        ),
        DrakosSidebarDest(
            id = "inventory",
            label = "Inventario",
            icon = Icons.Default.Inventory,
            minLevel = 1,
            selected = selectedScreen == KioscoNavScreen.INVENTORY,
            onClick = { navigateFromDrawer(KioscoNavScreen.INVENTORY) }
        ),
        DrakosSidebarDest(
            id = "shifts",
            label = "Caja y Turnos",
            icon = Icons.Default.Lock,
            minLevel = 1,
            selected = selectedScreen == KioscoNavScreen.SHIFTS,
            onClick = { navigateFromDrawer(KioscoNavScreen.SHIFTS) }
        ),
        DrakosSidebarDest(
            id = "transfers",
            label = "Transferencias",
            icon = Icons.Default.SwapHoriz,
            minLevel = 2,
            selected = selectedScreen == KioscoNavScreen.MANAGEMENT && managementSubTab == 0,
            onClick = { navigateFromDrawer(KioscoNavScreen.MANAGEMENT, 0) }
        ),
        DrakosSidebarDest(
            id = "comms",
            label = "Locales y Mensajes",
            icon = Icons.Default.Forum,
            minLevel = 2,
            selected = selectedScreen == KioscoNavScreen.MANAGEMENT && managementSubTab == 1,
            badgeCount = unreadMsgCount,
            onClick = { navigateFromDrawer(KioscoNavScreen.MANAGEMENT, 1) }
        ),
        DrakosSidebarDest(
            id = "access",
            label = "Accesos y Dispositivos",
            icon = Icons.Default.AdminPanelSettings,
            minLevel = 3,
            selected = selectedScreen == KioscoNavScreen.MANAGEMENT && managementSubTab == 4,
            badgeCount = pendingDeviceCount,
            onClick = { navigateFromDrawer(KioscoNavScreen.MANAGEMENT, 4) }
        ),
        DrakosSidebarDest(
            id = "expenses",
            label = "Gastos Operativos",
            icon = Icons.Default.AttachMoney,
            minLevel = 3,
            selected = selectedScreen == KioscoNavScreen.MANAGEMENT && managementSubTab == 2,
            onClick = { navigateFromDrawer(KioscoNavScreen.MANAGEMENT, 2) }
        ),
        DrakosSidebarDest(
            id = "audit",
            label = "Auditoría Forense",
            icon = Icons.Default.Shield,
            minLevel = 3,
            selected = selectedScreen == KioscoNavScreen.MANAGEMENT && managementSubTab == 3,
            onClick = { navigateFromDrawer(KioscoNavScreen.MANAGEMENT, 3) }
        ),
        DrakosSidebarDest(
            id = "cloud",
            label = "Nube Sync",
            icon = Icons.Default.CloudSync,
            minLevel = 2,
            selected = selectedScreen == KioscoNavScreen.MANAGEMENT && managementSubTab == 5,
            onClick = { navigateFromDrawer(KioscoNavScreen.MANAGEMENT, 5) }
        )
    )

    val (pageTitle, pageSubtitle) = when (selectedScreen) {
        KioscoNavScreen.DASHBOARD -> when (userLevel) {
            3 -> "Dashboard" to "Resumen general de tu cadena (${branches.size} sucursales)"
            2 -> "Métricas de Sucursal" to "${currentBranch?.name ?: "Drakos"} • Operación y reposición"
            else -> "Mostrador Drakos" to "Ventas rápidas e ingreso de mercadería"
        }
        KioscoNavScreen.POS -> "Punto de Venta" to "Escáner EAN, carrito en tiempo real y cobro ágil"
        KioscoNavScreen.INVENTORY -> "Inventario" to "Existencias, planilla multi-local, pedidos y lotes"
        KioscoNavScreen.SHIFTS -> "Caja y Turnos" to "Arqueo ciego, retiros de seguridad y auditoría"
        KioscoNavScreen.MANAGEMENT -> "Administración & Seguridad" to "Transferencias, locales, dispositivos, gastos y nube"
    }

    // Volver al Dashboard inicial con el botón atrás desde pantallas secundarias
    BackHandler(enabled = selectedScreen != KioscoNavScreen.DASHBOARD || drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            selectedScreen = KioscoNavScreen.DASHBOARD
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DrakosWhite)
    ) {
        val isWideScreen = maxWidth >= 760.dp

        val appShellContent: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DrakosLightCanvas)
                    .statusBarsPadding()
            ) {
                if (isWideScreen) {
                    AnimatedVisibility(visible = sidebarExpandedOnDesktop) {
                        DrakosSidebarContent(
                            currentUser = currentUser,
                            destinations = sidebarDestinations,
                            modifier = Modifier
                                .width(232.dp)
                                .fillMaxHeight()
                                .navigationBarsPadding()
                        )
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    containerColor = DrakosLightCanvas,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        DrakosSaasTopBar(
                            pageTitle = pageTitle,
                            pageSubtitle = pageSubtitle,
                            currentUser = currentUser,
                            currentBranch = currentBranch,
                            branches = branches,
                            activeSession = activeSession,
                            isCloudConnected = firebaseUser != null,
                            alerts = headerAlerts,
                            onToggleMenu = {
                                if (isWideScreen) {
                                    sidebarExpandedOnDesktop = !sidebarExpandedOnDesktop
                                } else {
                                    scope.launch { drawerState.open() }
                                }
                            },
                            onBranchSelected = { viewModel.selectBranch(it) },
                            onOpenUserSwitch = { showUserSwitchModal = true },
                            onOpenCloudSync = {
                                if (userLevel >= 2) {
                                    managementSubTab = 5
                                    selectedScreen = KioscoNavScreen.MANAGEMENT
                                } else {
                                    showAccessDeniedDialog = true
                                }
                            },
                            onNavigateToShifts = { selectedScreen = KioscoNavScreen.SHIFTS }
                        )
                    },
                    bottomBar = {
                        if (!isWideScreen) {
                            Surface(
                                color = DrakosBlack,
                                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                                tonalElevation = 6.dp
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                ) {
                                    visibleScreens.forEach { screen ->
                                        val navLabel = when {
                                            screen == KioscoNavScreen.DASHBOARD && userLevel == 3 -> "Finanzas"
                                            screen == KioscoNavScreen.DASHBOARD && userLevel == 2 -> "Métricas"
                                            else -> screen.title
                                        }
                                        NavigationBarItem(
                                            selected = selectedScreen == screen,
                                            onClick = { selectedScreen = screen },
                                            icon = {
                                                Icon(
                                                    imageVector = if (screen == KioscoNavScreen.DASHBOARD && userLevel >= 2) {
                                                        Icons.Default.Assessment
                                                    } else {
                                                        screen.icon
                                                    },
                                                    contentDescription = navLabel,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = navLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (selectedScreen == screen) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = DrakosWhite,
                                                selectedTextColor = DrakosWhite,
                                                indicatorColor = DrakosRed,
                                                unselectedIconColor = Color(0xFF9CA3AF),
                                                unselectedTextColor = Color(0xFF9CA3AF)
                                            ),
                                            modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedScreen) {
                            KioscoNavScreen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToPos = { selectedScreen = KioscoNavScreen.POS },
                                onNavigateToStockIntake = { openScanner ->
                                    if (openScanner) intakeScannerSignal++
                                    selectedScreen = KioscoNavScreen.INVENTORY
                                },
                                onNavigateToShifts = { selectedScreen = KioscoNavScreen.SHIFTS },
                                onNavigateToComms = {
                                    managementSubTab = 1
                                    selectedScreen = KioscoNavScreen.MANAGEMENT
                                }
                            )
                            KioscoNavScreen.POS -> PosScreen(
                                viewModel = viewModel,
                                onNavigateToShifts = { selectedScreen = KioscoNavScreen.SHIFTS },
                                onNavigateBack = { selectedScreen = KioscoNavScreen.DASHBOARD }
                            )
                            KioscoNavScreen.INVENTORY -> InventoryScreen(
                                viewModel = viewModel,
                                intakeScannerSignal = intakeScannerSignal
                            )
                            KioscoNavScreen.SHIFTS -> ShiftsScreen(viewModel = viewModel)
                            KioscoNavScreen.MANAGEMENT -> ManagementHubScreen(
                                viewModel = viewModel,
                                firebaseUser = firebaseUser,
                                selectedSubTab = managementSubTab,
                                onSelectSubTab = { managementSubTab = it }
                            )
                        }
                    }
                }
            }
        }

        if (isWideScreen) {
            appShellContent()
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = DrakosBlack,
                        modifier = Modifier.width(272.dp)
                    ) {
                        DrakosSidebarContent(
                            currentUser = currentUser,
                            destinations = sidebarDestinations,
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        )
                    }
                }
            ) {
                appShellContent()
            }
        }
    }

    if (showUserSwitchModal) {
        PinLoginModal(
            currentUser = currentUser,
            failedAttempts = failedAttempts,
            lockoutUntilMillis = lockoutUntilMillis,
            onLoginWithPin = { pin, onResult ->
                viewModel.loginWithPin(pin) { ok ->
                    if (ok) {
                        val updatedUser = viewModel.currentUser.value
                        if (updatedUser != null && updatedUser.role.level < selectedScreen.minRoleLevel) {
                            selectedScreen = KioscoNavScreen.DASHBOARD
                        }
                    }
                    onResult(ok)
                }
            },
            onLogout = {
                viewModel.logout()
                selectedScreen = KioscoNavScreen.DASHBOARD
            },
            onDismiss = { showUserSwitchModal = false }
        )
    }

    if (showAccessDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showAccessDeniedDialog = false },
            containerColor = DrakosWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = DangerRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acceso Denegado (RBAC)", fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Su perfil actual '${currentUser?.fullName}' posee rol '${currentUser?.role?.label}'.",
                        fontSize = 13.sp,
                        color = DrakosDarkGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Esta sección está restringida únicamente a Encargados (Nivel 2) o Propietario (Nivel 3).",
                        fontSize = 12.sp,
                        color = DrakosMutedGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAccessDeniedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
                ) {
                    Text("Comprendido")
                }
            }
        )
    }
}
