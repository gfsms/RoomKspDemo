package com.example.roomkspdemo.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.roomkspdemo.data.relations.InspeccionConCAEX
import com.example.roomkspdemo.data.entities.Inspeccion
import com.example.roomkspdemo.data.relations.RespuestaConDetalles
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.FontFactory
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clase utilitaria para generar archivos PDF de las inspecciones.
 */
class PdfGenerator {

    companion object {
        private const val TAG = "PdfGenerator"

        /**
         * Genera un PDF con el historial de inspecciones y lo comparte.
         *
         * @param context Contexto de la aplicación
         * @param inspecciones Lista de inspecciones para incluir en el PDF
         * @return Verdadero si el PDF se generó correctamente, falso en caso contrario
         */
        fun generateHistorialPdf(context: Context, inspecciones: List<InspeccionConCAEX>): Boolean {
            try {
                // Crear un archivo temporal para el PDF
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "Historial_Inspecciones_$timeStamp.pdf"

                // Usamos el directorio de archivos de la aplicación para evitar problemas de permisos
                val pdfFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

                // Crear documento PDF
                val document = Document()
                PdfWriter.getInstance(document, FileOutputStream(pdfFile))
                document.open()

                // Añadir título
                val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
                val title = Paragraph("Historial de Inspecciones CAEX", titleFont)
                title.alignment = Element.ALIGN_CENTER
                title.spacingAfter = 20f
                document.add(title)

                // Añadir fecha de generación
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val dateString = "Generado: ${dateFormat.format(Date())}"
                val datePhrase = Paragraph(dateString)
                datePhrase.alignment = Element.ALIGN_RIGHT
                datePhrase.spacingAfter = 20f
                document.add(datePhrase)

                // Si no hay inspecciones, mostrar mensaje
                if (inspecciones.isEmpty()) {
                    val emptyMessage = Paragraph("No hay inspecciones registradas actualmente.")
                    emptyMessage.alignment = Element.ALIGN_CENTER
                    document.add(emptyMessage)
                } else {
                    // Crear tabla para las inspecciones
                    val table = PdfPTable(5) // 5 columnas: ID CAEX, Modelo, Tipo, Estado, Fecha
                    table.widthPercentage = 100f
                    table.setWidths(floatArrayOf(1f, 1f, 1f, 1f, 1.5f)) // Proporción de ancho de columnas

                    // Añadir encabezados de tabla
                    val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)
                    addTableHeader(table, headerFont)

                    // Añadir datos de inspecciones
                    val contentFont = FontFactory.getFont(FontFactory.HELVETICA, 10f)
                    addTableData(table, inspecciones, contentFont, dateFormat)

                    document.add(table)

                    // Añadir resumen
                    document.add(Paragraph("\n"))
                    addResumen(document, inspecciones)
                }

                // Cerrar el documento
                document.close()

                // Compartir el PDF generado
                sharePdf(context, pdfFile)

                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error al generar PDF de historial: ${e.message}", e)
                return false
            }
        }

        /**
         * Añade el encabezado a la tabla del PDF.
         */
        private fun addTableHeader(table: PdfPTable, font: Font) {
            val headers = arrayOf("ID CAEX", "Modelo", "Tipo", "Estado", "Fecha")

            for (header in headers) {
                val cell = PdfPCell(Phrase(header, font))
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.verticalAlignment = Element.ALIGN_MIDDLE
                cell.paddingTop = 8f
                cell.paddingBottom = 8f
                cell.backgroundColor = BaseColor.LIGHT_GRAY
                table.addCell(cell)
            }
        }

