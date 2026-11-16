package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.BranchEntity
import com.example.data.entity.CashSessionEntity
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.UserProfileEntity
import com.example.domain.model.UserRole
import com.example.ui.theme.BrandNavy800
import com.example.ui.theme.BrandNavy900
import com.example.ui.theme.BrandSlate100
import com.example.ui.theme.BrandSlate700
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosCharcoal
import com.example.ui.theme.DrakosGraphite
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedBright
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Formateador de moneda para Argentina ($ 1.250,00)
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
    return format.format(amount)
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "AR"))
    return sdf.format(Date(timestamp))
}

@Composable
fun TopHeader(
    currentUser: UserProfileEntity?,
    currentBranch: BranchEntity?,
    branches: List<BranchEntity>,
    activeSession: CashSessionEntity?,
    isCloudConnected: Boolean = false,
    onBranchSelected: (BranchEntity) -> Unit,
    onOpenUserSwitch: () -> Unit,
    onOpenCloudSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var branchMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = DrakosBlack,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Bulldog Blanco Drakos & Nombre de la Cadena
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_drakos_bulldog),
                            contentDescription = "Drakos Bulldog Blanco",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, DrakosRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DRAKOS",
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp,
                                color = DrakosWhite
                            )
                            Text(
                                text = "24HS • 10 SUCURSALES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }

                    // Botón de perfil / relevo de operador
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val roleAccent = when (currentUser?.role) {
                            UserRole.OWNER -> DrakosRedBright
                            UserRole.BRANCH_MANAGER -> DrakosWhite
                            UserRole.EMPLOYEE -> SuccessGreen
                            null -> Color.Gray
                        }
                        val roleBg = when (currentUser?.role) {
                            UserRole.OWNER -> DrakosRed.copy(alpha = 0.22f)
                            UserRole.BRANCH_MANAGER -> DrakosGraphite
                            UserRole.EMPLOYEE -> SuccessGreen.copy(alpha = 0.2f)
                            null -> DrakosGraphite
                        }
                        val roleLabel = when (currentUser?.role) {
                            UserRole.OWNER -> "DUEÑO"
                            UserRole.BRANCH_MANAGER -> "ENCARGADO"
                            UserRole.EMPLOYEE -> "CAJERO"
                            null -> "SIN SESIÓN"
                        }

                        Surface(
                            color = roleBg,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, roleAccent),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable { onOpenUserSwitch() }
                                .testTag("user_profile_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Usuario",
                                    tint = roleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = currentUser?.fullName?.take(16) ?: "Invitado",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DrakosWhite
                                    )
                                    Text(
                                        text = roleLabel,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = roleAccent
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sub-bar: Sucursal Activa + Nube + Estado de Turno de Caja
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selector de Sucursal
                    Box {
                        Surface(
                            color = DrakosCharcoal,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DrakosGraphite),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable(enabled = currentUser?.role == UserRole.OWNER) {
                                    branchMenuExpanded = true
                                }
                                .testTag("branch_selector_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Sucursal",
                                    tint = DrakosRedBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentBranch?.name ?: "Seleccionar Sucursal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DrakosWhite
                                )
                                if (currentUser?.role == UserRole.OWNER) {
                                    Text(
                                        text = " ▼",
                                        fontSize = 10.sp,
                                        color = DrakosRedBright
                                    )
                                }
                            }
                        }

                        if (currentUser?.role == UserRole.OWNER) {
                            DropdownMenu(
                                expanded = branchMenuExpanded,
                                onDismissRequest = { branchMenuExpanded = false }
                            ) {
                                Text(
                                    text = "Red de 10 Kioscos Drakos",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = DrakosBlack
                                )
                                HorizontalDivider()
                                branches.forEach { branch ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(branch.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text(branch.address, fontSize = 11.sp, color = DrakosMutedInk)
                                            }
                                        },
                                        onClick = {
                                            onBranchSelected(branch)
                                            branchMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Estado de Nube + Estado de Turno de Caja
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (isCloudConnected) DrakosRed else DrakosCharcoal,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCloudConnected) DrakosRedBright else DrakosGraphite
                            ),
                            modifier = Modifier
                                .clickable { onOpenCloudSync() }
                                .testTag("top_cloud_status_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = if (isCloudConnected) "Nube conectada" else "Conectar nube",
                                    tint = DrakosWhite,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCloudConnected) "Nube OK" else "Nube",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrakosWhite
                                )
                            }
                        }

                        val isOpen = activeSession != null
                        Surface(
                            color = if (isOpen) SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isOpen) SuccessGreen else DangerRed
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isOpen) SuccessGreen else DangerRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOpen) "Caja Abierta" else "Caja Cerrada",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOpen) SuccessGreen else DangerRed
                                )
                            }
                        }
                    }
                }
            }
            // Línea inferior Carmesí característica de Drakos
            HorizontalDivider(thickness = 2.dp, color = DrakosRed)
        }
    }
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
    val isLockedOut = System.currentTimeMillis() < lockoutUntilMillis

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambio de Operador / Sesión", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentUser?.let { active ->
                    Surface(
                        color = BrandSlate100,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Operador activo:", fontSize = 11.sp, color = BrandSlate700)
                            Text(active.fullName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandNavy900)
                            Text("${active.role.label} • Usuario: ${active.username}", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text(
                    text = "Ingrese el PIN personal del nuevo operador para relevar el turno en esta terminal:",
                    fontSize = 12.sp,
                    color = BrandSlate700
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
                        text = "Terminal bloqueada temporalmente por intentos fallidos.",
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
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
                Text("Cancelar")
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
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ticket Emitido", fontWeight = FontWeight.Bold)
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
                    .background(DrakosCanvas, RoundedCornerShape(10.dp))
                    .border(1.dp, DrakosBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = companySettings.businessName.uppercase(),
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
                    color = Color.Gray,
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
                    color = Color.Gray,
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
                    Text("IVA discriminado (${companySettings.defaultVatRate.toInt()}%):", fontSize = 11.sp, color = Color.Gray, fontFamily = JetBrainsMonoFontFamily)
                    Text(formatCurrency(sale.vatAmount), fontSize = 11.sp, color = Color.Gray, fontFamily = JetBrainsMonoFontFamily)
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
                    color = Color.Gray,
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
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
        appendLine("   ${companySettings.businessName.uppercase()}   ")
        appendLine("           CUIT: ${companySettings.cuit}            ")
        appendLine("      ${companySettings.taxCondition.uppercase()}           ")
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
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cierre Z de Turno (Fiscal 80mm)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Surface(
                color = BrandSlate100,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = companySettings.businessName.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = JetBrainsMonoFontFamily,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "CUIT ${companySettings.cuit} • Sucursal: $branchName",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "*** CIERRE Z - TURNO #${session.id} ***",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = BrandNavy900,
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
                        val diffColor = if (diff < 0) DangerRed else if (diff > 0) PrimaryBlue else SuccessGreen
                        Text(diffText, fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.ExtraBold, color = diffColor)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val baseImponible = session.expectedCashAmount / vatDivisor
                    val ivaCalculado = session.expectedCashAmount - baseImponible
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Gravada ${vatRate.toInt()}%:", fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = Color.Gray)
                        Text(formatCurrency(baseImponible), fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = Color.Gray)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IVA Débito Fiscal:", fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = Color.Gray)
                        Text(formatCurrency(ivaCalculado), fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = Color.Gray)
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Cerrar")
            }
        }
    )
}
