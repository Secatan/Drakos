package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BranchEntity
import com.example.domain.model.SalesBarPoint
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceTint
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen

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
    var chartMode by remember { mutableIntStateOf(0) } // 0: Diario (14d), 1: Mensual (12m)
    var selectedBarIndex by remember(chartMode, selectedBranchId) { mutableIntStateOf(-1) }
    var showBranchMenu by remember { mutableStateOf(false) }

    val activePoints = if (chartMode == 0) dailyPoints else monthlyPoints
    val maxVal = remember(activePoints) {
        activePoints.maxOfOrNull { it.revenueAmount }?.takeIf { it > 0.0 } ?: 1000.0
    }
    val periodRevenue = remember(activePoints) { activePoints.sumOf { it.revenueAmount } }
    val periodTickets = remember(activePoints) { activePoints.sumOf { it.ticketCount } }
    val selectedPoint = activePoints.getOrNull(selectedBarIndex)
    val selectedBranchName = if (selectedBranchId == null) {
        "Todas las Sucursales (10)"
    } else {
        branches.find { it.id == selectedBranchId }?.name ?: "Sucursal K0$selectedBranchId"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Ventas Diarias y Mensuales",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DrakosBlack
                        )
                        Text(
                            text = selectedBranchName,
                            fontSize = 11.sp,
                            color = DrakosMutedInk
                        )
                    }
                }

                if (canChangeBranchFilter) {
                    Box {
                        OutlinedButton(
                            onClick = { showBranchMenu = true },
                            border = BorderStroke(1.dp, DrakosBlack),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("chart_branch_filter_button")
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = DrakosBlack, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedBranchId?.let { "K${it.toString().padStart(2, '0')}" } ?: "Todas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrakosBlack
                            )
                        }
                        DropdownMenu(
                            expanded = showBranchMenu,
                            onDismissRequest = { showBranchMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todas (10 Sucursales)", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    onSelectBranchId(null)
                                    showBranchMenu = false
                                }
                            )
                            branches.forEach { branch ->
                                DropdownMenuItem(
                                    text = { Text("${branch.code} - ${branch.name}") },
                                    onClick = {
                                        onSelectBranchId(branch.id)
                                        showBranchMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selector Diario vs Mensual + Totales del período
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DrakosBlack,
                        selectedLabelColor = DrakosWhite
                    )
                    FilterChip(
                        selected = chartMode == 0,
                        onClick = { chartMode = 0 },
                        colors = chipColors,
                        label = { Text("Diario (14d)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("chart_mode_daily")
                    )
                    FilterChip(
                        selected = chartMode == 1,
                        onClick = { chartMode = 1 },
                        colors = chipColors,
                        label = { Text("Mensual (12m)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("chart_mode_monthly")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(periodRevenue),
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = DrakosRed
                    )
                    Text(
                        text = "$periodTickets tickets válidos",
                        fontSize = 10.sp,
                        color = DrakosMutedInk
                    )
                }
            }

            // Tooltip interactivo al tocar una barra
            if (selectedPoint != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = DrakosBlack,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedPoint.fullLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrakosWhite
                            )
                            Text(
                                text = "${selectedPoint.ticketCount} operaciones",
                                fontSize = 10.sp,
                                color = Color(0xFFD4D4D8)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Venta: ${formatCurrency(selectedPoint.revenueAmount)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrakosWhite
                            )
                            if (showCostComparison) {
                                Text(
                                    text = "CMV: ${formatCurrency(selectedPoint.costAmount)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activePoints.all { it.revenueAmount <= 0.0 }) {
                Surface(
                    color = DrakosSurfaceTint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sin ventas registradas para este filtro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = DrakosBlack
                        )
                        Text(
                            text = "Realice una venta desde el POS o cambie a 'Todas las Sucursales' para visualizar el gráfico de barras.",
                            fontSize = 11.sp,
                            color = DrakosMutedInk
                        )
                    }
                }
            } else {
                // Gráfico de Barras Nativo estilo Recharts (CartesianGrid + Eje Y + Eje X + Barras interactivas)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    // Eje Y fijo a la izquierda
                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .height(162.dp)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(formatCompactArs(maxVal), fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedInk)
                        Text(formatCompactArs(maxVal * 0.66), fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedInk)
                        Text(formatCompactArs(maxVal * 0.33), fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedInk)
                        Text("$0", fontSize = 9.sp, fontFamily = JetBrainsMonoFontFamily, color = DrakosMutedInk)
                    }

                    // Área deslizable de barras
                    val scrollState = rememberScrollState(initial = Int.MAX_VALUE)
                    val barStepDp = 38.dp
                    val totalCanvasWidth = (barStepDp * activePoints.size).coerceAtLeast(280.dp)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(scrollState)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(totalCanvasWidth)
                                .height(162.dp)
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
                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                val gridColor = Color(0xFFE4E4E7)
                                val topPad = 8.dp.toPx()
                                val chartH = size.height - topPad

                                // Líneas de grilla CartesianGrid (0%, 33%, 66%, 100%)
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
                                val primaryBarW = (slotW * 0.50f).coerceIn(12.dp.toPx(), 24.dp.toPx())

                                activePoints.forEachIndexed { idx, pt ->
                                    val isSelected = idx == selectedBarIndex
                                    val barRatio = (pt.revenueAmount / maxVal).toFloat().coerceIn(0f, 1f)
                                    val barH = if (pt.revenueAmount > 0.0) (chartH * barRatio).coerceAtLeast(4.dp.toPx()) else 0f
                                    val centerX = idx * slotW + slotW / 2f

                                    if (isSelected) {
                                        drawRoundRect(
                                            color = DrakosRedContainer.copy(alpha = 0.7f),
                                            topLeft = Offset(idx * slotW + 2.dp.toPx(), 0f),
                                            size = Size(slotW - 4.dp.toPx(), size.height),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                        )
                                    }

                                    if (barH > 0f) {
                                        drawRoundRect(
                                            color = if (isSelected) DrakosBlack else DrakosRed,
                                            topLeft = Offset(centerX - primaryBarW / 2f, topPad + chartH - barH),
                                            size = Size(primaryBarW, barH),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                    }

                                    if (showCostComparison && pt.costAmount > 0.0) {
                                        val costRatio = (pt.costAmount / maxVal).toFloat().coerceIn(0f, 1f)
                                        val costH = (chartH * costRatio).coerceAtLeast(3.dp.toPx())
                                        val costW = primaryBarW * 0.42f
                                        drawRoundRect(
                                            color = Color(0xFF18181B),
                                            topLeft = Offset(centerX + primaryBarW / 2f - costW, topPad + chartH - costH),
                                            size = Size(costW, costH),
                                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }

                        // Etiquetas Eje X
                        Row(
                            modifier = Modifier
                                .width(totalCanvasWidth)
                                .height(24.dp),
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
                                        fontWeight = if (idx == selectedBarIndex) FontWeight.Bold else FontWeight.Normal,
                                        color = if (idx == selectedBarIndex) DrakosRed else DrakosMutedInk
                                    )
                                }
                            }
                        }
                    }
                }

                // Leyenda inferior
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = DrakosRed, label = "Facturación")
                    if (showCostComparison) {
                        LegendDot(color = DrakosBlack, label = "CMV (Costo)")
                    }
                    Text(
                        text = "Toque una barra para ver detalle",
                        fontSize = 10.sp,
                        color = DrakosMutedInk
                    )
                }
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
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DrakosBlack)
    }
}

private fun formatCompactArs(value: Double): String {
    return when {
        value >= 1_000_000.0 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000.0 -> "$${(value / 1_000.0).toInt()}k"
        else -> "$${value.toInt()}"
    }
}
