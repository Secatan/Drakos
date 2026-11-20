package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.theme.DrakosSurfaceSoft
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SuccessGreenDark
import com.example.ui.theme.WarningAmber

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

data class DrakosSegmentedTabItem(
    val id: Int,
    val label: String,
    val icon: ImageVector? = null,
    val testTag: String? = null
)

/**
 * Tabs minimalistas neutras sobre track #F1F3F6 con píldora seleccionada #FFFFFF/#08090B.
 * Libera al color rojo corporativo (#D50016) exclusivamente para el CTA crítico de la pantalla.
 */
@Composable
fun DrakosSegmentedTabs(
    items: List<DrakosSegmentedTabItem>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DrakosSurfaceSoft,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { tab ->
                val isSelected = tab.id == selectedId
                val tabModifier = Modifier
                    .heightIn(min = 38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .let { base ->
                        if (tab.testTag != null) base.testTag(tab.testTag) else base
                    }

                Surface(
                    color = if (isSelected) DrakosWhite else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    shape = RoundedCornerShape(9.dp),
                    modifier = tabModifier.clickable { onSelect(tab.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (tab.icon != null) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) DrakosDarkGray else DrakosMutedGray,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = tab.label,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) DrakosDarkGray else DrakosMutedGray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide-over Drawer adaptativo:
 * - En tablet/desktop (>=720dp): panel lateral derecho deslizable preservando contexto de fondo.
 * - En móvil (<720dp): hoja inferior ModalBottomSheet limpia y táctil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrakosSlideOverDrawer(
    visible: Boolean,
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x5208090B))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                ) {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Surface(
                            color = DrakosWhite,
                            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                            border = BorderStroke(1.dp, DrakosHairline),
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .width(396.dp)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            ) {
                                DrawerHeader(title = title, subtitle = subtitle, onDismiss = onDismiss)
                                HorizontalDivider(color = DrakosHairline)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    content = content
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = DrakosWhite,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    DrawerHeader(title = title, subtitle = subtitle, onDismiss = onDismiss)
                    HorizontalDivider(color = DrakosHairline)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DrakosDarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = DrakosMutedGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar panel",
                tint = DrakosMutedGray
            )
        }
    }
}

/**
 * Modal centrado enfocado para formularios de creación/edición y confirmaciones críticas.
 */
@Composable
fun DrakosFocusedModal(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    confirmColor: Color = DrakosRed,
    onConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    dismissLabel: String = "Cancelar",
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = DrakosWhite,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, DrakosHairline),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DrawerHeader(title = title, subtitle = subtitle, onDismiss = onDismiss)
                HorizontalDivider(color = DrakosHairline)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
                HorizontalDivider(color = DrakosHairline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(dismissLabel, color = DrakosMutedGray, fontWeight = FontWeight.SemiBold)
                    }
                    if (confirmLabel != null && onConfirm != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onConfirm,
                            enabled = confirmEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(confirmLabel, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Barra lateral oscura corporativa (#08090B) depurada estilo Linear:
 * conserva el logo original de Drako's Store, sustituye gradientes agresivos por un estado activo limpio
 * con barra indicadora vertical carmesí (#D50016) de 3dp.
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
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.dp, DrakosGlassBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = DrakosWhite)) { append("DRAKO'S ") }
                        withStyle(SpanStyle(color = DrakosRed)) { append("STORE") }
                    },
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "MAXIKIOSCO 24 HS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            HorizontalDivider(
                color = DrakosGlassBorder,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Column(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                destinations
                    .filter { userLevel >= it.minLevel }
                    .forEach { dest ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (dest.selected) DrakosCarbonElevated else Color.Transparent)
                                .clickable { dest.onClick() }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (dest.selected) DrakosRed else Color.Transparent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.label,
                                    tint = if (dest.selected) DrakosWhite else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = dest.label,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (dest.selected) FontWeight.SemiBold else FontWeight.Medium,
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
                                        .background(DrakosRed)
                                ) {
                                    Text(
                                        text = dest.badgeCount.coerceAtMost(9).toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DrakosWhite
                                    )
                                }
                            }
                        }
                    }
            }
        }

        Surface(
            color = DrakosCarbonElevated.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DrakosGlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.img_drakos_bulldog),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(44.dp)
                        .offset(x = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .alpha(0.18f)
                )
                Column {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = DrakosWhite)) { append("DRAKOS ") }
                            withStyle(SpanStyle(color = DrakosRed)) { append("POS") }
                        },
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tu negocio, en control.",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

