package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BranchEntity
import com.example.data.entity.CashSessionEntity
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.UserProfileEntity
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosLightCanvas
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosSurfaceSoft
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val localeEsAr: Locale = Locale.forLanguageTag("es-AR")

/**
 * Parsea importes o porcentajes ingresados en teclado español/latinoamericano
 * aceptando tanto coma decimal ("1250,50") como punto ("1250.50").
 */
fun parseArsDecimal(raw: String): Double? {
    val normalized = raw.trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}

// Formateador de moneda para Argentina ($ 1.250,00)
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(localeEsAr)
    return format.format(amount)
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", localeEsAr)
    return sdf.format(Date(timestamp))
}

@Composable
fun PinLoginModal(
    currentUser: UserProfileEntity?,
    failedAttempts: Int,
    lockoutUntilMillis: Long,
    onLoginWithPin: (String, (Boolean) -> Unit) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lockoutUntilMillis) {
        nowMillis = System.currentTimeMillis()
        while (nowMillis < lockoutUntilMillis) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    val isLockedOut = nowMillis < lockoutUntilMillis
    val remainingSeconds = ((lockoutUntilMillis - nowMillis) / 1000L).coerceAtLeast(0L)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = DrakosRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambio de Operador / Sesión", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DrakosDarkGray)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentUser?.let { active ->
                    Surface(
                        color = DrakosSurfaceSoft,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Operador activo:", fontSize = 11.sp, color = DrakosMutedGray)
                            Text(active.fullName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                            Text("${active.role.label} • Usuario: ${active.username}", fontSize = 12.sp, color = DrakosRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text(
                    text = "Ingrese el PIN personal del nuevo operador para relevar el turno en esta terminal:",
                    fontSize = 12.sp,
                    color = DrakosMutedGray
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        pinInput = digits
                        localError = null
                    },
                    label = { Text("PIN de Operador (4 dígitos)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (pinInput.length == 4 && !isLockedOut) {
                                onLoginWithPin(pinInput) { ok ->
                                    if (ok) {
                                        onDismiss()
                                    } else {
                                        pinInput = ""
                                        localError = "PIN inválido."
                                    }
                                }
                            }
                        }
                    ),
                    singleLine = true,
                    enabled = !isLockedOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("switch_pin_input")
                )

                if (isLockedOut) {
                    Text(
                        text = "Terminal bloqueada temporalmente (${remainingSeconds}s restantes) por intentos fallidos.",
                        fontSize = 11.sp,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                } else if (localError != null || failedAttempts > 0) {
                    Text(
                        text = localError ?: "Intentos fallidos recientes: $failedAttempts/5",
                        fontSize = 11.sp,
                        color = DangerRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        onLogout()
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("logout_terminal_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bloquear Terminal / Cerrar Sesión", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onLoginWithPin(pinInput) { ok ->
                        if (ok) {
                            onDismiss()
                        } else {
                            pinInput = ""
                            localError = "PIN incorrecto."
                        }
                    }
                },
                enabled = pinInput.length == 4 && !isLockedOut,
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Validar PIN")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar", color = DrakosBlack)
            }
        }
    )
}

@Composable
fun DigitalReceiptModal(
    sale: SaleEntity,
    branch: BranchEntity?,
    userName: String,
    companySettings: CompanySettingsEntity = CompanySettingsEntity(),
    onDismiss: () -> Unit
) {
    val receiptContext = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ticket Emitido", fontWeight = FontWeight.Bold, color = DrakosBlack)
                }
                Text(
                    text = "Venta Exitosa",
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DrakosLightCanvas, RoundedCornerShape(10.dp))
                    .border(1.dp, DrakosHairline, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = companySettings.businessName.uppercase(localeEsAr),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = JetBrainsMonoFontFamily
                )
                Text(
                    text = "CUIT: ${companySettings.cuit} • ${companySettings.taxCondition}",
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = DrakosMutedGray,
                    fontFamily = JetBrainsMonoFontFamily
                )
                Text(
                    text = branch?.name ?: "Sucursal Kiosco",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = JetBrainsMonoFontFamily
                )
                Text(
                    text = branch?.address ?: "Buenos Aires, Argentina",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = DrakosMutedGray,
                    fontFamily = JetBrainsMonoFontFamily
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ticket Nº:", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                    Text(sale.receiptNumber, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fecha:", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                    Text(formatDateTime(sale.createdAt), fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cajero:", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                    Text(userName, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Medio de Pago:", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                    Text(sale.paymentMethod.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal:", fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily)
                    Text(formatCurrency(sale.subtotal), fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily)
                }
                if (sale.discountAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Descuento:", fontSize = 12.sp, color = DangerRed, fontFamily = JetBrainsMonoFontFamily)
                        Text("- ${formatCurrency(sale.discountAmount)}", fontSize = 12.sp, color = DangerRed, fontFamily = JetBrainsMonoFontFamily)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("IVA discriminado (${companySettings.defaultVatRate.toInt()}%):", fontSize = 11.sp, color = DrakosMutedGray, fontFamily = JetBrainsMonoFontFamily)
                    Text(formatCurrency(sale.vatAmount), fontSize = 11.sp, color = DrakosMutedGray, fontFamily = JetBrainsMonoFontFamily)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL ABONADO:", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, fontFamily = JetBrainsMonoFontFamily)
                    Text(formatCurrency(sale.totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SuccessGreen, fontFamily = JetBrainsMonoFontFamily)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "*** COMPROBANTE DE CONTROL INTERNO ***",
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = DrakosMutedGray,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    com.example.util.ThermalPrinterHelper.printSaleTicket(
                        context = receiptContext,
                        sale = sale,
                        branch = branch,
                        cashierName = userName,
                        settings = companySettings
                    )
                },
                modifier = Modifier.testTag("print_receipt_button")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Imprimir 80mm")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Listo / Siguiente Venta")
            }
        }
    )
}

