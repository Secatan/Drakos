package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ProductEntity
import com.example.domain.analytics.DrakosAnalytics
import com.example.domain.model.PriceRoundingStep
import com.example.ui.components.formatCurrency
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosSurfaceTint
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen

@Composable
fun BulkPriceUpdateDialog(
    categories: List<CategoryEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        categoryId: Long?,
        costPercentDelta: Double,
        pricePercentDelta: Double,
        preserveMargin: Boolean,
        roundingStep: PriceRoundingStep
    ) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var costPctInput by remember { mutableStateOf("10") }
    var pricePctInput by remember { mutableStateOf("10") }
    var preserveMargin by remember { mutableStateOf(true) }
    var roundingStep by remember { mutableStateOf(PriceRoundingStep.FIFTY) }

    val costDelta = costPctInput.toDoubleOrNull() ?: 0.0
    val priceDelta = pricePctInput.toDoubleOrNull() ?: 0.0

    val previewRows = remember(products, selectedCategoryId, costDelta, priceDelta, preserveMargin, roundingStep) {
        DrakosAnalytics.buildBulkPricePreview(
            products = products,
            categoryId = selectedCategoryId,
            costPercentDelta = costDelta,
            pricePercentDelta = priceDelta,
            preserveMargin = preserveMargin,
            roundingStep = roundingStep
        )
    }

    val darkChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = DrakosBlack,
        selectedLabelColor = DrakosWhite
    )
    val redChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = DrakosRed,
        selectedLabelColor = DrakosWhite
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Percent, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Actualización Masiva de Precios",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DrakosBlack
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ajuste por rubro con redondeo anti-inflación y vista previa antes de aplicar:",
                    fontSize = 11.sp,
                    color = DrakosMutedInk
                )

                // Filtro de categoría
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            colors = darkChipColors,
                            label = { Text("Todo el catálogo", fontSize = 10.sp) }
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            colors = darkChipColors,
                            label = { Text(cat.name, fontSize = 10.sp) }
                        )
                    }
                }

                // Presets rápidos de %
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 20, -10).forEach { pct ->
                        item {
                            FilterChip(
                                selected = costPctInput == pct.toString() && pricePctInput == pct.toString(),
                                onClick = {
                                    costPctInput = pct.toString()
                                    pricePctInput = pct.toString()
                                },
                                colors = redChipColors,
                                label = {
                                    Text(
                                        text = if (pct > 0) "+$pct%" else "$pct%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPctInput,
                        onValueChange = { costPctInput = it },
                        label = { Text("Ajuste Costo (%)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_cost_pct_input")
                    )
                    OutlinedTextField(
                        value = if (preserveMargin) costPctInput else pricePctInput,
                        onValueChange = { pricePctInput = it },
                        enabled = !preserveMargin,
                        label = { Text("Ajuste Venta (%)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_price_pct_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = preserveMargin,
                        onCheckedChange = { preserveMargin = it },
                        colors = CheckboxDefaults.colors(checkedColor = DrakosRed)
                    )
                    Text(
                        text = "Mantener margen histórico s/ nuevo costo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DrakosBlack
                    )
                }

                // Selector de redondeo psicológico
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PriceRoundingStep.entries) { step ->
                        FilterChip(
                            selected = roundingStep == step,
                            onClick = { roundingStep = step },
                            colors = darkChipColors,
                            label = { Text(step.label, fontSize = 10.sp) }
                        )
                    }
                }

                // Vista previa en vivo
                Surface(
                    color = DrakosSurfaceTint,
                    border = BorderStroke(1.dp, DrakosBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Vista previa (${previewRows.size} artículos):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DrakosBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(previewRows.take(20), key = { it.productId }) { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(row.name.take(22), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DrakosBlack)
                                        Text(
                                            text = "Margen est.: ${String.format("%.1f", row.newMarginPercent)}%",
                                            fontSize = 9.sp,
                                            color = SuccessGreen
                                        )
                                    }
                                    Text(
                                        text = "${formatCurrency(row.oldSellingPrice)} -> ${formatCurrency(row.newSellingPrice)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DrakosRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedCategoryId,
                        costDelta,
                        if (preserveMargin) costDelta else priceDelta,
                        preserveMargin,
                        roundingStep
                    )
                },
                enabled = previewRows.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("confirm_bulk_price_button")
            ) {
                Text("Aplicar (${previewRows.size})", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = DrakosBlack)
            }
        }
    )
}
