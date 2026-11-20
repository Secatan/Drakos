package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BranchEntity
import com.example.data.entity.StockTransferEntity
import com.example.domain.model.TransferStatus
import com.example.domain.model.UserRole
import com.example.ui.components.formatDateTime
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceSoft
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun TransfersScreen(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val transfers by viewModel.branchTransfers.collectAsStateWithLifecycle()
    val allTransfers by viewModel.allTransfers.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val products by viewModel.allBranchProducts.collectAsStateWithLifecycle()

    val activeList: List<StockTransferEntity> = if (currentUser?.role == UserRole.OWNER) allTransfers else transfers
    var showNewTransferModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.DrakosCanvas)
            .padding(14.dp)
    ) {
        // Barra compacta de resumen y acción principal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Envíos entre Sucursales (${activeList.size})",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DrakosDarkGray
                )
                Text(
                    text = "Reposición y seguimiento de mercadería en tránsito",
                    fontSize = 11.sp,
                    color = DrakosMutedGray
                )
            }

            if (currentUser?.role != UserRole.EMPLOYEE) {
                Button(
                    onClick = { showNewTransferModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("new_transfer_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Solicitar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeList.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(48.dp), tint = DrakosMutedGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No hay transferencias registradas.", color = DrakosMutedGray, fontSize = 14.sp)
                Text("Solicite el envío de mercadería desde otro kiosco de la red.", fontSize = 12.sp, color = DrakosMutedGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeList, key = { it.id }) { transfer: StockTransferEntity ->
                    TransferCard(
                        transfer = transfer,
                        branches = branches,
                        userRole = currentUser?.role ?: UserRole.EMPLOYEE,
                        onDispatch = { viewModel.dispatchTransfer(transfer) },
                        onReceive = { viewModel.receiveTransfer(transfer) }
                    )
                }
            }
        }
    }

    // Modal Nueva Transferencia
    if (showNewTransferModal) {
        val originBranchId = currentBranch?.id ?: 1L
        val otherBranches = remember(branches, originBranchId) {
            branches.filter { it.id != originBranchId }
        }
        var targetBranchId by remember(otherBranches) {
            mutableLongStateOf(otherBranches.firstOrNull()?.id ?: 0L)
        }
        var selectedProdId by remember(products) {
            mutableLongStateOf(products.firstOrNull { it.quantityOnHand > 0 }?.productId ?: products.firstOrNull()?.productId ?: 0L)
        }
        var qtyInput by remember { mutableStateOf("10") }
        var notesInput by remember { mutableStateOf("Reposición por quiebre de stock") }
        var validationError by remember { mutableStateOf<String?>(null) }
        val selectedProduct = products.find { it.productId == selectedProdId }

        AlertDialog(
            onDismissRequest = { showNewTransferModal = false },
            containerColor = DrakosWhite,
            title = {
                Text(
                    "Solicitar Transferencia de Mercadería",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = DrakosDarkGray
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Sucursal de Destino (Kiosco Drakos):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (otherBranches.isEmpty()) {
                        Text("No hay otras sucursales disponibles en la red.", fontSize = 11.sp, color = DangerRed)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            for (b in otherBranches) {
                                val selected = targetBranchId == b.id
                                Surface(
                                    color = if (selected) DrakosRedContainer else DrakosSurfaceSoft,
                                    border = BorderStroke(1.dp, if (selected) DrakosRed else DrakosHairline),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            targetBranchId = b.id
                                            validationError = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = {
                                                targetBranchId = b.id
                                                validationError = null
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = DrakosRed)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${b.code} • ${b.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DrakosDarkGray)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Producto a Transferir:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(products, key = { it.productId }) { p ->
                            FilterChip(
                                selected = selectedProdId == p.productId,
                                onClick = {
                                    selectedProdId = p.productId
                                    validationError = null
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DrakosBlack,
                                    selectedLabelColor = DrakosWhite
                                ),
                                label = {
                                    Text("${p.name.take(18)} (${p.quantityOnHand} u.)", fontSize = 10.sp)
                                }
                            )
                        }
                    }
                    if (selectedProduct != null) {
                        Text(
                            text = "Stock disponible en origen: ${selectedProduct.quantityOnHand} u.",
                            fontSize = 10.sp,
                            color = DrakosMutedGray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Cantidad a Transferir:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it; validationError = null },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("transfer_qty_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Motivo / Observación") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!validationError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(validationError ?: "", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = qtyInput.trim().toIntOrNull()
                        val prod = selectedProduct
                        when {
                            targetBranchId == 0L || targetBranchId == originBranchId ->
                                validationError = "Seleccione una sucursal de destino distinta a la de origen."
                            prod == null ->
                                validationError = "Seleccione un producto válido del catálogo."
                            qty == null || qty <= 0 ->
                                validationError = "Ingrese una cantidad entera mayor a 0."
                            qty > prod.quantityOnHand ->
                                validationError = "Stock insuficiente en origen (disponible: ${prod.quantityOnHand} u.)."
                            else -> {
                                viewModel.createTransfer(targetBranchId, prod.productId, qty, notesInput.trim())
                                showNewTransferModal = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
                ) {
                    Text("Crear Solicitud")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewTransferModal = false }) {
                    Text("Cancelar", color = DrakosDarkGray)
                }
            }
        )
    }
}

@Composable
fun TransferCard(
    transfer: StockTransferEntity,
    branches: List<BranchEntity>,
    userRole: UserRole,
    onDispatch: () -> Unit,
    onReceive: () -> Unit
) {
    val source = branches.find { it.id == transfer.sourceBranchId }?.name ?: "Sucursal ${transfer.sourceBranchId}"
    val target = branches.find { it.id == transfer.targetBranchId }?.name ?: "Sucursal ${transfer.targetBranchId}"

    val statusColor = when (transfer.status) {
        TransferStatus.REQUESTED -> WarningAmber
        TransferStatus.DISPATCHED -> InfoBlue
        TransferStatus.RECEIVED -> SuccessGreen
        TransferStatus.CANCELLED -> DangerRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
        border = BorderStroke(1.dp, DrakosHairline),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transferencia #TRF-${transfer.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DrakosDarkGray
                )
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = transfer.status.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(source, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DrakosDarkGray)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.DoubleArrow, contentDescription = null, tint = DrakosMutedGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(target, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DrakosDarkGray)
            }

            if (transfer.notes.isNotEmpty()) {
                Text(
                    text = "Nota: ${transfer.notes}",
                    fontSize = 11.sp,
                    color = DrakosMutedGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Fecha: ${formatDateTime(transfer.createdAt)}",
                fontSize = 10.sp,
                color = DrakosMutedGray,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Acciones de Despacho y Recepción
            if (userRole != UserRole.EMPLOYEE) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (transfer.status == TransferStatus.REQUESTED) {
                        Button(
                            onClick = onDispatch,
                            colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Despachar (En Tránsito)", fontSize = 11.sp)
                        }
                    } else if (transfer.status == TransferStatus.DISPATCHED) {
                        Button(
                            onClick = onReceive,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recibir Mercadería", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
