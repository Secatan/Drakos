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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.components.DrakosSegmentedTabItem
import com.example.ui.components.DrakosSegmentedTabs
import com.example.ui.components.DrakosSlideOverDrawer
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
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosLightCanvas
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceSoft
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
    var currentTab by remember { mutableIntStateOf(0) } // 0: Resumen, 1: Sucursales, 2: Tickets
    var showExportModal by remember { mutableStateOf(false) }
    var showEditSettingsModal by remember { mutableStateOf(false) }
    var showFiscalDrawer by remember { mutableStateOf(false) }
    var selectedBranchForDrawer by remember { mutableStateOf<BranchEntity?>(null) }
    var selectedSaleForDrawer by remember { mutableStateOf<SaleEntity?>(null) }
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
                branchId = branch.id,
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
                    title = "Venta ${sale.receiptNumber}",
                    subtitle = "${formatDateTime(sale.createdAt)} • ${branchMap[sale.branchId] ?: "Sucursal"}",
                    badgeText = "+ ${formatCurrency(sale.totalAmount)}",
                    kind = ActivityKind.SALE,
                    saleId = sale.id
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

    val sectionTabs = remember(allSales.size) {
        listOf(
            DrakosSegmentedTabItem(0, "Resumen", Icons.Default.BarChart),
            DrakosSegmentedTabItem(1, "Sucursales", Icons.Default.LocationOn),
            DrakosSegmentedTabItem(2, "Tickets (${allSales.size})", Icons.Default.Receipt)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosLightCanvas)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Fila superior limpia: Tabs segmentadas neutras + Exportar CSV + Drawer Fiscal & Red
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrakosSegmentedTabs(
                items = sectionTabs,
                selectedId = currentTab,
                onSelect = { currentTab = it },
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DrakosWhite,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DrakosHairline),
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .clickable { showFiscalDrawer = true }
                        .testTag("open_fiscal_drawer_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Fiscal y Empresa",
                            tint = DrakosDarkGray,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fiscal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DrakosDarkGray
                        )
                    }
                }

                Surface(
                    color = DrakosWhite,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DrakosHairline),
                    modifier = Modifier
                        .heightIn(min = 42.dp)
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
                            tint = DrakosDarkGray,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CSV",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DrakosDarkGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (currentTab) {
            0 -> {
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val isDesktopGrid = maxWidth >= 820.dp
                    val isTabletGrid = maxWidth >= 560.dp

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 18.dp)
                    ) {
                        // Fila 1: 4 Tarjetas KPI Ultra-Clean
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
                                        sparklinePoints = revenueSeries,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DrakosSparklineKpiCard(
                                        title = "Margen Bruto (${String.format(java.util.Locale.forLanguageTag("es-AR"), "%.1f", marginPct)}%)",
                                        value = formatCurrency(grossMargin),
                                        trendText = if (netProfit >= 0) "↑ Neto: ${formatCurrency(netProfit)}" else "↓ Revisar gastos",
                                        trendColor = if (netProfit >= 0) DrakosEmerald else DangerRed,
                                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                                        accentColor = DrakosEmerald,
                                        sparklinePoints = marginSeries,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DrakosSparklineKpiCard(
                                        title = "Ticket Promedio",
                                        value = formatCurrency(avgTicket),
                                        trendText = "CMV: ${formatCurrency(totalCogs)}",
                                        trendColor = DrakosMutedGray,
                                        icon = Icons.Default.AttachMoney,
                                        accentColor = DrakosBlack,
                                        sparklinePoints = ticketSeries,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DrakosSparklineKpiCard(
                                        title = "Unidades Vendidas",
                                        value = "$unitsSold u.",
                                        trendText = "Gastos: ${formatCurrency(totalExpenses)}",
                                        trendColor = DrakosMutedGray,
                                        icon = Icons.Default.AccountBalance,
                                        accentColor = DrakosDarkGray,
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
                                            sparklinePoints = revenueSeries,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DrakosSparklineKpiCard(
                                            title = "Margen (${String.format(java.util.Locale.forLanguageTag("es-AR"), "%.1f", marginPct)}%)",
                                            value = formatCurrency(grossMargin),
                                            trendText = if (netProfit >= 0) "↑ Rentable" else "↓ Negativo",
                                            trendColor = if (netProfit >= 0) DrakosEmerald else DangerRed,
                                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                                            accentColor = DrakosEmerald,
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
                                            accentColor = DrakosBlack,
                                            sparklinePoints = ticketSeries,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DrakosSparklineKpiCard(
                                            title = "Unidades",
                                            value = "$unitsSold u.",
                                            trendText = "Neto ${formatCurrency(netProfit)}",
                                            trendColor = if (netProfit >= 0) DrakosEmerald else DangerRed,
                                            icon = Icons.Default.AccountBalance,
                                            accentColor = DrakosDarkGray,
                                            sparklinePoints = ticketSeries,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Fila 2: Ventas por Período + Ventas por Sucursal (clic abre Drawer de Sucursal)
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
                                        onBranchClick = { item ->
                                            selectedBranchForDrawer = branches.find { it.id == item.branchId }
                                        },
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
                                        onSeeAll = { currentTab = 1 },
                                        onBranchClick = { item ->
                                            selectedBranchForDrawer = branches.find { it.id == item.branchId }
                                        }
                                    )
                                }
                            }
                        }

                        // Fila 3: Donut Financiero + Actividad Reciente (sin tarjeta promocional distractora)
                        item {
                            if (isTabletGrid) {
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
                                        onItemClick = { act ->
                                            act.saleId?.let { sid ->
                                                selectedSaleForDrawer = allSales.find { it.id == sid }
                                            }
                                        },
                                        modifier = Modifier.weight(1.25f)
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
                                        onSeeMore = { currentTab = 2 },
                                        onItemClick = { act ->
                                            act.saleId?.let { sid ->
                                                selectedSaleForDrawer = allSales.find { it.id == sid }
                                            }
                                        }
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

                        // Fila 5: Resumen Fiscal compacto clickeable para abrir Drawer de Empresa & Fiscal
                        item {
                            Surface(
                                color = DrakosWhite,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, DrakosHairline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFiscalDrawer = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "IVA Débito (${companySettings.defaultVatRate.toInt()}%) • ${pendingInvoices.size} Fact. Pendientes",
                                            fontSize = 11.sp,
                                            color = DrakosMutedGray
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text(
                                                text = "IVA: ${formatCurrency(vatDebit)}",
                                                fontFamily = SpaceGroteskFontFamily,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DrakosDarkGray
                                            )
                                            Text(
                                                text = "Por pagar: ${formatCurrency(pendingInvoicesTotal)}",
                                                fontFamily = SpaceGroteskFontFamily,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pendingInvoicesTotal > 0) DrakosRed else DrakosMutedGray
                                            )
                                        }
                                    }
                                    Surface(color = DrakosSurfaceSoft, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "CUIT ${companySettings.cuit}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DrakosDarkGray,
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
                    items(branches, key = { it.id }) { branch: BranchEntity ->
                        val branchSales = completedSales.filter { it.branchId == branch.id }
                        val branchRev = branchSales.sumOf { it.totalAmount }
                        val branchCogs = allAnalytics.filter { it.branchId == branch.id }.sumOf { it.unitCost * it.quantity }
                        val branchExp = allExpenses.filter { it.branchId == branch.id }.sumOf { it.amount }
                        val branchNet = (branchRev - branchCogs) - branchExp

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBranchForDrawer = branch },
                            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                            border = BorderStroke(1.dp, DrakosHairline),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                                        fontWeight = FontWeight.Bold,
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

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(allSales, key = { it.id }) { sale ->
                        val isVoided = sale.status == "VOIDED"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSaleForDrawer = sale },
                            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                            border = BorderStroke(1.dp, DrakosHairline),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(sale.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosDarkGray)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (isVoided) DangerRed.copy(alpha = 0.12f) else SuccessGreen.copy(alpha = 0.12f),
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
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isVoided) DrakosMutedGray else SuccessGreen
                                    )
                                    if (!isVoided) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = { saleToVoid = sale },
                                            border = BorderStroke(1.dp, DrakosHairline),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.heightIn(min = 40.dp)
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
        }
    }

    // Slide-over Drawer: Fiscal, Parámetros y Red de Kioscos (antes pestaña incrustada)
    DrakosSlideOverDrawer(
        visible = showFiscalDrawer,
        title = "Fiscal & Red Drakos",
        subtitle = "Parámetros AFIP, umbrales de caja y sucursales",
        onDismiss = { showFiscalDrawer = false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DrakosBrandIdentityCard(
                branchesCount = branches.size,
                usersCount = allUsers.size
            )

            Surface(
                color = DrakosLightCanvas,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DrakosHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Parámetros Generales", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DrakosDarkGray)
                        OutlinedButton(
                            onClick = { showEditSettingsModal = true },
                            border = BorderStroke(1.dp, DrakosHairline),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("edit_company_settings_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar parámetros", tint = DrakosDarkGray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar", fontSize = 11.sp, color = DrakosDarkGray)
                        }
                    }
                    HorizontalDivider(color = DrakosHairline)
                    InfoRow("Razón Social", companySettings.businessName)
                    InfoRow("CUIT", "${companySettings.cuit} (${companySettings.taxCondition})")
                    InfoRow("Alícuota IVA", "${companySettings.defaultVatRate}%")
                    InfoRow("Tope Alerta Gaveta", formatCurrency(companySettings.cashDropAlertThreshold))
                    InfoRow("Tolerancia Descuadre", formatCurrency(companySettings.cashAuditTolerance))
                    InfoRow("Lista Blanca N3", if (companySettings.strictDeviceWhitelist) "Activa" else "Flexible")
                }
            }

            Text(
                text = "Sucursales Activas (${branches.size})",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DrakosDarkGray
            )

            branches.forEach { branch ->
                Surface(
                    color = DrakosWhite,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DrakosHairline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFiscalDrawer = false
                            selectedBranchForDrawer = branch
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${branch.code} • ${branch.name}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DrakosDarkGray)
                            Text(branch.address, fontSize = 10.sp, color = DrakosMutedGray)
                        }
                        Surface(color = SuccessGreen.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "OPERATIVO",
                                fontSize = 9.sp,
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

    // Slide-over Drawer: Detalle de Sucursal seleccionada
    selectedBranchForDrawer?.let { branch ->
        val bSales = completedSales.filter { it.branchId == branch.id }
        val bRev = bSales.sumOf { it.totalAmount }
        val bCogs = allAnalytics.filter { it.branchId == branch.id }.sumOf { it.unitCost * it.quantity }
        val bExp = allExpenses.filter { it.branchId == branch.id }.sumOf { it.amount }
        val bNet = (bRev - bCogs) - bExp

        DrakosSlideOverDrawer(
            visible = true,
            title = branch.name,
            subtitle = "${branch.code} • ${branch.address}",
            onDismiss = { selectedBranchForDrawer = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = DrakosLightCanvas,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DrakosHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow("Tickets Completados", "${bSales.size}")
                        InfoRow("Ventas Brutas", formatCurrency(bRev))
                        InfoRow("Costo Mercadería (CMV)", formatCurrency(bCogs))
                        InfoRow("Gastos Operativos", formatCurrency(bExp))
                        HorizontalDivider(color = DrakosHairline)
                        InfoRow("Resultado Neto", formatCurrency(bNet))
                        InfoRow("Teléfono", branch.phone)
                    }
                }

                Button(
                    onClick = {
                        chartBranchFilterId = branch.id
                        currentTab = 0
                        selectedBranchForDrawer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Filtrar Gráfico por esta Sucursal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Slide-over Drawer: Detalle de Ticket seleccionado
    selectedSaleForDrawer?.let { sale ->
        val isVoided = sale.status == "VOIDED"
        val branchName = branches.find { it.id == sale.branchId }?.name ?: "Sucursal K0${sale.branchId}"

        DrakosSlideOverDrawer(
            visible = true,
            title = "Ticket ${sale.receiptNumber}",
            subtitle = "$branchName • ${formatDateTime(sale.createdAt)}",
            onDismiss = { selectedSaleForDrawer = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = DrakosLightCanvas,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DrakosHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow("Estado", if (isVoided) "ANULADO" else "EMITIDO")
                        InfoRow("Medio de Cobro", sale.paymentMethod.label)
                        InfoRow("Subtotal", formatCurrency(sale.subtotal))
                        InfoRow("Descuento", formatCurrency(sale.discountAmount))
                        InfoRow("IVA Discriminado", formatCurrency(sale.vatAmount))
                        HorizontalDivider(color = DrakosHairline)
                        InfoRow("Total Final", formatCurrency(sale.totalAmount))
                    }
                }

                if (!isVoided) {
                    Button(
                        onClick = {
                            saleToVoid = sale
                            selectedSaleForDrawer = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Anular Ticket", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showExportModal) {
        CsvExportDialog(
            generateCsv = { type -> viewModel.generateCsvExport(type) },
            onDismiss = { showExportModal = false }
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = DrakosMutedGray)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DrakosDarkGray)
    }
}
