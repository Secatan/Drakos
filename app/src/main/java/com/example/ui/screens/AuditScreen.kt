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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.UserProfileEntity
import com.example.domain.model.UserRole
import com.example.ui.components.DrakosSegmentedTabItem
import com.example.ui.components.DrakosSegmentedTabs
import com.example.ui.components.formatDateTime
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel

@Composable
fun AuditScreen(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) } // 0: Auditoría Inmutable, 1: Plantel de Personal

    val auditTabs = remember(auditLogs.size, users.size) {
        listOf(
            DrakosSegmentedTabItem(0, "Registro de Auditoría (${auditLogs.size})", Icons.Default.Shield),
            DrakosSegmentedTabItem(1, "Plantel (${users.size} empleados)", Icons.Default.People)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.DrakosCanvas)
            .padding(14.dp)
    ) {
        DrakosSegmentedTabs(
            items = auditTabs,
            selectedId = currentTab,
            onSelect = { currentTab = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (currentTab == 0) {
            if (auditLogs.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = DrakosMutedGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay registros de auditoría aún.", color = DrakosMutedGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(auditLogs, key = { it.id }) { log: AuditLogEntity ->
                        val actionColor = when {
                            log.action.contains("SALE") -> SuccessGreen
                            log.action.contains("CLOSE") || log.action.contains("DROP") -> WarningAmber
                            log.action.contains("LOSS") || log.action.contains("EXPENSE") -> DangerRed
                            else -> InfoBlue
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                            border = BorderStroke(1.dp, DrakosHairline),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = actionColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = log.action,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = actionColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = formatDateTime(log.timestamp),
                                        fontSize = 10.sp,
                                        color = DrakosMutedGray
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = log.details,
                                    fontSize = 12.sp,
                                    color = DrakosDarkGray,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Operador: ${log.userName} • Sucursal: K0${log.branchId} • Entidad: ${log.entityType} (${log.entityId})",
                                    fontSize = 10.sp,
                                    color = DrakosMutedGray
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(users, key = { it.id }) { u: UserProfileEntity ->
                    val branchName = branches.find { it.id == u.assignedBranchId }?.name ?: "Sucursal ${u.assignedBranchId}"
                    val badgeColor = when (u.role) {
                        UserRole.OWNER -> com.example.ui.theme.DrakosRed
                        UserRole.BRANCH_MANAGER -> com.example.ui.theme.DrakosBlack
                        UserRole.EMPLOYEE -> SuccessGreen
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                        border = BorderStroke(1.dp, DrakosHairline),
                        elevation = CardDefaults.cardElevation(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosDarkGray)
                                Text("$branchName • Usuario: ${u.username} • PIN Protegido", fontSize = 11.sp, color = DrakosMutedGray)
                            }
                            Surface(
                                color = badgeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = u.role.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
