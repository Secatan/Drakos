package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.KioscoApp
import com.example.R
import com.example.data.cloud.onGoogleSignInClicked
import com.example.data.cloud.signOutGoogle
import com.example.data.repository.CloudSyncRepository
import com.example.ui.components.formatCurrency
import com.example.ui.theme.BrandNavy900
import com.example.ui.theme.BrandSlate100
import com.example.ui.theme.BrandSlate700
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.CloudSyncViewModel
import com.example.ui.viewmodel.CloudUiState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CloudSyncRoute(
    firebaseUser: FirebaseUser?,
    operatorId: Long,
    operatorName: String,
    isOwner: Boolean,
    modifier: Modifier = Modifier
) {
    if (firebaseUser == null) {
        CloudSignInGateCard(modifier = modifier)
    } else {
        // Solo se instancia CloudSyncViewModel cuando existe FirebaseUser autenticado
        val cloudViewModel: CloudSyncViewModel = viewModel(
            key = firebaseUser.uid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) as KioscoApp
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    CloudSyncViewModel(
                        cloudRepository = CloudSyncRepository(db),
                        localRepository = app.repository,
                        currentUserId = firebaseUser.uid,
                        accountDisplayName = firebaseUser.displayName ?: operatorName,
                        accountEmail = firebaseUser.email
                    )
                }
            }
        )

        CloudSyncDashboard(
            firebaseUser = firebaseUser,
            cloudViewModel = cloudViewModel,
            operatorId = operatorId,
            operatorName = operatorName,
            isOwner = isOwner,
            modifier = modifier
        )
    }
}

@Composable
private fun CloudSignInGateCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.DrakosCanvas)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Nube desconectada",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Nube Multisucursal Drakos",
                    fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy900,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Conecte su cuenta Google empresarial para respaldar y consolidar en tiempo real ventas, cierres Z, transferencias, gastos y parámetros AFIP entre los 10 kioscos.",
                    fontSize = 12.sp,
                    color = BrandSlate700,
                    textAlign = TextAlign.Center
                )

                if (!authError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = DangerRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(authError ?: "", fontSize = 11.sp, color = DangerRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isSigningIn = true
                        authError = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = { isSigningIn = false },
                            onAuthError = { msg ->
                                isSigningIn = false
                                authError = msg
                            },
                            scope = scope,
                            onAuthCancelled = { isSigningIn = false }
                        )
                    },
                    enabled = !isSigningIn,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("google_signin_button")
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Conectando con Google...")
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar sesión con Google", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudSyncDashboard(
    firebaseUser: FirebaseUser,
    cloudViewModel: CloudSyncViewModel,
    operatorId: Long,
    operatorName: String,
    isOwner: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val uiState by cloudViewModel.uiState.collectAsStateWithLifecycle()
    val isSyncing by cloudViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by cloudViewModel.syncMessage.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.DrakosCanvas),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = firebaseUser.displayName ?: "Cuenta Google Conectada",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BrandNavy900
                                )
                                Text(
                                    text = firebaseUser.email ?: firebaseUser.uid,
                                    fontSize = 11.sp,
                                    color = BrandSlate700
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { signOutGoogle(credentialManager, scope) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("google_signout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Desconectar", fontSize = 11.sp)
                        }
                    }

                    if (!syncMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = syncMessage ?: "",
                                fontSize = 11.sp,
                                color = BrandNavy900,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { cloudViewModel.syncAllLocalToCloud() },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("sync_now_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Subiendo...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizar Todo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isOwner) {
                            OutlinedButton(
                                onClick = { cloudViewModel.restoreSettingsFromCloud(operatorId, operatorName) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("restore_cloud_settings_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Traer Fiscal", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        when (val state = uiState) {
            is CloudUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            }

            is CloudUiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.message, fontSize = 12.sp, color = DangerRed)
                        }
                    }
                }
            }

            is CloudUiState.Success -> {
                val data = state.data
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CloudStatTile("Ventas Nube", "${data.sales.size}", PrimaryBlue, Modifier.weight(1f))
                        CloudStatTile("Turnos / Z", "${data.sessions.size}", SuccessGreen, Modifier.weight(1f))
                        CloudStatTile("Traslados", "${data.transfers.size}", WarningAmber, Modifier.weight(1f))
                        CloudStatTile("Gastos", "${data.expenses.size}", DangerRed, Modifier.weight(1f))
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resumen Financiero en la Nube", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandNavy900)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Facturación Sincronizada:", fontSize = 12.sp, color = BrandSlate700)
                                Text(formatCurrency(data.activeSalesTotal), fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gastos Sincronizados:", fontSize = 12.sp, color = BrandSlate700)
                                Text(formatCurrency(data.activeExpensesTotal), fontWeight = FontWeight.Bold, color = DangerRed)
                            }
                            data.companySettings?.let { cfg ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Fiscal Nube: ${cfg.businessName} • CUIT ${cfg.cuit} • IVA ${cfg.defaultVatRate.toInt()}%",
                                    fontSize = 11.sp,
                                    color = BrandSlate700
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Últimas Operaciones Sincronizadas (${data.sales.size} ventas):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandNavy900
                    )
                }

                if (data.sales.isEmpty()) {
                    item {
                        Surface(
                            color = BrandSlate100,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Aún no hay ventas sincronizadas en Firestore. Presione 'Sincronizar Todo' o realice una venta en el POS.",
                                fontSize = 12.sp,
                                color = BrandSlate700,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(data.sales.take(15)) { sale ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("${sale.receiptNumber} • ${sale.branchName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("${sale.paymentMethod} • ${sale.cashierName} • ${sale.status}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                Text(
                                    text = formatCurrency(sale.totalAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (sale.status == "VOIDED") DangerRed else SuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudStatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
            Text(label, fontSize = 10.sp, color = BrandSlate700, textAlign = TextAlign.Center)
        }
    }
}
