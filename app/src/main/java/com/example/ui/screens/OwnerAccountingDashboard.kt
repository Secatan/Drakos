package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BranchEntity
import com.example.data.entity.SaleEntity
import com.example.domain.analytics.DrakosAnalytics
import com.example.ui.components.ActivityKind
import com.example.ui.components.BranchSalesBreakdownItem
import com.example.ui.components.CategoryProfitabilityCard
import com.example.ui.components.DrakosBrandIdentityCard
import com.example.ui.components.DrakosBranchBreakdownCard
import com.example.ui.components.DrakosDonutSummaryCard
import com.example.ui.components.DrakosRecentActivityCard
import com.example.ui.components.DrakosSparklineKpiCard
import com.example.ui.components.FinancialBarChartCard
import com.example.ui.components.HourlyHeatmapCard
import com.example.ui.components.RecentActivityFeedItem
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.dialogs.CsvExportDialog
import com.example.ui.dialogs.EditCompanySettingsDialog
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
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun OwnerAccountingDashboard(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allSales by viewModel.allChainSales.collectAsStateWithLifecycle()
    val allAnalytics by viewModel.allSaleAnalytics.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val allProductsEntity by viewModel.allProductsEntity.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var chartBranchFilterId by remember { mutableStateOf<Long?>(null) }
    var currentTab by remember { mutableIntStateOf(0) } // 0: Resumen Ejecutivo, 1: Sucursales, 2: Tickets, 3: Fiscal & Red
    var showExportModal by remember { mutableStateOf(false) }
    var showEditSettingsModal by remember { mutableStateOf(false) }
    var saleToVoid by remember { mutableStateOf<SaleEntity?>(null) }

    val completedSales = remember(allSales) { allSales.filter { it.status == "COMPLETED" } }
    val grossRevenue = remember(completedSales) { completedSales.sumOf { it.totalAmount } }
    val totalCogs = remember(allAnalytics) { allAnalytics.sumOf { it.unitCost * it.quantity } }
    val grossMargin = grossRevenue - totalCogs
    val marginPct = if (grossRevenue > 0.0) (grossMargin / grossRevenue) * 100.0 else 0.0
    val totalExpenses = remember(allExpenses) { allExpenses.sumOf { it.amount } }
    val netProfit = grossMargin - totalExpenses
    val avgTicket = if (completedSales.isNotEmpty()) grossRevenue / completedSales.size else 0.0
    val unitsSold = remember(allAnalytics) { allAnalytics.sumOf { it.quantity } }
    val vatDebit = remember(completedSales) { completedSales.sumOf { it.vatAmount } }
    val pendingInvoices = remember(allInvoices) { allInvoices.filter { it.paymentStatus != "PAID" } }
    val pendingInvoicesTotal = remember(pendingInvoices) { pendingInvoices.sumOf { it.totalAmount } }

    val dailyPoints = remember(allSales, allAnalytics, chartBranchFilterId) {
        DrakosAnalytics.buildDailySalesBars(allSales, allAnalytics, chartBranchFilterId)
    }
    val monthlyPoints = remember(allSales, allAnalytics, chartBranchFilterId) {
        DrakosAnalytics.buildMonthlySalesBars(allSales, allAnalytics, chartBranchFilterId)
    }
    val hourlySlots = remember(allSales, chartBranchFilterId) {
        DrakosAnalytics.buildHourlyHeatmap(allSales, chartBranchFilterId)
    }
    val categoryProfitability = remember(allAnalytics, allProductsEntity, categories, allSales, chartBranchFilterId) {
        DrakosAnalytics.buildCategoryProfitability(allAnalytics, allProductsEntity, categories, allSales, chartBranchFilterId)
    }

    val branchBreakdown = remember(branches, completedSales, grossRevenue) {
        branches.map { branch ->
            val amt = completedSales.filter { it.branchId == branch.id }.sumOf { it.totalAmount }
            val pct = if (grossRevenue > 0.0) (amt / grossRevenue) * 100.0 else 0.0
            BranchSalesBreakdownItem(
                branchName = branch.name,
                amount = amt,
                sharePercent = pct
            )
        }.sortedByDescending { it.amount }
    }

    val recentFeed = remember(allSales, allExpenses, branches) {
        val branchMap = branches.associate { it.id to it.name }
        val saleItems = allSales.take(4).map { sale ->
            Triple(
                sale.createdAt,
                RecentActivityFeedItem(
                    title = "Venta realizada - ${formatCurrency(sale.totalAmount)}",
                    subtitle = "${formatDateTime(sale.createdAt)} • ${branchMap[sale.branchId] ?: "Sucursal"}",
                    badgeText = "+ ${formatCurrency(sale.totalAmount)}",
                    kind = ActivityKind.SALE
                ),
                true
            )
        }
        val expItems = allExpenses.take(3).map { exp ->
            Triple(
                exp.createdAt,
                RecentActivityFeedItem(
                    title = "Egreso: ${exp.category.label}",
                    subtitle = "${formatDateTime(exp.createdAt)} • ${branchMap[exp.branchId] ?: "Caja"}",
                    badgeText = "- ${formatCurrency(exp.amount)}",
                    kind = ActivityKind.EXPENSE
                ),
                false
            )
        }
        (saleItems + expItems)
            .sortedByDescending { it.first }
            .take(5)
            .map { it.second }
    }

    val revenueSeries = remember(dailyPoints) {
        val raw = dailyPoints.takeLast(7).map { it.revenueAmount.toFloat() }
        if (raw.all { it == 0f }) listOf(0.2f, 0.35f, 0.28f, 0.52f, 0.48f, 0.72f, 0.85f) else raw
    }
    val marginSeries = remember(dailyPoints) {
        val raw = dailyPoints.takeLast(7).map { (it.revenueAmount - it.costAmount).coerceAtLeast(0.0).toFloat() }
        if (raw.all { it == 0f }) listOf(0.25f, 0.3f, 0.42f, 0.4f, 0.6f, 0.66f, 0.8f) else raw
    }
    val ticketSeries = remember(dailyPoints) {
        val raw = dailyPoints.takeLast(7).map { it.ticketCount.toFloat() }
        if (raw.all { it == 0f }) listOf(0.22f, 0.32f, 0.38f, 0.45f, 0.52f, 0.68f, 0.75f) else raw
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosLightCanvas)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Fila superior de pestañas SaaS + Exportación CSV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OwnerSectionPill(
                        label = "Resumen Ejecutivo",
                        icon = Icons.Default.BarChart,
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 }
                    )
                }
                item {
                    OwnerSectionPill(
                        label = "Sucursales",
                        icon = Icons.Default.LocationOn,
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 }
                    )
                }
                item {
                    OwnerSectionPill(
                        label = "Tickets (${allSales.size})",
                        icon = Icons.Default.Storefront,
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 }
                    )
                }
                item {
                    OwnerSectionPill(
                        label = "Fiscal & Red",
                        icon = Icons.Default.Settings,
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = DrakosWhite,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DrakosHairline),
                modifier = Modifier
                    .clickable { showExportModal = true }
                    .testTag("export_csv_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Exportar reportes CSV",
                        tint = DrakosRed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CSV",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DrakosDarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (currentTab) {
            0 -> {
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val isDesktopGrid = maxWidth >= 820.dp
                    val isTabletGrid = maxWidth >= 560.dp

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // Fila 1: 4 Tarjetas KPI Blancas (4 columnas en desktop, 2x2 en tablet/móvil)
                        item {
                            if (isDesktopGrid) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    DrakosSparklineKpiCard(
                                        title = "Ventas Totales",
                                        value = formatCurrency(grossRevenue),
                                        trendText = "↑ ${completedSales.size} tickets",
                                        trendColor = DrakosEmerald,
                                        icon = Icons.Default.ShoppingBag,
                                        accentColor = DrakosRed,
                                        badgeContainerColor = DrakosBlack,
                                        sparklinePoints = revenueSeries,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DrakosSparklineKpiCard(
                                        title = "Ganancias (${String.format("%.1f", marginPct)}%)",
                                        value = formatCurrency(grossMargin),
                                        trendText = if (netProfit >= 0) "↑ Neto: ${formatCurrency(netProfit)}" else "↓ Revisar gastos",
                                        trendColor = if (netProfit >= 0) DrakosEmerald else DangerRed,
                                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                                        accentColor = DrakosEmerald,
                                        badgeContainerColor = DrakosBlack,
                                        sparklinePoints = marginSeries,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DrakosSparklineKpiCard(
                                        title = "Ticket Promedio",
                                        value = formatCurrency(avgTicket),
                                        trendText = "→ CMV: ${formatCurrency(totalCogs)}",
                                        trendColor = DrakosMutedGray,
                                        icon = Icons.Default.AttachMoney,
                                        accentColor = DrakosRed,
                                        badgeContainerColor = DrakosRed,
                                        sparklinePoints = ticketSeries,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DrakosSparklineKpiCard(
                                        title = "Productos Vendidos",
                                        value = "$unitsSold u.",
                                        trendText = "→ Gastos: ${formatCurrency(totalExpenses)}",
                                        trendColor = DrakosDarkGray,
                                        icon = Icons.Default.AccountBalance,
                                        accentColor = DrakosDarkGray,
                                        badgeContainerColor = DrakosBlack,
                                        sparklinePoints = ticketSeries,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DrakosSparklineKpiCard(
                                            title = "Ventas Totales",
                                            value = formatCurrency(grossRevenue),
                                            trendText = "↑ ${completedSales.size} tk",
                                            trendColor = DrakosEmerald,
                                            icon = Icons.Default.ShoppingBag,
                                            accentColor = DrakosRed,
                                            badgeContainerColor = DrakosBlack,
                                            sparklinePoints = revenueSeries,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DrakosSparklineKpiCard(
                                            title = "Ganancias (${String.format("%.1f", marginPct)}%)",
                                            value = formatCurrency(grossMargin),
                                            trendText = if (netProfit >= 0) "↑ Rentable" else "↓ Negativo",
                                            trendColor = if (netProfit >= 0) DrakosEmerald else DangerRed,
                                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                                            accentColor = DrakosEmerald,
                                            badgeContainerColor = DrakosBlack,
                                            sparklinePoints = marginSeries,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DrakosSparklineKpiCard(
                                            title = "Ticket Promedio",
                                            value = formatCurrency(avgTicket),
                                            trendText = "CMV ${formatCurrency(totalCogs)}",
                                            trendColor = DrakosMutedGray,
                                            icon = Icons.Default.AttachMoney,
                                            accentColor = DrakosRed,
                                            badgeContainerColor = DrakosRed,
                                            sparklinePoints = ticketSeries,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DrakosSparklineKpiCard(
                                            title = "Unidades Vendidas",
                                            value = "$unitsSold u.",
                                            trendText = "Neto ${formatCurrency(netProfit)}",
                                            trendColor = if (netProfit >= 0) DrakosEmerald else DangerRed,
                                            icon = Icons.Default.AccountBalance,
                                            accentColor = DrakosDarkGray,
                                            badgeContainerColor = DrakosBlack,
                                            sparklinePoints = ticketSeries,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Fila 2: Ventas por Período (2/3) + Ventas por Sucursal (1/3)
                        item {
                            if (isDesktopGrid) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    FinancialBarChartCard(
                                        dailyPoints = dailyPoints,
                                        monthlyPoints = monthlyPoints,
                                        branches = branches,
                                        selectedBranchId = chartBranchFilterId,
                                        onSelectBranchId = { chartBranchFilterId = it },
                                        canChangeBranchFilter = true,
                                        showCostComparison = true,
                                        modifier = Modifier.weight(1.85f)
                                    )
                                    DrakosBranchBreakdownCard(
                                        items = branchBreakdown,
                                        onSeeAll = { currentTab = 1 },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FinancialBarChartCard(
                                        dailyPoints = dailyPoints,
                                        monthlyPoints = monthlyPoints,
                                        branches = branches,
                                        selectedBranchId = chartBranchFilterId,
                                        onSelectBranchId = { chartBranchFilterId = it },
                                        canChangeBranchFilter = true,
                                        showCostComparison = true
                                    )
                                    DrakosBranchBreakdownCard(
                                        items = branchBreakdown,
                                        onSeeAll = { currentTab = 1 }
                                    )
                                }
                            }
                        }

                        // Fila 3: Resumen Financiero Donut + Actividad Reciente + Identidad DRAKOS STORE
                        item {
                            if (isDesktopGrid) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    DrakosDonutSummaryCard(
                                        grossRevenue = grossRevenue,
                                        cogsAmount = totalCogs,
                                        expensesAmount = totalExpenses,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DrakosRecentActivityCard(
                                        items = recentFeed,
                                        onSeeMore = { currentTab = 2 },
                                        modifier = Modifier.weight(1.25f)
                                    )
                                    DrakosBrandIdentityCard(
                                        branchesCount = branches.size,
                                        usersCount = allUsers.size,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else if (isTabletGrid) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        DrakosDonutSummaryCard(
                                            grossRevenue = grossRevenue,
                                            cogsAmount = totalCogs,
                                            expensesAmount = totalExpenses,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DrakosBrandIdentityCard(
                                            branchesCount = branches.size,
                                            usersCount = allUsers.size,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    DrakosRecentActivityCard(
                                        items = recentFeed,
                                        onSeeMore = { currentTab = 2 }
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    DrakosDonutSummaryCard(
                                        grossRevenue = grossRevenue,
                                        cogsAmount = totalCogs,
                                        expensesAmount = totalExpenses
                                    )
                                    DrakosRecentActivityCard(
                                        items = recentFeed,
                                        onSeeMore = { currentTab = 2 }
                                    )
                                    DrakosBrandIdentityCard(
                                        branchesCount = branches.size,
                                        usersCount = allUsers.size
                                    )
                                }
                            }
                        }

                        // Fila 4: Analítica avanzada (Mapa de calor 24h + Rentabilidad ABC)
                        item {
                            if (isTabletGrid) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    HourlyHeatmapCard(slots = hourlySlots, modifier = Modifier.weight(1f))
                                    CategoryProfitabilityCard(categoriesStats = categoryProfitability, modifier = Modifier.weight(1f))
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HourlyHeatmapCard(slots = hourlySlots)
                                    CategoryProfitabilityCard(categoriesStats = categoryProfitability)
                                }
                            }
                        }

                        // Fila 5: Tarjeta Fiscal AFIP y Facturas de Proveedores Pendientes (blanca limpia)
                        item {
                            Surface(
                                color = DrakosWhite,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, DrakosHairline),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "IVA Débito Fiscal (${companySettings.defaultVatRate.toInt()}%) • ${pendingInvoices.size} Fact. Prov. Pendientes",
                                            fontSize = 11.sp,
                                            color = DrakosMutedGray
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Text(
                                                text = "IVA: ${formatCurrency(vatDebit)}",
                                                fontFamily = SpaceGroteskFontFamily,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DrakosDarkGray
                                            )
                                            Text(
                                                text = "Por pagar: ${formatCurrency(pendingInvoicesTotal)}",
                                                fontFamily = SpaceGroteskFontFamily,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pendingInvoicesTotal > 0) DrakosRed else DrakosMutedGray
                                            )
                                        }
                                    }
                                    Surface(color = DrakosRedContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "CUIT ${companySettings.cuit}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DrakosRed,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(branches) { branch: BranchEntity ->
                        val branchSales = completedSales.filter { it.branchId == branch.id }
                        val branchRev = branchSales.sumOf { it.totalAmount }
                        val branchCogs = allAnalytics.filter { it.branchId == branch.id }.sumOf { it.unitCost * it.quantity }
                        val branchExp = allExpenses.filter { it.branchId == branch.id }.sumOf { it.amount }
                        val branchNet = (branchRev - branchCogs) - branchExp

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                            border = BorderStroke(1.dp, DrakosHairline),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${branch.code} • ${branch.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DrakosDarkGray)
                                        Text("${branchSales.size} tickets • ${branch.address}", fontSize = 11.sp, color = DrakosMutedGray)
                                    }
                                    Text(
                                        text = "Neto: ${formatCurrency(branchNet)}",
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (branchNet >= 0) SuccessGreen else DangerRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Venta: ${formatCurrency(branchRev)}", fontSize = 11.sp, color = DrakosDarkGray)
                                    Text("CMV: ${formatCurrency(branchCogs)}", fontSize = 11.sp, color = DrakosMutedGray)
                                    Text("Gastos: ${formatCurrency(branchExp)}", fontSize = 11.sp, color = DangerRed)
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(allSales) { sale ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(sale.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosDarkGray)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (isVoided) DangerRed.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isVoided) "ANULADO" else "EMITIDO",
                                                color = if (isVoided) DangerRed else SuccessGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "K${sale.branchId.toString().padStart(2, '0')} • ${formatDateTime(sale.createdAt)} • ${sale.paymentMethod.label}",
                                        fontSize = 11.sp,
                                        color = DrakosMutedGray
                                    )
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
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Anular", fontSize = 10.sp, color = DangerRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                            border = BorderStroke(1.dp, DrakosHairline),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Parámetros Generales Drakos", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DrakosDarkGray)
                                    }
                                    OutlinedButton(
                                        onClick = { showEditSettingsModal = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("edit_company_settings_button")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar parámetros", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Editar", fontSize = 11.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                InfoRow("Razón Social:", companySettings.businessName)
                                InfoRow("CUIT Central:", "${companySettings.cuit} (${companySettings.taxCondition})")
                                InfoRow("Alícuota IVA General:", "${companySettings.defaultVatRate}%")
                                InfoRow("Tope Alerta Gaveta:", formatCurrency(companySettings.cashDropAlertThreshold))
                                InfoRow("Tolerancia Descuadre:", formatCurrency(companySettings.cashAuditTolerance))
                                InfoRow("Lista Blanca Dispositivos:", if (companySettings.strictDeviceWhitelist) "ACTIVA (Solo Nivel 3)" else "Flexible")
                            }
                        }
                    }

                    items(branches) { branch ->
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
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${branch.code} - ${branch.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosDarkGray)
                                    }
                                    Text("${branch.address} • Tel: ${branch.phone}", fontSize = 11.sp, color = DrakosMutedGray)
                                }
                                Surface(color = SuccessGreen.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        "OPERATIVO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
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

    if (showEditSettingsModal) {
        EditCompanySettingsDialog(
            companySettings = companySettings,
            onDismiss = { showEditSettingsModal = false },
            onConfirm = { updated ->
                viewModel.updateCompanySettings(updated)
                showEditSettingsModal = false
            }
        )
    }

    if (showExportModal) {
        CsvExportDialog(
            generateCsv = { type -> viewModel.generateCsvExport(type) },
            onDismiss = { showExportModal = false }
        )
    }
}

@Composable
private fun OwnerSectionPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) DrakosRed else DrakosWhite,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) DrakosRed else DrakosHairline),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) DrakosWhite else DrakosDarkGray,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) DrakosWhite else DrakosDarkGray
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = DrakosMutedGray)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DrakosDarkGray)
    }
}
