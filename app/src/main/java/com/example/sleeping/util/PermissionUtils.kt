package com.example.sleeping.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utilidad para manejar permisos requeridos por la aplicación
 */
object PermissionUtils {
    
    const val REQUEST_CODE_PERMISSIONS = 1001
    
    /**
     * Permisos requeridos para el funcionamiento de la app
     */
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.WAKE_LOCK
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }
    }.toTypedArray()
    
    /**
     * Verifica si todos los permisos requeridos están concedidos
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Obtiene la lista de permisos que aún no han sido concedidos
     */
    fun getDeniedPermissions(context: Context): Array<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
    
    /**
     * Solicita todos los permisos requeridos
     */
    fun requestAllPermissions(activity: Activity) {
        val deniedPermissions = getDeniedPermissions(activity)
        if (deniedPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                deniedPermissions,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    /**
     * Verifica si se debe mostrar una explicación para un permiso específico
     */
    fun shouldShowRationalForAnyPermission(activity: Activity): Boolean {
        return REQUIRED_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    /**
     * Obtiene el mensaje de explicación para el usuario sobre por qué necesitamos los permisos
     */
    fun getPermissionExplanationMessage(): String {
        return """
            Para analizar la calidad de su sueño, necesitamos acceso a:
            
            🎤 Micrófono: Para detectar ruidos y ronquidos durante la noche
            🔔 Notificaciones: Para mantenerle informado del estado del monitoreo
            🔋 Ejecutar en segundo plano: Para continuar el análisis mientras duerme
            
            Sus datos de audio se procesan localmente y nunca se envían a servidores externos.
        """.trimIndent()
    }
    
    /**
     * Maneja el resultado de la solicitud de permisos
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onAllGranted: () -> Unit,
        onSomeGranted: (granted: List<String>, denied: List<String>) -> Unit,
        onAllDenied: () -> Unit
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val granted = mutableListOf<String>()
            val denied = mutableListOf<String>()
            
            permissions.forEachIndexed { index, permission ->
                if (grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED) {
                    granted.add(permission)
                } else {
                    denied.add(permission)
                }
            }
            
            when {
                denied.isEmpty() -> onAllGranted()
                granted.isEmpty() -> onAllDenied()
                else -> onSomeGranted(granted, denied)
            }
        }
    }
    
    /**
     * Obtiene los nombres legibles de los permisos para mostrar al usuario
     */
    fun getPermissionDisplayNames(permissions: List<String>): List<String> {
        return permissions.map { permission ->
            when (permission) {
                Manifest.permission.RECORD_AUDIO -> "Acceso al micrófono"
                Manifest.permission.POST_NOTIFICATIONS -> "Mostrar notificaciones"
                Manifest.permission.FOREGROUND_SERVICE -> "Ejecutar en segundo plano"
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE -> "Servicio de micrófono"
                Manifest.permission.WAKE_LOCK -> "Mantener dispositivo activo"
                else -> permission.substringAfterLast(".")
            }
        }
    }
}
