package com.example.domain.analytics

import com.example.data.entity.BranchInventoryEntity
import com.example.data.entity.CashSessionEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ProductBatchEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.UserProfileEntity
import com.example.domain.model.BulkPricePreviewRow
import com.example.domain.model.CashierVarianceStat
import com.example.domain.model.CategoryProfitabilityStat
import com.example.domain.model.DifferenceStatus
import com.example.domain.model.ExcelStockRow
import com.example.domain.model.HourlySlotStat
import com.example.domain.model.PriceRoundingStep
import com.example.domain.model.SaleItemAnalytics
import com.example.domain.model.SalesBarPoint
import com.example.domain.model.ShiftStatus
import com.example.domain.model.StockHealthStatus
import com.example.domain.model.WeeklyOrderSuggestion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToLong

object DrakosAnalytics {
    private val localeEsAr = Locale("es", "AR")
    private const val DAY_MS = 86_400_000L

    fun buildDailySalesBars(
        sales: List<SaleEntity>,
        analytics: List<SaleItemAnalytics>,
        filterBranchId: Long?,
        days: Int = 14,
        nowMillis: Long = System.currentTimeMillis()
    ): List<SalesBarPoint> {
        val completedSales = sales.filter {
            it.status == "COMPLETED" && (filterBranchId == null || it.branchId == filterBranchId)
        }
        val filteredItems = analytics.filter { filterBranchId == null || it.branchId == filterBranchId }

        val keyFmt = SimpleDateFormat("yyyy-MM-dd", localeEsAr)
        val shortFmt = SimpleDateFormat("dd/MM", localeEsAr)
        val fullFmt = SimpleDateFormat("EEEE dd/MM/yyyy", localeEsAr)

        val revenueByDay = completedSales.groupBy { keyFmt.format(Date(it.createdAt)) }
        val costByDay = filteredItems.groupBy { keyFmt.format(Date(it.createdAt)) }

        val result = mutableListOf<SalesBarPoint>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }

