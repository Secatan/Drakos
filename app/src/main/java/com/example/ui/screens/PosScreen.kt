package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CategoryEntity
import com.example.data.entity.SaleEntity
import com.example.domain.model.CartItem
import com.example.domain.model.ProductWithStock
import com.example.domain.model.PromotionDisplay
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.DigitalReceiptModal
import com.example.ui.components.DrakosSlideOverDrawer
import com.example.ui.components.formatCurrency
import com.example.ui.dialogs.PaymentDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosLightCanvas
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceSoft
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SuccessGreenDark
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun PosScreen(
    viewModel: KioscoViewModel,
    onNavigateToShifts: () -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeCashSession.collectAsStateWithLifecycle()
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allBranchProducts by viewModel.allBranchProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCatId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val activePromotions by viewModel.activePromotions.collectAsStateWithLifecycle()
    val appliedPromoDiscount by viewModel.appliedPromoDiscount.collectAsStateWithLifecycle()
    val appliedPromoLabels by viewModel.appliedPromoLabels.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()

    val canCreatePromos = (currentUser?.role?.level ?: 1) >= 2
    var showPaymentModal by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showPromoBuilderModal by remember { mutableStateOf(false) }
    var showQuickDrawer by remember { mutableStateOf(false) }
    var lastCompletedSale by remember { mutableStateOf<SaleEntity?>(null) }
    var barcodeInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosLightCanvas)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabecera POS minimalista: Buscador + Escáner + botón Promos/Express + pills de categoría
        Surface(
            color = DrakosWhite,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DrakosHairline),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = {
                            barcodeInput = it
                            viewModel.setSearchQuery(it)
                        },
                        placeholder = {
                            Text(
                                "Buscar producto o código EAN...",
                                fontSize = 13.sp,
                                color = DrakosMutedGray
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = DrakosMutedGray)
                        },
                        trailingIcon = {
                            if (barcodeInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    barcodeInput = ""
                                    viewModel.setSearchQuery("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = DrakosDarkGray)
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
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DrakosLightCanvas,
                            unfocusedContainerColor = DrakosLightCanvas,
                            focusedTextColor = DrakosDarkGray,
                            unfocusedTextColor = DrakosDarkGray,
                            focusedBorderColor = DrakosBlack,
                            unfocusedBorderColor = DrakosHairline,
                            cursorColor = DrakosRed
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_search_input")
                    )

                    Surface(
                        color = DrakosBlack,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(50.dp)
                            .clickable { showScannerDialog = true }
                            .testTag("camera_scanner_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear con cámara",
                                tint = DrakosWhite,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    Surface(
                        color = DrakosSurfaceSoft,
                        border = BorderStroke(1.dp, DrakosHairline),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                if (barcodeInput.isNotBlank()) {
                                    viewModel.addByBarcode(barcodeInput)
                                    barcodeInput = ""
                                    viewModel.setSearchQuery("")
                                } else {
                                    showScannerDialog = true
                                }
                            }
                            .testTag("scan_barcode_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar por código",
                                tint = DrakosDarkGray,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            CategoryPillChip(
                                label = "Todos",
                                selected = selectedCatId == null,
                                onClick = { viewModel.setCategoryFilter(null) }
                            )
                        }
                        items(categories) { cat: CategoryEntity ->
                            CategoryPillChip(
                                label = cat.name,
                                selected = selectedCatId == cat.id,
                                onClick = { viewModel.setCategoryFilter(cat.id) }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showQuickDrawer = true },
                        border = BorderStroke(1.dp, DrakosHairline),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("pos_quick_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = "Promociones y accesos rápidos",
                            tint = if (activePromotions.isNotEmpty()) DrakosRed else DrakosDarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activePromotions.isNotEmpty()) "Combos (${activePromotions.size})" else "Express",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DrakosDarkGray
                        )
                    }
                }
            }
        }

        // Alerta de Caja Cerrada
        if (activeSession == null) {
            Surface(
                color = WarningAmberContainer,
                border = BorderStroke(1.dp, WarningAmber),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Turno de caja cerrado. Abra turno para cobrar.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DrakosDarkGray
                        )
                    }
                    Button(
                        onClick = onNavigateToShifts,
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Text("Abrir Turno", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Contenido principal adaptable: Catálogo + Ticket Actual
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isSplitHorizontal = maxWidth >= 340.dp
            val catalogWeight = if (maxWidth >= 700.dp) 1.55f else 1.2f

            if (isSplitHorizontal) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PosCatalogPane(
                        products = products,
                        onAddProduct = { viewModel.addToCart(it) },
                        modifier = Modifier
                            .weight(catalogWeight)
                            .fillMaxSize()
                    )
                    PosCartPane(
                        cart = cart,
                        promoDiscount = appliedPromoDiscount,
                        promoLabels = appliedPromoLabels,
                        canCheckout = cart.isNotEmpty() && activeSession != null,
                        onClearCart = { viewModel.clearCart() },
                        onIncrease = { viewModel.updateCartQuantity(it, 1) },
                        onDecrease = { viewModel.updateCartQuantity(it, -1) },
                        onRemove = { viewModel.removeFromCart(it) },
                        onCheckout = { showPaymentModal = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PosCatalogPane(
                        products = products,
                        onAddProduct = { viewModel.addToCart(it) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    PosCartPane(
                        cart = cart,
                        promoDiscount = appliedPromoDiscount,
                        promoLabels = appliedPromoLabels,
                        canCheckout = cart.isNotEmpty() && activeSession != null,
                        onClearCart = { viewModel.clearCart() },
                        onIncrease = { viewModel.updateCartQuantity(it, 1) },
                        onDecrease = { viewModel.updateCartQuantity(it, -1) },
                        onRemove = { viewModel.removeFromCart(it) },
                        onCheckout = { showPaymentModal = true },
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    DrakosSlideOverDrawer(
        visible = showQuickDrawer,
        title = "Accesos Express y Combos",
        subtitle = "Agregue productos frecuentes o aplique promociones activas",
        onDismiss = { showQuickDrawer = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Venta Rápida",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DrakosDarkGray
            )
            val quickSamples = listOf(
                Triple("Favoritos", "", Icons.Default.Star),
                Triple("+Havanna", "7790010001", Icons.Default.Cookie),
                Triple("+Coca 500", "7790020001", Icons.Default.LocalDrink),
                Triple("+Marlboro", "7790040001", Icons.Default.SmokingRooms),
                Triple("+Beldent", "7790030001", Icons.Default.Icecream),
                Triple("+Chocolinas", "7790050001", Icons.Default.Fastfood)
            )
            quickSamples.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (label, ean, icon) ->
                        QuickPosTile(
                            label = label,
                            icon = icon,
                            highlighted = ean.isEmpty() && selectedCatId == null && barcodeInput.isEmpty(),
                            onClick = {
                                if (ean.isEmpty()) {
                                    barcodeInput = ""
                                    viewModel.setSearchQuery("")
                                    viewModel.setCategoryFilter(null)
                                    showQuickDrawer = false
                                } else {
                                    viewModel.addByBarcode(ean)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(color = DrakosHairline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = DrakosRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Promociones y Combos (${activePromotions.size})",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DrakosDarkGray
                    )
                }
                if (canCreatePromos) {
                    OutlinedButton(
                        onClick = {
                            showQuickDrawer = false
                            showPromoBuilderModal = true
                        },
                        border = BorderStroke(1.dp, DrakosHairline),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("pos_add_promo_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DrakosDarkGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nueva Promo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DrakosDarkGray)
                    }
                }
            }

            if (activePromotions.isEmpty()) {
                Text(
                    text = "No hay promociones activas configuradas en esta sucursal.",
                    fontSize = 12.sp,
                    color = DrakosMutedGray
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activePromotions.forEach { promo: PromotionDisplay ->
                        val priceTag = if (promo.savingsAmount > 0.0) {
                            "${promo.badge} • ${promo.title} (${formatCurrency(promo.finalPromoPrice)})"
                        } else {
                            "${promo.badge} • ${promo.title}"
                        }
                        ComboPosChip(
                            text = priceTag,
                            highlighted = promo.id == activePromotions.firstOrNull()?.id,
                            onClick = {
                                viewModel.applyPromotionToCart(promo)
                                showQuickDrawer = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showPaymentModal) {
        val totalToPay = cart.sumOf { it.subtotal }
        PaymentDialog(
            totalAmount = totalToPay,
            promoDiscount = appliedPromoDiscount,
            promoLabels = appliedPromoLabels,
            onDismiss = { showPaymentModal = false },
            onConfirmPayment = { method, discount, accountHolder ->
                viewModel.processSale(method, discount, accountHolder) { sale ->
                    showPaymentModal = false
                    lastCompletedSale = sale
                }
            }
        )
    }

    if (showPromoBuilderModal) {
        PromotionBuilderDialog(
            products = allBranchProducts,
            existingPromo = null,
            onDismiss = { showPromoBuilderModal = false },
            onSave = { title, badge, description, discountPct, fixedPrice, itemsSpec, schedule, isActive ->
                viewModel.savePromotion(
                    promoId = null,
                    title = title,
                    badge = badge,
                    description = description,
                    discountPercent = discountPct,
                    fixedPromoPrice = fixedPrice,
                    itemsSpec = itemsSpec,
                    validSchedule = schedule,
                    isActive = isActive,
                    onSuccess = { showPromoBuilderModal = false }
                )
            }
        )
    }

    if (showScannerDialog) {
        BarcodeScannerDialog(
            onBarcodeDetected = { code ->
                showScannerDialog = false
                viewModel.addByBarcode(code)
            },
            onDismiss = { showScannerDialog = false }
        )
    }

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
private fun PosCatalogPane(
    products: List<ProductWithStock>,
    onAddProduct: (ProductWithStock) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (products.isEmpty()) {
            Surface(
                color = DrakosWhite,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DrakosHairline),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(44.dp), tint = DrakosMutedGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No se encontraron productos", color = DrakosMutedGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products, key = { it.productId }) { prod: ProductWithStock ->
                    ProductCard(
                        product = prod,
                        onAdd = { onAddProduct(prod) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PosCartPane(
    cart: List<CartItem>,
    promoDiscount: Double = 0.0,
    promoLabels: List<String> = emptyList(),
    canCheckout: Boolean,
    onClearCart: () -> Unit,
    onIncrease: (Long) -> Unit,
    onDecrease: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DrakosWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DrakosHairline),
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = DrakosDarkGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ticket Actual",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DrakosDarkGray
                    )
                }
                if (cart.isNotEmpty()) {
                    TextButton(
                        onClick = onClearCart,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Vaciar", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = DrakosHairline, modifier = Modifier.padding(vertical = 6.dp))

            if (cart.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = DrakosMutedGray,
                        modifier = Modifier
                            .size(48.dp)
                            .alpha(0.24f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Carrito vacío",
                        color = DrakosDarkGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toque un producto, combo o escanee el código EAN.",
                        fontSize = 11.sp,
                        color = DrakosMutedGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(cart, key = { it.productId }) { item: CartItem ->
                        CartItemRow(
                            item = item,
                            onIncrease = { onIncrease(item.productId) },
                            onDecrease = { onDecrease(item.productId) },
                            onRemove = { onRemove(item.productId) }
                        )
                    }
                }
            }

            HorizontalDivider(color = DrakosHairline, modifier = Modifier.padding(vertical = 8.dp))

            val rawCartAmount = cart.sumOf { it.subtotal }
            val effectivePromoDiscount = promoDiscount.coerceAtMost(rawCartAmount)
            val totalCartAmount = (rawCartAmount - effectivePromoDiscount).coerceAtLeast(0.0)
            val totalItemsCount = cart.sumOf { it.quantity }

            if (effectivePromoDiscount > 0.0) {
                Surface(
                    color = SuccessGreenContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = promoLabels.distinct().joinToString(", ").ifBlank { "Promo aplicada" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreenDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "-${formatCurrency(effectivePromoDiscount)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreenDark
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ítems: $totalItemsCount", fontSize = 11.sp, color = DrakosMutedGray, fontWeight = FontWeight.Medium)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total a Pagar:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = DrakosMutedGray)
                    Text(
                        text = formatCurrency(totalCartAmount),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DrakosDarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCheckout,
                enabled = canCheckout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DrakosRed,
                    contentColor = DrakosWhite,
                    disabledContainerColor = DrakosRed.copy(alpha = 0.42f),
                    disabledContentColor = DrakosWhite.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pos_checkout_button")
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "COBRAR (F12)",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun QuickPosTile(
    label: String,
    icon: ImageVector,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (highlighted) DrakosBlack else DrakosLightCanvas,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (highlighted) DrakosBlack else DrakosHairline),
        modifier = modifier
            .height(54.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) DrakosWhite else DrakosDarkGray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold,
                color = if (highlighted) DrakosWhite else DrakosDarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ComboPosChip(
    text: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (highlighted) DrakosRedContainer else DrakosLightCanvas,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (highlighted) DrakosRed.copy(alpha = 0.35f) else DrakosHairline),
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlighted) DrakosRed else DrakosDarkGray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun CategoryPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) DrakosBlack else DrakosLightCanvas,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) DrakosBlack else DrakosHairline),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) DrakosWhite else DrakosDarkGray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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
        else -> SuccessGreenDark
    }
    val stockBg = when {
        product.quantityOnHand <= 0 -> DrakosRedContainer
        product.quantityOnHand <= product.minStock -> WarningAmberContainer
        else -> SuccessGreenContainer
    }
    val categoryVisual = remember(product.categoryName) {
        categoryIconAndTint(product.categoryName)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = product.quantityOnHand > 0) { onAdd() }
            .testTag("product_card_${product.productId}"),
        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
        border = BorderStroke(1.dp, DrakosHairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryVisual.second.copy(alpha = 0.10f))
            ) {
                Icon(
                    imageVector = categoryVisual.first,
                    contentDescription = product.categoryName,
                    tint = categoryVisual.second,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.categoryName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = DrakosMutedGray
                    )
                    Surface(
                        color = stockBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Stock: ${product.quantityOnHand}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = stockColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = product.name,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DrakosDarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "EAN: ${product.barcode}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = DrakosMutedGray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatCurrency(product.sellingPrice),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = DrakosDarkGray
                    )

                    IconButton(
                        onClick = onAdd,
                        enabled = product.quantityOnHand > 0,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (product.quantityOnHand > 0) DrakosBlack else DrakosSurfaceSoft
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar",
                                tint = if (product.quantityOnHand > 0) DrakosWhite else DrakosMutedGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun categoryIconAndTint(categoryName: String): Pair<ImageVector, Color> {
    val lower = categoryName.lowercase()
    return when {
        lower.contains("bebida") || lower.contains("agua") || lower.contains("energ") ->
            Icons.Default.LocalDrink to Color(0xFF0284C7)
        lower.contains("alfajor") || lower.contains("gallet") || lower.contains("chocol") ->
            Icons.Default.Cookie to Color(0xFFB45309)
        lower.contains("cigarr") || lower.contains("tabac") ->
            Icons.Default.SmokingRooms to DrakosMutedGray
        lower.contains("golos") || lower.contains("helad") ->
            Icons.Default.Icecream to DrakosRed
        lower.contains("snack") || lower.contains("comid") ->
            Icons.Default.Fastfood to Color(0xFFD97706)
        else -> Icons.Default.ShoppingBag to DrakosRed
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
        color = DrakosLightCanvas,
        border = BorderStroke(1.dp, DrakosHairline),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = DrakosDarkGray)
            Text(
                text = "${item.quantity} x ${formatCurrency(item.unitPrice)} = ${formatCurrency(item.subtotal)}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = DrakosDarkGray,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Disminuir cantidad", tint = DrakosDarkGray, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DrakosDarkGray,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(onClick = onIncrease, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Aumentar cantidad", tint = DrakosDarkGray, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar del carrito", tint = DangerRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
