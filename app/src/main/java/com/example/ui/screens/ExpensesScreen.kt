package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ExpenseEntity
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.UserRole
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.components.parseArsDecimal
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun ExpensesScreen(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val branchExpenses by viewModel.branchExpenses.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val activeList: List<ExpenseEntity> = if (currentUser?.role == UserRole.OWNER) allExpenses else branchExpenses
    var showNewExpenseModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.DrakosCanvas)
            .padding(14.dp)
    ) {
        // Resumen de egresos + acción directa
        val totalExpenses = activeList.sumOf { it.amount }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
            border = BorderStroke(1.dp, DrakosHairline),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Total Egresos Registrados", fontSize = 12.sp, color = DrakosMutedGray)
                        Surface(
                            color = DangerRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${activeList.size} comp.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(totalExpenses),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = DrakosDarkGray
                    )
                }

                if (currentUser?.role != UserRole.EMPLOYEE) {
                    Button(
                        onClick = { showNewExpenseModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("new_expense_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Registrar Gasto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(48.dp), tint = DrakosMutedGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No hay gastos registrados.", color = DrakosMutedGray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeList, key = { it.id }) { exp: ExpenseEntity ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                        border = BorderStroke(1.dp, DrakosHairline),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exp.category.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosDarkGray)
                                Text(exp.description, fontSize = 12.sp, color = DrakosMutedGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Origen: ${if (exp.paymentSource == "CASH_DRAWER") "Caja Chica (Gaveta)" else "Transferencia Bancaria"} • ${formatDateTime(exp.createdAt)}",
                                    fontSize = 10.sp,
                                    color = DrakosMutedGray
                                )
                            }
                            Text(
                                text = "- ${formatCurrency(exp.amount)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DangerRed
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Nuevo Gasto
    if (showNewExpenseModal) {
        var selectedCat by remember { mutableStateOf(ExpenseCategory.SERVICIOS) }
        var amountInput by remember { mutableStateOf("") }
        var descInput by remember { mutableStateOf("") }
        var sourceInput by remember { mutableStateOf("CASH_DRAWER") }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showNewExpenseModal = false },
            containerColor = DrakosWhite,
            title = {
                Text(
                    "Registrar Gasto Operativo",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = DrakosDarkGray
                )
            },
            text = {
                Column {
                    Text("Categoría de Gasto:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExpenseCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCat == cat,
                                onClick = { selectedCat = cat },
                                label = { Text(cat.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it; validationError = null },
                        label = { Text("Importe ($ ARS)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it; validationError = null },
                        label = { Text("Detalle / Comprobante") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Origen del Dinero:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosDarkGray)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        FilterChip(
                            selected = sourceInput == "CASH_DRAWER",
                            onClick = { sourceInput = "CASH_DRAWER" },
                            label = { Text("Caja Chica", fontSize = 11.sp) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = sourceInput == "BANK",
                            onClick = { sourceInput = "BANK" },
                            label = { Text("Transferencia Bancaria", fontSize = 11.sp) }
                        )
                    }

                    if (!validationError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(validationError ?: "", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = parseArsDecimal(amountInput)
                        val detail = descInput.trim()
                        when {
                            amount == null || amount <= 0.0 ->
                                validationError = "Ingrese un importe válido mayor a $0."
                            detail.isEmpty() ->
                                validationError = "Ingrese un detalle o comprobante del egreso."
                            else -> {
                                viewModel.registerExpense(selectedCat, amount, detail, sourceInput)
                                showNewExpenseModal = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
                ) {
                    Text("Guardar Gasto")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewExpenseModal = false }) {
                    Text("Cancelar", color = DrakosDarkGray)
                }
            }
        )
    }
}
