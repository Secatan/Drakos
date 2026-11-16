package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import com.example.domain.model.PaymentMethod
import com.example.ui.components.formatCurrency
import com.example.ui.theme.BrandNavy900
import com.example.ui.theme.BrandSlate100
import com.example.ui.theme.BrandSlate700
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun PaymentDialog(
    totalAmount: Double,
    onDismiss: () -> Unit,
    onConfirmPayment: (PaymentMethod, Double) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var cashPaidInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }

    val discount = discountInput.toDoubleOrNull() ?: 0.0
    val finalToPay = (totalAmount - discount).coerceAtLeast(0.0)

    val cashPaid = cashPaidInput.toDoubleOrNull() ?: 0.0
    val changeAmount = (cashPaid - finalToPay).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = SuccessGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cobro y Medios de Pago", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Total a Cobrar: ${formatCurrency(finalToPay)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandNavy900
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Seleccione Medio de Pago:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

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
                            onClick = { selectedMethod = method },
                            label = { Text(method.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedMethod == PaymentMethod.CASH) {
                    OutlinedTextField(
                        value = cashPaidInput,
                        onValueChange = { cashPaidInput = it },
                        label = { Text("Paga con billetes de ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("cash_paid_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("VUELTO A ENTREGAR:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = formatCurrency(changeAmount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (cashPaid >= finalToPay) SuccessGreen else DangerRed
                        )
                    }
                } else if (selectedMethod == PaymentMethod.MP_QR) {
                    Surface(
                        color = Color(0xFF009EE3).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Escaneo QR Mercado Pago / MODO en pantalla del cliente listo.", fontSize = 11.sp, color = Color(0xFF009EE3), fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (selectedMethod == PaymentMethod.ACCOUNT) {
                    var accountHolderInput by remember { mutableStateOf("") }
                    Surface(
                        color = WarningAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Cuenta Corriente / Fiado de Confianza", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandNavy900)
                            Text("Esta venta se imputará a la cuenta corriente del cliente frecuente o vecino.", fontSize = 10.sp, color = BrandSlate700)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = accountHolderInput,
                                onValueChange = { accountHolderInput = it },
                                label = { Text("Titular de cuenta (ej. Vecino Juan Dpto 4B)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else if (selectedMethod == PaymentMethod.TRANSFER) {
                    Surface(
                        color = BrandSlate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Transferencia Bancaria Inmediata", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandNavy900)
                            Text("Alias CBU: DRAKOS.STORE.MP • Comprobante requerido al cliente.", fontSize = 10.sp, color = BrandSlate700)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    label = { Text("Descuento especial ($ ARS)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPayment(selectedMethod, discount) },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("Confirmar Cobro")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
