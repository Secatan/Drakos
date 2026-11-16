package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.BrandNavy900
import com.example.ui.theme.BrandSlate100
import com.example.ui.theme.BrandSlate700
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
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
        // Cabecera SaaS
        Surface(
            color = com.example.ui.theme.DrakosWhite,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.DrakosHairline),
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
                    Text(
                        text = "Transferencias Inter-Sucursales",
                        fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.DrakosDarkGray
                    )
                    Text(
                        text = "Movimiento de mercadería y seguimiento en tránsito",
                        fontSize = 11.sp,
                        color = com.example.ui.theme.DrakosMutedGray
                    )
                }

                if (currentUser?.role != UserRole.EMPLOYEE) {
                    Button(
                        onClick = { showNewTransferModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.DrakosRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("new_transfer_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Solicitar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
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
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No hay transferencias registradas.", color = Color.Gray, fontSize = 14.sp)
                Text("Solicite el envío de mercadería desde otro kiosco de la red.", fontSize = 12.sp, color = Color.LightGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeList) { transfer: StockTransferEntity ->
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
        var targetBranchId by remember { mutableLongStateOf(2L) }
        var qtyInput by remember { mutableStateOf("10") }
        var notesInput by remember { mutableStateOf("Reposición por quiebre de stock") }

        AlertDialog(
            onDismissRequest = { showNewTransferModal = false },
            title = { Text("Solicitar Transferencia de Mercadería") },
            text = {
                Column {
                    Text("Sucursal de Destino (Kiosco Drakos):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val otherBranches = branches.filter { it.id != (currentBranch?.id ?: 1L) }
                    Column {
                        for (b in otherBranches.take(4)) {
                            Surface(
                                color = if (targetBranchId == b.id) PrimaryBlue.copy(alpha = 0.15f) else BrandSlate100,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = targetBranchId == b.id,
                                        onClick = { targetBranchId = b.id }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(b.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Cantidad a Transferir:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Motivo / Observación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = qtyInput.toIntOrNull() ?: 10
                        val prodId = products.firstOrNull()?.productId ?: 1L
                        viewModel.createTransfer(targetBranchId, prodId, qty, notesInput)
                        showNewTransferModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Crear Solicitud")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewTransferModal = false }) {
                    Text("Cancelar")
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
        TransferStatus.DISPATCHED -> PrimaryBlue
        TransferStatus.RECEIVED -> SuccessGreen
        TransferStatus.CANCELLED -> DangerRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp)
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
                    color = BrandNavy900
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
                Text(source, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandNavy900)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.DoubleArrow, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(target, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandNavy900)
            }

            if (transfer.notes.isNotEmpty()) {
                Text(
                    text = "Nota: ${transfer.notes}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Fecha: ${formatDateTime(transfer.createdAt)}",
                fontSize = 10.sp,
                color = Color.LightGray,
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
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Despachar (En Tránsito)", fontSize = 11.sp)
                        }
                    } else if (transfer.status == TransferStatus.DISPATCHED) {
                        Button(
                            onClick = onReceive,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
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
