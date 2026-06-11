param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet("portal", "daemon", "datalake", "data-api")]
    [string]$Component,

    [ValidateSet("jdbc", "memory")]
    [string]$Persistence = "jdbc"
)

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$runtimeRoot = Join-Path $repoRoot "runtime\local"

function Import-EnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Environment file not found: $Path. Create it from deploy/env.example first."
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $parts = $trimmed.Split("=", 2)
        if ($parts.Count -ne 2) {
            continue
        }

        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "Env:$name" -Value $value
    }
}

$componentEnvFiles = @{
    "portal" = "portal.env"
    "daemon" = "daemon.env"
    "datalake" = "datalake.env"
    "data-api" = "data-api.env"
}

Import-EnvFile -Path (Join-Path $repoRoot "deploy\env\common.env")
Import-EnvFile -Path (Join-Path $repoRoot "deploy\env\$($componentEnvFiles[$Component])")

$databasePath = (Join-Path $runtimeRoot "database\h2train").Replace("\", "/")
$datalakePath = (Join-Path $runtimeRoot "datalake").Replace("\", "/")
$logsPath = (Join-Path $runtimeRoot "logs\$Component").Replace("\", "/")

New-Item -ItemType Directory -Path (Split-Path $databasePath), $datalakePath, $logsPath -Force | Out-Null

$env:H2TRAIN_STORAGE_ROOT = $runtimeRoot.Replace("\", "/")
$env:H2TRAIN_DB_URL = "jdbc:h2:file:$databasePath;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false"
$env:DATALAKE_ROOT_PATH = $datalakePath
$env:LOGGING_FILE_PATH = $logsPath
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:KAFKA_TOPIC = "h2train.events.v1"

switch ($Component) {
    "portal" {
        $env:APP_PERSISTENCE_TYPE = $Persistence
        $env:APP_BUS_TYPE = "kafka"
        $env:KAFKA_CLIENT_ID = "h2train-portal"
    }
    "daemon" {
        $env:APP_PERSISTENCE_TYPE = $Persistence
        $env:APP_BUS_TYPE = "kafka"
        $env:KAFKA_CLIENT_ID = "h2train-daemon"
    }
    "datalake" {
        $env:APP_BUS_CONSUMER_TYPE = "kafka"
    }
    "data-api" {
        $env:APP_DATA_APP_BUS_TYPE = "kafka"
        $env:SERVER_PORT = "8081"
    }
}

Write-Host "Loaded local environment for $Component"
Write-Host "Kafka:   $env:KAFKA_BOOTSTRAP_SERVERS"
Write-Host "Database: $databasePath"
Write-Host "Datalake: $datalakePath"
