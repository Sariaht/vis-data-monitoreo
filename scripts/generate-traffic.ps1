# generate-traffic.ps1
# Script de tráfico sintético para Windows (PowerShell)
# Genera requests automáticos a la Todo API

param(
    [int]$Duration = 120,       # Duración en segundos (default: 2 minutos)
    [int]$Delay = 1,            # Delay entre requests en segundos
    [string]$BaseUrl = "http://localhost:3000"
)

$ErrorActionPreference = "SilentlyContinue"
$startTime = Get-Date
$endTime = $startTime.AddSeconds($Duration)
$requestCount = 0

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "   Todo API - Script de Tráfico Sintético  " -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "URL Base   : $BaseUrl" -ForegroundColor Yellow
Write-Host "Duración   : $Duration segundos" -ForegroundColor Yellow
Write-Host "Inicio     : $startTime" -ForegroundColor Yellow
Write-Host "Fin esperado: $endTime" -ForegroundColor Yellow
Write-Host "--------------------------------------------" -ForegroundColor Gray
Write-Host "Presiona Ctrl+C para detener" -ForegroundColor Red
Write-Host ""

function Invoke-Request {
    param([string]$Method, [string]$Url, [object]$Body = $null)

    $params = @{
        Method = $Method
        Uri    = $Url
        ContentType = "application/json"
        TimeoutSec  = 10
    }
    if ($Body) {
        $params.Body = ($Body | ConvertTo-Json)
    }

    try {
        $response = Invoke-WebRequest @params -UseBasicParsing
        return $response.StatusCode
    } catch {
        return $_.Exception.Response.StatusCode.value__
    }
}

$todoIds = @()

while ((Get-Date) -lt $endTime) {
    $requestCount++
    $elapsed = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 0)

    # Ciclo de operaciones variadas
    $op = $requestCount % 10

    switch ($op) {
        0 {
            # GET /
            $status = Invoke-Request "GET" "$BaseUrl/"
            Write-Host "[$elapsed s] GET /                    -> $status" -ForegroundColor Green
        }
        1 {
            # GET /api/todos
            $status = Invoke-Request "GET" "$BaseUrl/api/todos"
            Write-Host "[$elapsed s] GET /api/todos            -> $status" -ForegroundColor Green
        }
        2 {
            # POST /api/todos
            $titles = @("Revisar logs", "Actualizar dependencias", "Escribir tests", "Refactorizar código", "Hacer code review")
            $priorities = @("LOW", "MEDIUM", "HIGH")
            $body = @{
                title       = $titles[(Get-Random -Maximum $titles.Count)]
                description = "Tarea generada automáticamente #$requestCount"
                priority    = $priorities[(Get-Random -Maximum $priorities.Count)]
            }
            $status = Invoke-Request "POST" "$BaseUrl/api/todos" $body
            Write-Host "[$elapsed s] POST /api/todos           -> $status" -ForegroundColor Blue

            # Save ID for later operations
            if ($status -eq 201) {
                try {
                    $resp = Invoke-WebRequest -Method POST -Uri "$BaseUrl/api/todos" `
                        -ContentType "application/json" `
                        -Body ($body | ConvertTo-Json) -UseBasicParsing
                    $json = $resp.Content | ConvertFrom-Json
                    if ($json.id) { $todoIds += $json.id }
                } catch {}
            }
        }
        3 {
            # GET /api/stats
            $status = Invoke-Request "GET" "$BaseUrl/api/stats"
            Write-Host "[$elapsed s] GET /api/stats            -> $status" -ForegroundColor Green
        }
        4 {
            # GET /api/todos/filter/status
            $completed = @("true", "false")[(Get-Random -Maximum 2)]
            $status = Invoke-Request "GET" "$BaseUrl/api/todos/filter/status?completed=$completed"
            Write-Host "[$elapsed s] GET /filter/status?=$completed -> $status" -ForegroundColor Green
        }
        5 {
            # GET /api/todos/filter/priority
            $priorities = @("LOW", "MEDIUM", "HIGH")
            $p = $priorities[(Get-Random -Maximum $priorities.Count)]
            $status = Invoke-Request "GET" "$BaseUrl/api/todos/filter/priority?value=$p"
            Write-Host "[$elapsed s] GET /filter/priority?=$p  -> $status" -ForegroundColor Green
        }
        6 {
            # PATCH complete a random todo
            if ($todoIds.Count -gt 0) {
                $id = $todoIds[(Get-Random -Maximum $todoIds.Count)]
                $status = Invoke-Request "PATCH" "$BaseUrl/api/todos/$id/complete"
                Write-Host "[$elapsed s] PATCH /api/todos/$id/complete -> $status" -ForegroundColor Magenta
            } else {
                $status = Invoke-Request "GET" "$BaseUrl/api/todos/1"
                Write-Host "[$elapsed s] GET /api/todos/1         -> $status" -ForegroundColor Green
            }
        }
        7 {
            # GET /api/slow (endpoint lento)
            Write-Host "[$elapsed s] GET /api/slow             -> (esperando...)" -ForegroundColor Yellow
            $status = Invoke-Request "GET" "$BaseUrl/api/slow"
            Write-Host "[$elapsed s] GET /api/slow             -> $status" -ForegroundColor Yellow
        }
        8 {
            # GET not found (genera error 404)
            $fakeId = Get-Random -Minimum 9000 -Maximum 9999
            $status = Invoke-Request "GET" "$BaseUrl/api/todos/$fakeId"
            Write-Host "[$elapsed s] GET /api/todos/$fakeId    -> $status (404 esperado)" -ForegroundColor Red
        }
        9 {
            # DELETE a random todo
            if ($todoIds.Count -gt 0) {
                $id = $todoIds[(Get-Random -Maximum $todoIds.Count)]
                $todoIds = $todoIds | Where-Object { $_ -ne $id }
                $status = Invoke-Request "DELETE" "$BaseUrl/api/todos/$id"
                Write-Host "[$elapsed s] DELETE /api/todos/$id     -> $status" -ForegroundColor DarkRed
            } else {
                $status = Invoke-Request "GET" "$BaseUrl/"
                Write-Host "[$elapsed s] GET /                    -> $status" -ForegroundColor Green
            }
        }
    }

    Start-Sleep -Seconds $Delay
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "   Tráfico completado" -ForegroundColor Cyan
Write-Host "   Total de requests: $requestCount" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