        repeat(days) {
            val d = cal.time
            val key = keyFmt.format(d)
            val daySales = revenueByDay[key].orEmpty()
            val dayCosts = costByDay[key].orEmpty()
            result.add(
                SalesBarPoint(
                    key = key,
                    shortLabel = shortFmt.format(d),
                    fullLabel = fullFmt.format(d).replaceFirstChar { it.uppercase() },
                    revenueAmount = daySales.sumOf { it.totalAmount },
                    costAmount = dayCosts.sumOf { it.unitCost * it.quantity },
                    ticketCount = daySales.size
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    fun buildMonthlySalesBars(
        sales: List<SaleEntity>,
        analytics: List<SaleItemAnalytics>,
        filterBranchId: Long?,
        months: Int = 12,
        nowMillis: Long = System.currentTimeMillis()
    ): List<SalesBarPoint> {
        val completedSales = sales.filter {
            it.status == "COMPLETED" && (filterBranchId == null || it.branchId == filterBranchId)
        }
        val filteredItems = analytics.filter { filterBranchId == null || it.branchId == filterBranchId }

        val keyFmt = SimpleDateFormat("yyyy-MM", localeEsAr)
        val shortFmt = SimpleDateFormat("MMM yy", localeEsAr)
        val fullFmt = SimpleDateFormat("MMMM yyyy", localeEsAr)

        val revenueByMonth = completedSales.groupBy { keyFmt.format(Date(it.createdAt)) }
        val costByMonth = filteredItems.groupBy { keyFmt.format(Date(it.createdAt)) }

        val result = mutableListOf<SalesBarPoint>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 12)
            add(Calendar.MONTH, -(months - 1))
        }

        repeat(months) {
            val d = cal.time
            val key = keyFmt.format(d)
            val monthSales = revenueByMonth[key].orEmpty()
            val monthCosts = costByMonth[key].orEmpty()
            result.add(
                SalesBarPoint(
                    key = key,
                    shortLabel = shortFmt.format(d).replace(".", "").replaceFirstChar { it.uppercase() },
                    fullLabel = fullFmt.format(d).replaceFirstChar { it.uppercase() },
                    revenueAmount = monthSales.sumOf { it.totalAmount },
                    costAmount = monthCosts.sumOf { it.unitCost * it.quantity },
                    ticketCount = monthSales.size
                )
            )
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }

    fun buildExcelStockRows(
        products: List<ProductEntity>,
        categories: List<CategoryEntity>,
        allInventory: List<BranchInventoryEntity>,
        allBatches: List<ProductBatchEntity>,
        analytics: List<SaleItemAnalytics>,
        branchId: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): List<ExcelStockRow> {
        val catMap = categories.associate { it.id to it.name }
        val stockMap = allInventory.filter { it.branchId == branchId }.associateBy { it.productId }
        val branchBatches = allBatches.filter { it.branchId == branchId && it.remainingQuantity > 0 }
            .groupBy { it.productId }

        val since7d = nowMillis - 7L * DAY_MS
        val sold7dByProduct = analytics
            .filter { it.branchId == branchId && it.createdAt >= since7d }
            .groupBy { it.productId }
            .mapValues { (_, list) -> list.sumOf { it.quantity } }

        return products.map { prod ->
            val onHand = stockMap[prod.id]?.quantityOnHand ?: 0
            val sold7d = sold7dByProduct[prod.id] ?: 0
            val batches = branchBatches[prod.id].orEmpty()
            val nearestExpiryMillis = batches.minOfOrNull { it.expiryDateMillis }
            val nearestDays = nearestExpiryMillis?.let { ((it - nowMillis) / DAY_MS).toInt() }

            val expiryLabel = when {
                nearestDays == null -> "Sin lote"
                nearestDays <= 0 -> "VENCIDO (${nearestDays}d)"
                else -> "${nearestDays}d"
            }

            val health = when {
                onHand <= 0 || (nearestDays != null && nearestDays <= 7) -> StockHealthStatus.CRITICAL
                onHand <= prod.minStock -> StockHealthStatus.LOW
                nearestDays != null && nearestDays <= 20 -> StockHealthStatus.EXPIRING_SOON
                else -> StockHealthStatus.OPTIMAL
            }

            val targetLevel = if (sold7d > 0) sold7d + prod.minStock else (prod.minStock * 2)
            val suggested = (targetLevel - onHand).coerceAtLeast(0)

            ExcelStockRow(
                productId = prod.id,
                sku = prod.sku,
                barcode = prod.barcode,
                name = prod.name,
                categoryName = catMap[prod.categoryId] ?: "General",
                quantityOnHand = onHand,
                minStock = prod.minStock,
                costPrice = prod.costPrice,
                sellingPrice = prod.sellingPrice,
                weeklySoldQty = sold7d,
                suggestedWeeklyOrder = suggested,
                nearestExpiryDays = nearestDays,
                expiryLabel = expiryLabel,
                healthStatus = health
            )
        }
    }

    fun buildWeeklyOrderSuggestions(
        products: List<ProductEntity>,
        categories: List<CategoryEntity>,
        allInventory: List<BranchInventoryEntity>,
        allBatches: List<ProductBatchEntity>,
        analytics: List<SaleItemAnalytics>,
        branchId: Long,
        coverageDays: Int = 7,
        nowMillis: Long = System.currentTimeMillis()
    ): List<WeeklyOrderSuggestion> {
        val catMap = categories.associate { it.id to it.name }
        val stockMap = allInventory.filter { it.branchId == branchId }.associate { it.productId to it.quantityOnHand }
        val branchBatches = allBatches.filter { it.branchId == branchId && it.remainingQuantity > 0 }
            .groupBy { it.productId }

        val since7d = nowMillis - 7L * DAY_MS
        val since30d = nowMillis - 30L * DAY_MS

        val branchAnalytics = analytics.filter { it.branchId == branchId }
        val sold7dMap = branchAnalytics.filter { it.createdAt >= since7d }
            .groupBy { it.productId }
            .mapValues { (_, l) -> l.sumOf { it.quantity } }
        val sold30dMap = branchAnalytics.filter { it.createdAt >= since30d }
            .groupBy { it.productId }
            .mapValues { (_, l) -> l.sumOf { it.quantity } }

        return products.filter { it.id != 22L }.map { prod -> // Excluir recarga virtual SUBE del pedido físico
            val onHand = stockMap[prod.id] ?: 0
            val sold7d = sold7dMap[prod.id] ?: 0
            val sold30d = sold30dMap[prod.id] ?: 0
            val dailyVelocity = when {
                sold7d > 0 -> sold7d.toDouble() / 7.0
                sold30d > 0 -> sold30d.toDouble() / 30.0
                else -> 0.0
            }
            val projectedDemand = ceil(dailyVelocity * coverageDays).toInt()
            val (targetQty, basis) = if (dailyVelocity > 0.0) {
                (projectedDemand + prod.minStock) to "Estadística ventas (${sold7d}u/7d)"
            } else {
                (prod.minStock * 2) to "Mínimo operativo (${prod.minStock}u)"
            }
            val suggested = (targetQty - onHand).coerceAtLeast(0)
            val nearestDays = branchBatches[prod.id]
                ?.minOfOrNull { it.expiryDateMillis }
                ?.let { ((it - nowMillis) / DAY_MS).toInt() }

            WeeklyOrderSuggestion(
                productId = prod.id,
                sku = prod.sku,
                barcode = prod.barcode,
                name = prod.name,
                categoryName = catMap[prod.categoryId] ?: "General",
                currentStock = onHand,
                minStock = prod.minStock,
                soldLast7Days = sold7d,
                soldLast30Days = sold30d,
                suggestedQty = suggested,
                unitCost = prod.costPrice,
                nearestExpiryDays = nearestDays,
                basisLabel = basis
            )
        }.sortedWith(compareByDescending<WeeklyOrderSuggestion> { it.suggestedQty > 0 }.thenByDescending { it.suggestedQty })
    }

    fun roundPriceToStep(rawPrice: Double, step: PriceRoundingStep): Double {
        if (rawPrice <= 0.0) return 0.0
        val unit = step.step.toDouble()
        return if (unit <= 1.0) {
            rawPrice.roundToLong().toDouble()
        } else {
            (kotlin.math.round(rawPrice / unit) * unit).coerceAtLeast(unit)
        }
    }

    fun buildBulkPricePreview(
        products: List<ProductEntity>,
        categoryId: Long?,
        costPercentDelta: Double,
        pricePercentDelta: Double,
        preserveMargin: Boolean,
        roundingStep: PriceRoundingStep
    ): List<BulkPricePreviewRow> {
        return products
            .filter { it.isActive && (categoryId == null || it.categoryId == categoryId) }
            .map { prod ->
                val newCost = (prod.costPrice * (1.0 + costPercentDelta / 100.0)).coerceAtLeast(1.0)
                val rawNewSelling = if (preserveMargin && prod.costPrice > 0.0) {
                    val currentMarkupRatio = prod.sellingPrice / prod.costPrice
                    newCost * currentMarkupRatio
                } else {
                    prod.sellingPrice * (1.0 + pricePercentDelta / 100.0)
                }
                val roundedSelling = roundPriceToStep(rawNewSelling.coerceAtLeast(10.0), roundingStep)
                val marginPct = if (roundedSelling > 0.0) {
                    ((roundedSelling - newCost) / roundedSelling) * 100.0
                } else {
                    0.0
                }
                BulkPricePreviewRow(
                    productId = prod.id,
                    name = prod.name,
                    oldCostPrice = prod.costPrice,
                    newCostPrice = newCost.roundToLong().toDouble(),
                    oldSellingPrice = prod.sellingPrice,
                    newSellingPrice = roundedSelling,
                    newMarginPercent = marginPct
                )
            }
    }

    fun buildHourlyHeatmap(
        sales: List<SaleEntity>,
        filterBranchId: Long?
    ): List<HourlySlotStat> {
        val completed = sales.filter {
            it.status == "COMPLETED" && (filterBranchId == null || it.branchId == filterBranchId)
        }
        val slotDefs = listOf(
            Triple("00-06h", 0, 5),
            Triple("06-10h", 6, 9),
            Triple("10-14h", 10, 13),
            Triple("14-18h", 14, 17),
            Triple("18-22h", 18, 21),
            Triple("22-24h", 22, 23)
        )
        val cal = Calendar.getInstance()
        val grouped = completed.groupBy { sale ->
            cal.timeInMillis = sale.createdAt
            val h = cal.get(Calendar.HOUR_OF_DAY)
            slotDefs.indexOfFirst { (_, start, end) -> h in start..end }.coerceAtLeast(0)
        }
        val base = slotDefs.mapIndexed { idx, (label, start, end) ->
            val list = grouped[idx].orEmpty()
            HourlySlotStat(
                slotLabel = label,
                startHour = start,
                endHour = end,
                revenueAmount = list.sumOf { it.totalAmount },
                ticketCount = list.size,
                isPeak = false
            )
        }
        val maxRev = base.maxOfOrNull { it.revenueAmount } ?: 0.0
        return if (maxRev <= 0.0) {
            base
        } else {
            val peakIdx = base.indexOfFirst { it.revenueAmount == maxRev }
            base.mapIndexed { idx, item -> item.copy(isPeak = idx == peakIdx) }
        }
    }

    fun buildCategoryProfitability(
        analytics: List<SaleItemAnalytics>,
        products: List<ProductEntity>,
        categories: List<CategoryEntity>,
        sales: List<SaleEntity>,
        filterBranchId: Long?
    ): List<CategoryProfitabilityStat> {
        val prodMap = products.associateBy { it.id }
        val catMap = categories.associateBy { it.id }
        val filtered = analytics.filter { filterBranchId == null || it.branchId == filterBranchId }
        val byCat = filtered.groupBy { item -> prodMap[item.productId]?.categoryId ?: 0L }

        val rawRows = byCat.map { (catId, items) ->
            val units = items.sumOf { it.quantity }
            val revenue = items.sumOf { it.subtotal }
            val cost = items.sumOf { it.unitCost * it.quantity }
            val profit = revenue - cost
            val marginPct = if (revenue > 0.0) (profit / revenue) * 100.0 else 0.0
            val tag = when {
                marginPct >= 35.0 -> "Estrella Alto Margen"
                marginPct < 20.0 && revenue > 0.0 -> "Volumen / Tráfico"
                else -> "Equilibrado"
            }
            CategoryProfitabilityStat(
                categoryId = catId,
                categoryName = catMap[catId]?.name ?: "Otros",
                unitsSold = units,
                revenueAmount = revenue,
                costAmount = cost,
                grossProfit = profit,
                marginPercent = marginPct,
                shareOfProfitPercent = 0.0,
                abcClass = "C",
                strategicTag = tag
            )
        }.sortedByDescending { it.grossProfit }

        val totalPositiveProfit = rawRows.sumOf { it.grossProfit.coerceAtLeast(0.0) }.coerceAtLeast(1.0)
        var cumulativePct = 0.0
        return rawRows.map { row ->
            val sharePct = (row.grossProfit.coerceAtLeast(0.0) / totalPositiveProfit) * 100.0
            val prevCum = cumulativePct
            cumulativePct += sharePct
            val abc = when {
                prevCum < 70.0 -> "A"
                prevCum < 90.0 -> "B"
                else -> "C"
            }
            row.copy(shareOfProfitPercent = sharePct, abcClass = abc)
        }
    }

    fun buildCashierVarianceSummary(
        sessions: List<CashSessionEntity>,
        users: List<UserProfileEntity>,
        filterBranchId: Long?
    ): List<CashierVarianceStat> {
        val userMap = users.associate { it.id to it.fullName }
        return sessions
            .filter { it.status == ShiftStatus.CLOSED && (filterBranchId == null || it.branchId == filterBranchId) }
            .groupBy { it.userId }
            .map { (uid, list) ->
                CashierVarianceStat(
                    userId = uid,
                    cashierName = userMap[uid] ?: "Cajero #$uid",
                    closedSessionsCount = list.size,
                    totalSurplus = list.filter { it.cashDifference > 0.0 }.sumOf { it.cashDifference },
                    totalShortage = list.filter { it.cashDifference < 0.0 }.sumOf { it.cashDifference },
                    netDifference = list.sumOf { it.cashDifference },
                    pendingApprovals = list.count { it.differenceStatus == DifferenceStatus.PENDING_APPROVAL }
                )
            }
            .sortedBy { it.netDifference }
    }
}

