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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ProductWithStock
import com.example.domain.model.PromotionDisplay
import com.example.ui.components.formatCurrency
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosLightCanvas
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SuccessGreenDark
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun PromotionsManagementSection(
    viewModel: KioscoViewModel,
    onApplyToPos: ((PromotionDisplay) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val promotions by viewModel.allPromotions.collectAsStateWithLifecycle()
    val products by viewModel.allBranchProducts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val canEdit = (currentUser?.role?.level ?: 1) >= 2

    var showBuilderDialog by remember { mutableStateOf(false) }
    var editingPromo by remember { mutableStateOf<PromotionDisplay?>(null) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabecera Ejecutiva de Promociones
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = DrakosRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Promociones & Combos Drakos",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DrakosDarkGray
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${promotions.count { it.isActive }} activas • Sincronizadas en tiempo real con el Punto de Venta",
                        fontSize = 11.sp,
                        color = DrakosMutedGray
                    )
                }

                if (canEdit) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            editingPromo = null
                            showBuilderDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("create_promotion_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nueva Promo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (promotions.isEmpty()) {
            Surface(
                color = DrakosWhite,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DrakosHairline),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = DrakosMutedGray,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sin promociones registradas",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DrakosDarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cree combos 2x1, descuentos por horario o paquetes especiales para vender rápido en el POS.",
                        fontSize = 12.sp,
                        color = DrakosMutedGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(promotions, key = { it.id }) { promo ->
                    PromotionCard(
                        promo = promo,
                        canEdit = canEdit,
                        onToggleActive = { active ->
                            viewModel.togglePromotionActive(promo.id, active)
                        },
                        onEdit = {
                            editingPromo = promo
                            showBuilderDialog = true
                        },
                        onDelete = {
                            viewModel.deletePromotion(promo.id)
                        },
                        onApplyPos = onApplyToPos?.let { apply -> { apply(promo) } }
                    )
                }
            }
        }
    }

    if (showBuilderDialog) {
        PromotionBuilderDialog(
            products = products,
            existingPromo = editingPromo,
            onDismiss = { showBuilderDialog = false },
            onSave = { title, badge, description, discountPct, fixedPrice, itemsSpec, schedule, isActive ->
                viewModel.savePromotion(
                    promoId = editingPromo?.id,
                    title = title,
                    badge = badge,
                    description = description,
                    discountPercent = discountPct,
                    fixedPromoPrice = fixedPrice,
                    itemsSpec = itemsSpec,
                    validSchedule = schedule,
                    isActive = isActive
                )
                showBuilderDialog = false
            }
        )
    }
}

