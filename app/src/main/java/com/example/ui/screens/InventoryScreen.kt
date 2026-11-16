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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.MovementType
import com.example.domain.model.ProductWithStock
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.formatCurrency
import com.example.ui.dialogs.BatchIntakeDialog
import com.example.ui.dialogs.BulkPriceUpdateDialog
import com.example.ui.dialogs.GoodsReceiptDialog
import com.example.ui.dialogs.InventoryAdjustmentDialog
import com.example.ui.dialogs.NewProductDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun InventoryScreen(
    viewModel: KioscoViewModel,
    intakeScannerSignal: Int = 0,
    modifier: Modifier = Modifier
) {
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allCatalogProducts by viewModel.allBranchProducts.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val branchBatches by viewModel.branchBatches.collectAsStateWithLifecycle()
    val allProductsEntities by viewModel.allProductsEntity.collectAsStateWithLifecycle()

    val userLevel = currentUser?.role?.level ?: 1
    val isManagerOrOwner = userLevel >= 2

    var selectedProductForAdjustment by remember { mutableStateOf<ProductWithStock?>(null) }
    var adjustmentType by remember { mutableStateOf(MovementType.LOSS) }
    var showGoodsReceiptModal by remember { mutableStateOf(false) }
    var showBatchIntakeModal by remember { mutableStateOf(false) }
    var showBulkPriceModal by remember { mutableStateOf(false) }
    var receiptPreselectedProductId by remember { mutableStateOf<Long?>(null) }
    var showNewProductModal by remember { mutableStateOf(false) }
    var newProductInitialBarcode by remember { mutableStateOf("7790070001") }
    var scannerMode by remember { mutableStateOf<String?>(null) } // "INTAKE" o "SEARCH"

    // 0: Lista / Ingreso | 1: Excel Multi-Sucursal (N2+) | 2: Pedido Semanal (N2+) | 3: Proveedores/Fact. (N2+) | 4: Vencimientos
    var inventoryTab by remember(userLevel) { mutableIntStateOf(if (isManagerOrOwner) 1 else 0) }

    LaunchedEffect(intakeScannerSignal) {
        if (intakeScannerSignal > 0) {
            scannerMode = "INTAKE"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
            .padding(14.dp)
    ) {
        Surface(
            color = DrakosWhite,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DrakosBorder),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isManagerOrOwner) "Gestión Avanzada de Stock" else "Ingreso & Control de Stock",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DrakosBlack
                )
                Text(
                    text = if (isManagerOrOwner) {
                        "Sucursal: ${currentBranch?.name ?: "Drakos"} • Planilla, Pedidos y Facturación"
                    } else {
                        "Sucursal: ${currentBranch?.name ?: "Drakos"} • Escaneo e ingreso rápido de mercadería"
                    },
                    fontSize = 11.sp,
                    color = DrakosMutedInk
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Acciones operativas de ingreso (habilitadas para Nivel 1, 2 y 3)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Button(
                    onClick = { scannerMode = "INTAKE" },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("inventory_scan_intake_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Escanear Ingreso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Button(
                    onClick = { showBatchIntakeModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("batch_intake_button")
                ) {
                    Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ingreso por Lote", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        receiptPreselectedProductId = null
                        showGoodsReceiptModal = true
                    },
                    border = BorderStroke(1.dp, DrakosBlack),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("goods_receipt_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.CallReceived,
                        contentDescription = null,
                        tint = DrakosBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recepción Remito", fontSize = 12.sp, color = DrakosBlack, fontWeight = FontWeight.SemiBold)
                }
            }

            // Alta de productos nuevos disponible desde Nivel 2 (Encargado) y Nivel 3 (Dueño)
            if (isManagerOrOwner) {
                item {
                    Button(
                        onClick = {
                            newProductInitialBarcode = "7790070001"
                            showNewProductModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("new_product_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuevo Prod.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { showBulkPriceModal = true },
                        border = BorderStroke(1.dp, DrakosRed),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("bulk_price_update_button")
                    ) {
                        Icon(Icons.Default.Percent, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Precios %", fontSize = 12.sp, color = DrakosRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pestañas diferenciadas según nivel de acceso
        val invChipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = DrakosRed,
            selectedLabelColor = DrakosWhite,
            selectedLeadingIconColor = DrakosWhite
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isManagerOrOwner) {
                item {
                    FilterChip(
                        selected = inventoryTab == 1,
                        onClick = { inventoryTab = 1 },
                        colors = invChipColors,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Planilla Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("inv_tab_excel")
                    )
                }
            }
            item {
                FilterChip(
                    selected = inventoryTab == 0,
                    onClick = { inventoryTab = 0 },
                    colors = invChipColors,
                    shape = RoundedCornerShape(10.dp),
                    label = { Text("Stock Local (${products.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("inv_tab_list")
                )
            }
            if (isManagerOrOwner) {
                item {
                    FilterChip(
                        selected = inventoryTab == 5,
                        onClick = { inventoryTab = 5 },
                        colors = invChipColors,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Promociones", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("inv_tab_promos")
                    )
                }
                item {
                    FilterChip(
                        selected = inventoryTab == 2,
                        onClick = { inventoryTab = 2 },
                        colors = invChipColors,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Pedido Semanal", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("inv_tab_weekly_order")
                    )
                }
                item {
                    FilterChip(
                        selected = inventoryTab == 3,
                        onClick = { inventoryTab = 3 },
                        colors = invChipColors,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Proveedores y Fact.", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("inv_tab_suppliers")
                    )
                }
            }
            item {
                FilterChip(
                    selected = inventoryTab == 4,
                    onClick = { inventoryTab = 4 },
                    colors = invChipColors,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text("Vencimientos", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("inv_tab_expiry")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (inventoryTab) {
            1 -> if (isManagerOrOwner) {
                ExcelStockSection(
                    viewModel = viewModel,
                    onQuickReceiveProduct = { pid ->
                        receiptPreselectedProductId = pid
                        showGoodsReceiptModal = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            2 -> if (isManagerOrOwner) {
                WeeklyOrderSection(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }

            3 -> if (isManagerOrOwner) {
                SuppliersAndBillingSection(
                    viewModel = viewModel,
                    onOpenGoodsReceipt = {
                        receiptPreselectedProductId = null
                        showGoodsReceiptModal = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            5 -> if (isManagerOrOwner) {
                PromotionsManagementSection(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }

            4 -> {
                if (branchBatches.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = DrakosMutedInk, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sin lotes activos registrados", fontWeight = FontWeight.Bold, color = DrakosBlack)
                        Text("Escanee ingresos de mercadería para controlar lotes y fechas de caducidad.", fontSize = 12.sp, color = DrakosMutedInk)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(branchBatches, key = { it.batchId }) { item ->
                            val badgeColor = when {
                                item.daysUntilExpiry <= 10 -> DangerRed
                                item.daysUntilExpiry <= 30 -> WarningAmber
                                else -> SuccessGreen
                            }
                            val badgeText = when {
                                item.daysUntilExpiry <= 0 -> "¡VENCIDO!"
                                item.daysUntilExpiry <= 10 -> "¡CRÍTICO (<10d)!"
                                item.daysUntilExpiry <= 30 -> "PRÓXIMO"
                                else -> "ÓPTIMO"
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                                border = BorderStroke(1.dp, DrakosBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                                        Text("Lote: ${item.batchNumber} • Stock lote: ${item.quantity} u.", fontSize = 11.sp, color = DrakosMutedInk)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = badgeColor, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Caducidad: ${item.expiryDateFormatted}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(color = badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                            Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                        if (isManagerOrOwner && item.daysUntilExpiry in 1..20) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.applyExpiryClearanceMarkdown(
                                                        productId = item.productId,
                                                        batchNumber = item.batchNumber,
                                                        discountPercent = 25.0
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("clearance_markdown_${item.batchId}")
                                            ) {
                                                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Liquidar -25%", fontSize = 10.sp)
                                            }
                                        }
                                        if (item.isCritical && isManagerOrOwner) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.writeOffExpiredBatch(
                                                        batchId = item.batchId,
                                                        quantity = 1,
                                                        notes = "Baja de 1u por vencimiento de lote ${item.batchNumber}"
                                                    )
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Baja Vencido", fontSize = 10.sp, color = DangerRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Lista de existencias locales (disponible para N1, N2, N3)
                val lowStockProducts = products.filter { it.quantityOnHand <= it.minStock }
                if (lowStockProducts.isNotEmpty()) {
                    Surface(
                        color = DrakosRedContainer,
                        border = BorderStroke(1.dp, DrakosRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Atención: ${lowStockProducts.size} producto(s) bajo punto mínimo.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DrakosBlack
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Buscar por nombre o EAN...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DrakosMutedInk) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_search_input")
                    )

                    Button(
                        onClick = { scannerMode = "SEARCH" },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("inventory_search_scan_button")
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Escanear para buscar",
                            tint = DrakosWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.productId }) { prod: ProductWithStock ->
                        val stockColor = when {
                            prod.quantityOnHand <= 0 -> DangerRed
                            prod.quantityOnHand <= prod.minStock -> WarningAmber
                            else -> SuccessGreen
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                            border = BorderStroke(1.dp, DrakosBorder),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DrakosBlack
                                    )
                                    Text(
                                        text = "EAN: ${prod.barcode} • ${prod.categoryName}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = DrakosMutedInk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Venta: ${formatCurrency(prod.sellingPrice)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DrakosRed
                                        )
                                        if (isManagerOrOwner) {
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Costo: ${formatCurrency(prod.costPrice)}",
                                                fontSize = 11.sp,
                                                color = DrakosMutedInk
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        color = stockColor.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${prod.quantityOnHand} u.",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = stockColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                receiptPreselectedProductId = prod.productId
                                                showGoodsReceiptModal = true
                                            },
                                            border = BorderStroke(1.dp, DrakosRed),
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("inventory_quick_intake_${prod.productId}"),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.CallReceived,
                                                contentDescription = "Ingresar stock",
                                                tint = DrakosRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        if (isManagerOrOwner) {
                                            OutlinedButton(
                                                onClick = {
                                                    selectedProductForAdjustment = prod
                                                    adjustmentType = MovementType.LOSS
                                                },
                                                border = BorderStroke(1.dp, DrakosBorder),
                                                modifier = Modifier.size(48.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Merma", tint = DangerRed, modifier = Modifier.size(18.dp))
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    selectedProductForAdjustment = prod
                                                    adjustmentType = MovementType.ADJUSTMENT
                                                },
                                                border = BorderStroke(1.dp, DrakosBorder),
                                                modifier = Modifier.size(48.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Ajuste", tint = DrakosBlack, modifier = Modifier.size(18.dp))
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
    }

    scannerMode?.let { mode ->
        BarcodeScannerDialog(
            title = if (mode == "INTAKE") "Escanear Producto para Ingreso" else "Buscar en Inventario por EAN",
            subtitle = if (mode == "INTAKE") {
                "Escanee el código de barras de la mercadería entrante para sumarla al stock."
            } else {
                "Escanee el producto para filtrar sus existencias y lotes."
            },
            onBarcodeDetected = { code ->
                scannerMode = null
                if (mode == "INTAKE") {
                    val matched = viewModel.findProductByBarcode(code)
                    if (matched != null) {
                        receiptPreselectedProductId = matched.productId
                        showGoodsReceiptModal = true
                    } else if (isManagerOrOwner) {
                        newProductInitialBarcode = code.trim()
                        showNewProductModal = true
                        viewModel.notifyInfo("EAN ${code.trim()} no existe: complete el alta de producto.")
                    } else {
                        viewModel.notifyError("Código '${code.trim()}' no registrado. Solicite el alta al Encargado (Nivel 2).")
                    }
                } else {
                    viewModel.setSearchQuery(code.trim())
                }
            },
            onDismiss = { scannerMode = null }
        )
    }

    selectedProductForAdjustment?.let { prod ->
        InventoryAdjustmentDialog(
            product = prod,
            adjustmentType = adjustmentType,
            onDismiss = { selectedProductForAdjustment = null },
            onConfirm = { delta, notes ->
                viewModel.registerAdjustment(prod.productId, adjustmentType, delta, notes)
                selectedProductForAdjustment = null
            }
        )
    }

    if (showBatchIntakeModal) {
        BatchIntakeDialog(
            suppliers = suppliers,
            products = allCatalogProducts,
            onDismiss = { showBatchIntakeModal = false },
            onConfirmBatch = { lines, supplierId, invoiceRef ->
                viewModel.receiveBatchScanIntake(lines, supplierId, invoiceRef)
                showBatchIntakeModal = false
            }
        )
    }

    if (showGoodsReceiptModal) {
        GoodsReceiptDialog(
            suppliers = suppliers,
            products = allCatalogProducts,
            initialSelectedProductId = receiptPreselectedProductId,
            onDismiss = { showGoodsReceiptModal = false },
            onRequestNewProductWithBarcode = if (isManagerOrOwner) {
                { missingBarcode ->
                    showGoodsReceiptModal = false
                    newProductInitialBarcode = missingBarcode
                    showNewProductModal = true
                }
            } else {
                null
            },
            onConfirm = { productId, supplierId, qty, cost, invoiceRef, batchNumber, expiryDays ->
                viewModel.receiveGoods(
                    productId = productId,
                    supplierId = supplierId,
                    quantity = qty,
                    unitCost = cost,
                    invoiceRef = invoiceRef,
                    batchNumber = batchNumber,
                    expiryDays = expiryDays
                )
                showGoodsReceiptModal = false
            }
        )
    }

    if (showNewProductModal) {
        NewProductDialog(
            categories = categories,
            initialBarcode = newProductInitialBarcode,
            onDismiss = { showNewProductModal = false },
            onConfirm = { newProd ->
                viewModel.saveProduct(newProd)
                showNewProductModal = false
            }
        )
    }

    if (showBulkPriceModal) {
        BulkPriceUpdateDialog(
            categories = categories,
            products = allProductsEntities,
            onDismiss = { showBulkPriceModal = false },
            onConfirm = { catId, costDelta, priceDelta, preserveMargin, rounding ->
                viewModel.applyBulkPriceUpdate(
                    categoryId = catId,
                    costPercentDelta = costDelta,
                    pricePercentDelta = priceDelta,
                    preserveMargin = preserveMargin,
                    roundingStep = rounding
                )
                showBulkPriceModal = false
            }
        )
    }
}