/**
 * Barra superior SaaS minimalista (#FFFFFF + hairline #EEF0F4) unificada en toda la app.
 * Las notificaciones abren un Slide-over Drawer en lugar de un diálogo recargado.
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
    var showAlertsDrawer by remember { mutableStateOf(false) }

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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                        Spacer(modifier = Modifier.width(2.dp))
                        Column {
                            Text(
                                text = pageTitle,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                        Box(contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = { showAlertsDrawer = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("header_notifications_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificaciones operativas",
                                    tint = DrakosDarkGray,
                                    modifier = Modifier.size(21.dp)
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
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Surface(
                            color = DrakosSurfaceSoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
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
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(DrakosBlack)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Usuario activo",
                                        tint = DrakosWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(7.dp))
                                Column {
                                    Text(
                                        text = currentUser?.fullName?.substringBefore(" ") ?: "Equipo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DrakosDarkGray,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = roleLabel,
                                        fontSize = 10.sp,
                                        color = DrakosMutedGray,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = DrakosMutedGray,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                if (!isWideHeader) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
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
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
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

    DrakosSlideOverDrawer(
        visible = showAlertsDrawer,
        title = "Notificaciones (${alerts.size})",
        subtitle = "Alertas operativas y estado de sucursal",
        onDismiss = { showAlertsDrawer = false }
    ) {
        if (alerts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Todo está al día",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DrakosDarkGray
                )
                Text(
                    text = "No hay alertas pendientes ni incidencias operativas.",
                    fontSize = 12.sp,
                    color = DrakosMutedGray
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                alerts.forEach { alert ->
                    Surface(
                        color = if (alert.isCritical) DangerRed.copy(alpha = 0.05f) else DrakosLightCanvas,
                        border = BorderStroke(
                            1.dp,
                            if (alert.isCritical) DangerRed.copy(alpha = 0.25f) else DrakosHairline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
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
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = alert.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = DrakosDarkGray
                                    )
                                    Text(
                                        text = alert.detail,
                                        fontSize = 11.sp,
                                        color = DrakosMutedGray
                                    )
                                }
                            }
                            if (alert.actionLabel != null && alert.onAction != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        showAlertsDrawer = false
                                        alert.onAction.invoke()
                                    },
                                    border = BorderStroke(1.dp, DrakosHairline),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Text(
                                        text = alert.actionLabel,
                                        fontSize = 11.sp,
                                        color = if (alert.isCritical) DrakosRed else DrakosDarkGray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                if (activeSession == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            showAlertsDrawer = false
                            onNavigateToShifts()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Ir a Caja para Abrir Turno", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
            color = DrakosSurfaceSoft,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .heightIn(min = 36.dp)
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
                    modifier = Modifier.size(14.dp)
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
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cambiar sucursal",
                        tint = DrakosMutedGray,
                        modifier = Modifier.size(14.dp)
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
                HorizontalDivider(color = DrakosHairline)
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
        color = DrakosSurfaceSoft,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .heightIn(min = 36.dp)
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
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isCloudConnected) "Nube OK" else "Offline",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DrakosDarkGray
            )
        }
    }
}

/**
 * Tarjeta KPI SaaS Ultra-Clean (#FFFFFF + hairline #EEF0F4, cero sombras pesadas):
 * valor protagonista tabular, ícono suave sutil y sparkline limpio.
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
    badgeContainerColor: Color = DrakosSurfaceSoft
) {
    Surface(
        color = DrakosWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DrakosHairline),
        shadowElevation = 0.dp,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = DrakosMutedGray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.10f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = value,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DrakosDarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = trendText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = trendColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                MiniSparklineCanvas(
                    points = sparklinePoints,
                    strokeColor = accentColor,
                    modifier = Modifier
                        .width(64.dp)
                        .height(22.dp)
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
                    strokeColor.copy(alpha = 0.16f),
                    Color.Transparent
                )
            )
        )
        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(
                width = 1.8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
