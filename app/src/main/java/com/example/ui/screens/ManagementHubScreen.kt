package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.UserRole
import com.example.ui.components.DrakosSegmentedTabItem
import com.example.ui.components.DrakosSegmentedTabs
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosWhite
import com.example.ui.viewmodel.KioscoViewModel
import com.google.firebase.auth.FirebaseUser

@Composable
fun ManagementHubScreen(
    viewModel: KioscoViewModel,
    firebaseUser: FirebaseUser?,
    selectedSubTab: Int,
    onSelectSubTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userLevel = currentUser?.role?.level ?: 1

    val tabs = remember(userLevel) {
        buildList {
            add(DrakosSegmentedTabItem(0, "Transferencias", Icons.Default.SwapHoriz, "mgmt_tab_transfers"))
            add(DrakosSegmentedTabItem(1, "Locales", Icons.Default.Forum, "mgmt_tab_comms"))
            if (userLevel >= 3) {
                add(DrakosSegmentedTabItem(4, "Accesos y Equipos", Icons.Default.AdminPanelSettings, "mgmt_tab_access"))
                add(DrakosSegmentedTabItem(2, "Gastos", Icons.Default.AttachMoney, "mgmt_tab_expenses"))
                add(DrakosSegmentedTabItem(3, "Auditoría", Icons.Default.Shield, "mgmt_tab_audit"))
            }
            add(DrakosSegmentedTabItem(5, "Nube Sync", Icons.Default.CloudSync, "mgmt_tab_cloud"))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DrakosWhite)
        ) {
            DrakosSegmentedTabs(
                items = tabs,
                selectedId = selectedSubTab,
                onSelect = onSelectSubTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = DrakosHairline)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedSubTab) {
                0 -> TransfersScreen(viewModel = viewModel)
                1 -> BranchCommsScreen(viewModel = viewModel)
                2 -> if (userLevel >= 3) {
                    ExpensesScreen(viewModel = viewModel)
                } else {
                    RestrictedModuleNotice("Gastos Operativos y Tesorería", UserRole.OWNER.label)
                }
                3 -> if (userLevel >= 3) {
                    AuditScreen(viewModel = viewModel)
                } else {
                    RestrictedModuleNotice("Auditoría Forense", UserRole.OWNER.label)
                }
                4 -> if (userLevel >= 3) {
                    AccessControlScreen(viewModel = viewModel)
                } else {
                    RestrictedModuleNotice("Administración de Accesos y Dispositivos", UserRole.OWNER.label)
                }
                5 -> CloudSyncRoute(
                    firebaseUser = firebaseUser,
                    operatorId = currentUser?.id ?: 0L,
                    operatorName = currentUser?.fullName ?: "Operador",
                    isOwner = userLevel >= 3
                )
                else -> TransfersScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun RestrictedModuleNotice(
    moduleName: String,
    requiredRole: String
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
            border = BorderStroke(1.dp, DrakosHairline),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = DangerRed, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("Acceso Restringido (RBAC)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DrakosDarkGray)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "El módulo '$moduleName' está habilitado exclusivamente para el perfil $requiredRole.",
                    fontSize = 13.sp,
                    color = DrakosMutedGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
