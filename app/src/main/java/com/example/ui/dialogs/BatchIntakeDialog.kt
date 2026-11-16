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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SupplierEntity
import com.example.domain.model.ProductWithStock
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun BatchIntakeDialog(
    suppliers: List<SupplierEntity>,
    products: List<ProductWithStock>,
    onDismiss: () -> Unit,
    onConfirmBatch: (lines: List<Triple<Long, Int, Double>>, supplierId: Long, invoiceRef: String) -> Unit
) {
    val lines = remember { mutableStateMapOf<Long, Int>() }
    var selectedSupplierId by remember { mutableLongStateOf(suppliers.firstOrNull()?.id ?: 1L) }
    var invoiceRef by remember { mutableStateOf("ING-${System.currentTimeMillis() % 10000}") }
    var manualBarcode by remember { mutableStateOf("") }
    var packSize by remember { mutableIntStateOf(1) }
    var showScanner by remember { mutableStateOf(false) }
    var errorNotice by remember { mutableStateOf<String?>(null) }

    fun addScannedProduct(rawCode: String, increment: Int = packSize) {
        val code = rawCode.trim()
        val matched = products.find { it.barcode.equals(code, ignoreCase = true) }
        if (matched != null) {
            lines[matched.productId] = (lines[matched.productId] ?: 0) + increment.coerceAtLeast(1)
            errorNotice = null
        } else {
            errorNotice = "EAN '$code' no encontrado en catálogo."
        }
    }

    val totalUnits = lines.values.sum()
    val packChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = DrakosBlack,
        selectedLabelColor = DrakosWhite
    )
    val quickProdChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = DrakosRedContainer,
        selectedLabelColor = DrakosRed
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Text(
                text = "Ingreso Rápido por Lote / Bulto",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                color = DrakosBlack
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Escanee productos por unidad o bulto cerrado (x6, x12, x24) para sumarlos al stock.",
                    fontSize = 12.sp,
                    color = DrakosMutedInk
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bulto:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1 to "x1 U", 6 to "x6 Pack", 12 to "x12 Bulto", 24 to "x24 Caja").forEach { (mult, label) ->
                            item {
                                FilterChip(
                                    selected = packSize == mult,
                                    onClick = { packSize = mult },
                                    colors = packChipColors,
                                    label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        label = { Text("EAN / Código", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (manualBarcode.isNotBlank()) {
                                addScannedProduct(manualBarcode, packSize)
                                manualBarcode = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("+$packSize", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showScanner = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("batch_scan_next_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear", modifier = Modifier.size(18.dp))
                    }
                }

                if (!errorNotice.isNullOrBlank()) {
                    Text(errorNotice ?: "", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val quickStep = if (packSize > 1) packSize else 6
                    items(products.take(15), key = { it.productId }) { p ->
                        FilterChip(
                            selected = (lines[p.productId] ?: 0) > 0,
                            onClick = { lines[p.productId] = (lines[p.productId] ?: 0) + quickStep },
                            colors = quickProdChipColors,
                            label = { Text("+$quickStep ${p.name.take(12)}", fontSize = 10.sp) }
                        )
                    }
                }

                if (lines.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(lines.entries.toList(), key = { it.key }) { (productId, qty) ->
                            val prod = products.find { it.productId == productId }
                            if (prod != null) {
                                Surface(
                                    border = BorderStroke(1.dp, DrakosBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    color = DrakosWhite
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                                            val packNote = if (packSize > 1 && qty >= packSize) " (${qty / packSize} bulto/s x$packSize)" else ""
                                            Text("Stock actual: ${prod.quantityOnHand} u.$packNote", fontSize = 10.sp, color = DrakosMutedInk)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (qty <= packSize) lines.remove(productId) else lines[productId] = qty - packSize
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Menos", tint = DrakosBlack, modifier = Modifier.size(18.dp))
                                            }
                                            Text("$qty", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                            IconButton(
                                                onClick = { lines[productId] = qty + packSize },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Más", tint = DrakosRed, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                onClick = { lines.remove(productId) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = DangerRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = invoiceRef,
                    onValueChange = { invoiceRef = it },
                    label = { Text("Ref. Remito") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suppliers) { sup ->
                        FilterChip(
                            selected = selectedSupplierId == sup.id,
                            onClick = { selectedSupplierId = sup.id },
                            colors = packChipColors,
                            label = { Text(sup.name.take(14), fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val payload = lines.mapNotNull { (pid, qty) ->
                        val p = products.find { it.productId == pid } ?: return@mapNotNull null
                        Triple(pid, qty, p.costPrice)
                    }
                    onConfirmBatch(payload, selectedSupplierId, invoiceRef)
                },
                enabled = totalUnits > 0,
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                modifier = Modifier.testTag("confirm_batch_intake_button")
            ) {
                Text("Confirmar ($totalUnits u.)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar", color = DrakosBlack) }
        }
    )

    if (showScanner) {
        BarcodeScannerDialog(
            title = "Escanear Producto al Lote",
            subtitle = "Cada producto escaneado sumará $packSize unidad(es) a esta lista.",
            onBarcodeDetected = { code ->
                showScanner = false
                addScannedProduct(code, packSize)
            },
            onDismiss = { showScanner = false }
        )
    }
}
