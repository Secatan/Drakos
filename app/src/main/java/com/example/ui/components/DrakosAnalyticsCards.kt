package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CategoryProfitabilityStat
import com.example.domain.model.HourlySlotStat
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
import com.example.ui.theme.WarningAmber

@Composable
fun HourlyHeatmapCard(
    slots: List<HourlySlotStat>,
    modifier: Modifier = Modifier
) {
    val maxRevenue = (slots.maxOfOrNull { it.revenueAmount } ?: 1.0).coerceAtLeast(1.0)
    val peakSlot = slots.find { it.isPeak }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_heatmap_card"),
        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
        border = BorderStroke(1.dp, DrakosBorder),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mapa de Calor 24h por Franja",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DrakosBlack
                    )
                }
                if (peakSlot != null && peakSlot.revenueAmount > 0.0) {
                    Surface(color = DrakosRedContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "PICO ${peakSlot.slotLabel}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DrakosRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (slots.all { it.ticketCount == 0 }) {
                Text(
                    text = "Sin ventas registradas aún para calcular distribución horaria.",
                    fontSize = 11.sp,
                    color = DrakosMutedInk
                )
            } else {
                slots.forEach { slot ->
                    val fraction = (slot.revenueAmount / maxRevenue).toFloat().coerceIn(0f, 1f)
                    val barColor = when {
                        slot.isPeak -> DrakosRed
                        fraction >= 0.55f -> DrakosBlack
                        else -> Color(0xFF9CA3AF)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${slot.slotLabel} (${slot.ticketCount} tk)",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = if (slot.isPeak) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = DrakosBlack
                            )
                            Text(
                                text = formatCurrency(slot.revenueAmount),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (slot.isPeak) DrakosRed else DrakosBlack
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DrakosSurfaceTint)
                        ) {
                            if (fraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction.coerceAtLeast(0.04f))
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(barColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryProfitabilityCard(
    categoriesStats: List<CategoryProfitabilityStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_profitability_abc_card"),
        colors = CardDefaults.cardColors(containerColor = DrakosWhite),
        border = BorderStroke(1.dp, DrakosBorder),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = DrakosBlack, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rentabilidad por Rubro (Matriz ABC)",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DrakosBlack
                    )
                }
            }

            if (categoriesStats.isEmpty()) {
                Text(
                    text = "Aún no hay ítems vendidos para clasificar los rubros en matriz ABC.",
                    fontSize = 11.sp,
                    color = DrakosMutedInk
                )
            } else {
                categoriesStats.forEach { stat ->
                    val abcColor = when (stat.abcClass) {
                        "A" -> SuccessGreen
                        "B" -> WarningAmber
                        else -> DrakosMutedInk
                    }
                    Surface(
                        color = DrakosSurfaceTint,
                        border = BorderStroke(1.dp, DrakosBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(color = abcColor.copy(alpha = 0.16f), shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "CLASE ${stat.abcClass}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = abcColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stat.categoryName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = DrakosBlack
                                    )
                                }
                                Text(
                                    text = "+${formatCurrency(stat.grossProfit)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SuccessGreen
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stat.unitsSold} u. • Margen: ${String.format("%.1f", stat.marginPercent)}% (${String.format("%.0f", stat.shareOfProfitPercent)}% utilidad)",
                                    fontSize = 10.sp,
                                    color = DrakosMutedInk
                                )
                                Surface(
                                    color = if (stat.strategicTag.contains("Estrella")) DrakosRedContainer else DrakosWhite,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stat.strategicTag,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (stat.strategicTag.contains("Estrella")) DrakosRed else DrakosBlack,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
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
