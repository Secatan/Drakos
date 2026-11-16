package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.example.ui.theme.BrandSlate100
import com.example.ui.theme.BrandSlate700
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue

@Composable
fun VoidSaleDialog(
    sale: SaleEntity,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var voidReasonInput by remember { mutableStateOf("Error de tipeo de cajero / Devolución cliente") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anular Venta #${sale.receiptNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "La anulación reincorporará automáticamente todos los productos al stock local y registrará el ajuste en caja.",
                    fontSize = 12.sp,
                    color = BrandSlate700
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Importe a reintegrar: ${formatCurrency(sale.totalAmount)}", fontWeight = FontWeight.Bold, color = DangerRed)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = voidReasonInput,
                    onValueChange = { voidReasonInput = it },
                    label = { Text("Motivo de la anulación") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(voidReasonInput) },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("Confirmar Anulación")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Parámetros de la Cadena", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = businessNameInput,
                    onValueChange = { businessNameInput = it },
                    label = { Text("Razón Social") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cuitInput,
                    onValueChange = { cuitInput = it },
                    label = { Text("CUIT Central") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taxConditionInput,
                    onValueChange = { taxConditionInput = it },
                    label = { Text("Condición frente al IVA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vatInput,
                        onValueChange = { vatInput = it },
                        label = { Text("IVA (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = toleranceInput,
                        onValueChange = { toleranceInput = it },
                        label = { Text("Tolerancia ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = dropThresholdInput,
                    onValueChange = { dropThresholdInput = it },
                    label = { Text("Tope Alerta Gaveta ($ ARS)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        companySettings.copy(
                            businessName = businessNameInput.trim().ifBlank { companySettings.businessName },
                            cuit = cuitInput.trim().ifBlank { companySettings.cuit },
                            taxCondition = taxConditionInput.trim().ifBlank { companySettings.taxCondition },
                            defaultVatRate = vatInput.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: companySettings.defaultVatRate,
                            cashAuditTolerance = toleranceInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: companySettings.cashAuditTolerance,
                            cashDropAlertThreshold = dropThresholdInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: companySettings.cashDropAlertThreshold
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
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
        title = { Text("Exportación de Reporte CSV", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Seleccione tipo de reporte:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.padding(vertical = 6.dp)) {
                    FilterChip(
                        selected = exportType == "SALES",
                        onClick = { exportType = "SALES" },
                        label = { Text("Ventas", fontSize = 11.sp) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = exportType == "STOCK",
                        onClick = { exportType = "STOCK" },
                        label = { Text("Inventario", fontSize = 11.sp) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = exportType == "EXPENSES",
                        onClick = { exportType = "EXPENSES" },
                        label = { Text("Gastos", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = BrandSlate100,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    Text(
                        text = csvContent,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Copiar CSV al Portapapeles")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
