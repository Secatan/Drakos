package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.analytics.DrakosAnalytics
import com.example.domain.model.ExcelStockRow
import com.example.domain.model.StockHealthStatus
import com.example.domain.model.UserRole
import com.example.ui.components.formatCurrency
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceTint
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun ExcelStockSection(
    viewModel: KioscoViewModel,
    onQuickReceiveProduct: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val products by viewModel.allProductsEntity.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allInventory by viewModel.allBranchesInventory.collectAsStateWithLifecycle()
    val allBatches by viewModel.allActiveBatches.collectAsStateWithLifecycle()
    val allAnalytics by viewModel.allSaleAnalytics.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }
    val homeBranchId = currentBranch?.id ?: 1L
    var viewedBranchId by remember(homeBranchId) { mutableLongStateOf(homeBranchId) }
    var filterMode by remember { mutableIntStateOf(0) } // 0: Todos, 1: Bajo Mínimo, 2: Vencen <= 20d, 3: Sin Stock
    var sortMode by remember { mutableIntStateOf(0) } // 0: Críticos primero, 1: Vencimiento, 2: Venta 7d
    var query by remember { mutableStateOf("") }

    val isExternalBranch = viewedBranchId != homeBranchId
    val canEditViewedBranch = !isExternalBranch || currentUser?.role == UserRole.OWNER

    val rawRows = remember(products, categories, allInventory, allBatches, allAnalytics, viewedBranchId) {
        DrakosAnalytics.buildExcelStockRows(
            products = products,
            categories = categories,
            allInventory = allInventory,
            allBatches = allBatches,
            analytics = allAnalytics,
            branchId = viewedBranchId
        )
    }

    val lowCount = remember(rawRows) { rawRows.count { it.quantityOnHand <= it.minStock } }
    val expiringCount = remember(rawRows) { rawRows.count { (it.nearestExpiryDays ?: 999) <= 20 } }
    val zeroCount = remember(rawRows) { rawRows.count { it.quantityOnHand <= 0 } }

    val filteredRows = remember(rawRows, filterMode, sortMode, query) {
        val base = rawRows.filter { row ->
            val matchesSearch = query.isBlank() ||
                row.name.contains(query, ignoreCase = true) ||
                row.sku.contains(query, ignoreCase = true) ||
                row.barcode.contains(query, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                1 -> row.quantityOnHand <= row.minStock
                2 -> (row.nearestExpiryDays ?: 999) <= 20
                3 -> row.quantityOnHand <= 0
                else -> true
            }
            matchesSearch && matchesFilter
        }
        when (sortMode) {
            1 -> base.sortedBy { it.nearestExpiryDays ?: 9999 }
            2 -> base.sortedByDescending { it.weeklySoldQty }
            else -> base.sortedWith(
                compareBy<ExcelStockRow> { it.healthStatus.ordinal }
                    .thenBy { it.quantityOnHand - it.minStock }
            )
        }
    }

    val hScroll = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        // Selector de Sucursal (Nivel 2 puede ver todas en Solo Lectura; local editable)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Planilla de Stock Multi-Sucursal",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DrakosBlack
            )
            OutlinedButton(
                onClick = {
                    val csv = buildString {
                        appendLine("SKU;Producto;Categoria;Stock;Minimo;Venta7d;Sugerido;Vencimiento;Estado")
                        filteredRows.forEach { r ->
                            appendLine("${r.sku};${r.name};${r.categoryName};${r.quantityOnHand};${r.minStock};${r.weeklySoldQty};${r.suggestedWeeklyOrder};${r.expiryLabel};${r.healthStatus.label}")
                        }
                    }
                    clipboardManager.setPrimaryClip(
                        android.content.ClipData.newPlainText("Drakos_Stock", csv)
                    )
                    viewModel.notifyInfo("Planilla copiada (${filteredRows.size} filas) lista para pegar en Excel.")
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("copy_excel_stock_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = DrakosBlack)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copiar Excel", fontSize = 10.sp, color = DrakosBlack)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(branches) { _, branch ->
                val selected = viewedBranchId == branch.id
                FilterChip(
                    selected = selected,
                    onClick = { viewedBranchId = branch.id },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (branch.id == homeBranchId) DrakosRed else DrakosBlack,
                        selectedLabelColor = DrakosWhite
                    ),
                    label = {
                        Text(
                            text = if (branch.id == homeBranchId) "${branch.code} (Local)" else branch.code,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("excel_branch_${branch.code}")
                )
            }
        }

        if (!canEditViewedBranch) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = DrakosSurfaceTint,
                border = BorderStroke(1.dp, DrakosBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val targetName = branches.find { it.id == viewedBranchId }?.name ?: "Sucursal externa"
                    Text(
                        text = "MODO SOLO LECTURA • Viendo existencias de $targetName (edición bloqueada entre locales).",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DrakosBlack
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Banner de Alertas de Stock Bajo y Vencimientos Cercanos
        if (lowCount > 0 || expiringCount > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (lowCount > 0) {
                    Surface(
                        color = DrakosRedContainer,
                        border = BorderStroke(1.dp, DrakosRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { filterMode = if (filterMode == 1) 0 else 1 }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$lowCount bajo mínimo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                        }
                    }
                }
                if (expiringCount > 0) {
                    Surface(
                        color = WarningAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, WarningAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { filterMode = if (filterMode == 2) 0 else 2 }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$expiringCount venc. <=20d", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Filtrar planilla por producto, SKU o EAN...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        val excelChipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = DrakosBlack,
            selectedLabelColor = DrakosWhite
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(selected = filterMode == 0, onClick = { filterMode = 0 }, colors = excelChipColors, label = { Text("Todos (${rawRows.size})", fontSize = 10.sp) })
            }
            item {
                FilterChip(selected = filterMode == 1, onClick = { filterMode = 1 }, colors = excelChipColors, label = { Text("Bajo Mín. ($lowCount)", fontSize = 10.sp) })
            }
            item {
                FilterChip(selected = filterMode == 2, onClick = { filterMode = 2 }, colors = excelChipColors, label = { Text("Vencen Pronto ($expiringCount)", fontSize = 10.sp) })
            }
            item {
                FilterChip(selected = filterMode == 3, onClick = { filterMode = 3 }, colors = excelChipColors, label = { Text("Sin Stock ($zeroCount)", fontSize = 10.sp) })
            }
            item {
                FilterChip(
                    selected = sortMode == 2,
                    onClick = { sortMode = if (sortMode == 2) 0 else 2 },
                    colors = excelChipColors,
                    label = { Text("Orden: Más vendidos", fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grilla tipo Excel con desplazamiento horizontal y vertical
        Surface(
            border = BorderStroke(1.dp, DrakosBorder),
            shape = RoundedCornerShape(14.dp),
            color = DrakosWhite,
            shadowElevation = 1.dp,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Column(modifier = Modifier.horizontalScroll(hScroll)) {
                // Encabezado fijo de la planilla
                Row(
                    modifier = Modifier
                        .background(com.example.ui.theme.DrakosDarkGray)
                        .padding(vertical = 10.dp)
                ) {
                    ExcelHeaderCell("SKU", 86.dp)
                    ExcelHeaderCell("PRODUCTO", 180.dp)
                    ExcelHeaderCell("STOCK", 68.dp, TextAlign.End)
                    ExcelHeaderCell("MÍN.", 60.dp, TextAlign.End)
                    ExcelHeaderCell("VTA 7D", 68.dp, TextAlign.End)
                    ExcelHeaderCell("PEDIR", 68.dp, TextAlign.End)
                    ExcelHeaderCell("VENCE", 90.dp, TextAlign.Center)
                    ExcelHeaderCell("ESTADO", 108.dp, TextAlign.Center)
                    ExcelHeaderCell("VENTA ($)", 92.dp, TextAlign.End)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(filteredRows, key = { _, item -> item.productId }) { index, row ->
                        val bgColor = when {
                            row.healthStatus == StockHealthStatus.CRITICAL -> DrakosRedContainer.copy(alpha = 0.5f)
                            index % 2 == 1 -> DrakosSurfaceTint.copy(alpha = 0.55f)
                            else -> DrakosWhite
                        }
                        val statusColor = when (row.healthStatus) {
                            StockHealthStatus.CRITICAL -> DangerRed
                            StockHealthStatus.LOW -> WarningAmber
                            StockHealthStatus.EXPIRING_SOON -> WarningAmber
                            StockHealthStatus.OPTIMAL -> SuccessGreen
                        }

                        Row(
                            modifier = Modifier
                                .background(bgColor)
                                .border(0.5.dp, DrakosBorder)
                                .clickable(enabled = canEditViewedBranch) {
                                    onQuickReceiveProduct(row.productId)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExcelBodyCell(row.sku, 86.dp, mono = true)
                            ExcelBodyCell(row.name, 180.dp, bold = true)
                            ExcelBodyCell(
                                text = "${row.quantityOnHand}",
                                width = 68.dp,
                                align = TextAlign.End,
                                bold = true,
                                color = if (row.quantityOnHand <= row.minStock) DangerRed else DrakosBlack,
                                mono = true
                            )
                            ExcelBodyCell("${row.minStock}", 60.dp, TextAlign.End, mono = true, color = DrakosMutedInk)
                            ExcelBodyCell("${row.weeklySoldQty}", 68.dp, TextAlign.End, mono = true)
                            ExcelBodyCell(
                                text = if (row.suggestedWeeklyOrder > 0) "+${row.suggestedWeeklyOrder}" else "0",
                                width = 68.dp,
                                align = TextAlign.End,
                                bold = row.suggestedWeeklyOrder > 0,
                                color = if (row.suggestedWeeklyOrder > 0) DrakosRed else DrakosMutedInk,
                                mono = true
                            )
                            ExcelBodyCell(
                                text = row.expiryLabel,
                                width = 90.dp,
                                align = TextAlign.Center,
                                color = if ((row.nearestExpiryDays ?: 999) <= 15) DangerRed else DrakosBlack
                            )
                            Box(
                                modifier = Modifier.width(108.dp).padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(color = statusColor.copy(alpha = 0.16f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = row.healthStatus.label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            ExcelBodyCell(formatCurrency(row.sellingPrice), 92.dp, TextAlign.End, mono = true)
                        }
                    }
                }
            }
        }

        if (canEditViewedBranch) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tip: Toque cualquier fila de su sucursal local para reponer mercadería rápidamente.",
                fontSize = 10.sp,
                color = DrakosMutedInk
            )
        }
    }
}

@Composable
private fun ExcelHeaderCell(
    text: String,
    width: Dp,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = DrakosWhite,
        textAlign = align,
        maxLines = 1,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp)
    )
}

@Composable
private fun ExcelBodyCell(
    text: String,
    width: Dp,
    align: TextAlign = TextAlign.Start,
    bold: Boolean = false,
    mono: Boolean = false,
    color: Color = DrakosBlack
) {
    Text(
        text = text,
        fontFamily = if (mono) JetBrainsMonoFontFamily else SpaceGroteskFontFamily,
        fontSize = 11.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp)
    )
}
