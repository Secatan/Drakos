package com.example.util

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.text.TextUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.entity.BranchEntity
import com.example.data.entity.CashSessionEntity
import com.example.data.entity.CompanySettingsEntity
import com.example.data.entity.SaleEntity
import com.example.domain.model.ShiftStatus
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime

object ThermalPrinterHelper {

    fun printSaleTicket(
        context: Context,
        sale: SaleEntity,
        branch: BranchEntity?,
        cashierName: String,
        settings: CompanySettingsEntity
    ) {
        val jobName = "Ticket_${sale.receiptNumber}"
        val html = buildString {
            append("<html><body style='font-family:monospace;font-size:11px;width:280px;margin:0 auto;padding:8px;'>")
            append("<h3 style='text-align:center;margin:2px 0;'>${escape(settings.businessName.uppercase())}</h3>")
            append("<div style='text-align:center;font-size:10px;'>CUIT: ${escape(settings.cuit)}</div>")
            append("<div style='text-align:center;font-size:10px;'>${escape(settings.taxCondition)}</div>")
            append("<div style='text-align:center;font-size:10px;margin-top:4px;'><b>${escape(branch?.name ?: "Sucursal Kiosco")}</b></div>")
            append("<div style='text-align:center;font-size:9px;'>${escape(branch?.address ?: "")}</div>")
            append("<hr style='border:1px dashed #333;'/>")
            append("<div>Ticket Nro: <b>${escape(sale.receiptNumber)}</b></div>")
            append("<div>Fecha: ${escape(formatDateTime(sale.createdAt))}</div>")
            append("<div>Cajero: ${escape(cashierName)}</div>")
            append("<div>Pago: <b>${escape(sale.paymentMethod.label)}</b></div>")
            append("<hr style='border:1px dashed #333;'/>")
            append("<table style='width:100%;font-size:11px;'>")
            append("<tr><td>Subtotal</td><td style='text-align:right;'>${escape(formatCurrency(sale.subtotal))}</td></tr>")
            if (sale.discountAmount > 0) {
                append("<tr><td>Descuento</td><td style='text-align:right;'>-${escape(formatCurrency(sale.discountAmount))}</td></tr>")
            }
            append("<tr><td>IVA (${settings.defaultVatRate.toInt()}% incl.)</td><td style='text-align:right;'>${escape(formatCurrency(sale.vatAmount))}</td></tr>")
            append("<tr><td><b>TOTAL</b></td><td style='text-align:right;font-size:13px;'><b>${escape(formatCurrency(sale.totalAmount))}</b></td></tr>")
            append("</table>")
            append("<hr style='border:1px dashed #333;'/>")
            append("<div style='text-align:center;font-size:9px;'>*** COMPROBANTE DE CONTROL INTERNO ***</div>")
            append("</body></html>")
        }
        printHtmlDocument(context, jobName, html)
    }

    fun printClosingZReport(
        context: Context,
        session: CashSessionEntity,
        branchName: String,
        settings: CompanySettingsEntity
    ) {
        val jobName = "CierreZ_Turno_${session.id}"
        val isClosed = session.status == ShiftStatus.CLOSED
        val vatDivisor = 1.0 + (settings.defaultVatRate / 100.0)
        val baseImponible = session.expectedCashAmount / vatDivisor
        val ivaCalculado = session.expectedCashAmount - baseImponible

        val html = buildString {
            append("<html><body style='font-family:monospace;font-size:11px;width:280px;margin:0 auto;padding:8px;'>")
            append("<h3 style='text-align:center;margin:2px 0;'>${escape(settings.businessName.uppercase())}</h3>")
            append("<div style='text-align:center;font-size:10px;'>CUIT: ${escape(settings.cuit)} • ${escape(settings.taxCondition)}</div>")
            append("<div style='text-align:center;margin:6px 0;'><b>*** CIERRE Z - TURNO #${session.id} ***</b></div>")
            append("<hr style='border:1px dashed #333;'/>")
            append("<div>Sucursal: <b>${escape(branchName)}</b></div>")
            append("<div>Estado: ${if (isClosed) "CERRADO" else "EN CURSO"}</div>")
            append("<div>Apertura: ${escape(formatDateTime(session.openedAt))}</div>")
            if (session.closedAt != null) {
                append("<div>Cierre: ${escape(formatDateTime(session.closedAt))}</div>")
            }
            append("<hr style='border:1px dashed #333;'/>")
            append("<table style='width:100%;font-size:11px;'>")
            append("<tr><td>Fondo Inicial</td><td style='text-align:right;'>${escape(formatCurrency(session.openingAmount))}</td></tr>")
            append("<tr><td>Efectivo Esperado</td><td style='text-align:right;'>${escape(formatCurrency(session.expectedCashAmount))}</td></tr>")
            append("<tr><td>Efectivo Declarado</td><td style='text-align:right;'><b>${escape(formatCurrency(session.countedCashAmount))}</b></td></tr>")
            append("<tr><td>Diferencia</td><td style='text-align:right;'><b>${escape(formatCurrency(session.cashDifference))}</b></td></tr>")
            append("</table>")
            append("<div>Auditoria: ${escape(session.differenceStatus.label)}</div>")
            if (session.notes.isNotBlank()) {
                append("<div>Obs: ${escape(session.notes)}</div>")
            }
            append("<hr style='border:1px dashed #333;'/>")
            append("<table style='width:100%;font-size:10px;'>")
            append("<tr><td>Base Imponible</td><td style='text-align:right;'>${escape(formatCurrency(baseImponible))}</td></tr>")
            append("<tr><td>IVA ${settings.defaultVatRate.toInt()}%</td><td style='text-align:right;'>${escape(formatCurrency(ivaCalculado))}</td></tr>")
            append("</table>")
            append("<div style='text-align:center;font-size:9px;margin-top:8px;'>COMPROBANTE FISCAL 80MM POS</div>")
            append("</body></html>")
        }
        printHtmlDocument(context, jobName, html)
    }

    private fun printHtmlDocument(context: Context, jobName: String, htmlContent: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val mainHandler = Handler(Looper.getMainLooper())
        val webView = WebView(context)
        var cleanedUp = false
        var printDispatched = false

        val cleanup = Runnable {
            if (!cleanedUp) {
                cleanedUp = true
                webView.webViewClient = WebViewClient()
                webView.stopLoading()
                webView.destroy()
            }
        }
        mainHandler.postDelayed(cleanup, 120_000L)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                if (cleanedUp || printDispatched || view == null) return
                printDispatched = true
                val baseAdapter = view.createPrintDocumentAdapter(jobName)
                val cleanupAdapter = object : PrintDocumentAdapter() {
                    override fun onStart() {
                        baseAdapter.onStart()
                    }

                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        baseAdapter.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        baseAdapter.onWrite(pages, destination, cancellationSignal, callback)
                    }

                    override fun onFinish() {
                        try {
                            baseAdapter.onFinish()
                        } finally {
                            mainHandler.removeCallbacks(cleanup)
                            mainHandler.post(cleanup)
                        }
                    }
                }
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, cleanupAdapter, attributes)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun escape(text: String): String = TextUtils.htmlEncode(text)
}
