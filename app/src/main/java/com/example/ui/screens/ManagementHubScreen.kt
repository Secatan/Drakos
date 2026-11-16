package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.UserRole
import com.example.ui.theme.BrandNavy900
import com.example.ui.theme.BrandSlate700
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosRed
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
    ) {
        Column(modifier = Modifier.fillMaxWidth().background(DrakosWhite)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DrakosRed,
                    selectedLabelColor = DrakosWhite,
                    selectedLeadingIconColor = DrakosWhite
                )
                FilterChip(
                    selected = selectedSubTab == 0,
                    onClick = { onSelectSubTab(0) },
                    colors = chipColors,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Transferencias", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("mgmt_tab_transfers")
                )
                FilterChip(
                    selected = selectedSubTab == 1,
                    onClick = { onSelectSubTab(1) },
                    colors = chipColors,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Locales", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("mgmt_tab_comms")
                )
                if (userLevel >= 3) {
                    FilterChip(
                        selected = selectedSubTab == 4,
                        onClick = { onSelectSubTab(4) },
                        colors = chipColors,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Accesos y Equipos", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("mgmt_tab_access")
                    )
                    FilterChip(
                        selected = selectedSubTab == 2,
                        onClick = { onSelectSubTab(2) },
                        colors = chipColors,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Gastos", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("mgmt_tab_expenses")
                    )
                    FilterChip(
                        selected = selectedSubTab == 3,
                        onClick = { onSelectSubTab(3) },
                        colors = chipColors,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Auditoría", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("mgmt_tab_audit")
                    )
                }
                FilterChip(
                    selected = selectedSubTab == 5,
                    onClick = { onSelectSubTab(5) },
                    colors = chipColors,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Nube Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("mgmt_tab_cloud")
                )
            }
            androidx.compose.material3.HorizontalDivider(color = com.example.ui.theme.DrakosHairline)
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = DangerRed, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("Acceso Restringido (RBAC)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandNavy900)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "El módulo '$moduleName' está habilitado exclusivamente para el perfil $requiredRole.",
                    fontSize = 13.sp,
                    color = BrandSlate700,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
