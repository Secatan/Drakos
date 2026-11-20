package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SupplierEntity
import com.example.domain.model.MovementType
import com.example.domain.model.ProductWithStock
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.parseArsDecimal
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
import com.example.ui.theme.SuccessGreenContainer

@Composable
fun InventoryAdjustmentDialog(
    product: ProductWithStock,
    adjustmentType: MovementType,
    onDismiss: () -> Unit,
    onConfirm: (delta: Int, notes: String) -> Unit
) {
    var qtyChangeInput by remember { mutableStateOf("1") }
    var notesInput by remember { mutableStateOf("Rotura de empaque en góndola") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Text(
                text = if (adjustmentType == MovementType.LOSS) "Registrar Merma / Rotura" else "Ajuste por Recuento Físico",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                color = DrakosBlack
            )
        },
        text = {
            Column {
                Text("Producto: ${product.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                Text("Stock actual: ${product.quantityOnHand} unidades", fontSize = 12.sp, color = DrakosMutedInk)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = qtyChangeInput,
                    onValueChange = {
                        qtyChangeInput = it
                        validationError = null
                    },
                    label = {
                        Text(if (adjustmentType == MovementType.LOSS) "Cantidad a dar de baja (positiva)" else "Variación de unidades (+ o -)")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = {
                        notesInput = it
                        validationError = null
                    },
                    label = { Text("Motivo / Justificación obligatoria") },
                    modifier = Modifier.fillMaxWidth()
                )

                validationError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(err, color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawQty = qtyChangeInput.trim().toIntOrNull()
                    when {
                        rawQty == null || rawQty == 0 -> {
                            validationError = "Ingrese una variación de unidades distinta de cero."
                        }
                        adjustmentType == MovementType.LOSS && rawQty < 0 -> {
                            validationError = "Para mermas ingrese la cantidad positiva a descontar."
                        }
                        notesInput.isBlank() -> {
                            validationError = "El motivo del ajuste es obligatorio."
                        }
                        else -> {
                            val delta = if (adjustmentType == MovementType.LOSS) -kotlin.math.abs(rawQty) else rawQty
                            onConfirm(delta, notesInput.trim())
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (adjustmentType == MovementType.LOSS) DangerRed else DrakosRed
                )
            ) {
                Text("Asentar Movimiento", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = DrakosBlack)
            }
        }
    )
}

@Composable
fun GoodsReceiptDialog(
    suppliers: List<SupplierEntity>,
    products: List<ProductWithStock>,
    initialSelectedProductId: Long? = null,
    onDismiss: () -> Unit,
    onRequestNewProductWithBarcode: ((String) -> Unit)? = null,
    onConfirm: (productId: Long, supplierId: Long, qty: Int, cost: Double, invoiceRef: String, batchNumber: String, expiryDays: Int) -> Unit
) {
    val initialProd = products.find { it.productId == initialSelectedProductId } ?: products.firstOrNull()
    var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: 1L) }
    var selectedProdId by remember(initialSelectedProductId) { mutableStateOf(initialProd?.productId ?: 1L) }
    var barcodeFilterInput by remember(initialSelectedProductId) { mutableStateOf(initialProd?.barcode ?: "") }
    var qtyInput by remember { mutableStateOf("24") }
    var costInput by remember(initialSelectedProductId) { mutableStateOf(initialProd?.costPrice?.toInt()?.toString() ?: "950") }
    var isPackMode by remember { mutableStateOf(false) }
    var packCountInput by remember { mutableStateOf("2") }
    var unitsPerPackInput by remember { mutableStateOf("12") }
    var packCostInput by remember(initialSelectedProductId) {
        val unitCost = initialProd?.costPrice ?: 950.0
        mutableStateOf((unitCost * 12).toInt().toString())
    }
    var invoiceRefInput by remember { mutableStateOf("REM-2026-0045") }
    var batchNumberInput by remember { mutableStateOf("LOT-2026-A1") }
    var expiryDaysInput by remember { mutableStateOf("90") }

    var showInlineScanner by remember { mutableStateOf(false) }
    var unmatchedBarcode by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val selectedProduct = products.find { it.productId == selectedProdId }
    val modeChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = DrakosBlack,
        selectedLabelColor = DrakosWhite
    )
    val parsedPackCount = packCountInput.trim().toIntOrNull()
    val parsedUnitsPerPack = unitsPerPackInput.trim().toIntOrNull()
    val parsedSingleQty = qtyInput.trim().toIntOrNull()
    val parsedPackCost = parseArsDecimal(packCostInput)
    val parsedSingleCost = parseArsDecimal(costInput)

    val effectiveQty = if (isPackMode) {
        (parsedPackCount?.coerceAtLeast(1) ?: 1) * (parsedUnitsPerPack?.coerceAtLeast(1) ?: 1)
    } else {
        parsedSingleQty?.coerceAtLeast(1) ?: 1
    }
    val effectiveUnitCost = if (isPackMode) {
        val perPack = (parsedUnitsPerPack?.coerceAtLeast(1) ?: 1).toDouble()
        (parsedPackCost?.coerceAtLeast(0.0) ?: 0.0) / perPack
    } else {
        parsedSingleCost?.coerceAtLeast(0.0) ?: 0.0
    }

    fun applyScannedBarcode(code: String) {
        val clean = code.trim()
        barcodeFilterInput = clean
        val matched = products.find { it.barcode.equals(clean, ignoreCase = true) }
        if (matched != null) {
            selectedProdId = matched.productId
            costInput = matched.costPrice.toInt().toString()
            val perPack = unitsPerPackInput.toIntOrNull()?.coerceAtLeast(1) ?: 12
            packCostInput = (matched.costPrice * perPack).toInt().toString()
            unmatchedBarcode = null
        } else {
            unmatchedBarcode = clean
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ingreso de Mercadería a Stock",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DrakosBlack,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Escáner / Búsqueda rápida por Código de Barras en Recepción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcodeFilterInput,
                        onValueChange = { raw ->
                            barcodeFilterInput = raw
                            unmatchedBarcode = null
                            val exact = products.find { it.barcode.equals(raw.trim(), ignoreCase = true) }
                            if (exact != null) {
                                selectedProdId = exact.productId
                                costInput = exact.costPrice.toInt().toString()
                                val perPack = unitsPerPackInput.toIntOrNull()?.coerceAtLeast(1) ?: 12
                                packCostInput = (exact.costPrice * perPack).toInt().toString()
                            }
                        },
                        label = { Text("Código EAN / Lector USB", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (barcodeFilterInput.isNotBlank()) {
                                    applyScannedBarcode(barcodeFilterInput)
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receipt_barcode_input")
                    )

                    Button(
                        onClick = { showInlineScanner = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("receipt_scan_barcode_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear producto", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escanear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Tarjeta de estado del producto seleccionado o EAN no encontrado
                val missingCode = unmatchedBarcode
                if (!missingCode.isNullOrBlank()) {
                    Surface(
                        color = DrakosRedContainer,
                        border = BorderStroke(1.dp, DrakosRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EAN '$missingCode' no está registrado en el catálogo.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrakosBlack
                                )
                            }
                            if (onRequestNewProductWithBarcode != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { onRequestNewProductWithBarcode(missingCode) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("create_missing_product_from_receipt")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dar de alta EAN $missingCode", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else if (selectedProduct != null) {
                    Surface(
                        color = SuccessGreenContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedProduct.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrakosBlack
                                )
                                Text(
                                    text = "EAN: ${selectedProduct.barcode} • Stock actual: ${selectedProduct.quantityOnHand} u.",
                                    fontSize = 10.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = DrakosMutedInk
                                )
                            }
                        }
                    }
                }

                Text("Proveedor:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suppliers) { sup ->
                        FilterChip(
                            selected = selectedSupplierId == sup.id,
                            onClick = { selectedSupplierId = sup.id },
                            colors = modeChipColors,
                            label = { Text(sup.name.take(15), fontSize = 11.sp) }
                        )
                    }
                }

                Text("Catálogo rápido:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(products) { prod ->
                        FilterChip(
                            selected = selectedProdId == prod.productId,
                            onClick = {
                                selectedProdId = prod.productId
                                barcodeFilterInput = prod.barcode
                                costInput = prod.costPrice.toInt().toString()
                                val perPack = unitsPerPackInput.toIntOrNull()?.coerceAtLeast(1) ?: 12
                                packCostInput = (prod.costPrice * perPack).toInt().toString()
                                unmatchedBarcode = null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DrakosRed,
                                selectedLabelColor = DrakosWhite
                            ),
                            label = { Text(prod.name.take(16), fontSize = 11.sp) }
                        )
                    }
                }

                // Modo de ingreso: Por Unidad vs Por Bulto / Caja
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isPackMode,
                        onClick = { isPackMode = false },
                        colors = modeChipColors,
                        label = { Text("Por Unidad", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    FilterChip(
                        selected = isPackMode,
                        onClick = { isPackMode = true },
                        colors = modeChipColors,
                        label = { Text("Por Bulto / Caja", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                if (isPackMode) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = packCountInput,
                            onValueChange = { packCountInput = it },
                            label = { Text("Bultos", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitsPerPackInput,
                            onValueChange = { unitsPerPackInput = it },
                            label = { Text("U. x Bulto", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = packCostInput,
                            onValueChange = { packCostInput = it },
                            label = { Text("Costo Bulto $", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    Text(
                        text = "Fraccionamiento automático: +$effectiveQty u. a $${effectiveUnitCost.toInt()} c/u",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DrakosRed
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qtyInput,
                            onValueChange = { qtyInput = it },
                            label = { Text("Cantidad") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("receipt_qty_input")
                        )
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { costInput = it },
                            label = { Text("Costo ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = invoiceRefInput,
                    onValueChange = { invoiceRefInput = it },
                    label = { Text("Nº Remito / Factura Proveedor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = batchNumberInput,
                        onValueChange = { batchNumberInput = it },
                        label = { Text("Nº Lote") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = expiryDaysInput,
                        onValueChange = {
                            expiryDaysInput = it
                            validationError = null
                        },
                        label = { Text("Vence en (días)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                validationError?.let { err ->
                    Surface(
                        color = DrakosRedContainer,
                        border = BorderStroke(1.dp, DrakosRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = expiryDaysInput.trim().toIntOrNull()
                    val qtyInvalid = if (isPackMode) {
                        parsedPackCount == null || parsedPackCount <= 0 || parsedUnitsPerPack == null || parsedUnitsPerPack <= 0
                    } else {
                        parsedSingleQty == null || parsedSingleQty <= 0
                    }
                    val costInvalid = if (isPackMode) {
                        parsedPackCost == null || parsedPackCost < 0.0
                    } else {
                        parsedSingleCost == null || parsedSingleCost < 0.0
                    }
                    when {
                        selectedProduct == null -> validationError = "Seleccione un producto válido del catálogo."
                        qtyInvalid -> validationError = "Ingrese una cantidad entera mayor a 0."
                        costInvalid -> validationError = "Ingrese un costo numérico válido (>= 0)."
                        invoiceRefInput.isBlank() -> validationError = "Ingrese el número de remito o factura."
                        days == null || days <= 0 -> validationError = "Ingrese los días hasta vencimiento (> 0)."
                        else -> onConfirm(
                            selectedProdId,
                            selectedSupplierId,
                            effectiveQty,
                            effectiveUnitCost,
                            invoiceRefInput.trim(),
                            batchNumberInput.trim().ifBlank { "LOT-AUTO" },
                            days
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                modifier = Modifier.testTag("confirm_goods_receipt_button")
            ) {
                Text("Ingresar +$effectiveQty u.", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = DrakosBlack)
            }
        }
    )

    if (showInlineScanner) {
        BarcodeScannerDialog(
            title = "Escanear Producto para Ingreso",
            subtitle = "Escanee el código EAN-13 de la caja o unidad a ingresar.",
            onBarcodeDetected = { code ->
                showInlineScanner = false
                applyScannedBarcode(code)
            },
            onDismiss = { showInlineScanner = false }
        )
    }
}

@Composable
fun NewProductDialog(
    categories: List<CategoryEntity>,
    initialBarcode: String = "7790070001",
    onDismiss: () -> Unit,
    onConfirm: (ProductEntity) -> Unit
) {
    var barcodeInput by remember(initialBarcode) { mutableStateOf(initialBarcode) }
    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Text(
                "Alta de Nuevo Producto al Catálogo",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                color = DrakosBlack
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = {
                            barcodeInput = it
                            validationError = null
                        },
                        label = { Text("Código de Barras (EAN)") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = JetBrainsMonoFontFamily),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_product_barcode_input")
                    )
                    Surface(
                        color = DrakosSurfaceTint,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DrakosBorder)
                    ) {
                        IconButton(
                            onClick = { showBarcodeScanner = true },
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("new_product_scan_barcode_button")
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear EAN",
                                tint = DrakosRed
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        validationError = null
                    },
                    label = { Text("Nombre del Producto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoría:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = cat.id },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DrakosBlack,
                                selectedLabelColor = DrakosWhite
                            ),
                            label = { Text(cat.name, fontSize = 11.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = {
                            priceInput = it
                            validationError = null
                        },
                        label = { Text("Precio Venta ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = costInput,
                        onValueChange = {
                            costInput = it
                            validationError = null
                        },
                        label = { Text("Costo ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                validationError?.let { err ->
                    Surface(
                        color = DrakosRedContainer,
                        border = BorderStroke(1.dp, DrakosRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = parseArsDecimal(priceInput)
                    val cost = if (costInput.isBlank()) 0.0 else parseArsDecimal(costInput)
                    when {
                        barcodeInput.isBlank() -> validationError = "Ingrese el código de barras (EAN)."
                        nameInput.isBlank() -> validationError = "Ingrese el nombre del producto."
                        price == null || price <= 0.0 -> validationError = "Ingrese un precio de venta válido mayor a 0."
                        cost == null || cost < 0.0 -> validationError = "Ingrese un costo numérico válido (>= 0)."
                        else -> onConfirm(
                            ProductEntity(
                                id = 0,
                                barcode = barcodeInput.trim(),
                                sku = "SKU-${System.currentTimeMillis() % 10000}",
                                name = nameInput.trim(),
                                description = "Producto ingresado al catálogo Drakos",
                                categoryId = selectedCatId,
                                costPrice = cost,
                                sellingPrice = price,
                                vatRate = 21.0,
                                minStock = 10
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Crear Producto", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = DrakosBlack)
            }
        }
    )

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Escanear EAN Nuevo Producto",
            subtitle = "Capture el código de barras del nuevo artículo para registrarlo.",
            onBarcodeDetected = { code ->
                barcodeInput = code.trim()
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}
