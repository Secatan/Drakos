package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.SaleEntity
import com.example.ui.components.formatCurrency
import com.example.ui.components.parseArsDecimal
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceSoft
import com.example.ui.theme.DrakosWhite

@Composable
fun VoidSaleDialog(
    sale: SaleEntity,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var voidReasonInput by remember { mutableStateOf("Error de tipeo de cajero / Devolución cliente") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = { Text("Anular Venta #${sale.receiptNumber}", fontWeight = FontWeight.Bold, color = DrakosDarkGray) },
        text = {
            Column {
                Text(
                    text = "La anulación reincorporará automáticamente todos los productos al stock local y registrará el ajuste en caja.",
                    fontSize = 12.sp,
                    color = DrakosMutedGray
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Importe a reintegrar: ${formatCurrency(sale.totalAmount)}", fontWeight = FontWeight.Bold, color = DangerRed)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = voidReasonInput,
                    onValueChange = {
                        voidReasonInput = it
                        validationError = null
                    },
                    label = { Text("Motivo de la anulación") },
                    modifier = Modifier.fillMaxWidth()
                )
                validationError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(err, color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = voidReasonInput.trim()
                    if (trimmed.isBlank()) {
                        validationError = "Debe ingresar un motivo de anulación."
                    } else {
                        onConfirm(trimmed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("Confirmar Anulación", color = DrakosWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = DrakosDarkGray)
            }
        }
    )
}

@Composable
fun EditCompanySettingsDialog(
    companySettings: CompanySettingsEntity,
    onDismiss: () -> Unit,
    onConfirm: (CompanySettingsEntity) -> Unit
) {
    var businessNameInput by remember { mutableStateOf(companySettings.businessName) }
    var cuitInput by remember { mutableStateOf(companySettings.cuit) }
    var taxConditionInput by remember { mutableStateOf(companySettings.taxCondition) }
    var vatInput by remember { mutableStateOf(companySettings.defaultVatRate.toString()) }
    var dropThresholdInput by remember { mutableStateOf(companySettings.cashDropAlertThreshold.toInt().toString()) }
    var toleranceInput by remember { mutableStateOf(companySettings.cashAuditTolerance.toInt().toString()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = { Text("Editar Parámetros de la Cadena", fontWeight = FontWeight.Bold, color = DrakosDarkGray) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = businessNameInput,
                    onValueChange = {
                        businessNameInput = it
                        validationError = null
                    },
                    label = { Text("Razón Social") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cuitInput,
                    onValueChange = {
                        cuitInput = it
                        validationError = null
                    },
                    label = { Text("CUIT Central") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taxConditionInput,
                    onValueChange = {
                        taxConditionInput = it
                        validationError = null
                    },
                    label = { Text("Condición frente al IVA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vatInput,
                        onValueChange = {
                            vatInput = it
                            validationError = null
                        },
                        label = { Text("IVA (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = toleranceInput,
                        onValueChange = {
                            toleranceInput = it
                            validationError = null
                        },
                        label = { Text("Tolerancia ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = dropThresholdInput,
                    onValueChange = {
                        dropThresholdInput = it
                        validationError = null
                    },
                    label = { Text("Tope Alerta Gaveta ($ ARS)") },
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
                    val vat = parseArsDecimal(vatInput)
                    val tol = parseArsDecimal(toleranceInput)
                    val drop = parseArsDecimal(dropThresholdInput)
                    when {
                        businessNameInput.isBlank() || cuitInput.isBlank() || taxConditionInput.isBlank() -> {
                            validationError = "Razón social, CUIT y condición frente al IVA son obligatorios."
                        }
                        vat == null || vat < 0.0 || vat > 50.0 -> {
                            validationError = "Alícuota IVA inválida (debe estar entre 0 y 50%)."
                        }
                        tol == null || tol < 0.0 -> {
                            validationError = "La tolerancia de caja debe ser mayor o igual a 0."
                        }
                        drop == null || drop < 0.0 -> {
                            validationError = "El tope de alerta debe ser mayor o igual a 0."
                        }
                        else -> {
                            onConfirm(
                                companySettings.copy(
                                    businessName = businessNameInput.trim(),
                                    cuit = cuitInput.trim(),
                                    taxCondition = taxConditionInput.trim(),
                                    defaultVatRate = vat,
                                    cashAuditTolerance = tol,
                                    cashDropAlertThreshold = drop
                                )
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Guardar Cambios", color = DrakosWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = DrakosDarkGray)
            }
        }
    )
}

@Composable
fun CsvExportDialog(
    generateCsv: (String) -> String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var exportType by remember { mutableStateOf("SALES") }
    val csvContent = generateCsv(exportType)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = { Text("Exportación de Reporte CSV", fontWeight = FontWeight.Bold, color = DrakosDarkGray) },
        text = {
            Column {
                Text("Seleccione tipo de reporte:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                Row(modifier = Modifier.padding(vertical = 6.dp)) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DrakosRed,
                        selectedLabelColor = DrakosWhite
                    )
                    FilterChip(
                        selected = exportType == "SALES",
                        onClick = { exportType = "SALES" },
                        colors = chipColors,
                        label = { Text("Ventas", fontSize = 11.sp) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = exportType == "STOCK",
                        onClick = { exportType = "STOCK" },
                        colors = chipColors,
                        label = { Text("Inventario", fontSize = 11.sp) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = exportType == "EXPENSES",
                        onClick = { exportType = "EXPENSES" },
                        colors = chipColors,
                        label = { Text("Gastos", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = DrakosSurfaceSoft,
                    border = BorderStroke(1.dp, DrakosHairline),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    Text(
                        text = csvContent,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = DrakosDarkGray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Drakos_Export", csvContent)
                    clipboard.setPrimaryClip(clip)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Copiar CSV al Portapapeles", color = DrakosWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cerrar", color = DrakosDarkGray)
            }
        }
    )
}
