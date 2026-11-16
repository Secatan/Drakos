package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.BranchEntity
import com.example.data.entity.CashSessionEntity
import com.example.data.entity.UserProfileEntity
import com.example.domain.model.UserRole
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosCarbonElevated
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosGlassBorder
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosLightCanvas
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedDark
import com.example.ui.theme.DrakosSurfaceSoft
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SuccessGreenDark
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

data class DrakosAlertItem(
    val title: String,
    val detail: String,
    val isCritical: Boolean = false,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

data class DrakosSidebarDest(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val minLevel: Int = 1,
    val selected: Boolean,
    val badgeCount: Int = 0,
    val onClick: () -> Unit
)

/**
 * Barra lateral oscura corporativa (#08090B) con logo original Drako's Store
 * y resaltado rojo carmesí (#D50016), compartida entre el panel permanente en Tablet/Desktop
 * y el ModalNavigationDrawer en móviles.
 */
@Composable
fun DrakosSidebarContent(
    currentUser: UserProfileEntity?,
    destinations: List<DrakosSidebarDest>,
    modifier: Modifier = Modifier
) {
    val userLevel = currentUser?.role?.level ?: 1
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(DrakosBlack)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Identidad Original Drako's Store (Bulldog Blanco)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_drakos_bulldog),
                    contentDescription = "Logo oficial Drako's Store",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.5.dp, DrakosRed, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = DrakosWhite)) { append("DRAKO'S ") }
                        withStyle(SpanStyle(color = DrakosRed)) { append("STORE") }
                    },
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "MAXIKIOSCO 24 HS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            HorizontalDivider(
                color = DrakosGlassBorder,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Menú principal
            Column(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                destinations
                    .filter { userLevel >= it.minLevel }
                    .forEach { dest ->
                        val itemBg = if (dest.selected) {
                            Brush.horizontalGradient(listOf(DrakosRed, DrakosRedDark))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(itemBg)
                                .clickable { dest.onClick() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.label,
                                    tint = if (dest.selected) DrakosWhite else Color(0xFFB4B7C0),
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = dest.label,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (dest.selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (dest.selected) DrakosWhite else Color(0xFFD1D5DB),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (dest.badgeCount > 0) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (dest.selected) DrakosWhite else DrakosRed)
                                ) {
                                    Text(
                                        text = dest.badgeCount.coerceAtMost(9).toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (dest.selected) DrakosRed else DrakosWhite
                                    )
                                }
                            }
                        }
                    }
            }
        }

        // Tarjeta inferior corporativa DRAKOS MANAGEMENT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF3A060D), Color(0xFF15080B), DrakosCarbonElevated)
                    )
                )
                .border(1.dp, DrakosRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_drakos_bulldog),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(56.dp)
                    .offset(x = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .alpha(0.22f)
            )
            Column {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = DrakosWhite)) { append("DRAKOS\n") }
                        withStyle(SpanStyle(color = DrakosRed)) { append("M") }
                        withStyle(SpanStyle(color = DrakosWhite)) { append("ANAGEMENT") }
                    },
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tu negocio, en control.",
                    fontSize = 10.sp,
                    color = Color(0xFFD1D5DB)
                )
            }
        }
    }
}

/**
 * Barra superior SaaS limpia y luminosa (#FFFFFF + borde #E6E8ED) unificada en toda la app.
 */