        /**
         * Añade las filas de datos a la tabla del PDF.
         */
        private fun addTableData(
            table: PdfPTable,
            inspecciones: List<InspeccionConCAEX>,
            font: Font,
            dateFormat: SimpleDateFormat
        ) {
            for (inspeccion in inspecciones) {
                // Celda para el ID del CAEX
                val idCell = PdfPCell(Phrase(inspeccion.caex.numeroIdentificador.toString(), font))
                idCell.paddingTop = 5f
                idCell.paddingBottom = 5f
                idCell.paddingLeft = 5f
                idCell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(idCell)

                // Celda para el modelo del CAEX
                val modeloCell = PdfPCell(Phrase(inspeccion.caex.modelo, font))
                modeloCell.paddingTop = 5f
                modeloCell.paddingBottom = 5f
                modeloCell.paddingLeft = 5f
                modeloCell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(modeloCell)

                // Celda para el tipo de inspección
                val tipoTexto = if (inspeccion.inspeccion.tipo == Inspeccion.TIPO_RECEPCION) "Recepción" else "Entrega"
                val tipoCell = PdfPCell(Phrase(tipoTexto, font))
                tipoCell.paddingTop = 5f
                tipoCell.paddingBottom = 5f
                tipoCell.paddingLeft = 5f
                tipoCell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(tipoCell)

                // Celda para el estado de la inspección
                val estadoTexto = if (inspeccion.inspeccion.estado == Inspeccion.ESTADO_ABIERTA) "Abierta" else "Cerrada"
                val estadoCell = PdfPCell(Phrase(estadoTexto, font))
                estadoCell.paddingTop = 5f
                estadoCell.paddingBottom = 5f
                estadoCell.paddingLeft = 5f
                estadoCell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(estadoCell)

                // Celda para la fecha
                val fechaString = dateFormat.format(Date(inspeccion.inspeccion.fechaCreacion))
                val fechaCell = PdfPCell(Phrase(fechaString, font))
                fechaCell.paddingTop = 5f
                fechaCell.paddingBottom = 5f
                fechaCell.paddingLeft = 5f
                fechaCell.horizontalAlignment = Element.ALIGN_CENTER
                table.addCell(fechaCell)
            }
        }

        /**
         * Añade un resumen de estadísticas al documento.
         */
        private fun addResumen(document: Document, inspecciones: List<InspeccionConCAEX>) {
            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f)
            val normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12f)

            // Título del resumen
            val resumenTitle = Paragraph("Resumen de Inspecciones", titleFont)
            resumenTitle.spacingBefore = 15f
            resumenTitle.spacingAfter = 10f
            document.add(resumenTitle)

            // Contar inspecciones por tipo
            val totalInspecciones = inspecciones.size
            val inspeccionesRecepcion = inspecciones.count { it.inspeccion.tipo == Inspeccion.TIPO_RECEPCION }
            val inspeccionesEntrega = inspecciones.count { it.inspeccion.tipo == Inspeccion.TIPO_ENTREGA }

            // Contar inspecciones por estado
            val inspeccionesAbiertas = inspecciones.count { it.inspeccion.estado == Inspeccion.ESTADO_ABIERTA }
            val inspeccionesCerradas = inspecciones.count { it.inspeccion.estado == Inspeccion.ESTADO_CERRADA }

            // Contar inspecciones por modelo
            val inspecciones797F = inspecciones.count { it.caex.modelo == "797F" }
            val inspecciones798AC = inspecciones.count { it.caex.modelo == "798AC" }

            // Agregar estadísticas al documento
            document.add(Paragraph("Total de inspecciones: $totalInspecciones", normalFont))
            document.add(Paragraph("Inspecciones de recepción: $inspeccionesRecepcion", normalFont))
            document.add(Paragraph("Inspecciones de entrega: $inspeccionesEntrega", normalFont))
            document.add(Paragraph("Inspecciones abiertas: $inspeccionesAbiertas", normalFont))
            document.add(Paragraph("Inspecciones cerradas: $inspeccionesCerradas", normalFont))
            document.add(Paragraph("Inspecciones de CAEX 797F: $inspecciones797F", normalFont))
            document.add(Paragraph("Inspecciones de CAEX 798AC: $inspecciones798AC", normalFont))
        }

        /**
         * Comparte el archivo PDF generado.
         */
        private fun sharePdf(context: Context, pdfFile: File) {
            try {
                // Crear URI para el archivo usando FileProvider para compatibilidad con Android 7.0+
                val fileUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )

                // Crear intent para compartir
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                // Iniciar actividad para compartir
                context.startActivity(Intent.createChooser(intent, "Compartir PDF de Historial"))
            } catch (e: Exception) {
                Log.e(TAG, "Error al compartir PDF: ${e.message}", e)
            }
        }

        /**
         * Genera un PDF detallado de una inspección específica.
         *
         * @param context Contexto de la aplicación
         * @param inspeccion Inspección con CAEX
         * @param respuestas Lista de respuestas con detalles
         * @param mostrarSoloNoConformes Si true, muestra solo las respuestas no conformes
         * @return Verdadero si el PDF se generó correctamente, falso en caso contrario
         */
        fun generateDetalleInspeccionPdf(
            context: Context,
            inspeccion: InspeccionConCAEX,
            respuestas: List<RespuestaConDetalles>,
            mostrarSoloNoConformes: Boolean = false
        ): Boolean {
            // Esta es la función que implementaremos más adelante para
            // generar reportes detallados de inspecciones individuales
            // Por ahora retornamos false, indicando que no está implementada
            return false
        }
    }
}