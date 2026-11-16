package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SaleEntity
import com.example.domain.analytics.DrakosAnalytics
import com.example.domain.model.UserRole
import com.example.ui.components.FinancialBarChartCard
import com.example.ui.components.HourlyHeatmapCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.dialogs.VoidSaleDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun DashboardScreen(
    viewModel: KioscoViewModel,
    onNavigateToPos: () -> Unit = {},
    onNavigateToStockIntake: (openScanner: Boolean) -> Unit = {},
    onNavigateToShifts: () -> Unit = {},
    onNavigateToComms: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    when (currentUser?.role) {
        UserRole.OWNER -> OwnerAccountingDashboard(
            viewModel = viewModel,
            modifier = modifier
        )
        UserRole.BRANCH_MANAGER -> Level2BranchDashboard(
            viewModel = viewModel,
            onNavigateToPos = onNavigateToPos,
            onNavigateToStockIntake = onNavigateToStockIntake,
            onNavigateToComms = onNavigateToComms,
            modifier = modifier
        )
        else -> Level1OperatorDashboard(
            viewModel = viewModel,
            onNavigateToPos = onNavigateToPos,
            onNavigateToStockIntake = onNavigateToStockIntake,
            onNavigateToShifts = onNavigateToShifts,
            modifier = modifier
        )
    }
}

