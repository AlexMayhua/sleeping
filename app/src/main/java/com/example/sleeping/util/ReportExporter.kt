package com.example.sleeping.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.sleeping.data.entity.SleepSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidad para exportar reportes de sueño en formato CSV
 */
object ReportExporter {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    /**
     * Exporta sesiones de sueño a un archivo CSV
     */
    suspend fun exportToCSV(
        context: Context,
        sessions: List<SleepSession>,
        fileName: String = "sleep_report_${System.currentTimeMillis()}.csv"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val file = createTempFile(context, fileName)
            writeCSVContent(file, sessions)
            createFileUri(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Comparte el archivo CSV usando un Intent
     */
    fun shareCSVFile(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir reporte de sueño"))
    }
    
    private fun createTempFile(context: Context, fileName: String): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, fileName)
    }
    
    private fun writeCSVContent(file: File, sessions: List<SleepSession>) {
        FileWriter(file).use { writer ->
            // Encabezados
            writer.append("Fecha Inicio,Fecha Fin,Duración (horas),Ruido Promedio (dB),Ruido Máximo (dB),Eventos de Ruido,Luminosidad Promedio (lux),Interrupciones de Luz,Puntuación de Calidad,Estado,Notas\n")
            
            // Datos
            sessions.forEach { session ->
                val startTime = dateFormatter.format(session.startTime)
                val endTime = session.endTime?.let { dateFormatter.format(it) } ?: "En progreso"
                val duration = String.format("%.2f", session.estimatedSleepHours)
                val avgNoise = String.format("%.1f", session.averageNoiseLevel)
                val maxNoise = String.format("%.1f", session.maxNoiseLevel)
                val noiseEvents = session.noiseEvents.toString()
                val avgLight = String.format("%.1f", session.averageLightLevel)
                val lightInterruptions = session.lightInterruptions.toString()
                val qualityScore = String.format("%.1f", session.sleepQualityScore)
                val status = if (session.isCompleted) "Completada" else "En progreso"
                val notes = session.notes?.replace(",", ";") ?: ""
                
                writer.append("\"$startTime\",\"$endTime\",\"$duration\",\"$avgNoise\",\"$maxNoise\",\"$noiseEvents\",\"$avgLight\",\"$lightInterruptions\",\"$qualityScore\",\"$status\",\"$notes\"\n")
            }
        }
    }
    
