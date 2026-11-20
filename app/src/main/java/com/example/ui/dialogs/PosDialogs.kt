package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.foundation.layout.PaddingValues
import com.example.domain.model.PaymentMethod
import com.example.ui.components.formatCurrency
import com.example.ui.components.parseArsDecimal
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

@Composable
fun PaymentDialog(
    totalAmount: Double,
    promoDiscount: Double = 0.0,
    promoLabels: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmPayment: (PaymentMethod, Double, String?) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var cashPaidInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var accountHolderInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val parsedDiscount = parseArsDecimal(discountInput)
    val extraDiscount = (parsedDiscount ?: 0.0).coerceAtLeast(0.0)
    val totalDiscount = (promoDiscount + extraDiscount).coerceAtMost(totalAmount)
    val finalToPay = (totalAmount - totalDiscount).coerceAtLeast(0.0)

    val parsedCashPaid = parseArsDecimal(cashPaidInput)
    val cashPaid = parsedCashPaid ?: 0.0
    val changeAmount = (cashPaid - finalToPay).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cobro y Medios de Pago",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = DrakosDarkGray
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = DrakosBlack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal Ticket:", fontSize = 11.sp, color = DrakosWhite.copy(alpha = 0.7f))
                            Text(formatCurrency(totalAmount), fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = DrakosWhite)
                        }
                        if (totalDiscount > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (promoLabels.isNotEmpty()) "Promos + Desc. (${promoLabels.distinct().size}):" else "Descuento:",
                                    fontSize = 11.sp,
                                    color = SuccessGreen
                                )
                                Text(
                                    text = "-${formatCurrency(totalDiscount)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL A COBRAR", fontFamily = SpaceGroteskFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosWhite)
                            Text(
                                text = formatCurrency(finalToPay),
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DrakosWhite
                            )
                        }
                    }
                }

                if (promoLabels.isNotEmpty()) {
                    Surface(
                        color = SuccessGreenContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = SuccessGreenDark, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Promos: ${promoLabels.joinToString(", ")}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreenDark
                            )
                        }
                    }
                }

                Text("Medio de Pago:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)

                val methods = listOf(
                    PaymentMethod.CASH,
                    PaymentMethod.MP_QR,
                    PaymentMethod.DEBIT,
                    PaymentMethod.CREDIT,
                    PaymentMethod.ACCOUNT,
                    PaymentMethod.TRANSFER
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(methods) { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = {
                                selectedMethod = method
                                validationError = null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DrakosRed,
                                selectedLabelColor = DrakosWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            label = { Text(method.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                if (selectedMethod == PaymentMethod.CASH) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cashPaidInput,
                            onValueChange = {
                                cashPaidInput = it
                                validationError = null
                            },
                            label = { Text("Paga con ($ ARS)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cash_paid_input")
                        )
                        OutlinedButton(
                            onClick = {
                                cashPaidInput = if (finalToPay % 1.0 == 0.0) {
                                    finalToPay.toInt().toString()
                                } else {
                                    String.format(java.util.Locale.US, "%.2f", finalToPay)
                                }
                                validationError = null
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("exact_cash_button")
                        ) {
                            Text("Pago exacto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("VUELTO A ENTREGAR:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DrakosDarkGray)
                        Text(
                            text = if (cashPaidInput.isNotBlank() && cashPaid < finalToPay) {
                                "Faltan ${formatCurrency(finalToPay - cashPaid)}"
                            } else {
                                formatCurrency(changeAmount)
                            },
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (cashPaid >= finalToPay) SuccessGreenDark else DangerRed
                        )
                    }
                } else if (selectedMethod == PaymentMethod.MP_QR) {
                    Surface(
                        color = Color(0xFF009EE3).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF009EE3).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "QR Mercado Pago / MODO listo para cobro inmediato.",
                            fontSize = 11.sp,
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else if (selectedMethod == PaymentMethod.ACCOUNT) {
                    Surface(
                        color = WarningAmberContainer,
                        border = BorderStroke(1.dp, WarningAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Cuenta Corriente / Fiado de Confianza", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DrakosDarkGray)
                            Text("Esta venta se imputará a la cuenta corriente del cliente.", fontSize = 10.sp, color = DrakosMutedGray)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = accountHolderInput,
                                onValueChange = {
                                    accountHolderInput = it
                                    validationError = null
                                },
                                label = { Text("Titular de cuenta *", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fiado_account_holder_input")
                            )
                        }
                    }
                } else if (selectedMethod == PaymentMethod.TRANSFER) {
                    Surface(
                        color = DrakosLightCanvas,
                        border = BorderStroke(1.dp, DrakosHairline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Transferencia Bancaria Inmediata", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DrakosDarkGray)
                            Text("Alias CBU: DRAKOS.STORE.MP • Comprobante requerido al cliente.", fontSize = 10.sp, color = DrakosMutedGray)
                        }
                    }
                }

                OutlinedTextField(
                    value = discountInput,
                    onValueChange = {
                        discountInput = it
                        validationError = null
                    },
                    label = { Text("Descuento adicional ($ ARS)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                    val invalidDiscount = discountInput.isNotBlank() && (parsedDiscount == null || parsedDiscount < 0.0)
                    when {
                        invalidDiscount -> {
                            validationError = "Ingrese un descuento numérico válido mayor o igual a 0."
                        }
                        selectedMethod == PaymentMethod.CASH && parsedCashPaid == null -> {
                            validationError = "Ingrese el importe recibido en efectivo o toque 'Pago exacto'."
                        }
                        selectedMethod == PaymentMethod.CASH && cashPaid < finalToPay -> {
                            validationError = "El efectivo abonado es insuficiente para cubrir ${formatCurrency(finalToPay)}."
                        }
                        selectedMethod == PaymentMethod.ACCOUNT && accountHolderInput.isBlank() -> {
                            validationError = "Debe indicar el nombre del titular para fiado / cuenta corriente."
                        }
                        else -> {
                            onConfirmPayment(
                                selectedMethod,
                                totalDiscount,
                                accountHolderInput.trim().ifBlank { null }
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_payment_button")
            ) {
                Text("Confirmar Cobro", color = DrakosWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar", color = DrakosDarkGray)
            }
        }
    )
}

