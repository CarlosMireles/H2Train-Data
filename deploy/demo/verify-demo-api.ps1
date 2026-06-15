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
    -Expected @("demo-u001", "demo-u004", "demo-u005", "demo-u006", "demo-u009", "demo-u010")

Assert-Subjects `
    -Name "Pasos medios superiores a 10000" `
    -Query "/api/v1/datasets/query?metric=daily_steps&operator=gt&value=10000&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json" `
    -Expected @("demo-u002", "demo-u005", "demo-u006", "demo-u008", "demo-u010")

Assert-Subjects `
    -Name "Sueño medio inferior a seis horas" `
    -Query "/api/v1/datasets/query?metric=daily_sleep_duration&operator=lt&value=21600&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json" `
    -Expected @("demo-u003", "demo-u006", "demo-u008")

Assert-Subjects `
    -Name "Más de cuatro actividades semanales" `
    -Query "/api/v1/datasets/query?metric=weekly_activity_count&operator=gt&value=4&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json" `
    -Expected @("demo-u004", "demo-u005", "demo-u006", "demo-u010")

Assert-Subjects `
    -Name "Más de 30 km semanales corriendo" `
    -Query "/api/v1/datasets/query?metric=weekly_workout_distance_by_sport&operator=gt&value=30000&aggregation=avg&activityType=run&from=2026-01-01&to=2026-06-01&format=json" `
    -Expected @("demo-u005", "demo-u006")

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
