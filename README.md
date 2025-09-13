# 📱 Analizador de Calidad del Sueño Pasivo

Una aplicación Android que analiza la calidad del sueño utilizando el micrófono y sensores de luz del dispositivo para proporcionar reportes detallados y recomendaciones personalizadas.

## 🌟 Características Principales

### 🎯 Monitoreo Pasivo
- **Análisis de audio**: Detecta ronquidos, ruidos ambientales y calcula niveles de decibeles
- **Sensor de luminosidad**: Monitorea interrupciones por luz durante la noche
- **Funcionamiento en segundo plano**: Continúa el análisis mientras el usuario duerme

### 📊 Análisis Inteligente
- **Algoritmo de calidad**: Calcula una puntuación de 0-100 basada en múltiples factores
- **Detección de ronquidos**: Identifica patrones específicos de ronquidos
- **Métricas detalladas**: Ruido promedio/máximo, interrupciones de luz, eventos significativos

### 📈 Reportes Completos
- **Historial de sesiones**: Visualización de todas las noches monitoreadas
- **Recomendaciones personalizadas**: Sugerencias basadas en los datos recopilados
- **Exportación**: Genera reportes en formato CSV para compartir

## 🛠️ Arquitectura Técnica

### 📱 Patrón MVVM
```
├── UI Layer (Fragments + ViewBinding)
├── ViewModel Layer (LiveData + StateFlow)
├── Repository Layer (Abstracción de datos)
└── Data Layer (Room Database + DataStore)
```

### 🗄️ Base de Datos Room
- **SleepSession**: Sesiones completas de sueño
- **NoiseData**: Datos detallados de ruido por timestamp
- **LightData**: Datos de luminosidad por timestamp

### 🔧 Tecnologías Utilizadas
- **Kotlin**: Lenguaje principal
- **ViewBinding**: Vinculación segura de vistas
- **Room Database**: Persistencia local
- **Foreground Service**: Monitoreo en segundo plano
- **Material Design 3**: Interfaz moderna
- **Coroutines**: Programación asíncrona
- **Navigation Component**: Navegación entre fragmentos

## 🚀 Instalación y Configuración

### Prerrequisitos
- Android Studio Arctic Fox o superior
- Android SDK 24+ (Android 7.0)
- Dispositivo con micrófono y sensor de luz

### Pasos de Instalación
1. **Clonar el repositorio**
   ```bash
   git clone [url-del-repositorio]
   cd sleeping
   ```

2. **Abrir en Android Studio**
   - File → Open → Seleccionar la carpeta del proyecto

3. **Sincronizar dependencias**
   - El proyecto usará Gradle Version Catalogs automáticamente

4. **Ejecutar la aplicación**
   - Conectar dispositivo o usar emulador
   - Run → Run 'app'

## 📖 Guía de Uso

### 🏠 Pantalla Principal (Monitoreo)
1. **Verificar permisos**: Asegurar que se tienen todos los permisos necesarios
2. **Iniciar monitoreo**: Tocar "Iniciar Monitoreo"
3. **Colocar dispositivo**: Dejar en mesita de noche con pantalla hacia abajo
4. **Dormir**: El análisis se ejecuta automáticamente
5. **Detener**: Al despertar, tocar "Detener Monitoreo"

### 📊 Pantalla de Historial
- **Ver sesiones**: Lista de todas las noches monitoreadas
- **Filtros**: Última semana, último mes, o todas
- **Detalles**: Tocar una sesión para ver resumen

### 📈 Pantalla de Reportes
- **Reporte detallado**: Análisis completo de la última sesión
- **Recomendaciones**: Sugerencias personalizadas
- **Exportar**: Generar archivo CSV para compartir

## 🔒 Privacidad y Seguridad

### 🛡️ Protección de Datos
- **Procesamiento local**: Todo el análisis se realiza en el dispositivo
- **Sin envío de datos**: No se transmite información a servidores externos
- **Almacenamiento seguro**: Datos encriptados localmente

### 🎤 Uso del Micrófono
- **Solo análisis**: Se procesan niveles de volumen, no se graban conversaciones
- **Intervalos controlados**: Muestreo cada 30 segundos para optimizar batería
- **Transparencia**: Usuario siempre informado cuando está activo

## ⚡ Optimización de Batería

### 🔋 Estrategias Implementadas
- **Muestreo inteligente**: Intervalos adaptativos según nivel de batería
- **Foreground Service optimizado**: Minimiza uso de recursos
- **Configuración dinámica**: Ajusta precisión vs. consumo automáticamente

### 📋 Recomendaciones de Uso
- Conectar cargador durante monitoreo nocturno
- Activar modo avión con WiFi para reducir consumo
- Cerrar otras aplicaciones antes de dormir

## 🧪 Testing

### 🔬 Pruebas Unitarias
```bash
./gradlew test
```

### 📱 Pruebas Instrumentadas
```bash
./gradlew connectedAndroidTest
```

### 🎯 Cobertura de Pruebas
- Lógica de cálculo de calidad del sueño
- Validación de rangos de datos
- Configuración de optimización de batería

## 🤝 Contribución

### 📝 Guidelines
1. Fork del repositorio
2. Crear branch feature/nombre-feature
3. Implementar cambios con tests
4. Crear Pull Request con descripción detallada

### 🏗️ Estructura de Commits
```
tipo(alcance): descripción breve

Descripción más detallada si es necesario

- Cambio específico 1
- Cambio específico 2
```

## 🔄 Roadmap Futuro

### 🎯 Próximas Características
- [ ] **Integración con wearables**: Soporte para smartwatches
- [ ] **Machine Learning**: Mejores algoritmos de detección
- [ ] **Sincronización en la nube**: Backup opcional de datos
- [ ] **Análisis de tendencias**: Gráficos de evolución temporal
- [ ] **Alertas inteligentes**: Notificaciones sobre patrones anómalos

### 🔧 Mejoras Técnicas
- [ ] **Migración a Compose**: Modernizar interfaz de usuario
- [ ] **Modularización**: Separar en módulos feature-based
- [ ] **CI/CD**: Pipeline automatizado de testing y deployment
- [ ] **Accesibilidad**: Mejoras para usuarios con discapacidades

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👥 Equipo de Desarrollo

- **Desarrollador Principal**: [Tu Nombre]
- **Arquitectura**: Patrón MVVM con Repository Pattern
- **Diseño**: Material Design 3 Guidelines

## 📞 Soporte

Para reportar bugs o solicitar características:
1. Crear issue en GitHub
2. Incluir información del dispositivo
3. Pasos para reproducir el problema
4. Logs relevantes si están disponibles

---

*Aplicación desarrollada con 💤 para mejorar la calidad del sueño de todos*