@Composable
private fun PromotionCard(
    promo: PromotionDisplay,
    canEdit: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApplyPos: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("promo_card_${promo.id}"),
        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
        border = BorderStroke(1.dp, if (promo.isActive) DrakosHairline else DrakosHairline.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (promo.isActive) DrakosRed else DrakosMutedGray,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = promo.badge,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DrakosWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = promo.title,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DrakosDarkGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = promo.validSchedule,
                            fontSize = 11.sp,
                            color = DrakosMutedGray
                        )
                    }
                }

                if (canEdit) {
                    Switch(
                        checked = promo.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DrakosWhite,
                            checkedTrackColor = DrakosRed
                        ),
                        modifier = Modifier.testTag("promo_toggle_${promo.id}")
                    )
                }
            }

            if (promo.description.isNotBlank()) {
                Text(
                    text = promo.description,
                    fontSize = 12.sp,
                    color = DrakosMutedGray
                )
            }

            // Productos incluidos
            Surface(
                color = DrakosLightCanvas,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DrakosHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    promo.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DrakosDarkGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatCurrency(item.regularSubtotal),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = DrakosMutedGray
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (promo.savingsAmount > 0.0) {
                        Text(
                            text = formatCurrency(promo.regularPrice),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = DrakosMutedGray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Text(
                        text = formatCurrency(promo.finalPromoPrice),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DrakosRed
                    )
                    if (promo.savingsAmount > 0.0) {
                        Surface(
                            color = SuccessGreenContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Ahorro ${formatCurrency(promo.savingsAmount)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreenDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = if (promo.hasSufficientStock) SuccessGreenContainer else WarningAmberContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (promo.hasSufficientStock) "Stock OK" else "Faltante en local",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (promo.hasSufficientStock) SuccessGreenDark else WarningAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (canEdit || onApplyPos != null) {
                HorizontalDivider(color = DrakosHairline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onApplyPos != null && promo.isActive) {
                        Button(
                            onClick = onApplyPos,
                            enabled = promo.hasSufficientStock,
                            colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar al Ticket", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (canEdit) {
                        OutlinedButton(
                            onClick = onEdit,
                            border = BorderStroke(1.dp, DrakosHairline),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = DrakosDarkGray, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar", fontSize = 11.sp, color = DrakosDarkGray)
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("delete_promo_${promo.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar promoción", tint = DangerRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionBuilderDialog(
    products: List<ProductWithStock>,
    existingPromo: PromotionDisplay? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        badge: String,
        description: String,
        discountPercent: Double,
        fixedPromoPrice: Double?,
        itemsSpec: String,
        validSchedule: String,
        isActive: Boolean
    ) -> Unit
) {
    var titleInput by remember(existingPromo) { mutableStateOf(existingPromo?.title ?: "") }
    var badgeInput by remember(existingPromo) { mutableStateOf(existingPromo?.badge ?: "COMBO") }
    var descriptionInput by remember(existingPromo) { mutableStateOf(existingPromo?.description ?: "") }
    var scheduleInput by remember(existingPromo) { mutableStateOf(existingPromo?.validSchedule ?: "24hs • Todos los días") }
    var useFixedPrice by remember(existingPromo) { mutableStateOf(existingPromo?.fixedPromoPrice != null) }
    var discountPctInput by remember(existingPromo) {
        mutableStateOf(
            existingPromo?.discountPercent?.takeIf { it > 0 }?.toInt()?.toString() ?: "15"
        )
    }
    var fixedPriceInput by remember(existingPromo) {
        mutableStateOf(
            existingPromo?.fixedPromoPrice?.toInt()?.toString() ?: ""
        )
    }
    var productSearch by remember { mutableStateOf("") }

    val selectedQuantities = remember(existingPromo) {
        mutableStateMapOf<Long, Int>().apply {
            existingPromo?.items?.forEach { put(it.productId, it.quantity) }
        }
    }

    val prodMap = remember(products) { products.associateBy { it.productId } }
    val filteredCatalog = remember(products, productSearch) {
        if (productSearch.isBlank()) products.take(12)
        else products.filter {
            it.name.contains(productSearch, ignoreCase = true) ||
                it.barcode.contains(productSearch, ignoreCase = true)
        }.take(15)
    }

    val regularTotal = selectedQuantities.entries.sumOf { (pid, qty) ->
        (prodMap[pid]?.sellingPrice ?: 0.0) * qty
    }
    val pctValue = (discountPctInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 90.0)
    val fixedValue = fixedPriceInput.toDoubleOrNull()
    val previewFinalPrice = if (useFixedPrice) {
        (fixedValue ?: regularTotal).coerceAtLeast(0.0)
    } else {
        (regularTotal * (1.0 - pctValue / 100.0)).coerceAtLeast(0.0)
    }
    val previewSavings = (regularTotal - previewFinalPrice).coerceAtLeast(0.0)
    val itemsSpec = selectedQuantities.entries
        .filter { it.value > 0 }
        .joinToString(",") { "${it.key}:${it.value}" }

    val badgeOptions = listOf("COMBO", "2x1", "DUO", "NOCHE", "OFERTA", "FLASH")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingPromo == null) "Crear Promoción / Combo" else "Editar Promoción",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DrakosDarkGray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Nombre de la promoción / combo") },
                    placeholder = { Text("Ej: Combo Noche Monster + Oreo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("promo_title_input")
                )

                Text("Etiqueta / Badge:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(badgeOptions) { option ->
                        FilterChip(
                            selected = badgeInput.equals(option, ignoreCase = true),
                            onClick = { badgeInput = option },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DrakosRed,
                                selectedLabelColor = DrakosWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            label = { Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Selector de productos para la promo
                Text(
                    text = "Productos del combo (${selectedQuantities.size} seleccionados):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DrakosDarkGray
                )

                OutlinedTextField(
                    value = productSearch,
                    onValueChange = { productSearch = it },
                    placeholder = { Text("Buscar producto para sumar al combo...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DrakosMutedGray, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DrakosLightCanvas,
                        unfocusedContainerColor = DrakosLightCanvas,
                        focusedBorderColor = DrakosBlack,
                        unfocusedBorderColor = DrakosHairline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredCatalog, key = { it.productId }) { prod ->
                        val inComboQty = selectedQuantities[prod.productId] ?: 0
                        Surface(
                            color = if (inComboQty > 0) DrakosRedContainer else DrakosLightCanvas,
                            border = BorderStroke(1.dp, if (inComboQty > 0) DrakosRed else DrakosHairline),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable {
                                    selectedQuantities[prod.productId] = inComboQty + 1
                                }
                                .testTag("promo_pick_product_${prod.productId}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = if (inComboQty > 0) "${inComboQty}x ${prod.name.take(18)}" else "+ ${prod.name.take(18)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (inComboQty > 0) DrakosRed else DrakosDarkGray
                                )
                                Text(
                                    text = formatCurrency(prod.sellingPrice),
                                    fontSize = 10.sp,
                                    color = DrakosMutedGray
                                )
                            }
                        }
                    }
                }

                if (selectedQuantities.isNotEmpty()) {
                    Surface(
                        color = DrakosLightCanvas,
                        border = BorderStroke(1.dp, DrakosHairline),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedQuantities.entries.toList().forEach { (pid, qty) ->
                                val prod = prodMap[pid] ?: return@forEach
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prod.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DrakosDarkGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (qty <= 1) selectedQuantities.remove(pid)
                                                else selectedQuantities[pid] = qty - 1
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Quitar", tint = DrakosDarkGray, modifier = Modifier.size(14.dp))
                                        }
                                        Text(
                                            text = "$qty",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = DrakosDarkGray
                                        )
                                        IconButton(
                                            onClick = { selectedQuantities[pid] = qty + 1 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Sumar", tint = DrakosRed, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Modo de descuento
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !useFixedPrice,
                        onClick = { useFixedPrice = false },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DrakosBlack,
                            selectedLabelColor = DrakosWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        label = { Text("% Descuento", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = useFixedPrice,
                        onClick = { useFixedPrice = true },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DrakosBlack,
                            selectedLabelColor = DrakosWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        label = { Text("Precio Fijo ($)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (useFixedPrice) {
                    OutlinedTextField(
                        value = fixedPriceInput,
                        onValueChange = { fixedPriceInput = it },
                        label = { Text("Precio final del combo ($ ARS)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("promo_fixed_price_input")
                    )
                } else {
                    OutlinedTextField(
                        value = discountPctInput,
                        onValueChange = { discountPctInput = it },
                        label = { Text("Descuento aplicado (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("promo_discount_input")
                    )
                }

                // Vista previa de precio
                Surface(
                    color = DrakosBlack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Lista: ${formatCurrency(regularTotal)}", fontSize = 10.sp, color = DrakosWhite.copy(alpha = 0.7f))
                            Text(
                                text = "Ahorro cliente: ${formatCurrency(previewSavings)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PRECIO PROMO", fontSize = 9.sp, color = DrakosWhite.copy(alpha = 0.7f))
                            Text(
                                text = formatCurrency(previewFinalPrice),
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DrakosWhite
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = scheduleInput,
                    onValueChange = { scheduleInput = it },
                    label = { Text("Vigencia / Horario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Descripción opcional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val autoDesc = descriptionInput.ifBlank {
                        selectedQuantities.entries.joinToString(" + ") { (pid, qty) ->
                            "${qty}x ${prodMap[pid]?.name ?: "Prod"}"
                        }
                    }
                    onSave(
                        titleInput,
                        badgeInput,
                        autoDesc,
                        if (useFixedPrice) 0.0 else pctValue,
                        if (useFixedPrice) fixedValue else null,
                        itemsSpec,
                        scheduleInput,
                        existingPromo?.isActive ?: true
                    )
                },
                enabled = titleInput.isNotBlank() && itemsSpec.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_promotion_button")
            ) {
                Text("Guardar Promoción", color = DrakosWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancelar", color = DrakosDarkGray)
            }
        }
    )
}
