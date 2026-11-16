package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BranchEntity
import com.example.domain.model.SalesBarPoint
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosDarkGray
import com.example.ui.theme.DrakosHairline
import com.example.ui.theme.DrakosLightCanvas
import com.example.ui.theme.DrakosMutedGray
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosSurfaceSoft
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

/**
 * Tarjeta blanca SaaS "Ventas por Período" fiel al mockup ejecutivo:
 * barras comparativas Negro Profundo (#08090B = Ventas) + Rojo Principal (#D50016 = Ganancia/Margen),
 * selector segmentado 14d / 12m, filtro por sucursal y modo Curva opcional.
 */
@Composable
fun FinancialBarChartCard(
    dailyPoints: List<SalesBarPoint>,
    monthlyPoints: List<SalesBarPoint>,
    branches: List<BranchEntity>,
    selectedBranchId: Long?,
    onSelectBranchId: (Long?) -> Unit,
    canChangeBranchFilter: Boolean,
    showCostComparison: Boolean,
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableIntStateOf(0) } // 0: 14 días, 1: 12 meses
    var chartVisualStyle by remember { mutableIntStateOf(1) } // 1: Barras (default mockup), 0: Curva
    var selectedBarIndex by remember(chartMode, selectedBranchId) { mutableIntStateOf(-1) }
    var showBranchMenu by remember { mutableStateOf(false) }

    val activePoints = if (chartMode == 0) dailyPoints else monthlyPoints
    val maxVal = remember(activePoints) {
        activePoints.maxOfOrNull { it.revenueAmount }?.takeIf { it > 0.0 } ?: 1000.0
    }
    val dailyTotal = remember(dailyPoints) { dailyPoints.sumOf { it.revenueAmount } }
    val monthlyTotal = remember(monthlyPoints) { monthlyPoints.sumOf { it.revenueAmount } }
    val peakPointIndex = remember(activePoints) {
        activePoints.indices.maxByOrNull { activePoints[it].revenueAmount } ?: -1
    }
    val highlightIndex = if (selectedBarIndex in activePoints.indices) selectedBarIndex else peakPointIndex
    val highlightPoint = activePoints.getOrNull(highlightIndex)

    val selectedBranchName = if (selectedBranchId == null) {
        "Todas (${branches.size} suc.)"
    } else {
        branches.find { it.id == selectedBranchId }?.name ?: "Sucursal K0$selectedBranchId"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DrakosWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DrakosHairline),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabecera: Título izquierda + Segmented Pills derecha
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 460.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartTitleBlock(selectedBranchName = selectedBranchName)
                        ChartHeaderControls(
                            chartMode = chartMode,
                            onSelectMode = { chartMode = it },
                            canChangeBranchFilter = canChangeBranchFilter,
                            selectedBranchId = selectedBranchId,
                            branches = branches,
                            showBranchMenu = showBranchMenu,
                            onShowBranchMenuChange = { showBranchMenu = it },
                            onSelectBranchId = onSelectBranchId
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChartTitleBlock(
                                selectedBranchName = selectedBranchName,
                                modifier = Modifier.weight(1f)
                            )
                            if (canChangeBranchFilter) {
                                BranchFilterDropdownPill(
                                    selectedBranchId = selectedBranchId,
                                    branches = branches,
                                    expanded = showBranchMenu,
                                    onExpandChange = { showBranchMenu = it },
                                    onSelectBranchId = onSelectBranchId
                                )
                            }
                        }
                        PeriodSegmentedBar(
                            chartMode = chartMode,
                            dailyTotal = dailyTotal,
                            monthlyTotal = monthlyTotal,
                            onSelectMode = { chartMode = it }
                        )
                    }
                }
            }

            // Fila de Leyenda (Ventas Negro + Ganancia/Margen Rojo) + Toggle Curva/Barras + Tooltip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = DrakosBlack, label = "Ventas")
                    if (showCostComparison) {
                        LegendDot(color = DrakosRed, label = "Ganancia Est.")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (highlightPoint != null && highlightPoint.revenueAmount > 0.0) {
                        val marginVal = (highlightPoint.revenueAmount - highlightPoint.costAmount).coerceAtLeast(0.0)
                        Surface(
                            color = DrakosBlack,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (showCostComparison && highlightPoint.costAmount > 0.0) {
                                    "${highlightPoint.fullLabel}: ${formatCurrency(highlightPoint.revenueAmount)} (+${formatCompactArs(marginVal)})"
                                } else {
                                    "${highlightPoint.fullLabel}: ${formatCurrency(highlightPoint.revenueAmount)}"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrakosWhite,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = DrakosSurfaceSoft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            chartVisualStyle = if (chartVisualStyle == 1) 0 else 1
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (chartVisualStyle == 1) Icons.Default.BarChart else Icons.Default.ShowChart,
                                contentDescription = "Cambiar tipo de gráfico",
                                tint = DrakosDarkGray,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (chartVisualStyle == 1) "Barras" else "Curva",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrakosDarkGray
                            )
                        }
                    }
                }
            }

            if (activePoints.all { it.revenueAmount <= 0.0 }) {
                Surface(
                    color = DrakosLightCanvas,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DrakosHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sin ventas registradas para este filtro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = DrakosDarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Registre una venta en el POS o seleccione todas las sucursales para visualizar la serie.",
                            fontSize = 11.sp,
                            color = DrakosMutedGray
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(186.dp)
                ) {
                    // Eje Y numérico tabular
                    Column(
                        modifier = Modifier
                            .width(48.dp)
                            .height(158.dp)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(formatCompactArs(maxVal), fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedGray)
                        Text(formatCompactArs(maxVal * 0.66), fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedGray)
                        Text(formatCompactArs(maxVal * 0.33), fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedGray)
                        Text("$ 0", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedGray)
                    }

                    val scrollState = rememberScrollState(initial = Int.MAX_VALUE)
                    val stepDp = if (chartVisualStyle == 1) 42.dp else 34.dp
                    val totalCanvasWidth = (stepDp * activePoints.size).coerceAtLeast(300.dp)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(scrollState)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(totalCanvasWidth)
                                .height(158.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(activePoints) {
                                        detectTapGestures { offset ->
                                            val slotWidth = size.width / activePoints.size.coerceAtLeast(1)
                                            val idx = (offset.x / slotWidth).toInt()
                                                .coerceIn(0, activePoints.lastIndex)
                                            selectedBarIndex = if (selectedBarIndex == idx) -1 else idx
                                        }
                                    }
                            ) {
                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                val gridColor = DrakosHairline
                                val topPad = 10.dp.toPx()
                                val chartH = size.height - topPad

                                listOf(0f, 0.33f, 0.66f, 1f).forEach { ratio ->
                                    val y = topPad + chartH * (1f - ratio)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = if (ratio > 0f) dashEffect else null
                                    )
                                }

                                val count = activePoints.size.coerceAtLeast(1)
                                val slotW = size.width / count

                                if (chartVisualStyle == 0) {
                                    // Modo Curva limpia sobre fondo blanco
                                    val pointsCoords = activePoints.mapIndexed { idx, pt ->
                                        val ratio = (pt.revenueAmount / maxVal).toFloat().coerceIn(0f, 1f)
                                        val x = idx * slotW + slotW / 2f
                                        val y = topPad + chartH * (1f - ratio)
                                        Offset(x, y)
                                    }

                                    if (pointsCoords.size >= 2) {
                                        val linePath = Path().apply {
                                            moveTo(pointsCoords.first().x, pointsCoords.first().y)
                                            for (i in 0 until pointsCoords.lastIndex) {
                                                val p0 = pointsCoords[i]
                                                val p1 = pointsCoords[i + 1]
                                                val midX = (p0.x + p1.x) / 2f
                                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                            }
                                        }

                                        val areaPath = Path().apply {
                                            addPath(linePath)
                                            lineTo(pointsCoords.last().x, topPad + chartH)
                                            lineTo(pointsCoords.first().x, topPad + chartH)
                                            close()
                                        }

                                        drawPath(
                                            path = areaPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    DrakosRed.copy(alpha = 0.22f),
                                                    DrakosRed.copy(alpha = 0.02f),
                                                    Color.Transparent
                                                )
                                            )
                                        )

                                        drawPath(
                                            path = linePath,
                                            color = DrakosRed,
                                            style = Stroke(
                                                width = 2.4.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }

                                    pointsCoords.forEachIndexed { idx, offset ->
                                        val isHighlight = idx == highlightIndex
                                        if (isHighlight) {
                                            drawLine(
                                                color = DrakosMutedGray.copy(alpha = 0.35f),
                                                start = Offset(offset.x, offset.y),
                                                end = Offset(offset.x, topPad + chartH),
                                                strokeWidth = 1.dp.toPx(),
                                                pathEffect = dashEffect
                                            )
                                            drawCircle(color = DrakosWhite, radius = 5.5.dp.toPx(), center = offset)
                                            drawCircle(color = DrakosRed, radius = 3.8.dp.toPx(), center = offset)
                                        } else if (activePoints[idx].revenueAmount > 0.0) {
                                            drawCircle(color = DrakosBlack, radius = 2.5.dp.toPx(), center = offset)
                                        }
                                    }
                                } else {
                                    // Modo Barras Duales SaaS (Negro = Ventas, Rojo = Ganancia Bruta Est.)
                                    activePoints.forEachIndexed { idx, pt ->
                                        val isSelected = idx == selectedBarIndex
                                        val revRatio = (pt.revenueAmount / maxVal).toFloat().coerceIn(0f, 1f)
                                        val revH = if (pt.revenueAmount > 0.0) (chartH * revRatio).coerceAtLeast(4.dp.toPx()) else 0f
                                        val centerX = idx * slotW + slotW / 2f

                                        if (isSelected) {
                                            drawRoundRect(
                                                color = DrakosSurfaceSoft,
                                                topLeft = Offset(idx * slotW + 2.dp.toPx(), 0f),
                                                size = Size(slotW - 4.dp.toPx(), size.height),
                                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                            )
                                        }

                                        if (showCostComparison) {
                                            val barW = (slotW * 0.28f).coerceIn(7.dp.toPx(), 13.dp.toPx())
                                            val gap = 3.dp.toPx()
                                            val profitAmount = (pt.revenueAmount - pt.costAmount).coerceAtLeast(0.0)
                                            val profitRatio = (profitAmount / maxVal).toFloat().coerceIn(0f, 1f)
                                            val profitH = if (profitAmount > 0.0) (chartH * profitRatio).coerceAtLeast(3.dp.toPx()) else 0f

                                            if (revH > 0f) {
                                                drawRoundRect(
                                                    color = DrakosBlack,
                                                    topLeft = Offset(centerX - barW - gap / 2f, topPad + chartH - revH),
                                                    size = Size(barW, revH),
                                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                                )
                                            }
                                            if (profitH > 0f) {
                                                drawRoundRect(
                                                    color = DrakosRed,
                                                    topLeft = Offset(centerX + gap / 2f, topPad + chartH - profitH),
                                                    size = Size(barW, profitH),
                                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                                )
                                            }
                                        } else {
                                            val barW = (slotW * 0.44f).coerceIn(9.dp.toPx(), 18.dp.toPx())
                                            if (revH > 0f) {
                                                drawRoundRect(
                                                    color = if (isSelected) DrakosRed else DrakosBlack,
                                                    topLeft = Offset(centerX - barW / 2f, topPad + chartH - revH),
                                                    size = Size(barW, revH),
                                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Etiquetas Eje X
                        Row(
                            modifier = Modifier
                                .width(totalCanvasWidth)
                                .height(22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activePoints.forEachIndexed { idx, pt ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pt.shortLabel,
                                        fontSize = 9.sp,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = if (idx == highlightIndex) FontWeight.Bold else FontWeight.Normal,
                                        color = if (idx == highlightIndex) DrakosRed else DrakosMutedGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartTitleBlock(
    selectedBranchName: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.BarChart,
            contentDescription = null,
            tint = DrakosRed,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = "Ventas por Período",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = DrakosDarkGray
            )
            Text(
                text = selectedBranchName,
                fontSize = 11.sp,
                color = DrakosMutedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChartHeaderControls(
    chartMode: Int,
    onSelectMode: (Int) -> Unit,
    canChangeBranchFilter: Boolean,
    selectedBranchId: Long?,
    branches: List<BranchEntity>,
    showBranchMenu: Boolean,
    onShowBranchMenuChange: (Boolean) -> Unit,
    onSelectBranchId: (Long?) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = DrakosLightCanvas,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DrakosHairline)
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                PeriodTabPill(
                    label = "14 días",
                    selected = chartMode == 0,
                    onClick = { onSelectMode(0) },
                    modifier = Modifier.testTag("chart_mode_daily")
                )
                PeriodTabPill(
                    label = "12 meses",
                    selected = chartMode == 1,
                    onClick = { onSelectMode(1) },
                    modifier = Modifier.testTag("chart_mode_monthly")
                )
            }
        }

        if (canChangeBranchFilter) {
            BranchFilterDropdownPill(
                selectedBranchId = selectedBranchId,
                branches = branches,
                expanded = showBranchMenu,
                onExpandChange = onShowBranchMenuChange,
                onSelectBranchId = onSelectBranchId
            )
        }
    }
}

@Composable
private fun PeriodSegmentedBar(
    chartMode: Int,
    dailyTotal: Double,
    monthlyTotal: Double,
    onSelectMode: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = if (chartMode == 0) DrakosRed else DrakosLightCanvas,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (chartMode == 0) DrakosRed else DrakosHairline),
            modifier = Modifier
                .weight(1f)
                .clickable { onSelectMode(0) }
                .testTag("chart_mode_daily")
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text(
                    text = "14 días",
                    fontSize = 10.sp,
                    color = if (chartMode == 0) DrakosWhite.copy(alpha = 0.85f) else DrakosMutedGray
                )
                Text(
                    text = formatCurrency(dailyTotal),
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (chartMode == 0) DrakosWhite else DrakosDarkGray
                )
            }
        }
        Surface(
            color = if (chartMode == 1) DrakosRed else DrakosLightCanvas,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (chartMode == 1) DrakosRed else DrakosHairline),
            modifier = Modifier
                .weight(1f)
                .clickable { onSelectMode(1) }
                .testTag("chart_mode_monthly")
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text(
                    text = "12 meses",
                    fontSize = 10.sp,
                    color = if (chartMode == 1) DrakosWhite.copy(alpha = 0.85f) else DrakosMutedGray
                )
                Text(
                    text = formatCurrency(monthlyTotal),
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (chartMode == 1) DrakosWhite else DrakosDarkGray
                )
            }
        }
    }
}

@Composable
private fun PeriodTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) DrakosRed else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) DrakosWhite else DrakosDarkGray,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun BranchFilterDropdownPill(
    selectedBranchId: Long?,
    branches: List<BranchEntity>,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelectBranchId: (Long?) -> Unit
) {
    Box {
        Surface(
            color = DrakosLightCanvas,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DrakosHairline),
            modifier = Modifier
                .clickable { onExpandChange(true) }
                .testTag("chart_branch_filter_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = DrakosDarkGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = selectedBranchId?.let { "K${it.toString().padStart(2, '0')}" } ?: "Todas",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DrakosDarkGray
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Filtrar sucursal", tint = DrakosMutedGray, modifier = Modifier.size(14.dp))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Todas (${branches.size} Sucursales)", fontWeight = FontWeight.Bold, color = DrakosDarkGray) },
                onClick = {
                    onSelectBranchId(null)
                    onExpandChange(false)
                }
            )
            branches.forEach { branch ->
                DropdownMenuItem(
                    text = { Text("${branch.code} - ${branch.name}", color = DrakosDarkGray) },
                    onClick = {
                        onSelectBranchId(branch.id)
                        onExpandChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DrakosDarkGray)
    }
}

private fun formatCompactArs(value: Double): String {
    return when {
        value >= 1_000_000.0 -> String.format("$%.1fM", value / 1_000_000.0)
        value >= 1_000.0 -> "$${(value / 1_000.0).toInt()}k"
        else -> "$${value.toInt()}"
    }
}
