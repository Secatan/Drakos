package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CategoryEntity
import com.example.data.entity.SaleEntity
import com.example.domain.model.CartItem
import com.example.domain.model.ProductWithStock
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.DigitalReceiptModal
import com.example.ui.components.formatCurrency
import com.example.ui.dialogs.PaymentDialog
import com.example.ui.theme.BrandNavy900
import com.example.ui.theme.BrandSlate100
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosCharcoal
import com.example.ui.theme.DrakosGraphite
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosSurfaceTint
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun PosScreen(
    viewModel: KioscoViewModel,
    onNavigateToShifts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeCashSession.collectAsStateWithLifecycle()
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCatId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()

    var showPaymentModal by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var lastCompletedSale by remember { mutableStateOf<SaleEntity?>(null) }
    var barcodeInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
    ) {
        // Alerta si la caja está cerrada
        if (activeSession == null) {
            Surface(
                color = WarningAmber.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Turno de caja cerrado. Debe abrir turno con fondo inicial para cobrar.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandNavy900
                        )
                    }
                    Button(
                        onClick = onNavigateToShifts,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Abrir Turno", fontSize = 11.sp)
                    }
                }
            }
        }

        // Barra de búsqueda & Escáner de código de barras
        Surface(
            color = Color.White,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = {
                            barcodeInput = it
                            viewModel.setSearchQuery(it)
                        },
                        placeholder = { Text("Buscar producto o código...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (barcodeInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    barcodeInput = ""
                                    viewModel.setSearchQuery("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (barcodeInput.isNotBlank()) {
                                    viewModel.addByBarcode(barcodeInput)
                                    barcodeInput = ""
                                    viewModel.setSearchQuery("")
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_search_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledTonalIconButton(
                        onClick = { showScannerDialog = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = BrandSlate100,
                            contentColor = PrimaryBlue
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("camera_scanner_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Escanear con cámara")
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            if (barcodeInput.isNotBlank()) {
                                viewModel.addByBarcode(barcodeInput)
                                barcodeInput = ""
                                viewModel.setSearchQuery("")
                            } else {
                                showScannerDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("scan_barcode_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escanear", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Simulador de escáner rápido con botones directos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Escaneo rápido:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val quickSamples = listOf(
                            "Havanna" to "7790010001",
                            "Coca 500" to "7790020001",
                            "Marlboro" to "7790040001",
                            "Beldent" to "7790030001",
                            "Chocolinas" to "7790050001"
                        )
                        items(quickSamples) { (label, ean) ->
                            Surface(
                                color = BrandSlate100,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { viewModel.addByBarcode(ean) }
                            ) {
                                Text(
                                    text = "+ $label",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = BrandNavy900
                                )
                            }
                        }
                    }
                }
            }
        }

        // Combos y Promociones Drakos 24hs
        Surface(
            color = DrakosBlack,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DrakosRed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMBOS DRAKOS:",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = DrakosWhite
                )
                Spacer(modifier = Modifier.width(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Surface(
                            color = DrakosCharcoal,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DrakosRed),
                            modifier = Modifier.clickable {
                                viewModel.addByBarcode("7790010001") // Havanna
                                viewModel.addByBarcode("7790020001") // Coca
                            }
                        ) {
                            Text(
                                text = "Desayuno (Havanna + Coca)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DrakosWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    item {
                        Surface(
                            color = DrakosCharcoal,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DrakosGraphite),
                            modifier = Modifier.clickable {
                                viewModel.addByBarcode("7790030001") // Beldent
                                viewModel.addByBarcode("7790030001") // Beldent
                            }
                        ) {
                            Text(
                                text = "2x Beldent Menta",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DrakosWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    item {
                        Surface(
                            color = DrakosCharcoal,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DrakosGraphite),
                            modifier = Modifier.clickable {
                                viewModel.addByBarcode("7790020006") // Red Bull
                                viewModel.addByBarcode("7790050001") // Chocolinas
                            }
                        ) {
                            Text(
                                text = "Red Bull + Chocolinas",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DrakosWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Categorías en chips horizontales
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCatId == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DrakosBlack,
                        selectedLabelColor = DrakosWhite
                    ),
                    label = { Text("Todos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
            items(categories) { cat: CategoryEntity ->
                FilterChip(
                    selected = selectedCatId == cat.id,
                    onClick = { viewModel.setCategoryFilter(cat.id) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DrakosRed,
                        selectedLabelColor = DrakosWhite
                    ),
                    label = { Text(cat.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // Contenido Principal: Grilla de Productos y Panel de Carrito
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // Grilla de Productos
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxSize()
            ) {
                if (products.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No se encontraron productos", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products) { prod: ProductWithStock ->
                            ProductCard(
                                product = prod,
                                onAdd = { viewModel.addToCart(prod) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Panel Lateral de Carrito de Venta
            Surface(
                color = DrakosWhite,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DrakosBorder),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    // Cabecera Carrito
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ticket Actual", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DrakosBlack)
                        }
                        if (cart.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearCart() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Vaciar", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = DrakosBorder, modifier = Modifier.padding(vertical = 6.dp))

                    // Lista de ítems en carrito
                    if (cart.isEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Carrito vacío", color = DrakosMutedInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Toque un producto o escanee el código EAN.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cart) { item: CartItem ->
                                CartItemRow(
                                    item = item,
                                    onIncrease = { viewModel.updateCartQuantity(item.productId, 1) },
                                    onDecrease = { viewModel.updateCartQuantity(item.productId, -1) },
                                    onRemove = { viewModel.removeFromCart(item.productId) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DrakosBorder, modifier = Modifier.padding(vertical = 8.dp))

                    // Totales y Botón Cobrar
                    val totalCartAmount = cart.sumOf { it.subtotal }
                    val totalItemsCount = cart.sumOf { it.quantity }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ítems: $totalItemsCount", fontSize = 12.sp, color = DrakosMutedInk)
                        Text("Total a Pagar:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                    }

                    Text(
                        text = formatCurrency(totalCartAmount),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DrakosBlack,
                        modifier = Modifier.align(Alignment.End)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showPaymentModal = true },
                        enabled = cart.isNotEmpty() && activeSession != null,
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed, contentColor = DrakosWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pos_checkout_button")
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COBRAR (F12)", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    // Modal de Cobro con Métodos de Pago y Calculadora de Vuelto
    if (showPaymentModal) {
        val totalToPay = cart.sumOf { it.subtotal }
        PaymentDialog(
            totalAmount = totalToPay,
            onDismiss = { showPaymentModal = false },
            onConfirmPayment = { method, discount ->
                viewModel.processSale(method, discount) { sale ->
                    showPaymentModal = false
                    lastCompletedSale = sale
                }
            }
        )
    }

    // Escáner óptico CameraX + ML Kit
    if (showScannerDialog) {
        BarcodeScannerDialog(
            onBarcodeDetected = { code ->
                showScannerDialog = false
                viewModel.addByBarcode(code)
            },
            onDismiss = { showScannerDialog = false }
        )
    }

    // Modal de Ticket Digital luego de venta exitosa
    lastCompletedSale?.let { sale ->
        DigitalReceiptModal(
            sale = sale,
            branch = currentBranch,
            userName = currentUser?.fullName ?: "Cajero",
            companySettings = companySettings,
            onDismiss = { lastCompletedSale = null }
        )
    }
}

@Composable
fun ProductCard(
    product: ProductWithStock,
    onAdd: () -> Unit
) {
    val stockColor = when {
        product.quantityOnHand <= 0 -> DangerRed
        product.quantityOnHand <= product.minStock -> WarningAmber
        else -> SuccessGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = product.quantityOnHand > 0) { onAdd() }
            .testTag("product_card_${product.productId}"),
        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
        border = BorderStroke(1.dp, DrakosBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.categoryName.take(12),
                    fontSize = 10.sp,
                    color = DrakosMutedInk,
                    fontWeight = FontWeight.SemiBold
                )

                // Stock Badge
                Surface(
                    color = stockColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Stock: ${product.quantityOnHand}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = stockColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = product.name,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DrakosBlack,
                maxLines = 2
            )

            Text(
                text = "EAN: ${product.barcode}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = DrakosMutedInk
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(product.sellingPrice),
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = DrakosRed
                )

                IconButton(
                    onClick = onAdd,
                    enabled = product.quantityOnHand > 0,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar",
                        tint = if (product.quantityOnHand > 0) DrakosRed else Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = BrandSlate100,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
            Text(
                text = "${item.quantity} x ${formatCurrency(item.unitPrice)} = ${formatCurrency(item.subtotal)}",
                fontSize = 11.sp,
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Disminuir cantidad", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(onClick = onIncrease, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Aumentar cantidad", modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar del carrito", tint = DangerRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
