param(
    [string]$BaseUrl = "http://localhost:8082",
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()

function Wait-DemoApi {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $metadata = Invoke-RestMethod -Uri "$BaseUrl/api/v1/dataset/metadata"
            if ($metadata.subjectCount -eq 12 `
                    -and $metadata.lastRecord -eq "2026-06-01T23:59:00Z") {
                return $metadata
            }
        } catch {
            Start-Sleep -Seconds 2
            continue
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "La API de demostración no terminó de reconstruir el datamart en $TimeoutSeconds segundos."
}

function Assert-Subjects {
    param(
        [string]$Name,
        [string]$Query,
        [string[]]$Expected
    )

    $response = Invoke-RestMethod -Uri "$BaseUrl$Query"
    [string[]]$actual = @($response.subjects.userId)
    $actualText = $actual -join ","
    $expectedText = $Expected -join ","
    if ($actualText -ne $expectedText) {
        throw "$Name devolvió [$actualText], pero se esperaba [$expectedText]."
    }
    Write-Host "$Name`: $actualText"
}

$metadata = Wait-DemoApi
Write-Host "Datamart preparado: $($metadata.subjectCount) sujetos, $($metadata.metricCount) métricas y $($metadata.pointCount) puntos."

Assert-Subjects `
    -Name "Calorías medias superiores a 2500" `
    -Query "/api/v1/datasets/query?metric=daily_calories&operator=gt&value=2500&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json" `
    -Expected @(
        "0f3a2c71-8d4e-4b96-a125-6e7f9c2d1b40",
        "3d91a6c8-5e27-4b40-9f13-c7a2e8d56401",
        "4e28b7d5-91c3-46fa-a804-2d6f9b13e750",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24",
        "8d25a7e3-64b9-41f8-a730-3c9e5d12b684",
        "9e81c4f6-27d3-4a59-b142-f6a0387d25c1"
    )

Assert-Subjects `
    -Name "Pasos medios superiores a 10000" `
    -Query "/api/v1/datasets/query?metric=daily_steps&operator=gt&value=10000&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json" `
    -Expected @(
        "18c7e5a4-2b91-4d63-8f20-a6e4c9b17d52",
        "4e28b7d5-91c3-46fa-a804-2d6f9b13e750",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24",
        "7c46f1d9-30a8-4e72-95b3-d8f2146a0c57",
        "9e81c4f6-27d3-4a59-b142-f6a0387d25c1"
    )

Assert-Subjects `
    -Name "Sueño medio inferior a seis horas" `
    -Query "/api/v1/datasets/query?metric=daily_sleep_duration&operator=lt&value=21600&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json" `
    -Expected @(
        "2b64d8f1-73a5-49ce-b812-5f90a3d6e247",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24",
        "7c46f1d9-30a8-4e72-95b3-d8f2146a0c57"
    )

Assert-Subjects `
    -Name "Más de cuatro actividades semanales" `
    -Query "/api/v1/datasets/query?metric=weekly_activity_count&operator=gt&value=4&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json" `
    -Expected @(
        "3d91a6c8-5e27-4b40-9f13-c7a2e8d56401",
        "4e28b7d5-91c3-46fa-a804-2d6f9b13e750",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24",
        "9e81c4f6-27d3-4a59-b142-f6a0387d25c1"
    )

Assert-Subjects `
    -Name "Más de 30 km semanales corriendo" `
    -Query "/api/v1/datasets/query?metric=weekly_workout_distance_by_sport&operator=gt&value=30000&aggregation=avg&activityType=run&from=2026-01-01&to=2026-06-01&format=json" `
    -Expected @(
        "4e28b7d5-91c3-46fa-a804-2d6f9b13e750",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24"
    )

$exportRequest = @{
    metrics = @("daily_steps", "daily_calories", "daily_sleep_duration")
    filters = @(
        @{
            metric = "daily_calories"
            operator = "gt"
            value = 2500
            aggregation = "avg"
        }
    )
    from = "2026-01-01"
    to = "2026-06-01"
    format = "csv"
} | ConvertTo-Json -Depth 6

$export = Invoke-WebRequest `
    -Uri "$BaseUrl/api/v1/datasets/export" `
    -Method Post `
    -ContentType "application/json" `
    -Body $exportRequest

$header = ($export.Content -split "`n")[0].Trim()
if ($export.StatusCode -ne 200 `
        -or $export.Headers."Content-Type" -notlike "text/csv*" `
        -or $header -ne "userId,metric,value,periodStart,periodEnd,unit,period,provider,activityType,zone") {
    throw "La exportación CSV no tiene el contrato esperado."
}

$heartRate = Invoke-RestMethod `
    -Uri "$BaseUrl/api/v1/datasets/heart-rate-zones?from=2026-01-01&to=2026-06-01&format=json"
if ($heartRate.days.Count -eq 0) {
    throw "El dataset de zonas cardiacas está vacío."
}

Write-Host "Exportación CSV validada con $(($export.Content -split "`n").Count) líneas."
Write-Host "Dataset cardiaco validado con $($heartRate.days.Count) filas diarias."
Write-Host "Demostración validada correctamente."
