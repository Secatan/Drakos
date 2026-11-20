package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.example.ui.components.ActivityKind
import com.example.ui.components.DrakosRecentActivityCard
import com.example.ui.components.DrakosSegmentedTabItem
import com.example.ui.components.DrakosSegmentedTabs
import com.example.ui.components.DrakosSparklineKpiCard
import com.example.ui.components.FinancialBarChartCard
import com.example.ui.components.HourlyHeatmapCard
import com.example.ui.components.RecentActivityFeedItem
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.dialogs.VoidSaleDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosEmerald
import com.example.ui.theme.DrakosGold
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosLightCanvas
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceSoft
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
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
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeCashSession.collectAsStateWithLifecycle()
    val estimatedDrawerCash by viewModel.estimatedDrawerCash.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val showCashDropNotice = activeSession != null && estimatedDrawerCash >= companySettings.cashDropAlertThreshold

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosLightCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Estado limpio del turno de caja
        Surface(
            color = DrakosWhite,
            border = BorderStroke(1.dp, if (activeSession != null) DrakosHairline else DrakosRed.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (activeSession != null) SuccessGreenContainer else DrakosRedContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (activeSession != null) SuccessGreen else DrakosRed,
                            modifier = Modifier.padding(10.dp).size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (activeSession != null) "Caja Abierta • ${currentBranch?.name ?: "Drakos"}" else "Caja Cerrada",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DrakosDarkGray
                        )
                        Text(
                            text = if (activeSession != null) "Lista para registrar ventas e ingresos." else "Abra su turno en Caja para iniciar las ventas.",
                            fontSize = 11.sp,
                            color = DrakosMutedGray
                        )
                    }
                }
                if (activeSession == null) {
                    Button(
                        onClick = onNavigateToShifts,
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                        shape = RoundedCornerShape(10.dp),
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
                border = BorderStroke(1.dp, DrakosRed.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(14.dp),
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
                            fontWeight = FontWeight.SemiBold,
                            color = DrakosDarkGray
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToShifts,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("Ir a Caja", fontSize = 11.sp, color = DrakosRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tarjeta Principal 1: Punto de Venta (Único CTA rojo #D50016)
        Surface(
            color = DrakosWhite,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DrakosHairline),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = DrakosRedContainer, shape = RoundedCornerShape(10.dp)) {
                        Icon(
                            Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = DrakosRed,
                            modifier = Modifier.padding(10.dp).size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PUNTO DE VENTA (POS)",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = DrakosDarkGray
                        )
                        Text(
                            text = "Escanear códigos EAN, cobrar con QR, tarjetas o efectivo",
                            fontSize = 12.sp,
                            color = DrakosMutedGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onNavigateToPos,
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed, contentColor = DrakosWhite),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("level1_open_pos_button")
                ) {
                    Text("IR A COBRAR AHORA", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Tarjeta Principal 2: Ingreso de Mercadería al Stock (Superficie neutra limpia)
        Surface(
            color = DrakosWhite,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DrakosHairline),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = DrakosSurfaceSoft, shape = RoundedCornerShape(10.dp)) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            tint = DrakosDarkGray,
                            modifier = Modifier.padding(10.dp).size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "INGRESO DE MERCADERÍA AL STOCK",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = DrakosDarkGray
                        )
                        Text(
                            text = "Escanear cajas entrantes, carga por lote y control de remitos",
                            fontSize = 12.sp,
                            color = DrakosMutedGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onNavigateToStockIntake(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack, contentColor = DrakosWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("level1_scan_intake_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escanear", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onNavigateToStockIntake(false) },
                        border = BorderStroke(1.dp, DrakosHairline),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("level1_open_stock_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = DrakosDarkGray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Stock", color = DrakosDarkGray, fontWeight = FontWeight.Bold)
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

    val recentLocalFeed = remember(branchSales, currentBranch) {
        branchSales.take(4).map { sale ->
            RecentActivityFeedItem(
                title = "Venta ${sale.receiptNumber}",
                subtitle = "${formatDateTime(sale.createdAt)} • ${sale.paymentMethod.label}",
                badgeText = "+ ${formatCurrency(sale.totalAmount)}",
                kind = ActivityKind.SALE
            )
        }
    }

    val revenueSparkline = remember(dailyPoints) {
        val raw = dailyPoints.takeLast(7).map { it.revenueAmount.toFloat() }
        if (raw.all { it == 0f }) listOf(0.25f, 0.35f, 0.3f, 0.55f, 0.6f, 0.78f) else raw
    }
    val ticketsSparkline = remember(dailyPoints) {
        val raw = dailyPoints.takeLast(7).map { it.ticketCount.toFloat() }
        if (raw.all { it == 0f }) listOf(0.2f, 0.3f, 0.35f, 0.45f, 0.52f, 0.65f) else raw
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var saleToVoid by remember { mutableStateOf<SaleEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosLightCanvas)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Barra de acciones rápidas con jerarquía clara (POS rojo CTA, Ingresar & Locales neutros)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToPos,
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("POS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onNavigateToStockIntake(true) },
                border = BorderStroke(1.dp, DrakosHairline),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = DrakosDarkGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ingresar", fontSize = 12.sp, color = DrakosDarkGray, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onNavigateToComms,
                border = BorderStroke(1.dp, DrakosHairline),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Forum, contentDescription = null, tint = DrakosDarkGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Locales", fontSize = 12.sp, color = DrakosDarkGray, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        DrakosSegmentedTabs(
            items = listOf(
                DrakosSegmentedTabItem(0, "Resumen Sucursal"),
                DrakosSegmentedTabItem(1, "Tickets (${branchSales.size})")
            ),
            selectedId = currentTab,
            onSelect = { currentTab = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (currentTab == 0) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val isWide = maxWidth >= 700.dp

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DrakosSparklineKpiCard(
                                title = "Ventas Local",
                                value = formatCurrency(totalAmount),
                                trendText = "↑ ${completed.size} tickets",
                                trendColor = DrakosEmerald,
                                icon = Icons.Default.AttachMoney,
                                accentColor = DrakosEmerald,
                                badgeContainerColor = DrakosBlack,
                                sparklinePoints = revenueSparkline,
                                modifier = Modifier.weight(1.2f)
                            )
                            DrakosSparklineKpiCard(
                                title = "Alertas Stock",
                                value = "$lowStockCount bajo mín.",
                                trendText = if (criticalBatchesCount > 0) "$criticalBatchesCount por vencer" else "Lotes OK",
                                trendColor = if (lowStockCount > 0) DrakosGold else DrakosEmerald,
                                icon = Icons.Default.Warning,
                                accentColor = if (lowStockCount > 0) DrakosGold else DrakosRed,
                                badgeContainerColor = if (lowStockCount > 0) DrakosRed else DrakosBlack,
                                sparklinePoints = ticketsSparkline,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (lowStockCount > 0 || criticalBatchesCount > 0) {
                        item {
                            Surface(
                                color = DrakosRedContainer,
                                border = BorderStroke(1.dp, DrakosRed.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
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
                                            color = DrakosDarkGray
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
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                HourlyHeatmapCard(slots = hourlySlots, modifier = Modifier.weight(1f))
                                DrakosRecentActivityCard(
                                    items = recentLocalFeed,
                                    onSeeMore = { currentTab = 1 },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HourlyHeatmapCard(slots = hourlySlots)
                                DrakosRecentActivityCard(
                                    items = recentLocalFeed,
                                    onSeeMore = { currentTab = 1 }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(branchSales, key = { it.id }) { sale ->
                    val isVoided = sale.status == "VOIDED"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                        border = BorderStroke(1.dp, DrakosHairline),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sale.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosDarkGray)
                                Text("${formatDateTime(sale.createdAt)} • ${sale.paymentMethod.label}", fontSize = 11.sp, color = DrakosMutedGray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatCurrency(sale.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isVoided) DrakosMutedGray else SuccessGreen
                                )
                                if (!isVoided) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { saleToVoid = sale },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
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
        border = BorderStroke(1.dp, DrakosHairline),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 10.sp, color = DrakosMutedGray, fontWeight = FontWeight.SemiBold)
            Text(text = value, fontFamily = SpaceGroteskFontFamily, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = DrakosDarkGray)
        }
    }
}
