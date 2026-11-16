package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.components.PinLoginModal
import com.example.ui.components.TopHeader
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ManagementHubScreen
import com.example.ui.screens.PinLoginScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ShiftsScreen
import com.example.ui.theme.BrandNavy900
import com.example.ui.theme.BrandSlate700
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosGraphite
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.KioscoPOSTheme
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.KioscoViewModel
import com.example.ui.viewmodel.KioscoViewModelFactory
import com.example.util.DeviceIdentity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

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

    var selectedScreen by remember { mutableStateOf(KioscoNavScreen.DASHBOARD) }
    var managementSubTab by remember { mutableIntStateOf(0) }
    var intakeScannerSignal by remember { mutableIntStateOf(0) }
    var showUserSwitchModal by remember { mutableStateOf(false) }
    var showAccessDeniedDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeCashSession.collectAsStateWithLifecycle()
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
            isOwner = currentUser?.role == com.example.domain.model.UserRole.OWNER
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

    // Volver al Dashboard inicial con el botón atrás desde pantallas secundarias
    BackHandler(enabled = selectedScreen != KioscoNavScreen.DASHBOARD) {
        selectedScreen = KioscoNavScreen.DASHBOARD
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopHeader(
                    currentUser = currentUser,
                    currentBranch = currentBranch,
                    branches = branches,
                    activeSession = activeSession,
                    isCloudConnected = firebaseUser != null,
                    onBranchSelected = { viewModel.selectBranch(it) },
                    onOpenUserSwitch = { showUserSwitchModal = true },
                    onOpenCloudSync = {
                        if (userLevel >= 2) {
                            managementSubTab = 5
                            selectedScreen = KioscoNavScreen.MANAGEMENT
                        } else {
                            showAccessDeniedDialog = true
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    Column {
                        androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = DrakosGraphite)
                        NavigationBar(
                            containerColor = DrakosBlack,
                            tonalElevation = 8.dp
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
                                        unselectedIconColor = Color(0xFFA1A1AA),
                                        unselectedTextColor = Color(0xFFA1A1AA)
                                    ),
                                    modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = DrakosBlack,
                        contentColor = DrakosWhite
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        visibleScreens.forEach { screen ->
                            val navLabel = when {
                                screen == KioscoNavScreen.DASHBOARD && userLevel == 3 -> "Finanzas"
                                screen == KioscoNavScreen.DASHBOARD && userLevel == 2 -> "Métricas"
                                else -> screen.title
                            }
                            NavigationRailItem(
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
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = navLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedScreen == screen) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DrakosWhite,
                                    selectedTextColor = DrakosWhite,
                                    indicatorColor = DrakosRed,
                                    unselectedIconColor = Color(0xFFA1A1AA),
                                    unselectedTextColor = Color(0xFFA1A1AA)
                                ),
                                modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
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
                            onNavigateToShifts = { selectedScreen = KioscoNavScreen.SHIFTS }
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = DangerRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acceso Denegado (RBAC)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Su perfil actual '${currentUser?.fullName}' posee rol '${currentUser?.role?.label}'.",
                        fontSize = 13.sp,
                        color = BrandNavy900
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Esta sección está restringida únicamente a Encargados (Nivel 2) o Propietario (Nivel 3).",
                        fontSize = 12.sp,
                        color = BrandSlate700
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAccessDeniedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Comprendido")
                }
            }
        )
    }
}
