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
import com.example.roomkspdemo.data.entities.Respuesta
import com.itextpdf.text.Rectangle

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

        // Add this method to the PdfGenerator class in PdfGenerator.kt

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
            try {
                // Crear un archivo temporal para el PDF
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "Inspeccion_${inspeccion.caex.modelo}_${inspeccion.caex.numeroIdentificador}_$timeStamp.pdf"

                // Usamos el directorio de archivos de la aplicación para evitar problemas de permisos
                val pdfFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

                // Crear documento PDF
                val document = Document()
                PdfWriter.getInstance(document, FileOutputStream(pdfFile))
                document.open()

                // Añadir título
                val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
                val title = Paragraph("Informe de Inspección No Conformes", titleFont)
                title.alignment = Element.ALIGN_CENTER
                title.spacingAfter = 15f
                document.add(title)

                // Añadir información del CAEX e inspección
                addInspectionInfo(document, inspeccion)

                // Si no hay respuestas, mostrar mensaje
                if (respuestas.isEmpty()) {
                    val emptyMessage = Paragraph("No hay respuestas no conformes registradas.")
                    emptyMessage.alignment = Element.ALIGN_CENTER
                    document.add(emptyMessage)
                } else {
                    // Filtrar respuestas según el parámetro
                    val respuestasAMostrar = if (mostrarSoloNoConformes) {
                        respuestas.filter { it.respuesta.estado == Respuesta.ESTADO_NO_CONFORME }
                    } else {
                        respuestas
                    }

                    if (respuestasAMostrar.isEmpty()) {
                        val emptyMessage = Paragraph("No hay respuestas no conformes registradas.")
                        emptyMessage.alignment = Element.ALIGN_CENTER
                        document.add(emptyMessage)
                    } else {
                        // Añadir tabla de respuestas no conformes
                        document.add(Paragraph("\n"))
                        val subTitle = Paragraph("Detalle de Hallazgos No Conformes", titleFont)
                        subTitle.alignment = Element.ALIGN_LEFT
                        subTitle.spacingAfter = 10f
                        document.add(subTitle)

                        addNoConformeTable(document, respuestasAMostrar)
                    }
                }

                // Cerrar el documento
                document.close()

                // Compartir el PDF generado
                sharePdf(context, pdfFile)

                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error al generar PDF de detalle: ${e.message}", e)
                return false
            }
        }

        /**
         * Añade la información general de la inspección al documento.
         */
        private fun addInspectionInfo(document: Document, inspeccion: InspeccionConCAEX) {
            val infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12f)
            val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)

            // Crear tabla para información
            val infoTable = PdfPTable(2)
            infoTable.widthPercentage = 100f
            infoTable.setWidths(floatArrayOf(1f, 2f))

            // Añadir filas de información
            addInfoRow(infoTable, "CAEX:", "${inspeccion.caex.modelo} #${inspeccion.caex.numeroIdentificador}", boldFont, infoFont)

            val tipoInspeccion = if (inspeccion.inspeccion.tipo == Inspeccion.TIPO_RECEPCION)
                "Recepción (Control Inicio de Intervención)"
            else
                "Entrega (Control Término de Intervención)"
            addInfoRow(infoTable, "Tipo de Inspección:", tipoInspeccion, boldFont, infoFont)

            addInfoRow(infoTable, "Inspector:", inspeccion.inspeccion.nombreInspector, boldFont, infoFont)
            addInfoRow(infoTable, "Supervisor:", inspeccion.inspeccion.nombreSupervisor, boldFont, infoFont)

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaCreacion = dateFormat.format(Date(inspeccion.inspeccion.fechaCreacion))
            addInfoRow(infoTable, "Fecha:", fechaCreacion, boldFont, infoFont)

            // Añadir tabla al documento
            document.add(infoTable)
        }

        /**
         * Añade una fila a la tabla de información.
         */
        private fun addInfoRow(table: PdfPTable, label: String, value: String, labelFont: Font, valueFont: Font) {
            val labelCell = PdfPCell(Phrase(label, labelFont))
            labelCell.border = Rectangle.NO_BORDER
            labelCell.paddingBottom = 5f
            table.addCell(labelCell)

            val valueCell = PdfPCell(Phrase(value, valueFont))
            valueCell.border = Rectangle.NO_BORDER
            valueCell.paddingBottom = 5f
            table.addCell(valueCell)
        }

        /**
         * Añade la tabla de respuestas no conformes al documento.
         */
        private fun addNoConformeTable(document: Document, respuestas: List<RespuestaConDetalles>) {
            val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)
            val contentFont = FontFactory.getFont(FontFactory.HELVETICA, 9f)
            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f)

            // Agrupar respuestas por categoría para mejor organización
            val respuestasPorCategoria = respuestas.groupBy { it.pregunta.categoriaId }

            // Para cada categoría
            respuestasPorCategoria.forEach { (categoriaId, respuestasCategoria) ->
                // Añadir título de categoría
                val categoriaNombre = getCategoriaName(categoriaId)
                val categoriaTitle = Paragraph("Categoría: $categoriaNombre", titleFont)
                categoriaTitle.spacingBefore = 15f
                categoriaTitle.spacingAfter = 5f
                document.add(categoriaTitle)

                // Crear tabla para esta categoría
                val table = PdfPTable(3)
                table.widthPercentage = 100f
                table.setWidths(floatArrayOf(2.5f, 1f, 2f))

                // Añadir encabezados
                addTableCell(table, "Hallazgo No Conforme", headerFont, BaseColor.LIGHT_GRAY)
                addTableCell(table, "Tipo Acción", headerFont, BaseColor.LIGHT_GRAY)
                addTableCell(table, "ID Aviso/OT y Comentarios", headerFont, BaseColor.LIGHT_GRAY)

                // Añadir filas con los datos
                respuestasCategoria.forEach { respuesta ->
                    // Columna de pregunta/hallazgo
                    addTableCell(table, respuesta.pregunta.texto, contentFont)

                    // Columna de tipo de acción
                    val tipoAccion = when (respuesta.respuesta.tipoAccion) {
                        Respuesta.ACCION_INMEDIATO -> "Aviso (Inmediato)"
                        Respuesta.ACCION_PROGRAMADO -> "OT (Programado)"
                        else -> ""
                    }
                    addTableCell(table, tipoAccion, contentFont)

                    // Columna de ID y comentarios
                    val idYComentarios = "ID: ${respuesta.respuesta.idAvisoOrdenTrabajo ?: ""}\n" +
                            "Comentarios: ${respuesta.respuesta.comentarios}"
                    addTableCell(table, idYComentarios, contentFont)
                }

                document.add(table)

                // Añadir fotos si existen
                respuestasCategoria.forEach { respuesta ->
                    if (respuesta.fotos.isNotEmpty()) {
                        document.add(Paragraph("\n"))
                        val fotosTitle = Paragraph("Fotos para: ${respuesta.pregunta.texto.take(50)}...", contentFont)
                        fotosTitle.spacingBefore = 5f
                        fotosTitle.spacingAfter = 5f
                        document.add(fotosTitle)

                        // TODO: Implementar añadir fotos al PDF
                        document.add(Paragraph("Hay ${respuesta.fotos.size} fotos disponibles para este hallazgo.", contentFont))
                    }
                }
            }
        }

        /**
         * Añade una celda a la tabla.
         */
        private fun addTableCell(table: PdfPTable, text: String, font: Font, backgroundColor: BaseColor? = null) {
            val cell = PdfPCell(Phrase(text, font))
            cell.paddingTop = 5f
            cell.paddingBottom = 5f
            cell.paddingLeft = 5f
            cell.paddingRight = 5f

            if (backgroundColor != null) {
                cell.backgroundColor = backgroundColor
                cell.horizontalAlignment = Element.ALIGN_CENTER
            }

            table.addCell(cell)
        }

        /**
         * Obtiene el nombre de una categoría por su ID.
         * TODO: Esta es una función temporal que debería reemplazarse por
         * una consulta real a la base de datos.
         */
        private fun getCategoriaName(categoriaId: Long): String {
            return when (categoriaId) {
                1L -> "Condiciones Generales"
                2L -> "Cabina Operador"
                3L -> "Sistema de Dirección"
                4L -> "Sistema de frenos"
                5L -> "Motor Diesel"
                6L -> "Suspensiones delanteras"
                7L -> "Suspensiones traseras"
                8L -> "Sistema estructural"
                9L -> "Sistema eléctrico"
                else -> "Categoría $categoriaId"
            }
        }
    }
}