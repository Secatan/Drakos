package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SupplierEntity
import com.example.data.entity.SupplierInvoiceEntity
import com.example.data.entity.WeeklyOrderEntity
import com.example.domain.analytics.DrakosAnalytics
import com.example.domain.model.InvoicePaymentStatus
import com.example.domain.model.InvoiceType
import com.example.domain.model.UserRole
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun WeeklyOrderSection(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val products by viewModel.allProductsEntity.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allInventory by viewModel.allBranchesInventory.collectAsStateWithLifecycle()
    val allBatches by viewModel.allActiveBatches.collectAsStateWithLifecycle()
    val allAnalytics by viewModel.allSaleAnalytics.collectAsStateWithLifecycle()
    val weeklyOrders by viewModel.branchWeeklyOrders.collectAsStateWithLifecycle()

    val clipboard = LocalClipboardManager.current
    val branchId = currentBranch?.id ?: 1L
    var coverageDays by remember { mutableIntStateOf(7) }
    var selectedSupplierId by remember { mutableLongStateOf(suppliers.firstOrNull()?.id ?: 1L) }
    var showOnlySuggested by remember { mutableStateOf(true) }
    val customQuantities = remember { mutableStateMapOf<Long, Int>() }

    val suggestions = remember(products, categories, allInventory, allBatches, allAnalytics, branchId, coverageDays) {
        DrakosAnalytics.buildWeeklyOrderSuggestions(
            products = products,
            categories = categories,
            allInventory = allInventory,
            allBatches = allBatches,
            analytics = allAnalytics,
            branchId = branchId,
            coverageDays = coverageDays
        )
    }

    val displayed = remember(suggestions, showOnlySuggested) {
        if (showOnlySuggested) suggestions.filter { it.suggestedQty > 0 } else suggestions
    }

    val effectiveQuantities = suggestions.associate { s ->
        s.productId to (customQuantities[s.productId] ?: s.suggestedQty)
    }
    val totalOrderUnits = effectiveQuantities.values.sum()
    val totalOrderCost = suggestions.sumOf { s ->
        (effectiveQuantities[s.productId] ?: 0) * s.unitCost
    }
    val selectedSupplier = suppliers.find { it.id == selectedSupplierId } ?: suppliers.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        // Cabecera de Cobertura y Proveedor
        Card(
            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
            border = BorderStroke(1.dp, DrakosBorder),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Planificador de Pedido Semanal",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DrakosBlack
                        )
                        Text(
                            text = "Calculado automáticamente según rotación de ventas y punto mínimo.",
                            fontSize = 11.sp,
                            color = DrakosMutedInk
                        )
                    }
                    Surface(color = DrakosRed, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "$totalOrderUnits u.",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = DrakosWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Costo est.: ${formatCurrency(totalOrderCost)}",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = DrakosWhite
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                val text = buildString {
                                    appendLine("PEDIDO SEMANAL DRAKOS - ${currentBranch?.name ?: "Sucursal"}")
                                    appendLine("Proveedor: ${selectedSupplier?.name ?: "General"}")
                                    suggestions.forEach { s ->
                                        val q = effectiveQuantities[s.productId] ?: 0
                                        if (q > 0) appendLine("- ${s.name} (EAN ${s.barcode}): $q u.")
                                    }
                                    appendLine("Total unidades: $totalOrderUnits | Est: ${formatCurrency(totalOrderCost)}")
                                }
                                clipboard.setText(AnnotatedString(text))
                                viewModel.notifyInfo("Detalle de pedido copiado para enviar por WhatsApp / Email.")
                            },
                            border = BorderStroke(1.dp, DrakosWhite),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = DrakosWhite, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copiar", fontSize = 11.sp, color = DrakosWhite)
                        }
                        Button(
                            onClick = {
                                val sup = selectedSupplier ?: return@Button
                                val summary = suggestions.mapNotNull { s ->
                                    val q = effectiveQuantities[s.productId] ?: 0
                                    if (q > 0) "${s.name} x$q" else null
                                }.joinToString(", ")
                                viewModel.saveWeeklyOrder(
                                    supplierId = sup.id,
                                    supplierName = sup.name,
                                    totalEstimatedCost = totalOrderCost,
                                    totalUnits = totalOrderUnits,
                                    itemsSummary = summary,
                                    notes = "Cobertura ${coverageDays}d basada en estadística local."
                                )
                            },
                            enabled = totalOrderUnits > 0 && selectedSupplier != null,
                            colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("confirm_weekly_order_button")
                        ) {
                            Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Emitir Pedido", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Filtros de Cobertura y Proveedor
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = coverageDays == 7,
                    onClick = { coverageDays = 7; customQuantities.clear() },
                    label = { Text("Cobertura 7 días", fontSize = 10.sp) }
                )
            }
            item {
                FilterChip(
                    selected = coverageDays == 10,
                    onClick = { coverageDays = 10; customQuantities.clear() },
                    label = { Text("Cobertura 10 días", fontSize = 10.sp) }
                )
            }
            item {
                FilterChip(
                    selected = showOnlySuggested,
                    onClick = { showOnlySuggested = !showOnlySuggested },
                    label = { Text("Solo reposición (${suggestions.count { it.suggestedQty > 0 }})", fontSize = 10.sp) }
                )
            }
            items(suppliers) { sup ->
                FilterChip(
                    selected = selectedSupplierId == sup.id,
                    onClick = { selectedSupplierId = sup.id },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DrakosBlack,
                        selectedLabelColor = DrakosWhite
                    ),
                    label = { Text(sup.name.take(15), fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayed, key = { it.productId }) { item ->
                val qty = effectiveQuantities[item.productId] ?: item.suggestedQty
                Card(
                    colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                    border = BorderStroke(1.dp, DrakosBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                            Text(
                                text = "Stock: ${item.currentStock} u. (Mín ${item.minStock}) • ${item.basisLabel}",
                                fontSize = 11.sp,
                                color = DrakosMutedInk
                            )
                            Text(
                                text = "Costo unit: ${formatCurrency(item.unitCost)} • Subtotal: ${formatCurrency(qty * item.unitCost)}",
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = DrakosRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { customQuantities[item.productId] = (qty - 1).coerceAtLeast(0) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Restar", tint = DrakosBlack)
                            }
                            Text(
                                text = "$qty",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = DrakosBlack
                            )
                            IconButton(
                                onClick = { customQuantities[item.productId] = qty + 1 },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Sumar", tint = DrakosRed)
                            }
                        }
                    }
                }
            }

            if (weeklyOrders.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Historial de Pedidos Semanales (${weeklyOrders.size})",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DrakosBlack
                    )
                }
                items(weeklyOrders, key = { it.id }) { order: WeeklyOrderEntity ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                        border = BorderStroke(1.dp, DrakosBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${order.supplierName} • ${order.totalUnits} u.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DrakosBlack)
                                Text(order.itemsSummary.take(80), fontSize = 10.sp, color = DrakosMutedInk)
                                Text("${formatDateTime(order.createdAt)} • Est: ${formatCurrency(order.totalEstimatedCost)}", fontSize = 10.sp, color = DrakosRed)
                            }
                            if (order.status != "RECEIVED") {
                                OutlinedButton(
                                    onClick = { viewModel.markWeeklyOrderReceived(order.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Recibido", fontSize = 10.sp, color = SuccessGreen)
                                }
                            } else {
                                Surface(color = SuccessGreen.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                                    Text("RECIBIDO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuppliersAndBillingSection(
    viewModel: KioscoViewModel,
    onOpenGoodsReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val branchInvoices by viewModel.branchInvoices.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()

    val invoices = if (currentUser?.role == UserRole.OWNER) allInvoices else branchInvoices
    var showInvoiceDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Facturación de Proveedores (${invoices.size})",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DrakosBlack,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { showInvoiceDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("register_supplier_invoice_button")
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ingresar Factura", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(invoices, key = { it.id }) { inv: SupplierInvoiceEntity ->
                val isPaid = inv.paymentStatus == "PAID"
                val statusColor = if (isPaid) SuccessGreen else WarningAmber
                Card(
                    colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                    border = BorderStroke(1.dp, DrakosBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${inv.invoiceType} ${inv.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = InvoicePaymentStatus.entries.find { it.name == inv.paymentStatus }?.label ?: inv.paymentStatus,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(inv.supplierName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DrakosBlack)
                            Text("Cargada por ${inv.createdByUserName} • ${formatDateTime(inv.createdAt)}", fontSize = 10.sp, color = DrakosMutedInk)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatCurrency(inv.totalAmount),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = DrakosBlack
                            )
                            if (!isPaid) {
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { viewModel.markInvoicePaid(inv.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Marcar Pagada", fontSize = 10.sp, color = SuccessGreen)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Directorio de Proveedores Activos (${suppliers.size})",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DrakosBlack
                )
            }

            items(suppliers, key = { it.id }) { sup: SupplierEntity ->
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                            }
                            OutlinedButton(
                                onClick = onOpenGoodsReceipt,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remito", fontSize = 10.sp, color = DrakosRed)
                            }
                        }
                        Text("CUIT: ${sup.cuit} • Tel: ${sup.contactPhone}", fontSize = 11.sp, color = DrakosMutedInk)
                        Text("${sup.email} • ${sup.address}", fontSize = 10.sp, color = DrakosMutedInk)
                    }
                }
            }
        }
    }

    if (showInvoiceDialog) {
        RegisterSupplierInvoiceDialog(
            suppliers = suppliers,
            onDismiss = { showInvoiceDialog = false },
            onConfirm = { supplierId, supplierName, type, number, total, status, dueDays, notes ->
                viewModel.registerSupplierInvoice(supplierId, supplierName, type, number, total, status, dueDays, notes)
                showInvoiceDialog = false
            }
        )
    }
}

@Composable
private fun RegisterSupplierInvoiceDialog(
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, Double, String, Int, String) -> Unit
) {
    var supplierId by remember { mutableLongStateOf(suppliers.firstOrNull()?.id ?: 1L) }
    var invoiceType by remember { mutableStateOf(InvoiceType.FACTURA_A.name) }
    var invoiceNumber by remember { mutableStateOf("0001-00004521") }
    var amountInput by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf(InvoicePaymentStatus.PENDING.name) }
    var dueDaysInput by remember { mutableStateOf("15") }
    var notesInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Text("Registrar Comprobante Proveedor", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, color = DrakosBlack)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Proveedor:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suppliers) { sup ->
                        FilterChip(
                            selected = supplierId == sup.id,
                            onClick = { supplierId = sup.id },
                            label = { Text(sup.name.take(15), fontSize = 11.sp) }
                        )
                    }
                }
                Text("Tipo de Comprobante:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(InvoiceType.entries) { t ->
                        FilterChip(
                            selected = invoiceType == t.name,
                            onClick = { invoiceType = t.name },
                            label = { Text(t.label, fontSize = 10.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Número de Factura / Remito") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Importe Total ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Estado de Pago:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(InvoicePaymentStatus.entries) { st ->
                        FilterChip(
                            selected = paymentStatus == st.name,
                            onClick = { paymentStatus = st.name },
                            label = { Text(st.label, fontSize = 10.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dueDaysInput,
                        onValueChange = { dueDaysInput = it },
                        label = { Text("Venc. (días)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Observación") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val supName = suppliers.find { it.id == supplierId }?.name ?: "Proveedor Drakos"
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    val days = dueDaysInput.toIntOrNull() ?: 15
                    onConfirm(supplierId, supName, invoiceType, invoiceNumber, amount, paymentStatus, days, notesInput)
                },
                enabled = invoiceNumber.isNotBlank() && (amountInput.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Guardar Comprobante")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar", color = DrakosBlack) }
        }
    )
}
