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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.entity.CashSessionEntity
import com.example.ui.components.formatCurrency
import com.example.ui.theme.BrandNavy900
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosSurfaceTint
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.PrimaryBlue

@Composable
fun OpenShiftDialog(
    onDismiss: () -> Unit,
    onConfirm: (openingAmount: Double, notes: String) -> Unit
) {
    var openAmountInput by remember { mutableStateOf("15000") }
    var notesInput by remember { mutableStateOf("Fondo de cambio en billetes surtidos") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = { Text("Apertura de Turno de Caja", fontWeight = FontWeight.Bold, color = DrakosBlack) },
        text = {
            Column {
                Text("Ingrese el importe en efectivo con el que inicia la gaveta:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = openAmountInput,
                    onValueChange = { openAmountInput = it },
                    label = { Text("Fondo Inicial ($ ARS)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("opening_amount_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Observaciones / Billetes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = openAmountInput.toDoubleOrNull() ?: 15000.0
                    onConfirm(amount, notesInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Abrir Turno", color = Color.White, fontWeight = FontWeight.Bold)
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
fun BlindCloseShiftDialog(
    onDismiss: () -> Unit,
    onConfirm: (countedAmount: Double, notes: String) -> Unit
) {
    var countedAmountInput by remember { mutableStateOf("") }
    var qrDeclaredInput by remember { mutableStateOf("") }
    var cardDeclaredInput by remember { mutableStateOf("") }
    var closeNotesInput by remember { mutableStateOf("") }
    var showBillCounter by remember { mutableStateOf(false) }

    var b20k by remember { mutableIntStateOf(0) }
    var b10k by remember { mutableIntStateOf(0) }
    var b2k by remember { mutableIntStateOf(0) }
    var b1k by remember { mutableIntStateOf(0) }
    var b500 by remember { mutableIntStateOf(0) }

    val countedFromBills = (b20k * 20000L) + (b10k * 10000L) + (b2k * 2000L) + (b1k * 1000L) + (b500 * 500L)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = DrakosRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Arqueo Ciego de Cierre", fontWeight = FontWeight.Bold, color = DrakosBlack)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Cuente billetes y monedas físicos en gaveta sin referencia del esperado:",
                    fontSize = 12.sp,
                    color = BrandNavy900
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = countedAmountInput,
                    onValueChange = { countedAmountInput = it },
                    label = { Text("Efectivo Físico Contado ($ ARS)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("counted_cash_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { showBillCounter = !showBillCounter },
                    modifier = Modifier.fillMaxWidth().testTag("toggle_bill_counter_button")
                ) {
                    Text(
                        text = if (showBillCounter) "Ocultar Contador de Billetes" else "Abrir Contador Rápido de Billetes ARS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DrakosBlack
                    )
                }

                if (showBillCounter) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = DrakosSurfaceTint,
                        border = BorderStroke(1.dp, DrakosBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            BillCounterRow("$20.000", b20k, { b20k = (b20k - 1).coerceAtLeast(0) }, { b20k++ })
                            BillCounterRow("$10.000", b10k, { b10k = (b10k - 1).coerceAtLeast(0) }, { b10k++ })
                            BillCounterRow("$2.000", b2k, { b2k = (b2k - 1).coerceAtLeast(0) }, { b2k++ })
                            BillCounterRow("$1.000", b1k, { b1k = (b1k - 1).coerceAtLeast(0) }, { b1k++ })
                            BillCounterRow("$500", b500, { b500 = (b500 - 1).coerceAtLeast(0) }, { b500++ })

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Suma billetes: ${formatCurrency(countedFromBills.toDouble())}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrakosRed
                                )
                                Button(
                                    onClick = { countedAmountInput = countedFromBills.toString() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("apply_bill_sum_button")
                                ) {
                                    Text("Aplicar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = qrDeclaredInput,
                        onValueChange = { qrDeclaredInput = it },
                        label = { Text("QR/Transf ($)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cardDeclaredInput,
                        onValueChange = { cardDeclaredInput = it },
                        label = { Text("Posnet/Tarj ($)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = closeNotesInput,
                    onValueChange = { closeNotesInput = it },
                    label = { Text("Observaciones del turno") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "* El desvío respecto al esperado queda registrado en auditoría para Encargado/Dueño.",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val counted = countedAmountInput.toDoubleOrNull() ?: 0.0
                    val extraNotes = buildList {
                        if (qrDeclaredInput.isNotBlank()) add("QR decl.: $${qrDeclaredInput.trim()}")
                        if (cardDeclaredInput.isNotBlank()) add("Tarjetas decl.: $${cardDeclaredInput.trim()}")
                        if (closeNotesInput.isNotBlank()) add(closeNotesInput.trim())
                    }.joinToString(" | ")
                    onConfirm(counted, extraNotes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                modifier = Modifier.testTag("confirm_close_shift_button")
            ) {
                Text("Confirmar Cierre")
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
private fun BillCounterRow(
    label: String,
    count: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onMinus,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.width(48.dp).height(48.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, color = DrakosBlack)
            }
            Text(
                text = "  $count  ",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DrakosBlack
            )
            OutlinedButton(
                onClick = onPlus,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.width(48.dp).height(48.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, color = DrakosRed)
            }
        }
    }
}

@Composable
fun CashDropDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, reason: String) -> Unit
) {
    var dropAmountInput by remember { mutableStateOf("") }
    var dropReasonInput by remember { mutableStateOf("Retiro parcial a caja fuerte") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = { Text("Retiro Parcial de Efectivo (Drop)", fontWeight = FontWeight.Bold, color = DrakosBlack) },
        text = {
            Column {
                Text("Retire efectivo de la gaveta por seguridad para resguardo:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = dropAmountInput,
                    onValueChange = { dropAmountInput = it },
                    label = { Text("Importe a Retirar ($ ARS)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = dropReasonInput,
                    onValueChange = { dropReasonInput = it },
                    label = { Text("Motivo / Destino") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = dropAmountInput.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(amount, dropReasonInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Registrar Retiro")
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
fun ApproveDifferenceDialog(
    session: CashSessionEntity,
    onDismiss: () -> Unit,
    onConfirm: (notes: String) -> Unit
) {
    var approvalNoteInput by remember { mutableStateOf("Aprobado por redondeo y faltante de cambio metálico") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = { Text("Aprobar Descuadre de Caja (Turno #${session.id})", fontWeight = FontWeight.Bold, color = DrakosBlack) },
        text = {
            Column {
                Text("Diferencia registrada: ${formatCurrency(session.cashDifference)}", fontWeight = FontWeight.Bold, color = DangerRed)
                Text("Fondo inicial: ${formatCurrency(session.openingAmount)}", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = approvalNoteInput,
                    onValueChange = { approvalNoteInput = it },
                    label = { Text("Justificación de Aprobación") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(approvalNoteInput) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Confirmar Aprobación")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = DrakosBlack)
            }
        }
    )
}
