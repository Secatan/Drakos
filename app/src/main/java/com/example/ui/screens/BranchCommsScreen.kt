package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BranchEntity
import com.example.data.entity.BranchMessageEntity
import com.example.domain.model.MessagePriority
import com.example.ui.components.formatDateTime
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun BranchCommsScreen(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.branchMessages.collectAsStateWithLifecycle()
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()

    var showComposeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Comunicación entre Locales",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DrakosBlack
                )
                Text(
                    text = "Coordinación operativa y pedidos de stock entre las 10 sucursales Drakos.",
                    fontSize = 12.sp,
                    color = DrakosMutedInk
                )
            }

            Button(
                onClick = { showComposeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("new_branch_message_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enviar Aviso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Forum, contentDescription = null, tint = DrakosMutedInk, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sin mensajes entre locales todavía", fontWeight = FontWeight.Bold, color = DrakosBlack)
                Text("Envíe consultas de stock o avisos de traslado a otros kioscos.", fontSize = 12.sp, color = DrakosMutedInk)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg: BranchMessageEntity ->
                    val fromName = branches.find { it.id == msg.fromBranchId }?.code ?: "K0${msg.fromBranchId}"
                    val toName = if (msg.toBranchId == 0L) "TODAS LAS SUCURSALES" else (branches.find { it.id == msg.toBranchId }?.code ?: "K0${msg.toBranchId}")
                    val priorityColor = when (msg.priority) {
                        "URGENT" -> DangerRed
                        "STOCK_REQUEST" -> WarningAmber
                        else -> DrakosBlack
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                        border = BorderStroke(
                            width = if (!msg.isRead) 1.5.dp else 1.dp,
                            color = if (!msg.isRead) DrakosRed.copy(alpha = 0.6f) else DrakosBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = priorityColor.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = MessagePriority.entries.find { it.name == msg.priority }?.label ?: msg.priority,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = priorityColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("$fromName → $toName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                                }
                                Text(formatDateTime(msg.createdAt), fontSize = 10.sp, color = DrakosMutedInk)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(msg.subject, fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DrakosBlack)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(msg.message, fontSize = 12.sp, color = Color(0xFF27272A))

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Por: ${msg.senderName}", fontSize = 10.sp, color = DrakosMutedInk)
                                if (!msg.isRead && msg.fromBranchId != (currentBranch?.id ?: 0L)) {
                                    OutlinedButton(
                                        onClick = { viewModel.markMessageAsRead(msg.id) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Marcar Leído", fontSize = 10.sp, color = SuccessGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showComposeDialog) {
        ComposeBranchMessageDialog(
            branches = branches,
            currentBranchId = currentBranch?.id ?: 1L,
            onDismiss = { showComposeDialog = false },
            onSend = { toBranchId, priority, subject, body ->
                viewModel.sendBranchMessage(toBranchId, priority, subject, body)
                showComposeDialog = false
            }
        )
    }
}

@Composable
private fun ComposeBranchMessageDialog(
    branches: List<BranchEntity>,
    currentBranchId: Long,
    onDismiss: () -> Unit,
    onSend: (toBranchId: Long, priority: String, subject: String, body: String) -> Unit
) {
    var targetBranchId by remember { mutableLongStateOf(0L) }
    var priority by remember { mutableStateOf("STOCK_REQUEST") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = DrakosRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mensaje a Otros Locales", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Destino:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = targetBranchId == 0L,
                        onClick = { targetBranchId = 0L },
                        label = { Text("Toda la Red", fontSize = 11.sp) }
                    )
                    branches.filter { it.id != currentBranchId }.forEach { b ->
                        FilterChip(
                            selected = targetBranchId == b.id,
                            onClick = { targetBranchId = b.id },
                            label = { Text(b.code, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Prioridad:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MessagePriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p.name,
                            onClick = { priority = p.name },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DrakosRedContainer,
                                selectedLabelColor = DrakosRed
                            ),
                            label = { Text(p.label, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Asunto (ej: Consulta stock Coca 500ml)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Mensaje para los encargados") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(targetBranchId, priority, subject, body) },
                enabled = subject.isNotBlank() && body.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Enviar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar", color = DrakosBlack) }
        }
    )
}
