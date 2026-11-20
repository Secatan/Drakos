package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CashSessionEntity
import com.example.domain.analytics.DrakosAnalytics
import com.example.domain.model.DifferenceStatus
import com.example.domain.model.UserRole
import com.example.ui.components.DrakosSegmentedTabItem
import com.example.ui.components.DrakosSegmentedTabs
import com.example.ui.components.ThermalClosingZModal
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.dialogs.ApproveDifferenceDialog
import com.example.ui.dialogs.BlindCloseShiftDialog
import com.example.ui.dialogs.CashDropDialog
import com.example.ui.dialogs.OpenShiftDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceTint
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun ShiftsScreen(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeCashSession.collectAsStateWithLifecycle()
    val branchSessions by viewModel.branchSessions.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val activeMovements by viewModel.activeSessionMovements.collectAsStateWithLifecycle()
    val estimatedDrawerCash by viewModel.estimatedDrawerCash.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()

    val isManagerOrOwner = (currentUser?.role?.level ?: 1) >= 2
    val isOwner = currentUser?.role == UserRole.OWNER
    val userNamesMap = remember(adminUsers) { adminUsers.associate { it.id to it.fullName } }

    val cashierVariances = remember(allSessions, branchSessions, adminUsers, currentBranch, isOwner) {
        if (isManagerOrOwner) {
            DrakosAnalytics.buildCashierVarianceSummary(
                sessions = if (isOwner) allSessions else branchSessions,
                users = adminUsers,
                filterBranchId = if (isOwner) null else currentBranch?.id
            )
        } else {
            emptyList()
        }
    }

    var shiftsTab by remember { mutableIntStateOf(0) } // 0: Turno & Historial, 1: Desvíos x Cajero (N2+)
    var showOpenModal by remember { mutableStateOf(false) }
    var showCloseModal by remember { mutableStateOf(false) }
    var showCashDropModal by remember { mutableStateOf(false) }
    var sessionToApprove by remember { mutableStateOf<CashSessionEntity?>(null) }
    var sessionForClosingZ by remember { mutableStateOf<CashSessionEntity?>(null) }

    val shiftTabs = remember(cashierVariances.size) {
        listOf(
            DrakosSegmentedTabItem(0, "Turno & Historial", null, "shifts_tab_history"),
            DrakosSegmentedTabItem(1, "Desvíos x Cajero (${cashierVariances.size})", Icons.Default.PersonSearch, "shifts_tab_cashier_variance")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
            .padding(14.dp)
    ) {
        if (isManagerOrOwner) {
            DrakosSegmentedTabs(
                items = shiftTabs,
                selectedId = shiftsTab,
                onSelect = { shiftsTab = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (shiftsTab == 1 && isManagerOrOwner) {
            if (cashierVariances.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                    border = BorderStroke(1.dp, DrakosBorder),
                    elevation = CardDefaults.cardElevation(0.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = DrakosMutedInk, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Sin turnos cerrados para auditar aún.", fontSize = 12.sp, color = DrakosMutedInk)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cashierVariances, key = { it.userId }) { stat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                            border = BorderStroke(1.dp, DrakosBorder),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stat.cashierName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                                    Text(
                                        text = "Neto: ${formatCurrency(stat.netDifference)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (stat.netDifference < 0) DangerRed else SuccessGreen
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${stat.closedSessionsCount} cierre(s)", fontSize = 11.sp, color = DrakosMutedInk)
                                    Text("Faltantes: ${formatCurrency(stat.totalShortage)}", fontSize = 11.sp, color = DangerRed)
                                    Text("Sobrantes: ${formatCurrency(stat.totalSurplus)}", fontSize = 11.sp, color = SuccessGreen)
                                }
                                if (stat.pendingApprovals > 0) {
                                    Surface(color = WarningAmber.copy(alpha = 0.16f), shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "${stat.pendingApprovals} arqueo(s) pendiente(s) de aprobación",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarningAmber,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val currentSession: CashSessionEntity? = activeSession

            // Alerta de Gaveta Llena / Retiro Parcial (preserva ciego en Nivel 1)
            if (currentSession != null && estimatedDrawerCash >= companySettings.cashDropAlertThreshold) {
                Surface(
                    color = DrakosRedContainer,
                    border = BorderStroke(1.dp, DrakosRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("cash_drop_threshold_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DrakosRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isManagerOrOwner) {
                                    "Gaveta supera tope (${formatCurrency(estimatedDrawerCash)} >= ${formatCurrency(companySettings.cashDropAlertThreshold)}). Realice retiro parcial."
                                } else {
                                    "Tope de seguridad en gaveta alcanzado. Realice un retiro parcial a caja fuerte."
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrakosBlack
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { showCashDropModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Retirar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (currentSession != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                    border = BorderStroke(1.dp, DrakosBorder),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Turno de Caja Abierto", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SuccessGreen)
                            }
                            Text(
                                text = "Iniciado: ${formatDateTime(currentSession.openedAt)}",
                                fontSize = 11.sp,
                                color = DrakosMutedInk
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Fondo Inicial (Gaveta):", fontSize = 11.sp, color = DrakosMutedInk)
                                Text(
                                    text = formatCurrency(currentSession.openingAmount),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DrakosBlack
                                )
                            }
                            if (isManagerOrOwner) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Efectivo est. Gaveta:", fontSize = 11.sp, color = DrakosMutedInk)
                                    Text(
                                        text = formatCurrency(estimatedDrawerCash),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = DrakosBlack
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Cajero:", fontSize = 11.sp, color = DrakosMutedInk)
                                Text(
                                    text = userNamesMap[currentSession.userId] ?: currentUser?.fullName ?: "Operador",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = DrakosBlack
                                )
                            }
                        }

                        if (activeMovements.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = DrakosSurfaceTint,
                                border = BorderStroke(1.dp, DrakosBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Movimientos de gaveta (${activeMovements.size}):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                                    activeMovements.take(3).forEach { mov ->
                                        Text(
                                            text = "• ${mov.movementType}: ${formatCurrency(mov.amount)} (${mov.reason})",
                                            fontSize = 10.sp,
                                            color = DrakosMutedInk
                                        )
                                    }
                                }
                            }
                        }

                        if (currentSession.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Nota: ${currentSession.notes}", fontSize = 11.sp, color = DrakosMutedInk)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isManagerOrOwner) {
                                OutlinedButton(
                                    onClick = { sessionForClosingZ = currentSession },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(0.9f).heightIn(min = 48.dp).testTag("preview_z_ticket_button")
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ticket Z", fontSize = 11.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = { showCashDropModal = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.1f).heightIn(min = 48.dp).testTag("cash_drop_button")
                            ) {
                                Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retiro", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { showCloseModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.4f).heightIn(min = 48.dp).testTag("close_shift_button")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cierre / Arqueo", fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                    border = BorderStroke(1.dp, DrakosBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = DangerRed, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Caja Actualmente Cerrada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DrakosBlack)
                        Text("Para registrar ventas en el POS, debe abrir turno declarando el fondo inicial.", fontSize = 12.sp, color = DrakosMutedInk)
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showOpenModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("open_shift_button")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir Turno de Caja")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Historial de Turnos y Arqueos en Sucursal:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DrakosBlack
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(branchSessions, key = { it.id }) { session: CashSessionEntity ->
                    val isPending = session.differenceStatus == DifferenceStatus.PENDING_APPROVAL
                    val isApproved = session.differenceStatus == DifferenceStatus.APPROVED

                    val statusBadgeColor = when {
                        isPending -> WarningAmber
                        isApproved -> InfoBlue
                        else -> SuccessGreen
                    }
                    val statusBadgeLabel = when {
                        isPending -> "Descuadre (> ${formatCurrency(companySettings.cashAuditTolerance)})"
                        isApproved -> "Aprobado por Supervisor"
                        else -> "Arqueo Conforme"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                        border = BorderStroke(1.dp, DrakosBorder),
                        elevation = CardDefaults.cardElevation(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Turno #${session.id} • ${userNamesMap[session.userId] ?: "Operador"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DrakosBlack
                                )
                                Surface(
                                    color = statusBadgeColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = statusBadgeLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusBadgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fondo inicial: ${formatCurrency(session.openingAmount)}", fontSize = 11.sp, color = DrakosMutedInk)
                                if (session.closedAt != null && isManagerOrOwner) {
                                    Text(
                                        text = "Diferencia: ${formatCurrency(session.cashDifference)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (session.cashDifference < 0) DangerRed else SuccessGreen
                                    )
                                }
                            }

                            if (session.notes.isNotEmpty()) {
                                Text("Nota: ${session.notes}", fontSize = 10.sp, color = DrakosMutedInk, modifier = Modifier.padding(top = 2.dp))
                            }

                            if (isManagerOrOwner) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { sessionForClosingZ = session },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ver Cierre Z", fontSize = 11.sp)
                                    }

                                    if (isPending) {
                                        Button(
                                            onClick = { sessionToApprove = session },
                                            colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Aprobar Descuadre", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOpenModal) {
        OpenShiftDialog(
            onDismiss = { showOpenModal = false },
            onConfirm = { amount, notes ->
                viewModel.openShift(amount, notes)
                showOpenModal = false
            }
        )
    }

    if (showCloseModal) {
        BlindCloseShiftDialog(
            onDismiss = { showCloseModal = false },
            onConfirm = { counted, notes ->
                viewModel.closeShift(counted, notes)
                showCloseModal = false
            }
        )
    }

    if (showCashDropModal) {
        CashDropDialog(
            onDismiss = { showCashDropModal = false },
            onConfirm = { amount, reason ->
                viewModel.registerCashDrop(amount, reason)
                showCashDropModal = false
            }
        )
    }

    sessionToApprove?.let { sess ->
        ApproveDifferenceDialog(
            session = sess,
            onDismiss = { sessionToApprove = null },
            onConfirm = { notes ->
                viewModel.approveCashDifference(sess.id, notes)
                sessionToApprove = null
            }
        )
    }

    sessionForClosingZ?.let { sess ->
        ThermalClosingZModal(
            session = sess,
            branchName = currentBranch?.name ?: "Drakos",
            companySettings = companySettings,
            onDismiss = { sessionForClosingZ = null }
        )
    }
}