@Composable
fun ThermalClosingZModal(
    session: CashSessionEntity,
    branchName: String,
    companySettings: CompanySettingsEntity = CompanySettingsEntity(),
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var copiedToClipboard by remember { mutableStateOf(false) }

    val isClosed = session.status == com.example.domain.model.ShiftStatus.CLOSED
    val diff = session.cashDifference
    val diffText = when {
        diff == 0.0 -> "CONFORME ($ 0,00)"
        diff > 0 -> "SOBRANTE (+ ${formatCurrency(diff)})"
        else -> "FALTANTE (${formatCurrency(diff)})"
    }
    val vatRate = companySettings.defaultVatRate
    val vatDivisor = 1.0 + (vatRate / 100.0)

    val ticketText = buildString {
        appendLine("==========================================")
        appendLine("   ${companySettings.businessName.uppercase(localeEsAr)}   ")
        appendLine("           CUIT: ${companySettings.cuit}            ")
        appendLine("      ${companySettings.taxCondition.uppercase(localeEsAr)}           ")
        appendLine("==========================================")
        appendLine("SUCURSAL: $branchName")
        appendLine("TURNO ID: #${session.id} | ESTADO: ${if (isClosed) "CERRADO" else "EN CURSO"}")
        appendLine("APERTURA: ${formatDateTime(session.openedAt)}")
        if (session.closedAt != null) {
            appendLine("CIERRE:   ${formatDateTime(session.closedAt)}")
        }
        appendLine("------------------------------------------")
        appendLine("FONDO FIJO INICIAL:   ${formatCurrency(session.openingAmount)}")
        appendLine("EFECTIVO ESPERADO:    ${formatCurrency(session.expectedCashAmount)}")
        appendLine("EFECTIVO DECLARADO:   ${formatCurrency(session.countedCashAmount)}")
        appendLine("DIFERENCIA DE CAJA:   $diffText")
        appendLine("ESTADO AUDITORIA:     ${session.differenceStatus.label}")
        if (session.notes.isNotBlank()) {
            appendLine("OBSERVACIONES:        ${session.notes}")
        }
        appendLine("------------------------------------------")
        val baseImponible = session.expectedCashAmount / vatDivisor
        val ivaCalculado = session.expectedCashAmount - baseImponible
        appendLine("BASE IMPONIBLE (NETO): ${formatCurrency(baseImponible)}")
        appendLine("DEBITO FISCAL IVA ${vatRate.toInt()}%: ${formatCurrency(ivaCalculado)}")
        appendLine("TOTAL ARQUEO EFECTIVO: ${formatCurrency(session.countedCashAmount)}")
        appendLine("==========================================")
        appendLine("       COMPROBANTE HOMOLOGADO POS         ")
        appendLine("==========================================")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = DrakosRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cierre Z de Turno (Fiscal 80mm)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DrakosBlack)
            }
        },
        text = {
            Surface(
                color = DrakosSurfaceSoft,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DrakosHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = companySettings.businessName.uppercase(localeEsAr),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = DrakosDarkGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "CUIT ${companySettings.cuit} • Sucursal: $branchName",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = DrakosMutedGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "*** CIERRE Z - TURNO #${session.id} ***",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = DrakosDarkGray,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Apertura: ${formatDateTime(session.openedAt)}", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                    if (session.closedAt != null) {
                        Text("Cierre:   ${formatDateTime(session.closedAt)}", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fondo Inicial:", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                        Text(formatCurrency(session.openingAmount), fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Efectivo Esperado:", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                        Text(formatCurrency(session.expectedCashAmount), fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Efectivo Declarado:", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                        Text(formatCurrency(session.countedCashAmount), fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diferencia Arqueo:", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                        val diffColor = if (diff < 0) DangerRed else if (diff > 0) DrakosRed else SuccessGreen
                        Text(diffText, fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.ExtraBold, color = diffColor)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val baseImponible = session.expectedCashAmount / vatDivisor
                    val ivaCalculado = session.expectedCashAmount - baseImponible
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Gravada ${vatRate.toInt()}%:", fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedGray)
                        Text(formatCurrency(baseImponible), fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedGray)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IVA Débito Fiscal:", fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedGray)
                        Text(formatCurrency(ivaCalculado), fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedGray)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Ticket Cierre Z", ticketText)
                                clipboard.setPrimaryClip(clip)
                                copiedToClipboard = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (copiedToClipboard) "¡Copiado!" else "Copiar", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                com.example.util.ThermalPrinterHelper.printClosingZReport(
                                    context = context,
                                    session = session,
                                    branchName = branchName,
                                    settings = companySettings
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("print_closing_z_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Imprimir 80mm", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Cerrar")
            }
        }
    )
}
