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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BranchEntity
import com.example.data.entity.SaleEntity
import com.example.domain.analytics.DrakosAnalytics
import com.example.ui.components.CategoryProfitabilityCard
import com.example.ui.components.FinancialBarChartCard
import com.example.ui.components.HourlyHeatmapCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.dialogs.CsvExportDialog
import com.example.ui.dialogs.EditCompanySettingsDialog
import com.example.ui.dialogs.VoidSaleDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun OwnerAccountingDashboard(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val allSales by viewModel.allChainSales.collectAsStateWithLifecycle()
    val allAnalytics by viewModel.allSaleAnalytics.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val allProductsEntity by viewModel.allProductsEntity.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var chartBranchFilterId by remember { mutableStateOf<Long?>(null) }
    var currentTab by remember { mutableIntStateOf(0) } // 0: Reportes Financieros, 1: Rentabilidad Sucursales, 2: Tickets, 3: Fiscal AFIP
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
    val vatDebit = remember(completedSales) { completedSales.sumOf { it.vatAmount } }
    val pendingInvoicesTotal = remember(allInvoices) {
        allInvoices.filter { it.paymentStatus != "PAID" }.sumOf { it.totalAmount }
    }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Contabilidad & Finanzas Drakos",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DrakosBlack
                )
                Text(
                    text = "Balance consolidado, CMV, márgenes e IVA (10 Sucursales)",
                    fontSize = 11.sp,
                    color = DrakosMutedInk
                )
            }
            OutlinedButton(
                onClick = { showExportModal = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("export_csv_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CSV", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val ownerChipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = DrakosBlack,
            selectedLabelColor = DrakosWhite
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    colors = ownerChipColors,
                    label = { Text("Reportes Financieros", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
            item {
                FilterChip(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    colors = ownerChipColors,
                    label = { Text("Resultados x Sucursal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
            item {
                FilterChip(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    colors = ownerChipColors,
                    label = { Text("Tickets (${allSales.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
            item {
                FilterChip(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    colors = ownerChipColors,
                    label = { Text("Fiscal & Red", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (currentTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // KPIs Contables Fila 1
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard(
                                title = "Facturación Bruta",
                                value = formatCurrency(grossRevenue),
                                icon = Icons.Default.AttachMoney,
                                color = DrakosBlack,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Margen Bruto (${String.format("%.1f", marginPct)}%)",
                                value = formatCurrency(grossMargin),
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                color = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // KPIs Contables Fila 2
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard(
                                title = "CMV (Costo Merc.)",
                                value = formatCurrency(totalCogs),
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                color = DrakosRed,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Gastos Operativos",
                                value = formatCurrency(totalExpenses),
                                icon = Icons.Default.MoneyOff,
                                color = DangerRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // KPIs Contables Fila 3
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard(
                                title = "Resultado Neto Est.",
                                value = formatCurrency(netProfit),
                                icon = Icons.Default.AccountBalance,
                                color = if (netProfit >= 0) SuccessGreen else DangerRed,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Fact. Prov. Pendientes",
                                value = formatCurrency(pendingInvoicesTotal),
                                icon = Icons.Default.Receipt,
                                color = WarningAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Gráfico de Barras Diario y Mensual con Filtro por Sucursal
                    item {
                        FinancialBarChartCard(
                            dailyPoints = dailyPoints,
                            monthlyPoints = monthlyPoints,
                            branches = branches,
                            selectedBranchId = chartBranchFilterId,
                            onSelectBranchId = { chartBranchFilterId = it },
                            canChangeBranchFilter = true,
                            showCostComparison = true
                        )
                    }

                    // Analítica por Turno / Franja Horaria 24h + Matriz ABC de Rentabilidad
                    item {
                        HourlyHeatmapCard(slots = hourlySlots)
                    }

                    item {
                        CategoryProfitabilityCard(categoriesStats = categoryProfitability)
                    }

                    // Resumen Impositivo AFIP
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DrakosBlack),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("IVA Débito Fiscal Estimado (${companySettings.defaultVatRate.toInt()}%)", fontSize = 11.sp, color = Color(0xFFD4D4D8))
                                    Text(formatCurrency(vatDebit), fontFamily = SpaceGroteskFontFamily, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = DrakosWhite)
                                }
                                Surface(color = DrakosRed, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "CUIT ${companySettings.cuit}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DrakosWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            border = BorderStroke(1.dp, DrakosBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${branch.code} • ${branch.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                                        Text("${branchSales.size} tickets • ${branch.address}", fontSize = 11.sp, color = DrakosMutedInk)
                                    }
                                    Text(
                                        text = "Neto: ${formatCurrency(branchNet)}",
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (branchNet >= 0) SuccessGreen else DangerRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Venta: ${formatCurrency(branchRev)}", fontSize = 11.sp, color = DrakosBlack)
                                    Text("CMV: ${formatCurrency(branchCogs)}", fontSize = 11.sp, color = DrakosMutedInk)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allSales) { sale ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(sale.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (isVoided) DangerRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (isVoided) "ANULADO" else "EMITIDO",
                                                color = if (isVoided) DangerRed else SuccessGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "K${sale.branchId.toString().padStart(2, '0')} • ${formatDateTime(sale.createdAt)} • ${sale.paymentMethod.label}",
                                        fontSize = 11.sp,
                                        color = DrakosMutedInk
                                    )
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

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                            border = BorderStroke(1.dp, DrakosBorder),
                            shape = RoundedCornerShape(10.dp)
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
                                        Text("Parámetros Generales Drakos", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DrakosBlack)
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
                            border = BorderStroke(1.dp, DrakosBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${branch.code} - ${branch.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                                    }
                                    Text("${branch.address} • Tel: ${branch.phone}", fontSize = 11.sp, color = DrakosMutedInk)
                                }
                                Surface(color = SuccessGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text("OPERATIVO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = DrakosMutedInk)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DrakosBlack)
    }
}