@Composable
fun DrakosSaasTopBar(
    pageTitle: String,
    pageSubtitle: String,
    currentUser: UserProfileEntity?,
    currentBranch: BranchEntity?,
    branches: List<BranchEntity>,
    activeSession: CashSessionEntity?,
    isCloudConnected: Boolean,
    alerts: List<DrakosAlertItem>,
    onToggleMenu: () -> Unit,
    onBranchSelected: (BranchEntity) -> Unit,
    onOpenUserSwitch: () -> Unit,
    onOpenCloudSync: () -> Unit,
    onNavigateToShifts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var branchMenuExpanded by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }

    val roleLabel = when (currentUser?.role) {
        UserRole.OWNER -> "Dueño"
        UserRole.BRANCH_MANAGER -> "Encargado"
        UserRole.EMPLOYEE -> "Cajero"
        null -> "Sin sesión"
    }

    Surface(
        color = DrakosWhite,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWideHeader = maxWidth >= 720.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Izquierda: Botón Menú + Título y Subtítulo del módulo activo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onToggleMenu,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("header_quick_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú de navegación",
                                tint = DrakosDarkGray
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = pageTitle,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DrakosDarkGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pageSubtitle,
                                fontSize = 11.sp,
                                color = DrakosMutedGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Derecha: Controles inline en ancho amplio + Campana y Usuario siempre visibles
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isWideHeader) {
                            TopBranchSelectorPill(
                                currentUser = currentUser,
                                currentBranch = currentBranch,
                                branches = branches,
                                expanded = branchMenuExpanded,
                                onExpandChange = { branchMenuExpanded = it },
                                onBranchSelected = onBranchSelected
                            )
                            TopCloudStatusPill(
                                isCloudConnected = isCloudConnected,
                                activeSession = activeSession,
                                onClick = onOpenCloudSync
                            )
                        }

                        // Campana de notificaciones con badge rojo carmesí
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = { showAlertsDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("header_notifications_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificaciones operativas",
                                    tint = DrakosDarkGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            if (alerts.isNotEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-6).dp, y = 6.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(DrakosRed)
                                        .border(1.5.dp, DrakosWhite, CircleShape)
                                ) {
                                    Text(
                                        text = alerts.size.coerceAtMost(9).toString(),
                                        color = DrakosWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Pill de perfil de usuario (cambia operador / cierra sesión)
                        Surface(
                            color = DrakosLightCanvas,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DrakosHairline),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable { onOpenUserSwitch() }
                                .testTag("user_profile_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(DrakosBlack)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Usuario activo",
                                        tint = DrakosWhite,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = currentUser?.fullName?.substringBefore(" ") ?: "Equipo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DrakosDarkGray,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = roleLabel,
                                        fontSize = 10.sp,
                                        color = DrakosMutedGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = DrakosMutedGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Segunda fila compacta en móvil: Sucursal + Estado Nube + Estado Caja
                if (!isWideHeader) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopBranchSelectorPill(
                            currentUser = currentUser,
                            currentBranch = currentBranch,
                            branches = branches,
                            expanded = branchMenuExpanded,
                            onExpandChange = { branchMenuExpanded = it },
                            onBranchSelected = onBranchSelected
                        )
                        TopCloudStatusPill(
                            isCloudConnected = isCloudConnected,
                            activeSession = activeSession,
                            onClick = onOpenCloudSync
                        )
                        Surface(
                            color = if (activeSession != null) SuccessGreenContainer else DrakosRed.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (activeSession != null) SuccessGreen.copy(alpha = 0.35f) else DrakosRed.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .clickable { onNavigateToShifts() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (activeSession != null) SuccessGreen else DrakosRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeSession != null) "Caja Abierta" else "Caja Cerrada",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (activeSession != null) SuccessGreenDark else DrakosRed
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = DrakosHairline, thickness = 1.dp)
            }
        }
    }

    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            containerColor = DrakosWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = DrakosRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alertas Operativas (${alerts.size})",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = DrakosDarkGray
                    )
                }
            },
            text = {
                if (alerts.isEmpty()) {
                    Text(
                        text = "Sin alertas pendientes. Todos los indicadores están en orden.",
                        fontSize = 13.sp,
                        color = DrakosMutedGray
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        alerts.take(5).forEach { alert ->
                            Surface(
                                color = if (alert.isCritical) DangerRed.copy(alpha = 0.06f) else DrakosLightCanvas,
                                border = BorderStroke(
                                    1.dp,
                                    if (alert.isCritical) DangerRed.copy(alpha = 0.32f) else DrakosHairline
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (alert.isCritical) DangerRed else WarningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DrakosDarkGray)
                                            Text(alert.detail, fontSize = 11.sp, color = DrakosMutedGray)
                                        }
                                    }
                                    if (alert.actionLabel != null && alert.onAction != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                showAlertsDialog = false
                                                alert.onAction.invoke()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.heightIn(min = 36.dp)
                                        ) {
                                            Text(alert.actionLabel, fontSize = 10.sp, color = DrakosRed, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        if (activeSession == null) {
                            OutlinedButton(
                                onClick = {
                                    showAlertsDialog = false
                                    onNavigateToShifts()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ir a Caja para Abrir Turno", color = DrakosDarkGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAlertsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun TopBranchSelectorPill(
    currentUser: UserProfileEntity?,
    currentBranch: BranchEntity?,
    branches: List<BranchEntity>,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onBranchSelected: (BranchEntity) -> Unit
) {
    Box {
        Surface(
            color = DrakosLightCanvas,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DrakosHairline),
            modifier = Modifier
                .heightIn(min = 38.dp)
                .clickable(enabled = currentUser?.role == UserRole.OWNER) { onExpandChange(true) }
                .testTag("branch_selector_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = "Sucursal",
                    tint = DrakosDarkGray,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentBranch?.name ?: "Sucursal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DrakosDarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentUser?.role == UserRole.OWNER) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cambiar sucursal",
                        tint = DrakosMutedGray,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        if (currentUser?.role == UserRole.OWNER) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) }
            ) {
                Text(
                    text = "Red de ${branches.size} Kioscos Drakos",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = DrakosDarkGray
                )
                HorizontalDivider()
                branches.forEach { branch ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(branch.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DrakosDarkGray)
                                Text(branch.address, fontSize = 11.sp, color = DrakosMutedGray)
                            }
                        },
                        onClick = {
                            onBranchSelected(branch)
                            onExpandChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCloudStatusPill(
    isCloudConnected: Boolean,
    activeSession: CashSessionEntity?,
    onClick: () -> Unit
) {
    Surface(
        color = DrakosLightCanvas,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DrakosHairline),
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clickable { onClick() }
            .testTag("top_cloud_status_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = "Estado Nube",
                tint = if (isCloudConnected) SuccessGreen else WarningAmber,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isCloudConnected) "Nube OK" else "Nube Offline",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DrakosDarkGray
            )
        }
    }
}

/**
 * Tarjeta KPI SaaS Blanca (#FFFFFF + borde sutil #E6E8ED), con badge cuadrado negro/rojo,
 * métrica principal en #17191E, tendencia y sparkline suave inferior derecho (exacto al mockup).
 */
@Composable
fun DrakosSparklineKpiCard(
    title: String,
    value: String,
    trendText: String,
    trendColor: Color,
    icon: ImageVector,
    accentColor: Color,
    sparklinePoints: List<Float>,
    modifier: Modifier = Modifier,
    badgeContainerColor: Color = DrakosBlack
) {
    Surface(
        color = DrakosWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DrakosHairline),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeContainerColor)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DrakosWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        color = DrakosMutedGray,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = value,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DrakosDarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = trendText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )

                MiniSparklineCanvas(
                    points = sparklinePoints,
                    strokeColor = accentColor,
                    modifier = Modifier
                        .width(72.dp)
                        .height(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniSparklineCanvas(
    points: List<Float>,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val safePoints = if (points.size >= 2) points else listOf(0.3f, 0.35f, 0.32f, 0.4f)
        val min = safePoints.minOrNull() ?: 0f
        val max = safePoints.maxOrNull() ?: 1f
        val span = (max - min).takeIf { it > 0.0001f } ?: 1f

        val padY = 3.dp.toPx()
        val usableH = (size.height - padY * 2f).coerceAtLeast(1f)
        val stepX = size.width / (safePoints.size - 1).coerceAtLeast(1)

        val coords = safePoints.mapIndexed { idx, v ->
            val norm = if (max - min <= 0.0001f) 0.5f else (v - min) / span
            Offset(
                x = idx * stepX,
                y = padY + usableH * (1f - norm)
            )
        }

        val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 0 until coords.lastIndex) {
                val p0 = coords[i]
                val p1 = coords[i + 1]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(coords.last().x, size.height)
            lineTo(coords.first().x, size.height)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    strokeColor.copy(alpha = 0.22f),
                    Color.Transparent
                )
            )
        )
        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