    private fun createFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    private val fileNameFormatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())

    /**
     * Exporta una sesión de sueño individual a CSV
     */
    suspend fun exportSessionToCsv(
        context: Context,
        session: SleepSession,
        includeRawData: Boolean = false
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val fileName = "reporte_sueno_${fileNameFormatter.format(session.startTime)}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileWriter(file).use { writer ->
                // Encabezado
                writer.append("REPORTE DE ANÁLISIS DEL SUEÑO\n")
                writer.append("Generado el: ${dateFormatter.format(Date())}\n\n")
                
                // Información básica de la sesión
                writer.append("INFORMACIÓN BÁSICA\n")
                writer.append("Fecha de inicio,${dateFormatter.format(session.startTime)}\n")
                session.endTime?.let { 
                    writer.append("Fecha de fin,${dateFormatter.format(it)}\n")
                }
                writer.append("Duración estimada,${session.estimatedSleepHours} horas\n")
                writer.append("Estado,${if (session.isCompleted) "Completado" else "En progreso"}\n\n")
                
                // Métricas de calidad
                writer.append("MÉTRICAS DE CALIDAD\n")
                writer.append("Puntuación de calidad,${session.sleepQualityScore}/100\n")
                writer.append("Descripción,${getQualityDescription(session.sleepQualityScore)}\n\n")
                
                // Análisis de ruido
                writer.append("ANÁLISIS DE RUIDO\n")
                writer.append("Nivel promedio,${session.averageNoiseLevel} dB\n")
                writer.append("Nivel máximo,${session.maxNoiseLevel} dB\n")
                writer.append("Eventos de ruido,${session.noiseEvents}\n\n")
                
                // Análisis de luz
                writer.append("ANÁLISIS DE LUMINOSIDAD\n")
                writer.append("Nivel promedio,${session.averageLightLevel} lux\n")
                writer.append("Interrupciones por luz,${session.lightInterruptions}\n\n")
                
                // Notas adicionales
                session.notes?.let {
                    writer.append("NOTAS\n")
                    writer.append("$it\n\n")
                }
                
                // Recomendaciones
                writer.append("RECOMENDACIONES\n")
                val recommendations = generateRecommendations(session)
                recommendations.forEach { recommendation ->
                    writer.append("- $recommendation\n")
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exporta múltiples sesiones a CSV
     */
    suspend fun exportMultipleSessionsToCsv(
        context: Context,
        sessions: List<SleepSession>
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val fileName = "historial_sueno_${fileNameFormatter.format(Date())}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileWriter(file).use { writer ->
                // Encabezado
                writer.append("HISTORIAL DE SUEÑO\n")
                writer.append("Generado el: ${dateFormatter.format(Date())}\n")
                writer.append("Total de sesiones: ${sessions.size}\n\n")
                
                // Encabezados de columnas
                writer.append("Fecha Inicio,Fecha Fin,Duración (horas),Calidad,Ruido Promedio (dB),Ruido Máximo (dB),Eventos Ruido,Luz Promedio (lux),Interrupciones Luz,Estado\n")
                
                // Datos de cada sesión
                sessions.forEach { session ->
                    writer.append("${dateFormatter.format(session.startTime)},")
                    writer.append("${session.endTime?.let { dateFormatter.format(it) } ?: "En progreso"},")
                    writer.append("${session.estimatedSleepHours},")
                    writer.append("${session.sleepQualityScore},")
                    writer.append("${session.averageNoiseLevel},")
                    writer.append("${session.maxNoiseLevel},")
                    writer.append("${session.noiseEvents},")
                    writer.append("${session.averageLightLevel},")
                    writer.append("${session.lightInterruptions},")
                    writer.append("${if (session.isCompleted) "Completado" else "En progreso"}\n")
                }
                
                // Estadísticas generales
                if (sessions.isNotEmpty()) {
                    val completedSessions = sessions.filter { it.isCompleted }
                    if (completedSessions.isNotEmpty()) {
                        writer.append("\nESTADÍSTICAS GENERALES\n")
                        writer.append("Promedio calidad,${completedSessions.map { it.sleepQualityScore }.average()}\n")
                        writer.append("Promedio duración,${completedSessions.map { it.estimatedSleepHours }.average()} horas\n")
                        writer.append("Promedio ruido,${completedSessions.map { it.averageNoiseLevel }.average()} dB\n")
                        writer.append("Total eventos ruido,${completedSessions.sumOf { it.noiseEvents }}\n")
                        writer.append("Total interrupciones luz,${completedSessions.sumOf { it.lightInterruptions }}\n")
                    }
                }
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Comparte un archivo exportado
     */
    fun shareReport(context: Context, fileUri: Uri, fileName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte de Análisis del Sueño")
            putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte de análisis del sueño generado por la aplicación.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Compartir reporte"))
    }

    private fun getQualityDescription(score: Float): String {
        return when {
            score >= 80f -> "Excelente"
            score >= 60f -> "Buena"
            score >= 40f -> "Regular"
            score >= 20f -> "Mala"
            else -> "Muy mala"
        }
    }

    private fun generateRecommendations(session: SleepSession): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (session.averageNoiseLevel > 35f) {
            recommendations.add("Considere usar tapones para los oídos o mejorar el aislamiento acústico")
        }
        
        if (session.noiseEvents > 10) {
            recommendations.add("Hay muchos eventos de ruido. Revise posibles fuentes de interferencia")
        }
        
        if (session.lightInterruptions > 2) {
            recommendations.add("Use cortinas blackout o antifaz para evitar interrupciones por luz")
        }
        
        if (session.estimatedSleepHours < 6f) {
            recommendations.add("Intente acostarse más temprano para obtener al menos 7-8 horas de sueño")
        } else if (session.estimatedSleepHours > 10f) {
            recommendations.add("El exceso de sueño también puede afectar la calidad. Mantenga un horario regular")
        }
        
        if (session.sleepQualityScore >= 80f) {
            recommendations.add("Excelente calidad de sueño. Mantenga estos hábitos")
        } else if (session.sleepQualityScore < 40f) {
            recommendations.add("Considere consultar con un especialista en trastornos del sueño")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Continúe monitoreando su sueño para obtener más insights")
        }
        
        return recommendations
    }
}
