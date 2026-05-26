# 📋 Todo API - Monitoreo con Prometheus y Grafana

Sistema de monitoreo completo para una API REST de tareas (To-do list), construida con **Spring Boot** e instrumentada con **Prometheus** y **Grafana**, ejecutándose en **Docker**.

---

## 👤 Información del Estudiante

- **Nombre:** Sariaht Eyleen Xiomara Ariza Vargas
- **Repositorio:** (https://github.com/Sariaht/vis-data-monitoreo)
- **Video:** [URL del video demostrativo]

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────┐
│                   Docker Network                    │
│                                                     │
│  ┌──────────────┐    scraping    ┌───────────────┐  │
│  │  Spring Boot │ ◄────────────  │  Prometheus   │  │
│  │  Todo API    │                │  :9090        │  │
│  │  :3000       │                └──────┬────────┘  │
│  │  /actuator/  │                       │ datasource│
│  │  prometheus  │                ┌──────▼────────┐  │
│  └──────────────┘                │   Grafana     │  │
│                                  │   :3001       │  │
└─────────────────────────────────────────────────────┘
```

---

## 🚀 Inicio Rápido

### Requisitos
- Docker Desktop instalado y corriendo
- Puertos disponibles: `3000`, `9090`, `3001`

### Ejecutar el proyecto

```powershell
# 1. Clonar el repositorio
git clone [URL_REPOSITORIO]
cd todo-monitoring

# 2. Levantar todos los servicios
docker-compose up -d

# 3. Verificar que los servicios estén corriendo
docker-compose ps
```

> ⚠️ **Nota:** La primera ejecución tarda ~3-5 minutos porque Docker debe descargar las imágenes base y compilar la API con Maven.

### Acceder a los servicios

| Servicio    | URL                          | Credenciales       |
|-------------|------------------------------|--------------------|
| Todo API    | http://localhost:3000        | -                  |
| Prometheus  | http://localhost:9090        | -                  |
| Grafana     | http://localhost:3001        | admin / admin      |

---

## 📡 Endpoints de la API

| Método   | Endpoint                            | Descripción                         |
|----------|-------------------------------------|-------------------------------------|
| `GET`    | `/`                                 | Información general de la API       |
| `GET`    | `/api/todos`                        | Listar todas las tareas             |
| `GET`    | `/api/todos/{id}`                   | Obtener una tarea por ID            |
| `POST`   | `/api/todos`                        | Crear una nueva tarea               |
| `PUT`    | `/api/todos/{id}`                   | Actualizar una tarea                |
| `PATCH`  | `/api/todos/{id}/complete`          | Marcar tarea como completada        |
| `DELETE` | `/api/todos/{id}`                   | Eliminar una tarea                  |
| `GET`    | `/api/todos/filter/status`          | Filtrar por estado (completed)      |
| `GET`    | `/api/todos/filter/priority`        | Filtrar por prioridad (LOW/MEDIUM/HIGH) |
| `GET`    | `/api/stats`                        | Estadísticas de tareas              |
| `GET`    | `/api/slow`                         | Endpoint que simula procesamiento lento (2-3s) |
| `GET`    | `/actuator/prometheus`              | Métricas en formato Prometheus      |

### Ejemplos de uso

```powershell
# Listar tareas
Invoke-WebRequest http://localhost:3000/api/todos | ConvertFrom-Json

# Crear tarea
$body = @{ title="Nueva tarea"; description="Descripción"; priority="HIGH" } | ConvertTo-Json
Invoke-WebRequest -Method POST http://localhost:3000/api/todos -Body $body -ContentType "application/json"

# Completar tarea
Invoke-WebRequest -Method PATCH http://localhost:3000/api/todos/1/complete

# Ver métricas raw
Invoke-WebRequest http://localhost:3000/actuator/prometheus | Select-Object -ExpandProperty Content
```

---

## 📊 Métricas Implementadas

### Métricas Custom (negocio)

| Métrica                            | Tipo    | Descripción                              |
|------------------------------------|---------|------------------------------------------|
| `todo_created_total`               | Counter | Total de tareas creadas                  |
| `todo_completed_total`             | Counter | Total de tareas completadas              |
| `todo_deleted_total`               | Counter | Total de tareas eliminadas               |
| `todo_not_found_total`             | Counter | Total de errores 404                     |
| `todo_active_count`                | Gauge   | Tareas activas en tiempo real            |
| `todo_completed_count`             | Gauge   | Tareas completadas en tiempo real        |
| `todo_created_by_priority_total`   | Counter | Tareas creadas segmentadas por prioridad |

### Métricas HTTP (Spring Boot Actuator)

| Métrica                                      | Descripción                     |
|----------------------------------------------|---------------------------------|
| `http_request_duration_seconds_count`        | Conteo de requests por endpoint |
| `http_request_duration_seconds_sum`          | Suma de duraciones              |
| `http_server_requests_seconds_bucket`        | Histograma de latencias         |

---

## 📈 Queries PromQL Útiles

```promql
# Requests por segundo por endpoint
sum by (endpoint) (rate(http_request_duration_seconds_count[1m]))

# Latencia promedio
rate(http_request_duration_seconds_sum[1m])
/ rate(http_request_duration_seconds_count[1m])

# Total de requests acumulados
sum(increase(http_request_duration_seconds_count[5m]))

# Tasa de errores 404
rate(todo_not_found_total[5m])

# Tareas activas vs completadas
todo_active_count
todo_completed_count
```

---

## 🎨 Dashboard de Grafana

El dashboard se carga automáticamente al iniciar. Incluye **6 paneles**:

1. **📈 Throughput** - Requests por segundo por endpoint (timeseries)
2. **⏱️ Latencia** - Tiempo de respuesta promedio por endpoint (timeseries)
3. **📋 Tareas Activas** - Gauge en tiempo real (stat)
4. **✅ Tareas Completadas** - Gauge en tiempo real (stat)
5. **📊 Operaciones** - Creadas, completadas y eliminadas en últimos 5 min
6. **❌ Tasa de Errores** - Errores 404 en el tiempo

Para acceder: http://localhost:3001 → Dashboards → Todo API → Todo API - Dashboard de Monitoreo

---

## 🚨 Alertas Configuradas

Definidas en `prometheus/alerts.yml`:

| Alerta              | Severidad | Condición                                      |
|---------------------|-----------|------------------------------------------------|
| `ApiDown`           | critical  | API no responde por más de 1 minuto            |
| `HighNotFoundRate`  | warning   | Más de 0.5 errores 404/seg en 5 minutos        |
| `HighLatency`       | warning   | Latencia promedio en /api/slow > 3 segundos    |
| `HighRequestRate`   | info      | Más de 50 requests/seg por 2 minutos           |

Ver alertas activas en: http://localhost:9090/alerts

---

## 🔄 Script de Tráfico Sintético

### Windows (PowerShell)
```powershell
# Ejecutar por 2 minutos con delay de 1 segundo
.\scripts\generate-traffic.ps1

# Personalizar duración y delay
.\scripts\generate-traffic.ps1 -Duration 300 -Delay 0.5
```

### Linux / Mac (Bash)
```bash
chmod +x scripts/generate-traffic.sh
./scripts/generate-traffic.sh 120 1   # 120 segundos, delay 1s
```

El script genera automáticamente:
- Requests GET a múltiples endpoints
- Creación de tareas con títulos y prioridades aleatorias
- Completación y eliminación de tareas
- Requests a `/api/slow` para generar latencia
- Requests a IDs inexistentes para generar errores 404

---

## 🛑 Detener el proyecto

```powershell
# Detener servicios (conserva datos)
docker-compose stop

# Detener y eliminar contenedores
docker-compose down

# Eliminar TODO incluyendo volúmenes (datos de Prometheus y Grafana)
docker-compose down -v
```

---

## 🗂️ Estructura del Proyecto

```
todo-monitoring/
├── docker-compose.yml              # Orquestación de servicios
├── README.md                       # Este archivo
├── api/
│   ├── Dockerfile                  # Build multi-stage (Maven + JRE)
│   ├── pom.xml                     # Dependencias Spring Boot
│   └── src/main/
│       ├── java/com/monitoring/todo/
│       │   ├── TodoApiApplication.java
│       │   ├── controller/TodoController.java   # 11 endpoints
│       │   ├── model/Todo.java
│       │   ├── service/TodoService.java         # Métricas custom
│       │   └── config/MetricsConfig.java
│       └── resources/
│           └── application.properties
├── prometheus/
│   ├── prometheus.yml              # Scraping config (10s interval)
│   └── alerts.yml                  # 4 reglas de alerta
├── grafana/
│   └── provisioning/
│       ├── datasources/prometheus.yml
│       └── dashboards/
│           ├── dashboard.yml
│           └── todo-dashboard.json  # 6 paneles
└── scripts/
    ├── generate-traffic.ps1         # Windows PowerShell
    └── generate-traffic.sh          # Linux / Mac bash
```

---

## 🔧 Solución de Problemas

**La API tarda en iniciar:**
> Normal en la primera ejecución. Maven descarga dependencias (~2-3 min). Verifica con `docker-compose logs api`.

**Grafana no muestra datos:**
> Espera al menos 30 segundos después de que Prometheus esté corriendo. Verifica en http://localhost:9090/targets que el estado sea `UP`.

**Puerto ya en uso:**
> Cambia los puertos en `docker-compose.yml`. Ej: `"3002:3000"` para la API.

**Ver logs de un servicio:**
```powershell
docker-compose logs api         # Logs de la API
docker-compose logs prometheus  # Logs de Prometheus
docker-compose logs grafana     # Logs de Grafana
```