@Composable
private fun Level1OperatorDashboard(
    viewModel: KioscoViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToStockIntake: (openScanner: Boolean) -> Unit,
    onNavigateToShifts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeCashSession.collectAsStateWithLifecycle()
    val estimatedDrawerCash by viewModel.estimatedDrawerCash.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val showCashDropNotice = activeSession != null && estimatedDrawerCash >= companySettings.cashDropAlertThreshold

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Hola, ${currentUser?.fullName ?: "Operador"}",
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DrakosBlack
            )
            Text(
                text = "${currentBranch?.name ?: "Drakos"} • Modo Operativo (Nivel 1)",
                fontSize = 12.sp,
                color = DrakosMutedInk
            )
        }

        // Estado mínimo del turno actual (sin cifras contables sensibles)
        Surface(
            color = if (activeSession != null) SuccessGreen.copy(alpha = 0.12f) else DrakosRedContainer,
            border = BorderStroke(1.dp, if (activeSession != null) SuccessGreen else DrakosRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (activeSession != null) SuccessGreen else DrakosRed
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (activeSession != null) "Caja Abierta • Listo para cobrar" else "Caja Cerrada",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DrakosBlack
                        )
                        Text(
                            text = if (activeSession != null) "Puede registrar ventas e ingresar mercadería." else "Abra su turno en Caja para iniciar las ventas.",
                            fontSize = 11.sp,
                            color = DrakosMutedInk
                        )
                    }
                }
                if (activeSession == null) {
                    Button(
                        onClick = onNavigateToShifts,
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Abrir Caja", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showCashDropNotice) {
            Surface(
                color = DrakosRedContainer,
                border = BorderStroke(1.dp, DrakosRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("level1_cash_drop_notice")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DrakosRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gaveta con alto efectivo: se recomienda realizar un retiro parcial a caja fuerte.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DrakosBlack
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToShifts,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Ir a Caja", fontSize = 11.sp, color = DrakosRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Acción Principal 1: Punto de Venta
        Card(
            colors = CardDefaults.cardColors(containerColor = DrakosRed),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, tint = DrakosWhite, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PUNTO DE VENTA (POS)",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = DrakosWhite
                        )
                        Text(
                            text = "Escanear códigos EAN, cobrar con QR, tarjetas o efectivo",
                            fontSize = 12.sp,
                            color = DrakosWhite.copy(alpha = 0.88f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onNavigateToPos,
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosWhite, contentColor = DrakosBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("level1_open_pos_button")
                ) {
                    Text("IR A COBRAR AHORA", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Acción Principal 2: Ingreso de Mercadería al Stock
        Card(
            colors = CardDefaults.cardColors(containerColor = DrakosBlack),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = DrakosWhite, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "INGRESO DE MERCADERÍA AL STOCK",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = DrakosWhite
                        )
                        Text(
                            text = "Escanear cajas entrantes, carga por lote y control de remitos",
                            fontSize = 12.sp,
                            color = Color(0xFFD4D4D8)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onNavigateToStockIntake(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed, contentColor = DrakosWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("level1_scan_intake_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escanear", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onNavigateToStockIntake(false) },
                        border = BorderStroke(1.dp, DrakosWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("level1_open_stock_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = DrakosWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Stock", color = DrakosWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Level2BranchDashboard(
    viewModel: KioscoViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToStockIntake: (openScanner: Boolean) -> Unit,
    onNavigateToComms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val branchSales by viewModel.branchSales.collectAsStateWithLifecycle()
    val allAnalytics by viewModel.allSaleAnalytics.collectAsStateWithLifecycle()
    val branchProducts by viewModel.allBranchProducts.collectAsStateWithLifecycle()
    val branchBatches by viewModel.branchBatches.collectAsStateWithLifecycle()

    val branchId = currentBranch?.id ?: 1L
    val completed = remember(branchSales) { branchSales.filter { it.status == "COMPLETED" } }
    val totalAmount = remember(completed) { completed.sumOf { it.totalAmount } }
    val lowStockCount = remember(branchProducts) { branchProducts.count { it.quantityOnHand <= it.minStock } }
    val criticalBatchesCount = remember(branchBatches) { branchBatches.count { it.isCritical } }

    val dailyPoints = remember(branchSales, allAnalytics, branchId) {
        DrakosAnalytics.buildDailySalesBars(branchSales, allAnalytics, branchId)
    }
    val monthlyPoints = remember(branchSales, allAnalytics, branchId) {
        DrakosAnalytics.buildMonthlySalesBars(branchSales, allAnalytics, branchId)
    }
    val hourlySlots = remember(branchSales, branchId) {
        DrakosAnalytics.buildHourlyHeatmap(branchSales, branchId)
    }

    var currentTab by remember { mutableIntStateOf(0) } // 0: Resumen & Gráfico | 1: Tickets Locales
    var saleToVoid by remember { mutableStateOf<SaleEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
            .padding(14.dp)
    ) {
        Text(
            text = "Panel Encargado • ${currentBranch?.name ?: "Sucursal"}",
            fontFamily = SpaceGroteskFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DrakosBlack
        )
        Text(
            text = "Operación local, alertas de stock, pedidos semanales y comunicación entre locales",
            fontSize = 11.sp,
            color = DrakosMutedInk
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Atajos directos para Nivel 2 (Ventas + Ingreso + Comunicación + Pedido)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onNavigateToPos,
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("POS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onNavigateToStockIntake(true) },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ingresar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onNavigateToComms,
                border = BorderStroke(1.dp, DrakosBlack),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Forum, contentDescription = null, tint = DrakosBlack, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Locales", fontSize = 11.sp, color = DrakosBlack, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Ventas Local", formatCurrency(totalAmount), Icons.Default.AttachMoney, SuccessGreen, Modifier.weight(1f))
            MetricCard("Tickets", "${completed.size}", Icons.AutoMirrored.Filled.ReceiptLong, DrakosBlack, Modifier.weight(1f))
            MetricCard("Bajo Mín.", "$lowStockCount", Icons.Default.Warning, WarningAmber, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = DrakosBlack,
                selectedLabelColor = DrakosWhite
            )
            FilterChip(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                colors = chipColors,
                label = { Text("Gráfico & Alertas", fontSize = 11.sp) }
            )
            FilterChip(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                colors = chipColors,
                label = { Text("Tickets Local (${branchSales.size})", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (currentTab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (lowStockCount > 0 || criticalBatchesCount > 0) {
                    item {
                        Surface(
                            color = DrakosRedContainer,
                            border = BorderStroke(1.dp, DrakosRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = DrakosRed)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$lowStockCount prod. bajo mínimo • $criticalBatchesCount lotes próximos a vencer.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DrakosBlack
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onNavigateToStockIntake(false) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reponer", fontSize = 10.sp, color = DrakosRed)
                                }
                            }
                        }
                    }
                }

                item {
                    FinancialBarChartCard(
                        dailyPoints = dailyPoints,
                        monthlyPoints = monthlyPoints,
                        branches = branches,
                        selectedBranchId = branchId,
                        onSelectBranchId = {},
                        canChangeBranchFilter = false,
                        showCostComparison = false
                    )
                }

                item {
                    HourlyHeatmapCard(slots = hourlySlots)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(branchSales, key = { it.id }) { sale ->
                    val isVoided = sale.status == "VOIDED"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                        border = BorderStroke(1.dp, DrakosBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sale.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                                Text("${formatDateTime(sale.createdAt)} • ${sale.paymentMethod.label}", fontSize = 11.sp, color = DrakosMutedInk)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatCurrency(sale.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isVoided) DrakosMutedInk else SuccessGreen
                                )
                                if (!isVoided) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { saleToVoid = sale },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = "Anular", tint = DangerRed, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Anular", fontSize = 10.sp, color = DangerRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    saleToVoid?.let { sale ->
        VoidSaleDialog(
            sale = sale,
            onDismiss = { saleToVoid = null },
            onConfirm = { reason ->
                viewModel.voidSale(sale.id, reason)
                saleToVoid = null
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
        border = BorderStroke(1.dp, DrakosBorder),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 10.sp, color = DrakosMutedInk, fontWeight = FontWeight.SemiBold)
            Text(text = value, fontFamily = SpaceGroteskFontFamily, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = DrakosBlack)
        }
    }
}
