package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.AuthorizedDeviceEntity
import com.example.data.entity.BranchEntity
import com.example.data.entity.UserProfileEntity
import com.example.domain.model.UserRole
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosCanvas
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceTint
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import com.example.data.cloud.authStateFlow
import com.example.ui.viewmodel.KioscoViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun AccessControlScreen(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.adminUsers.collectAsStateWithLifecycle()
    val devices by viewModel.authorizedDevices.collectAsStateWithLifecycle()
    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val currentDeviceId by viewModel.currentDeviceId.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudAccessSyncStatus.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudAccessSyncing.collectAsStateWithLifecycle()
    val firebaseAuth = remember { Firebase.auth }
    val firebaseUser by remember { firebaseAuth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = firebaseAuth.currentUser)
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var subTab by remember { mutableIntStateOf(0) } // 0: Dispositivos, 1: Usuarios N1/N2
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserProfileEntity?>(null) }
    var showInviteDeviceDialog by remember { mutableStateOf(false) }
    var generatedToken by remember { mutableStateOf<String?>(null) }
    var generatedPortablePackage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrakosCanvas)
            .padding(14.dp)
    ) {
        Text(
            text = "Accesos y Dispositivos (Nivel 3)",
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = DrakosBlack
        )
        Text(
            text = "Control centralizado de permisos Nivel 1 / Nivel 2 y lista blanca de teléfonos autorizados.",
            fontSize = 12.sp,
            color = DrakosMutedInk
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = DrakosBlack,
                selectedLabelColor = DrakosWhite,
                selectedLeadingIconColor = DrakosRed
            )
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                colors = chipColors,
                leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Dispositivos (${devices.count { it.status == "AUTHORIZED" }})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("access_tab_devices")
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                colors = chipColors,
                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Accesos N1 / N2 (${users.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("access_tab_users")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (subTab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DrakosBlack),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bloqueo Estricto de Nuevos Dispositivos",
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DrakosWhite
                                    )
                                    Text(
                                        text = "Si está activo, ningún teléfono nuevo puede operar Nivel 1 o 2 sin autorización tuya.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD4D4D8)
                                    )
                                }
                                Switch(
                                    checked = companySettings.strictDeviceWhitelist,
                                    onCheckedChange = { viewModel.toggleStrictDeviceWhitelist(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DrakosWhite,
                                        checkedTrackColor = DrakosRed
                                    ),
                                    modifier = Modifier.testTag("strict_device_whitelist_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                color = Color(0xFF1F1F27),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Esta Terminal Central:", fontSize = 10.sp, color = Color(0xFFA1A1AA))
                                        Text(
                                            text = currentDeviceId.ifBlank { "DRK-PRINCIPAL" },
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DrakosWhite
                                        )
                                    }
                                    Button(
                                        onClick = { showInviteDeviceDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("generate_device_token_button")
                                    ) {
                                        Icon(Icons.Default.AddModerator, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Habilitar Teléfono Nuevo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                        border = BorderStroke(1.dp, if (firebaseUser != null) SuccessGreen.copy(alpha = 0.5f) else WarningAmber),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (firebaseUser != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (firebaseUser != null) SuccessGreen else WarningAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (firebaseUser != null) {
                                            "Nube PINs Conectada (${firebaseUser?.email ?: "Google"})"
                                        } else {
                                            "Cuenta Google no conectada en este celular"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = DrakosBlack
                                    )
                                    Text(
                                        text = cloudSyncStatus ?: if (firebaseUser != null) {
                                            "${users.size} perfiles locales listos para respaldar en la nube."
                                        } else {
                                            "Conectá Google en 'Nube' para compartir PINs automáticamente, o usá la Llave Portátil (DRKPRO1:...)."
                                        },
                                        fontSize = 11.sp,
                                        color = DrakosMutedInk
                                    )
                                }
                            }
                            if (firebaseUser != null) {
                                Button(
                                    onClick = { viewModel.pushLocalAccessToCloud(silent = false) },
                                    enabled = !isCloudSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("push_all_pins_to_cloud_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = DrakosWhite, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCloudSyncing) "Subiendo PINs a la nube..." else "Subir Todos los PINs y Terminales a Nube Ahora",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DrakosWhite
                                    )
                                }
                            }
                        }
                    }
                }

                if (!generatedToken.isNullOrBlank() || !generatedPortablePackage.isNullOrBlank()) {
                    item {
                        Surface(
                            color = DrakosRedContainer,
                            border = BorderStroke(1.dp, DrakosRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!generatedToken.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Código Corto (Misma cuenta Google en la nube):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                                            Text(
                                                text = generatedToken!!,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DrakosRed
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(generatedToken!!))
                                                viewModel.notifyInfo("Código $generatedToken copiado al portapapeles.")
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Copiar", fontSize = 11.sp, color = DrakosBlack)
                                        }
                                    }
                                }

                                if (!generatedPortablePackage.isNullOrBlank()) {
                                    val pkg = generatedPortablePackage!!
                                    Surface(
                                        color = DrakosWhite,
                                        border = BorderStroke(1.dp, DrakosBorder),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Llave Portátil de PINs y Terminal (Para otro celular sin cuenta Google del dueño):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DrakosBlack
                                            )
                                            Text(
                                                text = pkg.take(44) + "...",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                color = DrakosMutedInk
                                            )
                                            Text(
                                                text = "Envíe esta llave por WhatsApp al otro teléfono y péguela en la pantalla de PIN -> 'Vincular / Importar'.",
                                                fontSize = 10.sp,
                                                color = DrakosMutedInk
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        clipboard.setText(AnnotatedString(pkg))
                                                        viewModel.notifyInfo("Llave portátil copiada. Péguela en el otro teléfono en 'Vincular / Importar'.")
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag("copy_portable_bundle_button")
                                                ) {
                                                    Icon(Icons.Default.Key, contentDescription = null, tint = DrakosBlack, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Copiar Llave", fontSize = 11.sp, color = DrakosBlack)
                                                }
                                                Button(
                                                    onClick = {
                                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(Intent.EXTRA_TEXT, pkg)
                                                        }
                                                        context.startActivity(Intent.createChooser(sendIntent, "Enviar llave de acceso Drakos"))
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag("share_portable_bundle_button")
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Enviar por WhatsApp", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Terminales y Solicitudes Registradas (${devices.size})",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DrakosBlack
                    )
                }

                items(devices, key = { it.deviceId }) { dev: AuthorizedDeviceEntity ->
                    val branchName = branches.find { it.id == dev.assignedBranchId }?.name ?: "Sucursal K0${dev.assignedBranchId}"
                    val isCurrent = dev.deviceId == currentDeviceId
                    val statusColor = when (dev.status) {
                        "AUTHORIZED" -> SuccessGreen
                        "PENDING" -> WarningAmber
                        else -> DangerRed
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
                        border = BorderStroke(1.dp, DrakosBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = DrakosBlack, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = dev.deviceName + if (isCurrent) " (Este equipo)" else "",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DrakosBlack
                                        )
                                        Text(
                                            text = "ID: ${dev.deviceId} • $branchName",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = DrakosMutedInk
                                        )
                                        if (dev.pairingCode.isNotBlank()) {
                                            Text(
                                                text = "Token pendiente: ${dev.pairingCode}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DrakosRed
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = dev.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (!isCurrent) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (dev.status != "AUTHORIZED" && !dev.deviceId.startsWith("INVITE-")) {
                                        Button(
                                            onClick = { viewModel.authorizeDevice(dev.deviceId, dev.deviceName, dev.assignedBranchId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Autorizar", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    if (dev.status != "REVOKED") {
                                        OutlinedButton(
                                            onClick = { viewModel.revokeDevice(dev.deviceId) },
                                            border = BorderStroke(1.dp, DangerRed),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, tint = DangerRed, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Revocar", fontSize = 11.sp, color = DangerRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Sub-tab 1: Gestión de Usuarios y Niveles 1 / 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Otorgar y editar accesos de Nivel 1 y Nivel 2",
                    fontSize = 12.sp,
                    color = DrakosMutedInk,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { showCreateUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("create_staff_access_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nuevo Acceso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val branchId = branches.firstOrNull()?.id ?: 1L
                    viewModel.buildAllStaffPortablePackage(branchId) { pkg ->
                        generatedPortablePackage = pkg
                        clipboard.setText(AnnotatedString(pkg))
                        viewModel.notifyInfo("Llave portátil con todos los PINs copiada. Péguela en el otro celular en 'Vincular / Importar'.")
                        subTab = 0
                    }
                },
                border = BorderStroke(1.dp, DrakosBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("export_portable_pins_button")
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Exportar PINs a Otro Celular (Llave Portátil)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DrakosBlack
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users, key = { it.id }) { u ->
                    val branchName = branches.find { it.id == u.assignedBranchId }?.name ?: "Sucursal K0${u.assignedBranchId}"
                    val roleColor = when (u.role) {
                        UserRole.OWNER -> DrakosRed
                        UserRole.BRANCH_MANAGER -> DrakosBlack
                        UserRole.EMPLOYEE -> SuccessGreen
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (u.isActive) DrakosWhite else DrakosSurfaceTint),
                        border = BorderStroke(1.dp, DrakosBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DrakosBlack)
                                    if (!u.isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = DangerRed.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                                            Text("SUSPENDIDO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DangerRed, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                Text("@${u.username} • $branchName", fontSize = 11.sp, color = DrakosMutedInk)
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(color = roleColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = u.role.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = roleColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (u.role != UserRole.OWNER) {
                                OutlinedButton(
                                    onClick = { editingUser = u },
                                    border = BorderStroke(1.dp, DrakosBlack),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar acceso", tint = DrakosBlack, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Permisos", fontSize = 11.sp, color = DrakosBlack)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInviteDeviceDialog) {
        InviteDeviceDialog(
            branches = branches,
            onDismiss = { showInviteDeviceDialog = false },
            onConfirm = { alias, branchId ->
                viewModel.createDevicePairingPackage(alias, branchId) { token, portable ->
                    generatedToken = token
                    generatedPortablePackage = portable
                }
                showInviteDeviceDialog = false
            }
        )
    }

    if (showCreateUserDialog) {
        CreateStaffUserDialog(
            branches = branches,
            onDismiss = { showCreateUserDialog = false },
            onConfirm = { username, fullName, pin, role, branchId ->
                viewModel.createStaffUser(username, fullName, pin, role, branchId)
                showCreateUserDialog = false
            }
        )
    }

    editingUser?.let { target ->
        EditStaffAccessDialog(
            user = target,
            branches = branches,
            onDismiss = { editingUser = null },
            onConfirm = { newRole, newBranchId, isActive, newPin ->
                viewModel.updateStaffAccess(target.id, newRole, newBranchId, isActive, newPin)
                editingUser = null
            }
        )
    }
}

@Composable
private fun InviteDeviceDialog(
    branches: List<BranchEntity>,
    onDismiss: () -> Unit,
    onConfirm: (alias: String, branchId: Long) -> Unit
) {
    var alias by remember { mutableStateOf("Teléfono Caja 2") }
    var branchId by remember { mutableLongStateOf(branches.firstOrNull()?.id ?: 1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Text("Habilitar Dispositivo Nuevo", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, color = DrakosBlack)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se generará un código único para autorizar el ingreso de un teléfono externo a Drakos.", fontSize = 12.sp, color = DrakosMutedInk)
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Nombre o alias del teléfono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Sucursal asignada:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrakosBlack)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    branches.forEach { b ->
                        FilterChip(
                            selected = branchId == b.id,
                            onClick = { branchId = b.id },
                            label = { Text("${b.code} - ${b.name}", fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(alias, branchId) },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generar Código")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar", color = DrakosBlack) }
        }
    )
}

@Composable
private fun CreateStaffUserDialog(
    branches: List<BranchEntity>,
    onDismiss: () -> Unit,
    onConfirm: (username: String, fullName: String, pin: String, role: UserRole, branchId: Long) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.EMPLOYEE) }
    var branchId by remember { mutableLongStateOf(branches.firstOrNull()?.id ?: 1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Text("Nuevo Acceso (Nivel 1 o 2)", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, color = DrakosBlack)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre y Apellido") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario (ej: cajero_norte)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pin = it },
                    label = { Text("PIN numérico (4 dígitos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Nivel de Acceso:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedRole == UserRole.EMPLOYEE,
                        onClick = { selectedRole = UserRole.EMPLOYEE },
                        label = { Text("Nivel 1 (Ventas/Stock)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedRole == UserRole.BRANCH_MANAGER,
                        onClick = { selectedRole = UserRole.BRANCH_MANAGER },
                        label = { Text("Nivel 2 (Encargado)", fontSize = 11.sp) }
                    )
                }
                Text("Sucursal:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    branches.forEach { b ->
                        FilterChip(
                            selected = branchId == b.id,
                            onClick = { branchId = b.id },
                            label = { Text("${b.code} - ${b.name}", fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username, fullName, pin, selectedRole, branchId) },
                enabled = fullName.isNotBlank() && username.length >= 3 && pin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
            ) {
                Text("Crear Acceso")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar", color = DrakosBlack) }
        }
    )
}

@Composable
private fun EditStaffAccessDialog(
    user: UserProfileEntity,
    branches: List<BranchEntity>,
    onDismiss: () -> Unit,
    onConfirm: (newRole: UserRole, newBranchId: Long, isActive: Boolean, newPin: String?) -> Unit
) {
    var role by remember { mutableStateOf(user.role) }
    var branchId by remember { mutableLongStateOf(user.assignedBranchId) }
    var isActive by remember { mutableStateOf(user.isActive) }
    var newPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Text("Administrar: ${user.fullName}", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, color = DrakosBlack)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nivel asignado:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = role == UserRole.EMPLOYEE,
                        onClick = { role = UserRole.EMPLOYEE },
                        label = { Text("Nivel 1", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = role == UserRole.BRANCH_MANAGER,
                        onClick = { role = UserRole.BRANCH_MANAGER },
                        label = { Text("Nivel 2", fontSize = 11.sp) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Acceso Activo:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("Blanquear PIN (opcional, 4 dígitos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Sucursal asignada:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    branches.forEach { b ->
                        FilterChip(
                            selected = branchId == b.id,
                            onClick = { branchId = b.id },
                            label = { Text("${b.code} - ${b.name}", fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(role, branchId, isActive, newPin.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = DrakosBlack)
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar", color = DrakosBlack) }
        }
    )
}
