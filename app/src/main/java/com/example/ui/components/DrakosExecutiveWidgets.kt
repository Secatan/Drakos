package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosCarbonElevated
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosGlassBorder
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosSurfaceSoft
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SuccessGreenDark

data class BranchSalesBreakdownItem(
    val branchId: Long = 0L,
    val branchName: String,
    val amount: Double,
    val sharePercent: Double
)

data class RecentActivityFeedItem(
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val kind: ActivityKind,
    val saleId: Long? = null
)

enum class ActivityKind {
    SALE,
    INVENTORY,
    EXPENSE
}

/**
 * Tarjeta blanca "Ventas por Sucursal" minimalista con filas interactivas para abrir el Drawer de detalle.
 */
@Composable
fun DrakosBranchBreakdownCard(
    items: List<BranchSalesBreakdownItem>,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
    onBranchClick: ((BranchSalesBreakdownItem) -> Unit)? = null
) {
    Surface(
        color = DrakosWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DrakosHairline),
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = DrakosDarkGray,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ventas por Sucursal",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DrakosDarkGray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSeeAll() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Ver todas",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DrakosMutedGray
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ver todas las sucursales",
                        tint = DrakosMutedGray,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "Aún no se registran ventas por sucursal en el período.",
                    fontSize = 12.sp,
                    color = DrakosMutedGray
                )
            } else {
                items.take(4).forEachIndexed { idx, row ->
                    val barColor = when (idx) {
                        0 -> DrakosRed
                        1 -> DrakosBlack
                        else -> Color(0xFF9CA3AF)
                    }
                    val fraction = (row.sharePercent / 100.0).toFloat().coerceIn(0f, 1f)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .let { m ->
                                if (onBranchClick != null) m.clickable { onBranchClick(row) } else m
                            }
                            .padding(vertical = 3.dp)
                    ) {
                        Text(
                            text = row.branchName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DrakosDarkGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DrakosSurfaceSoft)
                            ) {
                                if (fraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction.coerceAtLeast(0.04f))
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(barColor)
                                    )
                                }
                            }
                            Text(
                                text = "${String.format(java.util.Locale.forLanguageTag("es-AR"), "%.1f", row.sharePercent)}%",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = DrakosMutedGray
                            )
                            Text(
                                text = formatCurrency(row.amount),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrakosDarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta blanca "Resumen Financiero" con gráfico tipo dona (Canvas Ring)
 * distribuyendo Ingresos Brutos, Costo de Mercadería (CMV) y Gastos Operativos.
 */
@Composable
fun DrakosDonutSummaryCard(
    grossRevenue: Double,
    cogsAmount: Double,
    expensesAmount: Double,
    modifier: Modifier = Modifier
) {
    val netPositive = (grossRevenue - cogsAmount - expensesAmount).coerceAtLeast(0.0)
    val totalPool = (netPositive + cogsAmount + expensesAmount).takeIf { it > 0.0 } ?: 1.0
    val netPct = if (grossRevenue > 0.0 || cogsAmount > 0.0) (netPositive / totalPool) * 100.0 else 0.0
    val cogsPct = if (grossRevenue > 0.0 || cogsAmount > 0.0) (cogsAmount / totalPool) * 100.0 else 0.0
    val expPct = if (grossRevenue > 0.0 || expensesAmount > 0.0) (expensesAmount / totalPool) * 100.0 else 0.0

    Surface(
        color = DrakosWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DrakosHairline),
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = DrakosDarkGray,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Resumen Financiero",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DrakosDarkGray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(116.dp)
                ) {
                    Canvas(modifier = Modifier.size(116.dp)) {
                        val strokeW = 18.dp.toPx()
                        val pad = strokeW / 2f
                        val arcSize = Size(size.width - strokeW, size.height - strokeW)
                        val topLeft = Offset(pad, pad)

                        if (grossRevenue <= 0.0 && cogsAmount <= 0.0 && expensesAmount <= 0.0) {
                            drawArc(
                                color = DrakosSurfaceSoft,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW)
                            )
                        } else {
                            val slices = listOf(
                                netPct.toFloat() to DrakosRed,
                                cogsPct.toFloat() to DrakosBlack,
                                expPct.toFloat() to Color(0xFF9CA3AF)
                            )
                            var startAngle = -90f
                            slices.forEach { (pct, color) ->
                                if (pct > 0.5f) {
                                    val sweep = (pct / 100f) * 360f
                                    val gap = if (sweep > 8f) 3f else 0f
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = (sweep - gap).coerceAtLeast(1f),
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCompactTotal(grossRevenue),
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = DrakosDarkGray
                        )
                        Text(
                            text = "Facturado",
                            fontSize = 10.sp,
                            color = DrakosMutedGray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DonutLegendRow(color = DrakosRed, label = "Margen Neto", pct = netPct)
                    DonutLegendRow(color = DrakosBlack, label = "CMV Mercadería", pct = cogsPct)
                    DonutLegendRow(color = Color(0xFF9CA3AF), label = "Gastos Operat.", pct = expPct)
                }
            }
        }
    }
}

