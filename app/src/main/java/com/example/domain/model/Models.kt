package com.example.domain.model

enum class UserRole(val label: String, val level: Int) {
    EMPLOYEE("Empleado (Nivel 1)", 1),
    BRANCH_MANAGER("Encargado de Sucursal (Nivel 2)", 2),
    OWNER("Propietario / Superadmin (Nivel 3)", 3)
}

enum class MovementType(val label: String) {
    SALE("Venta POS"),
    PURCHASE_RECEIPT("Recepción de Compra"),
    ADJUSTMENT("Ajuste de Inventario"),
    LOSS("Merma / Rotura"),
    EXPIRY("Producto Vencido"),
    TRANSFER_OUT("Transferencia Saliente"),
    TRANSFER_IN("Transferencia Entrante")
}

enum class PaymentMethod(val label: String) {
    CASH("Efectivo"),
    DEBIT("Tarjeta Débito"),
    CREDIT("Tarjeta Crédito"),
    MP_QR("Mercado Pago QR / MODO"),
    TRANSFER("Transferencia Bancaria"),
    ACCOUNT("Cuenta Corriente (Fiado)")
}

enum class TransferStatus(val label: String) {
    REQUESTED("Solicitada"),
    DISPATCHED("Despachada (En Tránsito)"),
    RECEIVED("Recibida"),
    CANCELLED("Cancelada")
}

enum class ShiftStatus(val label: String) {
    OPEN("Turno Abierto"),
    CLOSED("Turno Cerrado")
}

enum class DifferenceStatus(val label: String) {
    NORMAL("Normal"),
    PENDING_APPROVAL("Requiere Aprobación"),
    APPROVED("Aprobado")
}

enum class ExpenseCategory(val label: String) {
    ALQUILER("Alquiler del Kiosco"),
    SERVICIOS("Luz / Agua / Internet"),
    SUELDOS_ADELANTO("Adelanto de Sueldo"),
    FLETE("Flete y Logística"),
    MANTENIMIENTO("Mantenimiento y Reparación"),
    VARIOS("Gastos Varios / Limpieza")
}

data class CartItem(
    val productId: Long,
    val barcode: String,
    val name: String,
    val unitPrice: Double,
    val unitCost: Double,
    val quantity: Int,
    val stockAvailable: Int
) {
    val subtotal: Double get() = unitPrice * quantity
}

data class BranchSalesSummary(
    val branchId: Long,
    val branchName: String,
    val totalSalesAmount: Double,
    val totalTransactions: Int,
    val cashDifferenceSum: Double,
    val activeSessionUser: String?
)

data class ProductWithStock(
    val productId: Long,
    val barcode: String,
    val sku: String,
    val name: String,
    val description: String,
    val categoryName: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val vatRate: Double,
    val quantityOnHand: Int,
    val minStock: Int,
    val isActive: Boolean
)

data class KioscoPromo(
    val id: String,
    val title: String,
    val badge: String,
    val description: String,
    val discountPercent: Double,
    val items: List<CartItem>
)

data class PromoComponentItem(
    val productId: Long,
    val barcode: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double,
    val stockAvailable: Int
) {
    val regularSubtotal: Double get() = unitPrice * quantity
}

data class PromotionDisplay(
    val id: Long,
    val title: String,
    val badge: String,
    val description: String,
    val discountPercent: Double,
    val fixedPromoPrice: Double?,
    val validSchedule: String,
    val isActive: Boolean,
    val items: List<PromoComponentItem>,
    val regularPrice: Double,
    val finalPromoPrice: Double,
    val savingsAmount: Double,
    val hasSufficientStock: Boolean
)

data class BatchExpiryItem(
    val batchId: Long = 0L,
    val productId: Long,
    val barcode: String,
    val name: String,
    val categoryName: String,
    val batchNumber: String,
    val quantity: Int,
    val daysUntilExpiry: Int,
    val expiryDateFormatted: String,
    val isCritical: Boolean
)

