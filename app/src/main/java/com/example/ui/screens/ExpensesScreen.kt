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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.BrandNavy900
import com.example.ui.theme.BrandSlate700
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
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
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gastos Operativos y Caja Chica",
                    fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy900
                )
                Text(
                    text = "Control de egresos, servicios y mantenimiento",
                    fontSize = 12.sp,
                    color = BrandSlate700
                )
            }

            if (currentUser?.role != UserRole.EMPLOYEE) {
                Button(
                    onClick = { showNewExpenseModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.testTag("new_expense_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Registrar Gasto", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Total Egresos
        val totalExpenses = activeList.sumOf { it.amount }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Egresos Registrados:", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = formatCurrency(totalExpenses),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = DangerRed
                    )
                }
                Surface(
                    color = DangerRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${activeList.size} comprobantes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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
                Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No hay gastos registrados.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeList) { exp: ExpenseEntity ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exp.category.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandNavy900)
                                Text(exp.description, fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Origen: ${if (exp.paymentSource == "CASH_DRAWER") "Caja Chica (Gaveta)" else "Transferencia Bancaria"} • ${formatDateTime(exp.createdAt)}",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                            Text(
                                text = "- ${formatCurrency(exp.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
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

        AlertDialog(
            onDismissRequest = { showNewExpenseModal = false },
            title = { Text("Registrar Gasto Operativo") },
            text = {
                Column {
                    Text("Categoría de Gasto:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExpenseCategory.values()) { cat ->
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
                        onValueChange = { amountInput = it },
                        label = { Text("Importe ($ ARS)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Detalle / Comprobante") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Origen del Dinero:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.registerExpense(selectedCat, amount, descInput, sourceInput)
                            showNewExpenseModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Guardar Gasto")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewExpenseModal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