@Composable
private fun DonutLegendRow(color: Color, label: String, pct: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, color = DrakosDarkGray, fontWeight = FontWeight.Medium)
        }
        Text(
            text = "${String.format(java.util.Locale.forLanguageTag("es-AR"), "%.1f", pct)}%",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DrakosDarkGray
        )
    }
}

/**
 * Tarjeta blanca "Actividad Reciente" alimentada por ventas reales, movimientos de stock y egresos.
 */
@Composable
fun DrakosRecentActivityCard(
    items: List<RecentActivityFeedItem>,
    onSeeMore: () -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: ((RecentActivityFeedItem) -> Unit)? = null
) {
    Surface(
        color = DrakosWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DrakosHairline),
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = DrakosDarkGray,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Actividad Reciente",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DrakosDarkGray
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSeeMore() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Ver más", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DrakosMutedGray)
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ver más actividad",
                        tint = DrakosMutedGray,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "Sin movimientos operativos recientes.",
                    fontSize = 12.sp,
                    color = DrakosMutedGray
                )
            } else {
                items.take(4).forEachIndexed { idx, act ->
                    val (icon, iconTint, pillBg, pillTextColor) = when (act.kind) {
                        ActivityKind.SALE -> Quad(Icons.Default.ShoppingCart, SuccessGreen, SuccessGreenContainer, SuccessGreenDark)
                        ActivityKind.INVENTORY -> Quad(Icons.AutoMirrored.Filled.ReceiptLong, InfoBlue, InfoBlueContainer, InfoBlue)
                        ActivityKind.EXPENSE -> Quad(Icons.Default.Payments, DrakosRed, DrakosRed.copy(alpha = 0.12f), DrakosRed)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .let { m ->
                                if (onItemClick != null) m.clickable { onItemClick(act) } else m
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(iconTint.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = act.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DrakosDarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = act.subtitle,
                                    fontSize = 10.sp,
                                    color = DrakosMutedGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = pillBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = act.badgeText,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillTextColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (idx < items.take(4).lastIndex) {
                        HorizontalDivider(color = DrakosHairline)
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta oscura de identidad corporativa "DRAKOS STORE" con el Bulldog oficial intacto
 * y contadores reales de la red de sucursales y plantel activo.
 */
@Composable
fun DrakosBrandIdentityCard(
    branchesCount: Int,
    usersCount: Int,
    activeStatusLabel: String = "100%",
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DrakosGlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF33040B), DrakosBlack, DrakosCarbonElevated)
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_drakos_bulldog),
                    contentDescription = "Identidad corporativa Drako's Store",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.5.dp, DrakosRed, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = DrakosWhite)) { append("DRAKOS ") }
                            withStyle(SpanStyle(color = DrakosRed)) { append("STORE") }
                        },
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Gestión Inteligente para un negocio más fuerte",
                        fontSize = 11.sp,
                        color = Color(0xFFD1D5DB),
                        lineHeight = 14.sp
                    )
                }
            }

            HorizontalDivider(color = DrakosGlassBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BrandMetricCell(
                    icon = Icons.Default.Storefront,
                    value = "$branchesCount",
                    label = "Sucursales"
                )
                BrandMetricCell(
                    icon = Icons.Default.Groups,
                    value = "$usersCount",
                    label = "Empleados"
                )
                BrandMetricCell(
                    icon = Icons.Default.VerifiedUser,
                    value = activeStatusLabel,
                    label = "Operativo"
                )
            }
        }
    }
}

@Composable
private fun BrandMetricCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = value,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = DrakosWhite
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private fun formatCompactTotal(value: Double): String {
    val locale = java.util.Locale.forLanguageTag("es-AR")
    return when {
        value >= 1_000_000.0 -> String.format(locale, "$ %.2fM", value / 1_000_000.0)
        value >= 1_000.0 -> String.format(locale, "$ %.1fk", value / 1_000.0)
        else -> formatCurrency(value)
    }
}