enum class DeviceStatus(val label: String) {
    AUTHORIZED("Autorizado"),
    PENDING("Pendiente Nivel 3"),
    REVOKED("Bloqueado")
}

enum class InvoiceType(val label: String) {
    FACTURA_A("Factura A"),
    FACTURA_B("Factura B"),
    FACTURA_C("Factura C"),
    REMITO_X("Remito X")
}

enum class InvoicePaymentStatus(val label: String) {
    PENDING("Pendiente de Pago"),
    PAID("Pagada"),
    CREDIT_ACCOUNT("Cuenta Corriente")
}

enum class MessagePriority(val label: String) {
    NORMAL("General"),
    STOCK_REQUEST("Consulta de Stock"),
    URGENT("Urgente")
}

enum class WeeklyOrderStatus(val label: String) {
    DRAFT("Borrador"),
    SENT("Enviado a Proveedor"),
    RECEIVED("Recibido en Local")
}

enum class StockHealthStatus(val label: String) {
    CRITICAL("CRÍTICO"),
    LOW("BAJO MÍN."),
    EXPIRING_SOON("PRÓX. VENC."),
    OPTIMAL("ÓPTIMO")
}

data class SalesBarPoint(
    val key: String,
    val shortLabel: String,
    val fullLabel: String,
    val revenueAmount: Double,
    val costAmount: Double,
    val ticketCount: Int
)

data class ExcelStockRow(
    val productId: Long,
    val sku: String,
    val barcode: String,
    val name: String,
    val categoryName: String,
    val quantityOnHand: Int,
    val minStock: Int,
    val costPrice: Double,
    val sellingPrice: Double,
    val weeklySoldQty: Int,
    val suggestedWeeklyOrder: Int,
    val nearestExpiryDays: Int?,
    val expiryLabel: String,
    val healthStatus: StockHealthStatus
)

data class WeeklyOrderSuggestion(
    val productId: Long,
    val sku: String,
    val barcode: String,
    val name: String,
    val categoryName: String,
    val currentStock: Int,
    val minStock: Int,
    val soldLast7Days: Int,
    val soldLast30Days: Int,
    val suggestedQty: Int,
    val unitCost: Double,
    val nearestExpiryDays: Int?,
    val basisLabel: String
)

data class SoldProductStat(
    val productId: Long,
    val totalQty: Int,
    val totalRevenue: Double,
    val totalCost: Double
)

data class SaleItemAnalytics(
    val branchId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double,
    val subtotal: Double,
    val createdAt: Long
)

enum class PriceRoundingStep(val step: Int, val label: String) {
    NONE(1, "Exacto ($1)"),
    TEN(10, "Múltiplo $10"),
    FIFTY(50, "Múltiplo $50"),
    HUNDRED(100, "Múltiplo $100")
}

data class BulkPricePreviewRow(
    val productId: Long,
    val name: String,
    val oldCostPrice: Double,
    val newCostPrice: Double,
    val oldSellingPrice: Double,
    val newSellingPrice: Double,
    val newMarginPercent: Double
)

data class HourlySlotStat(
    val slotLabel: String,
    val startHour: Int,
    val endHour: Int,
    val revenueAmount: Double,
    val ticketCount: Int,
    val isPeak: Boolean
)

data class CategoryProfitabilityStat(
    val categoryId: Long,
    val categoryName: String,
    val unitsSold: Int,
    val revenueAmount: Double,
    val costAmount: Double,
    val grossProfit: Double,
    val marginPercent: Double,
    val shareOfProfitPercent: Double,
    val abcClass: String,
    val strategicTag: String
)

data class CashierVarianceStat(
    val userId: Long,
    val cashierName: String,
    val closedSessionsCount: Int,
    val totalSurplus: Double,
    val totalShortage: Double,
    val netDifference: Double,
    val pendingApprovals: Int
)


